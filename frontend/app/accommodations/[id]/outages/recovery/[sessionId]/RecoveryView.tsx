"use client";
import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import useReservations from "@/components/useReservations";
import ReservationContext from "@/components/ReservationContext";
import RecoveryDetails from "@/components/RecoveryDetails";
import ConfirmDialog from "@/components/ConfirmDialog";
import { api, ApiError, base, sessionKey } from "@/lib/api";
import type { Room, Session, Offer, Move, Outage } from "@/lib/types";

export default function Recovery({
  id,
  sessionId,
}: {
  id: string;
  sessionId: string;
}) {
  const router = useRouter();
  const [finishing, setFinishing] = useState(false);
  const [session, setSession] = useState<Session | null>(null);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [reservationRevision, setReservationRevision] = useState(0);
  const booking = useReservations(id, reservationRevision);
  const [outages, setOutages] = useState<Outage[]>([]);
  const [outageError, setOutageError] = useState("");

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

  const loadOutages = useCallback(async () => {
    setOutageError("");
    try {
      setOutages(await api<Outage[]>(`${base(id)}/unavailabilities`));
    } catch (error) {
      setOutages([]);
      setOutageError((error as Error).message);
    }
  }, [id]);

  useEffect(() => {
    void load();
    void loadOutages();
  }, [load, loadOutages]);

  async function updateOffer(
    offer: Offer,
    action: "ACCEPTED" | "CANCEL_REQUESTED" | Move,
    roomId?: number,
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
          body: JSON.stringify(move ? action : { decision: action, roomId }),
        },
      );
      setSession(result);
      if (!move) setReservationRevision((revision) => revision + 1);
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

  async function finish() {
    setFinishing(false);
    setBusy(true);
    setError("");
    try {
      await api<void>(`${path}/finish`, { method: "POST" });
      if (localStorage.getItem(sessionKey(id)) === sessionId) {
        localStorage.removeItem(sessionKey(id));
      }
      router.replace(`/accommodations/${id}/outages/history`);
    } catch (error) {
      setError((error as Error).message);
    } finally {
      setBusy(false);
    }
  }

  const unresolvedCount = session?.plan?.unresolvedReservationIds.length ?? 0;

  return (
    <div className="shell">
      {finishing && session && (
        <ConfirmDialog
          title="복구 세션 종료"
          message={`세션을 종료하시겠습니까?${unresolvedCount ? ` 대체 객실을 찾지 못한 예약 ${unresolvedCount}건은 별도 대응이 필요합니다.` : ""} 처리 결과는 복구 이력에서 볼 수 있습니다.`}
          confirmLabel="세션 종료"
          onClose={() => setFinishing(false)}
          onConfirm={() => void finish()}
        />
      )}
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
            <button className="text-button" onClick={() => {
              void load();
              void loadOutages();
              setReservationRevision((revision) => revision + 1);
            }} disabled={busy}>
              다시 불러오기
            </button>
          )}
        </div>
      )}
      {!session && !error && (
        <p className="message">세션을 불러오는 중입니다.</p>
      )}
      {session && (
        <>
          <a className="back" href="#reservation-context">예약 조회 기간으로 이동 ↓</a>
          <div className="operations-layout">
            <div>
              <RecoveryDetails
                session={session}
                reservations={booking.reservations}
                rooms={rooms}
                busy={busy}
                onDecision={updateOffer}
                onCancel={setCancelTarget}
                onMove={updateOffer}
              />
            </div>
            <div>
              {outageError && (
                <div className="notice error" role="alert">
                  장애 정보를 불러오지 못해 장애 겹침을 표시할 수 없습니다. {outageError}{" "}
                  <button className="text-button" onClick={loadOutages}>다시 불러오기</button>
                </div>
              )}
              <ReservationContext
                rooms={rooms}
                reservations={booking.reservations}
                error={booking.error}
                onReload={booking.load}
                outages={outages}
                referenceDate={session.referenceDate}
                initialEnd={session.offers
                  .flatMap((offer) => offer.segments.map((segment) => segment.endDate))
                  .sort().at(-1)}
              />
            </div>
          </div>
          <section className="card">
            {session.finishBlockedReason && <p>{session.finishBlockedReason}</p>}
            <button
              className="button-link"
              disabled={busy || !!session.finishBlockedReason}
              onClick={() => setFinishing(true)}
            >
              복구 세션 종료
            </button>
          </section>
        </>
      )}
    </div>
  );
}
