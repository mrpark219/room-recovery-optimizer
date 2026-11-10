import { roomName, statusName } from "@/lib/api";
import type { Room, Reservation } from "@/lib/types";
export default function ReservationTable({
  reservations,
  rooms,
}: {
  reservations: Reservation[];
  rooms: Room[];
}) {
  return (
    <div className="table-wrap">
      <table role="table">
        <thead role="rowgroup">
          <tr role="row">
            <th role="columnheader">예약 번호</th>
            <th role="columnheader">숙박 기간</th>
            <th role="columnheader">객실 유형</th>
            <th role="columnheader">객실 배정</th>
            <th role="columnheader">상태</th>
          </tr>
        </thead>
        <tbody role="rowgroup">
          {reservations.map((r) => (
            <tr key={r.id} role="row">
              <td role="cell" data-label="예약 번호">
                #{r.id}
              </td>
              <td role="cell" data-label="숙박 기간">
                {r.startDate} ~ {r.endDate}
              </td>
              <td role="cell" data-label="객실 유형">
                {r.roomTypeName}
              </td>
              <td role="cell" data-label="객실 배정">
                {r.assignments.length
                  ? r.assignments.map((s, i) => (
                      <div key={i}>
                        {roomName(rooms, s.roomId)}{" "}
                        {!(
                          r.assignments.length === 1 &&
                          s.startDate === r.startDate &&
                          s.endDate === r.endDate
                        ) && (
                          <span className="muted">
                            ({s.startDate} ~ {s.endDate})
                          </span>
                        )}
                      </div>
                    ))
                  : "미배정"}
              </td>
              <td role="cell" data-label="상태">
                <span className="badge">{statusName(r.status)}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
