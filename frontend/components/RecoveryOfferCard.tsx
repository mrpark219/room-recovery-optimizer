"use client";
import { useState } from "react";
import ReservationSummary from "@/components/ReservationSummary";
import { money, roomName } from "@/lib/api";
import type { Offer, Room, Move, Reservation, Placement } from "@/lib/types";
export default function RecoveryOfferCard({
  o,
  rooms,
  reservation,
  urgent,
  busy,
  moveHistory,
  alternatives,
  onDecision,
  onCancel,
  onMove,
}: {
  o: Offer;
  rooms: Room[];
  reservation?: Reservation;
  urgent: number[];
  busy: boolean;
  moveHistory: (Move & { reservationId: number })[];
  alternatives: Placement[];
  onDecision: (offer: Offer, decision: "ACCEPTED", roomId?: number) => void;
  onCancel: (offer: Offer) => void;
  onMove: (offer: Offer, move: Move) => void;
}) {
  const [roomId, setRoomId] = useState<number>();
  const selected = alternatives.find((item) => item.segments[0].roomId === roomId);
  const differentRooms = alternatives.filter((item) =>
    o.segments.length !== 1 || o.segments[0].roomId !== item.segments[0].roomId,
  );

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
  const completedMoves = moveHistory.filter((move) => move.reservationId === o.reservationId);
  const lastCompletedMove = completedMoves.at(-1);
  return (
    <article className="offer" key={o.id}>
      <div className="offer-top">
        <h3>예약 #{o.reservationId}</h3>
        <div>
          {urgent.includes(o.reservationId) && (
            <span className="badge urgent">기준일에 이동 필요</span>
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
      <ReservationSummary reservation={reservation} rooms={rooms} />
      {lastCompletedMove ? (
        <p className="muted">
          마지막 이동 완료 객실 {roomName(rooms, lastCompletedMove.toRoomId)}
        </p>
      ) : o.initialRoomId !== null && (
        <p className="muted">
          재배정 기준일의 투숙 객실 {roomName(rooms, o.initialRoomId)}
        </p>
      )}
      <ul className="segments">
        {(selected?.segments ?? o.segments).map((s, i) => (
          <li key={i}>
            <strong>{roomName(rooms, s.roomId)}</strong>{" "}
            <span className="muted">
              · {s.startDate} ~ {s.endDate}
            </span>
          </li>
        ))}
      </ul>
      {!selected && o.plannedMoves.length > 0 && (
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
      {!o.decision && differentRooms.length > 0 && (
        <label>
          재배정 객실 직접 선택
          <select
            disabled={busy}
            value={selected ? roomId : ""}
            onChange={(event) => setRoomId(event.target.value ? Number(event.target.value) : undefined)}
          >
            <option value="">추천 일정 사용</option>
            {differentRooms.map(({ segments, additionalCost }) => (
              <option key={segments[0].roomId} value={segments[0].roomId}>
                {roomName(rooms, segments[0].roomId)} · 추가 비용 {money(additionalCost)}
              </option>
            ))}
          </select>
        </label>
      )}
      {!o.decision && (
        <div className="offer-actions">
          <button disabled={busy} onClick={() => onDecision(o, "ACCEPTED", selected?.segments[0].roomId)}>
            {selected ? "선택 객실로 수락 기록" : "수락"}
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
