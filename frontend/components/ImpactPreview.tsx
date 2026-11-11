import ReservationSummary from "@/components/ReservationSummary";
import { money, roomName } from "@/lib/api";
import type { Calculation, Reservation, Room, Segment } from "@/lib/types";
function RoomSchedule({ segments, rooms }: { segments: Segment[]; rooms: Room[] }) {
  return (
    <ul className="segments">
      {segments.map((segment) => (
        <li key={`${segment.roomId}-${segment.startDate}`}>
          <strong>{roomName(rooms, segment.roomId)}</strong>{" "}
          <span className="muted">{segment.startDate} ~ {segment.endDate}</span>
        </li>
      ))}
    </ul>
  );
}

export default function ImpactPreview({ preview, reservations, rooms }: {
  preview: Calculation;
  reservations: Reservation[] | null;
  rooms: Room[];
}) {
  return (
    <div className="preview">
      <div className="metrics">
        <div>
          <span>객실 변경 필요</span>
          <strong>{preview.affectedReservations.length}건</strong>
        </div>
        <div className="metric-urgent">
          <span>당일 변경 필요</span>
          <strong>{preview.urgentReservationIds.length}건</strong>
        </div>
        <div>
          <span>대체 객실 없음</span>
          <strong>{preview.unresolvedReservationIds.length}건</strong>
        </div>
        <div>
          <span>예상 추가 비용</span>
          <strong>{money(preview.estimatedAdditionalCost)}</strong>
        </div>
      </div>
      {preview.affectedReservations.length > 0 && (
        <div>
          <h3>객실 변경이 필요한 예약</h3>
          {preview.affectedReservations.map((r) => {
            const reservation = reservations?.find((item) => item.id === r.reservationId);
            const placement = preview.placements.find((item) => item.reservationId === r.reservationId);
            const remainingAssignments = reservation?.assignments
              .filter((segment) => segment.endDate > preview.referenceDate)
              .map((segment) => ({
                ...segment,
                startDate: segment.startDate < preview.referenceDate
                  ? preview.referenceDate
                  : segment.startDate,
              }));
            return (
              <div key={r.reservationId} className="history-row">
                <strong>예약 #{r.reservationId}</strong>{" "}
                {preview.unresolvedReservationIds.includes(r.reservationId) && (
                  <span className="badge urgent">대체 객실 없음</span>
                )}
                {reservation ? (
                  <ReservationSummary reservation={reservation} rooms={rooms} showAssignments={false} />
                ) : (
                  <p className="muted">
                    체크인 {r.startDate} · 체크아웃 {r.endDate} · 객실 및 인원 정보를 불러올 수 없습니다.
                  </p>
                )}
                <div className="schedule-comparison">
                  <div>
                    <strong>기존 일정</strong>
                    {remainingAssignments?.length ? (
                      <RoomSchedule segments={remainingAssignments} rooms={rooms} />
                    ) : (
                      <p className="muted">{reservation ? "남은 객실 배정 없음" : "객실 배정 정보 없음"}</p>
                    )}
                  </div>
                  <div>
                    <strong>새 제안</strong>
                    {placement ? (
                      <>
                        <RoomSchedule segments={placement.segments} rooms={rooms} />
                        <p className="muted">
                          예상 추가 비용 {money(placement.additionalCost)} · 이동 {placement.moves}회
                        </p>
                      </>
                    ) : (
                      <p className="muted">남은 숙박 전체를 배정할 대체 객실을 찾지 못했습니다.</p>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
      {preview.placements.length > 0 && (
        <p className="footnote">
          고객 수락을 기록하면 새 일정으로 배정이 바뀝니다.
        </p>
      )}
    </div>
  );
}
