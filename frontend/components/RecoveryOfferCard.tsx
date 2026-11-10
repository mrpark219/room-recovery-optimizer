"use client";
import { roomName } from "@/lib/api";
import type { Offer, Room, Move } from "@/lib/types";
export default function RecoveryOfferCard({
  o,
  rooms,
  urgent,
  busy,
  moveHistory,
  onDecision,
  onCancel,
  onMove,
}: {
  o: Offer;
  rooms: Room[];
  urgent: number[];
  busy: boolean;
  moveHistory: (Move & { reservationId: number })[];
  onDecision: (offer: Offer, decision: "ACCEPTED") => void;
  onCancel: (offer: Offer) => void;
  onMove: (offer: Offer, move: Move) => void;
}) {
  function nextMove(o: Offer) {
    return o.plannedMoves.find(
      (m) =>
        !moveHistory.some(
          (h) =>
            h.reservationId === o.reservationId &&
            h.date === m.date &&
            h.fromRoomId === m.fromRoomId &&
            h.toRoomId === m.toRoomId,
        ),
    );
  }
  const next = o.decision === "ACCEPTED" ? nextMove(o) : undefined;
  return (
    <article className="offer" key={o.id}>
      <div className="offer-top">
        <h3>예약 #{o.reservationId}</h3>
        <div>
          {urgent.includes(o.reservationId) && (
            <span className="badge urgent">긴급</span>
          )}{" "}
          <span className="badge">
            {o.decision === "ACCEPTED"
              ? "수락됨"
              : o.decision === "CANCEL_REQUESTED"
                ? "취소 요청"
                : "응답 대기"}
          </span>
        </div>
      </div>
      {o.initialRoomId !== null && (
        <p className="muted">
          현재 투숙 객실 {roomName(rooms, o.initialRoomId)}
        </p>
      )}
      <ul className="segments">
        {o.segments.map((s, i) => (
          <li key={i}>
            <strong>{roomName(rooms, s.roomId)}</strong>{" "}
            <span className="muted">
              · {s.startDate} ~ {s.endDate}
            </span>
          </li>
        ))}
      </ul>
      {o.plannedMoves.length > 0 && (
        <p className="footnote">
          계획된 이동:{" "}
          {o.plannedMoves
            .map(
              (m) =>
                `${m.date} ${roomName(rooms, m.fromRoomId)} → ${roomName(rooms, m.toRoomId)}`,
            )
            .join(" / ")}
        </p>
      )}
      {!o.decision && (
        <div className="offer-actions">
          <button disabled={busy} onClick={() => onDecision(o, "ACCEPTED")}>
            수락
          </button>
          <button
            disabled={busy}
            className="secondary"
            onClick={() => onCancel(o)}
          >
            취소 요청
          </button>
        </div>
      )}
      {next && (
        <div className="offer-actions">
          <button disabled={busy} onClick={() => onMove(o, next)}>
            다음 이동 완료 · {next.date}
          </button>
        </div>
      )}
    </article>
  );
}
