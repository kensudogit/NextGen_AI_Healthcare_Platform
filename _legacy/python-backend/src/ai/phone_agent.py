"""AI 電話受付エージェント"""

import json
import logging
import re
import uuid
from datetime import datetime, timedelta, timezone

from openai import OpenAI
from sqlalchemy.orm import Session

from src.config import settings
from src.db import models as m

logger = logging.getLogger(__name__)

EMERGENCY_KEYWORDS = ("胸が痛い", "意識", "呼吸", "救急", "119", "血が", "倒れた")
TRANSFER_KEYWORDS = ("苦情", "弁護士", "診断", "処方", "薬を", "人と話")

SYSTEM = f"""あなたは{settings.hospital_name}のAI電話受付です。
予約・問い合わせ・診療時間案内に対応します。
診断・処方・医療判断は行わず、必要なら人への転送を案内します。
JSONのみ返答:
{{
  "reply": "電話向け短い日本語応答（2-3文）",
  "intent": "appointment|inquiry|hours|emergency|transfer|other",
  "action": "continue|book_appointment|transfer|end",
  "appointment": {{"name": "", "phone": "", "department": "", "datetime_hint": ""}}
}}"""


def _rule_reply(user_text: str, state: dict) -> dict:
    t = user_text.lower()
    if any(k in user_text for k in EMERGENCY_KEYWORDS):
        return {
            "reply": "緊急の症状の可能性があります。ただちに119番または救急外来へ。オペレーターへおつなぎします。",
            "intent": "emergency",
            "action": "transfer",
        }
    if any(k in user_text for k in TRANSFER_KEYWORDS):
        return {
            "reply": "担当者におつなぎします。少々お待ちください。",
            "intent": "transfer",
            "action": "transfer",
        }
    if "予約" in user_text or "予約" in t:
        return {
            "reply": "予約ですね。お名前とご希望の診療科、日時をお知らせください。",
            "intent": "appointment",
            "action": "continue",
        }
    if "時間" in user_text or "診療" in user_text or "休診" in user_text:
        return {
            "reply": f"{settings.hospital_name}の診療時間は平日9:00-17:00、土曜9:00-12:00です。日曜・祝日は休診です。",
            "intent": "hours",
            "action": "continue",
        }
    return {
        "reply": f"{settings.hospital_name}です。予約・診療時間・アクセスなどお伺いします。",
        "intent": "inquiry",
        "action": "continue",
    }


def _ai_reply(user_text: str, history: list[dict]) -> dict:
    if not settings.openai_api_key:
        return _rule_reply(user_text, {})
    try:
        client = OpenAI(api_key=settings.openai_api_key, timeout=30.0)
        messages = [{"role": "system", "content": SYSTEM}]
        for h in history[-6:]:
            messages.append({"role": h["role"], "content": h["text"]})
        messages.append({"role": "user", "content": user_text})
        resp = client.chat.completions.create(
            model=settings.openai_model,
            messages=messages,
            response_format={"type": "json_object"},
            temperature=0.3,
        )
        return json.loads(resp.choices[0].message.content or "{}")
    except Exception as e:
        logger.warning("Phone AI failed: %s", e)
        r = _rule_reply(user_text, {})
        r["fallback"] = True
        return r


def _try_book_appointment(data: dict, db: Session) -> m.Appointment | None:
    ap = data.get("appointment") or {}
    name = ap.get("name") or data.get("patient_name")
    if not name:
        return None
    dept = ap.get("department") or "内科"
    when = datetime.now(timezone.utc) + timedelta(days=3)
    hint = ap.get("datetime_hint") or ""
    if "明日" in hint:
        when = datetime.now(timezone.utc) + timedelta(days=1)
    elif "来週" in hint:
        when = datetime.now(timezone.utc) + timedelta(days=7)
    apt = m.Appointment(
        patient_name=name.strip(),
        patient_phone=ap.get("phone"),
        department=dept,
        purpose="電話予約",
        scheduled_at=when.replace(hour=10, minute=0, second=0, microsecond=0),
        source="phone_ai",
        status="booked",
    )
    db.add(apt)
    db.commit()
    db.refresh(apt)
    return apt


def start_call(caller_number: str | None, db: Session) -> dict:
    sid = f"call-{uuid.uuid4().hex[:12]}"
    call = m.PhoneCall(call_sid=sid, caller_number=caller_number, status="active")
    db.add(call)
    db.flush()
    greeting = (
        f"{settings.hospital_name}です。AI受付がご案内します。"
        f"予約・診療時間・お問い合わせをお伺いします。"
    )
    db.add(m.PhoneTurn(call_id=call.id, seq=0, role="assistant", text=greeting))
    db.commit()
    return {"call_sid": sid, "call_id": call.id, "reply": greeting, "action": "continue"}


def process_utterance(call_sid: str, text: str, db: Session) -> dict:
    call = db.query(m.PhoneCall).filter(m.PhoneCall.call_sid == call_sid).first()
    if not call or call.status != "active":
        raise ValueError("Call not found or ended")

    turns = db.query(m.PhoneTurn).filter(m.PhoneTurn.call_id == call.id).order_by(m.PhoneTurn.seq).all()
    seq = len(turns)
    db.add(m.PhoneTurn(call_id=call.id, seq=seq, role="user", text=text))

    history = [{"role": t.role, "text": t.text} for t in turns if t.role in ("user", "assistant")]
    if any(k in text for k in EMERGENCY_KEYWORDS):
        data = _rule_reply(text, {})
    else:
        data = _ai_reply(text, history)

    reply = data.get("reply", "かしこまりました。")
    action = data.get("action", "continue")
    call.intent = data.get("intent")

    appointment_id = None
    if action == "book_appointment" or (
        data.get("intent") == "appointment" and re.search(r"[\u4e00-\u9fff]{2,}", text)
    ):
        apt = _try_book_appointment(data, db)
        if not apt and "予約" in text:
            name_match = re.search(r"([^\s、,]{2,8})\s*(?:です|と申します)", text)
            if name_match:
                apt = _try_book_appointment({"appointment": {"name": name_match.group(1)}}, db)
        if apt:
            appointment_id = apt.id
            call.appointment_id = apt.id
            reply = f"{apt.patient_name}様、{apt.department}の予約を{apt.scheduled_at.strftime('%m月%d日 %H:%M')}で承りました。"

    if action == "transfer":
        call.transferred = True
        call.status = "transferred"

    db.add(m.PhoneTurn(call_id=call.id, seq=seq + 1, role="assistant", text=reply))
    db.commit()
    return {
        "call_sid": call_sid,
        "reply": reply,
        "intent": call.intent,
        "action": action,
        "appointment_id": appointment_id,
        "transferred": call.transferred,
    }


def end_call(call_sid: str, db: Session) -> dict:
    call = db.query(m.PhoneCall).filter(m.PhoneCall.call_sid == call_sid).first()
    if not call:
        raise ValueError("Call not found")
    turns = db.query(m.PhoneTurn).filter(m.PhoneTurn.call_id == call.id).order_by(m.PhoneTurn.seq).all()
    transcript = "\n".join(f"{t.role}: {t.text}" for t in turns)
    summary = f"意図: {call.intent or '不明'} / ターン数: {len(turns)}"
    if settings.openai_api_key and len(turns) > 2:
        try:
            client = OpenAI(api_key=settings.openai_api_key, timeout=30.0)
            resp = client.chat.completions.create(
                model=settings.openai_model,
                messages=[
                    {"role": "system", "content": "通話要約を2-3文の日本語で。"},
                    {"role": "user", "content": transcript},
                ],
                temperature=0.2,
            )
            summary = resp.choices[0].message.content or summary
        except Exception:
            pass
    call.summary = summary
    call.status = "completed"
    call.ended_at = datetime.now(timezone.utc)
    db.commit()
    return {"call_sid": call_sid, "summary": summary, "status": "completed"}
