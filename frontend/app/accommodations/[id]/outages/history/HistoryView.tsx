"use client";

import { useCallback, useEffect, useState } from "react";
import AccommodationHeader from "@/components/AccommodationHeader";
import OutageTabs from "@/components/OutageTabs";
import { api, base } from "@/lib/api";
import type { Accommodation } from "@/lib/types";

type Schedule = { roomName: string; startDate: string; endDate: string };
type Incident = Schedule & { reason: string };
type HistoryAction =
  | {
      action: "ACCEPTED" | "CANCEL_REQUESTED";
      details: {
        reservationId: number;
        previousSchedule: Schedule[];
        confirmedSchedule: Schedule[];
        incidents: Incident[];
      };
    }
  | {
      action: "MOVE_COMPLETED";
      details: { reservationId: number; date: string; fromRoomName: string; toRoomName: string };
    }
  | { action: "FINISHED"; details: { unresolvedReservationIds: number[]; incidents: Incident[] } };

type HistoryItem = HistoryAction & {
  id: number;
  sessionId: string;
  referenceDate: string;
  recordedAt: string;
};

const recordedAt = new Intl.DateTimeFormat("ko-KR", {
  dateStyle: "medium",
  timeStyle: "short",
  timeZone: "Asia/Seoul",
});

function ScheduleList({ schedule }: { schedule: Schedule[] }) {
  return (
    <ul className="segments">
      {schedule.map((segment) => (
        <li key={`${segment.roomName}-${segment.startDate}`}>
          {segment.roomName} <span className="muted">{segment.startDate} ~ {segment.endDate}</span>
        </li>
      ))}
    </ul>
  );
}

function HistoryActionRow({ item }: { item: HistoryItem }) {
  if (item.action === "FINISHED") return null;

  const { details } = item;
  return (
    <div className="history-row">
      <strong>예약 #{details.reservationId}</strong>{" · "}
      {item.action === "MOVE_COMPLETED" ? "이동 완료" : item.action === "ACCEPTED" ? "수락" : "취소 처리"}
      <p className="muted">{recordedAt.format(new Date(item.recordedAt))}</p>
      {item.action === "MOVE_COMPLETED" ? (
        <p>{item.details.date} · {item.details.fromRoomName} → {item.details.toRoomName}</p>
      ) : (
        <div className="schedule-comparison">
          <div><strong>기존 일정</strong><ScheduleList schedule={item.details.previousSchedule} /></div>
          <div>
            <strong>처리 결과</strong>
            {item.action === "ACCEPTED" ? (
              <ScheduleList schedule={item.details.confirmedSchedule} />
            ) : <p>예약 취소</p>}
          </div>
        </div>
      )}
    </div>
  );
}

export default function HistoryView({ id }: { id: string }) {
  const [name, setName] = useState("");
  const [items, setItems] = useState<HistoryItem[] | null>(null);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setError("");
    try {
      const [accommodations, entries] = await Promise.all([
        api<Accommodation[]>("/accommodations"),
        api<HistoryItem[]>(`${base(id)}/recovery-sessions/history`),
      ]);
      setName(accommodations.find((accommodation) => String(accommodation.id) === id)?.name || `숙소 ${id}`);
      setItems(entries);
    } catch (failure) {
      setError((failure as Error).message);
    }
  }, [id]);

  useEffect(() => { void load(); }, [load]);

  const sessions = new Map<string, HistoryItem[]>();
  for (const item of items ?? []) {
    const entries = sessions.get(item.sessionId);
    if (entries) entries.push(item);
    else sessions.set(item.sessionId, [item]);
  }

  return (
    <div className="shell">
      <AccommodationHeader id={id} name={name} tab="outages" />
      <OutageTabs id={id} history />
      {error ? (
        <p className="notice error" role="alert">{error}{" "}<button className="text-button" onClick={load}>다시 불러오기</button></p>
      ) : !items ? (
        <p className="message">복구 이력을 불러오는 중입니다.</p>
      ) : !sessions.size ? (
        <p className="message">기록된 복구 이력이 없습니다.</p>
      ) : (
        Array.from(sessions, ([sessionId, entries]) => {
          const latest = entries[0];
          const finished = entries.find((entry) => entry.action === "FINISHED");
          const incidentDetails = entries.map((entry) => entry.details).find((details) => "incidents" in details);
          const incidents = incidentDetails && "incidents" in incidentDetails ? incidentDetails.incidents : [];
          const accepted = entries.filter((entry) => entry.action === "ACCEPTED").length;
          const cancelled = entries.filter((entry) => entry.action === "CANCEL_REQUESTED").length;
          return (
            <details className="card" key={sessionId}>
              <summary>
                <strong>재배정 기준일 {latest.referenceDate}</strong>{" · "}
                수락 {accepted}건 · 취소 {cancelled}건
                {finished && ` · 대체 객실 없음 ${finished.details.unresolvedReservationIds.length}건`}
                <p className="muted">최근 처리 {recordedAt.format(new Date(latest.recordedAt))}</p>
              </summary>
              {incidents.map((incident, index) => (
                <p key={index}>
                  {incident.roomName} · {incident.startDate} ~ {incident.endDate} · {incident.reason}
                </p>
              ))}
              {entries.slice().reverse().map((item) => <HistoryActionRow key={item.id} item={item} />)}
            </details>
          );
        })
      )}
    </div>
  );
}
