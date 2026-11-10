import { roomName } from "@/lib/api";
import type { Outage, Room } from "@/lib/types";
export default function OutageList({
  outages,
  rooms,
  busy,
  error,
  onEdit,
  onDelete,
}: {
  outages: Outage[] | null;
  rooms: Room[];
  busy: boolean;
  error: string;
  onEdit: (o: Outage) => void;
  onDelete: (o: Outage) => void;
}) {
  return outages === null ? (
    <p className="message">
      {error
        ? "장애 목록을 불러오지 못했습니다."
        : "장애 목록을 불러오는 중입니다."}
    </p>
  ) : outages.length === 0 ? (
    <p className="message">등록된 객실 장애가 없습니다.</p>
  ) : (
    <div className="outage-list">
      {outages.map((o) => (
        <div className="outage-row" key={o.id}>
          <div>
            <strong>{roomName(rooms, o.roomId)}</strong>
            <p>
              {o.startDate} ~ {o.endDate}{" "}
              <span className="muted">· 종료일 미포함</span>
            </p>
            <p className="muted">{o.reason}</p>
          </div>
          <div className="row-actions">
            <button
              className="secondary small"
              disabled={busy}
              onClick={() => onEdit(o)}
            >
              수정
            </button>
            <button
              className="text-button danger"
              disabled={busy}
              onClick={() => onDelete(o)}
            >
              삭제
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
