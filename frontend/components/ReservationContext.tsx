"use client";
import { useEffect, useRef, useState } from "react";
import ReservationTable from "@/components/ReservationTable";
import { roomName, today } from "@/lib/api";
import type { Outage, Reservation, Room, Segment } from "@/lib/types";

function addDays(date: string, days: number) {
  const value = new Date(`${date}T00:00:00Z`);
  value.setUTCDate(value.getUTCDate() + days);
  return value.toISOString().slice(0, 10);
}

export default function ReservationContext({
  rooms, outages, referenceDate, draft, selected, initialEnd, reservations, error, onReload,
}: {
  rooms: Room[];
  outages: Outage[];
  referenceDate?: string;
  draft?: Segment | null;
  selected?: Segment | null;
  initialEnd?: string;
  reservations: Reservation[] | null;
  error: string;
  onReload: () => void;
}) {
  const initial = referenceDate || today();
  const [start, setStart] = useState(initial);
  const [end, setEnd] = useState(() =>
    initialEnd && initialEnd > initial ? initialEnd : addDays(initial, 30),
  );
  const [source, setSource] = useState("기본 기간");
  const hadDraft = useRef(false);
  const hadSelection = useRef(false);
  useEffect(() => {
    if (selected) {
      hadSelection.current = true;
      setStart(selected.startDate);
      setEnd(selected.endDate);
      setSource(`선택한 장애 · ${roomName(rooms, selected.roomId)}`);
    } else if (hadSelection.current) {
      hadSelection.current = false;
      if (!draft) setSource("직접 선택한 기간");
    }
  }, [selected, rooms, draft?.roomId, draft?.startDate, draft?.endDate]);
  useEffect(() => {
    if (draft && !selected) {
      hadDraft.current = true;
      setStart(draft.startDate);
      setEnd(draft.endDate);
      setSource(`입력 중인 장애 · ${roomName(rooms, draft.roomId)}`);
    } else if (!draft && hadDraft.current) {
      hadDraft.current = false;
      if (!selected) {
        setStart(initial);
        setEnd(initialEnd && initialEnd > initial ? initialEnd : addDays(initial, 30));
        setSource("기본 기간");
      }
    }
  }, [draft?.roomId, draft?.startDate, draft?.endDate, rooms, selected, initial, initialEnd]);

  const valid = !!start && !!end && start < end;
  const filtered = valid
    ? reservations?.filter((r) => r.startDate < end && start < r.endDate)
    : [];
  const disruptions: Segment[] = selected
    ? [selected]
    : [...outages, ...(draft ? [draft] : [])];
  const affected = filtered?.filter((reservation) =>
    reservation.status !== "CANCELLED" && reservation.assignments.some((assignment) =>
      disruptions.some((outage) => {
        const overlapStart = [start, assignment.startDate, outage.startDate].sort().at(-1)!;
        const overlapEnd = [end, assignment.endDate, outage.endDate].sort()[0];
        return assignment.roomId === outage.roomId && overlapStart < overlapEnd;
      }),
    ),
  ).map((reservation) => reservation.id) || [];

  return (
    <section id="reservation-context" className="card reservation-card compact-reservations">
      <div className="section-heading">
        <div>
          <h2>예약 조회 기간</h2>
          <p>이 기간에 숙박하는 예약을 보여줍니다. 객실 배정이 {selected ? "선택한 장애 기간" : "장애 기간"}과 겹치면 표시합니다.</p>
        </div>
      </div>
      <p><strong>조회 기준: {source}</strong> · {start || "시작일 선택"} ~ {end || "종료일 선택"}</p>
      <div className="context-controls">
        <label>조회 시작일<input type="date" value={start} onChange={(e) => { setStart(e.target.value); setSource("직접 선택한 기간"); }} /></label>
        <label>조회 종료일<input type="date" value={end} onChange={(e) => { setEnd(e.target.value); setSource("직접 선택한 기간"); }} /></label>
        <button type="button" className="secondary small" onClick={onReload}>예약 새로고침</button>
      </div>
      {!valid ? (
        <p className="field-error" role="alert">조회 종료일은 시작일보다 뒤여야 합니다.</p>
      ) : error ? (
        <p className="message error" role="alert">{error} <button className="text-button" onClick={onReload}>다시 불러오기</button></p>
      ) : reservations === null ? (
        <p className="message">예약을 불러오는 중입니다.</p>
      ) : filtered?.length ? (
        <>
          <p className="footnote">{filtered.length}건 · 장애 겹침 {affected.length}건</p>
          <ReservationTable reservations={filtered} rooms={rooms} affectedIds={affected} />
        </>
      ) : (
        <p className="message">이 기간과 겹치는 예약이 없습니다.</p>
      )}
    </section>
  );
}
