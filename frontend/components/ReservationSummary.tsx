import { roomName } from "@/lib/api";
import type { Reservation, Room } from "@/lib/types";

export default function ReservationSummary({ reservation, rooms, showAssignments = true }: {
  reservation?: Reservation;
  rooms: Room[];
  showAssignments?: boolean;
}) {
  if (!reservation) return <p className="muted">예약 상세 정보를 불러올 수 없습니다.</p>;
  return (
    <p className="muted">
      체크인 {reservation.startDate} · 체크아웃 {reservation.endDate} · {reservation.guestCount}명
      {showAssignments && (
        <>
          <br />
          배정 일정{" "}
          {reservation.assignments.length
            ? reservation.assignments.map((segment) => `${roomName(rooms, segment.roomId)} (${segment.startDate} ~ ${segment.endDate})`).join(" / ")
            : "미배정"}
        </>
      )}
    </p>
  );
}
