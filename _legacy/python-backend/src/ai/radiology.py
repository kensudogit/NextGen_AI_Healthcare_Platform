"""AI 読影レポート要約"""

import json
import logging

from openai import OpenAI
from sqlalchemy.orm import Session

from src.config import settings
from src.db import models as m

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """あなたは放射線科医向けアシスタントです。読影レポートを要約しJSONで返してください。
{
  "summary": "3-5文の要約（日本語、臨床医向け）",
  "key_findings": ["所見1", "所見2"],
  "urgency": "routine|urgent|critical",
  "follow_up": "フォローアップ推奨（あれば）"
}
診断は確定せず、レポート内容に基づく要約のみ。"""


def _rule_summary(report_text: str) -> dict:
    lines = [ln.strip() for ln in report_text.splitlines() if ln.strip()]
    findings = [ln for ln in lines if "所見" in ln or "結節" in ln or "影" in ln][:5]
    return {
        "summary": " ".join(lines[:3])[:400] if lines else report_text[:300],
        "key_findings": findings or lines[:3],
        "urgency": "urgent" if any(w in report_text for w in ("緊急", "critical", "即時")) else "routine",
        "follow_up": "レポート記載のフォロー指示を確認してください",
        "source": "rule_based",
    }


def summarize_radiology_report(report_text: str) -> dict:
    if not settings.openai_api_key:
        result = _rule_summary(report_text)
        return result

    try:
        client = OpenAI(api_key=settings.openai_api_key, timeout=60.0)
        resp = client.chat.completions.create(
            model=settings.openai_model,
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": report_text},
            ],
            response_format={"type": "json_object"},
            temperature=0.2,
        )
        data = json.loads(resp.choices[0].message.content or "{}")
        data["source"] = "openai"
        return data
    except Exception as e:
        logger.warning("OpenAI radiology summary failed: %s", e)
        result = _rule_summary(report_text)
        result["fallback_reason"] = str(e)[:120]
        return result


def summarize_and_save(report_id: int, db: Session) -> dict:
    report = db.get(m.RadiologyReport, report_id)
    if not report:
        raise ValueError("Report not found")
    result = summarize_radiology_report(report.report_text)
    report.ai_summary = result.get("summary")
    report.ai_findings = json.dumps(result.get("key_findings", []), ensure_ascii=False)
    report.urgency = result.get("urgency", "routine")
    db.commit()
    return {"report_id": report_id, **result}
