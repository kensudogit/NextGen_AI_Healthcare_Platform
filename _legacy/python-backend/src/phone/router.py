"""AI 電話受付 API"""

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from src.ai.phone_agent import end_call, process_utterance, start_call
from src.db import models as m
from src.db.database import get_db

router = APIRouter(prefix="/api/phone", tags=["AI Phone"])


class StartBody(BaseModel):
    caller_number: str | None = None


class UtteranceBody(BaseModel):
    call_sid: str
    text: str = Field(min_length=1, max_length=2000)


class EndBody(BaseModel):
    call_sid: str


@router.post("/start")
def phone_start(body: StartBody, db: Session = Depends(get_db)):
    return start_call(body.caller_number, db)


@router.post("/utterance")
def phone_utterance(body: UtteranceBody, db: Session = Depends(get_db)):
    try:
        return process_utterance(body.call_sid, body.text, db)
    except ValueError as e:
        raise HTTPException(404, str(e)) from e


@router.post("/end")
def phone_end(body: EndBody, db: Session = Depends(get_db)):
    try:
        return end_call(body.call_sid, db)
    except ValueError as e:
        raise HTTPException(404, str(e)) from e


@router.get("/calls")
def list_calls(limit: int = 30, db: Session = Depends(get_db)):
    rows = db.query(m.PhoneCall).order_by(m.PhoneCall.started_at.desc()).limit(limit).all()
    return {
        "calls": [
            {
                "id": c.id,
                "call_sid": c.call_sid,
                "caller_number": c.caller_number,
                "intent": c.intent,
                "status": c.status,
                "summary": c.summary,
                "transferred": c.transferred,
                "appointment_id": c.appointment_id,
                "started_at": c.started_at.isoformat(),
                "ended_at": c.ended_at.isoformat() if c.ended_at else None,
            }
            for c in rows
        ]
    }


@router.get("/calls/{call_id}/turns")
def call_turns(call_id: int, db: Session = Depends(get_db)):
    turns = (
        db.query(m.PhoneTurn)
        .filter(m.PhoneTurn.call_id == call_id)
        .order_by(m.PhoneTurn.seq)
        .all()
    )
    return {
        "turns": [
            {"seq": t.seq, "role": t.role, "text": t.text, "at": t.created_at.isoformat()}
            for t in turns
        ]
    }
