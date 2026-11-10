import RecoveryOfferCard from "@/components/RecoveryOfferCard";
import { money, roomName } from "@/lib/api";
import type { Session, Room, Offer, Move } from "@/lib/types";

type Props = {
  session: Session;
  rooms: Room[];
  busy: boolean;
  onDecision: (offer: Offer, decision: "ACCEPTED") => void;
  onCancel: (offer: Offer) => void;
  onMove: (offer: Offer, move: Move) => void;
};

type OfferSectionProps = Omit<Props, "session"> & {
  moveHistory: Session["moveHistory"];
  title: string;
  offers: Offer[];
  urgent: number[];
  showCount?: boolean;
};
function OfferSection({
  title,
  showCount,
  offers,
  moveHistory,
  rooms,
  urgent,
  busy,
  onDecision,
  onCancel,
  onMove,
}: OfferSectionProps) {
  return (
    <section className="card">
      <div className="section-heading">
        <div>
          <h2>{title}</h2>
        </div>
        {showCount && <span className="badge">{offers.length}건</span>}
      </div>
      {offers.length ? (
        <div className="offer-grid">
          {offers.map((offer) => (
            <RecoveryOfferCard
              key={offer.id}
              o={offer}
              rooms={rooms}
              urgent={urgent}
              busy={busy}
              moveHistory={moveHistory}
              onDecision={onDecision}
              onCancel={onCancel}
              onMove={onMove}
            />
          ))}
        </div>
      ) : (
        <p className="message">응답을 기다리는 제안이 없습니다.</p>
      )}
    </section>
  );
}

export default function RecoveryDetails({
  session,
  rooms,
  busy,
  onDecision,
  onCancel,
  onMove,
}: Props) {
  const urgent = session.plan?.urgentReservationIds || [];
  const pending = session.offers
    .filter((offer) => !offer.decision)
    .sort(
      (a, b) =>
        Number(urgent.includes(b.reservationId)) -
        Number(urgent.includes(a.reservationId)),
    );
  const decided = session.offers.filter((offer) => offer.decision);
  return (
    <>
      <section className="card">
        <div className="section-heading">
          <div>
            <h2>재배정 현황</h2>
            <p>
              기준일 {session.referenceDate} · 종료일은 숙박에 포함되지
              않습니다.
            </p>
          </div>
        </div>
        {session.plan ? (
          <div className="metrics">
            <div>
              <span>긴급 예약</span>
              <strong>{session.plan.urgentReservationIds.length}건</strong>
            </div>
            <div>
              <span>배정 가능</span>
              <strong>{session.plan.score.served}건</strong>
            </div>
            <div>
              <span>미해결</span>
              <strong>{session.plan.unresolvedReservationIds.length}건</strong>
            </div>
            <div>
              <span>예상 추가 비용</span>
              <strong>{money(session.plan.score.additionalCost)}</strong>
            </div>
          </div>
        ) : (
          <p className="message">계산 결과가 없습니다.</p>
        )}
      </section>
      <OfferSection
        title="응답 대기 제안"
        showCount
        offers={pending}
        moveHistory={session.moveHistory}
        rooms={rooms}
        urgent={urgent}
        busy={busy}
        onDecision={onDecision}
        onCancel={onCancel}
        onMove={onMove}
      />
      {decided.length > 0 && (
        <OfferSection
          title="처리된 제안"
          offers={decided}
          moveHistory={session.moveHistory}
          rooms={rooms}
          urgent={urgent}
          busy={busy}
          onDecision={onDecision}
          onCancel={onCancel}
          onMove={onMove}
        />
      )}
      {session.moveHistory.length > 0 && (
        <section className="card">
          <div className="section-heading">
            <div>
              <h2>이동 완료 내역</h2>
            </div>
          </div>
          {session.moveHistory.map((move, index) => (
            <div className="history-row" key={index}>
              <strong>예약 #{move.reservationId}</strong> · {move.date} ·{" "}
              {roomName(rooms, move.fromRoomId)} →{" "}
              {roomName(rooms, move.toRoomId)}
            </div>
          ))}
        </section>
      )}
      {!!session.plan?.unresolvedReservationIds.length && (
        <section className="card">
          <div className="section-heading">
            <div>
              <h2>미해결 예약</h2>
            </div>
          </div>
          <p>
            {session.plan.unresolvedReservationIds.map((reservationId) => (
              <span key={reservationId}>
                #{reservationId}{" "}
                {urgent.includes(reservationId) && (
                  <span className="badge urgent">긴급</span>
                )}{" "}
                &nbsp;{" "}
              </span>
            ))}
          </p>
        </section>
      )}
    </>
  );
}
