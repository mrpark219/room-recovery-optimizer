"use client";
import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import RecoveryDetails from "@/components/RecoveryDetails";
import ConfirmDialog from "@/components/ConfirmDialog";
import { api, ApiError, base, sessionKey } from "@/lib/api";
import type { Room, Session, Offer, Move } from "@/lib/types";

export default function Recovery({
  id,
  sessionId,
}: {
  id: string;
  sessionId: string;
}) {
  const [session, setSession] = useState<Session | null>(null);
  const [rooms, setRooms] = useState<Room[]>([]);

  const [error, setError] = useState("");
  const [expired, setExpired] = useState(false);
  const [busy, setBusy] = useState(false);
  const [cancelTarget, setCancelTarget] = useState<Offer | null>(null);

  const path = `${base(id)}/recovery-sessions/${encodeURIComponent(sessionId)}`;

  const load = useCallback(async () => {
    setError("");
    try {
      const [loadedSession, loadedRooms] = await Promise.all([
        api<Session>(path),
        api<Room[]>(`${base(id)}/rooms`),
      ]);
      setSession(loadedSession);
      setRooms(loadedRooms);
      setExpired(false);
      localStorage.setItem(sessionKey(id), sessionId);
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        setExpired(true);
        setSession(null);
        if (localStorage.getItem(sessionKey(id)) === sessionId) {
          localStorage.removeItem(sessionKey(id));
        }
        setError(
          "세션을 찾을 수 없습니다. 만료되었거나 서버가 재시작되었을 수 있습니다. 장애 관리에서 새로 시작해 주세요.",
        );
      } else {
        setError((error as Error).message);
      }
    }
  }, [id, path, sessionId]);

  useEffect(() => {
    load();
  }, [load]);

  async function updateOffer(
    offer: Offer,
    action: "ACCEPTED" | "CANCEL_REQUESTED" | Move,
  ) {
    if (busy) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      const move = typeof action === "object";
      const result = await api<Session>(
        `${path}/offers/${offer.id}/${move ? "moves" : "decision"}`,
        {
          method: "POST",
          body: JSON.stringify(move ? action : { decision: action }),
        },
      );
      setSession(result);
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setError(
          `현재 상태가 변경되어 처리할 수 없습니다. 세션을 다시 불러와 확인해 주세요. ${error.message}`,
        );
      } else if (error instanceof ApiError && error.status === 404) {
        setExpired(false);
        setError(
          "세션 또는 제안을 찾을 수 없습니다. 세션을 다시 불러와 확인해 주세요.",
        );
      } else {
        setError((error as Error).message);
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="shell">
      {cancelTarget && (
        <ConfirmDialog
          title="예약 취소"
          message={`예약 #${cancelTarget.reservationId}을 취소 처리하시겠습니까? 예약 상태가 취소로 변경되고 다시 계산됩니다.`}
          confirmLabel="예약 취소"
          onClose={() => setCancelTarget(null)}
          onConfirm={() => {
            const target = cancelTarget;
            setCancelTarget(null);
            void updateOffer(target, "CANCEL_REQUESTED");
          }}
        />
      )}
      <Link href={`/accommodations/${id}/outages`} className="back">
        ‹ &nbsp;객실 장애 관리
      </Link>
      <h1>객실 재배정</h1>
      {error && (
        <div role="alert" className="notice error">
          {error}{" "}
          {!expired && (
            <button className="text-button" onClick={load} disabled={busy}>
              다시 불러오기
            </button>
          )}
        </div>
      )}
      {!session && !error && (
        <p className="message">세션을 불러오는 중입니다.</p>
      )}
      {session && (
        <RecoveryDetails
          session={session}
          rooms={rooms}
          busy={busy}
          onDecision={updateOffer}
          onCancel={setCancelTarget}
          onMove={updateOffer}
        />
      )}
    </div>
  );
}
