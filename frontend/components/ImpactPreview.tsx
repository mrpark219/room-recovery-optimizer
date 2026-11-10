import { money } from "@/lib/api";
import type { Calculation } from "@/lib/types";
export default function ImpactPreview({ preview }: { preview: Calculation }) {
  return (
    <div className="preview">
      {preview.placements.length === 0 && (
        <p>제안할 수 있는 배정안이 없습니다.</p>
      )}
      <div className="metrics">
        <div>
          <span>영향 예약</span>
          <strong>{preview.affectedReservations.length}건</strong>
        </div>
        <div>
          <span>긴급 예약</span>
          <strong>{preview.urgentReservationIds.length}건</strong>
        </div>
        <div>
          <span>미해결</span>
          <strong>{preview.unresolvedReservationIds.length}건</strong>
        </div>
        <div>
          <span>예상 추가 비용</span>
          <strong>{money(preview.estimatedAdditionalCost)}</strong>
        </div>
      </div>
      {preview.affectedReservations.length > 0 && (
        <p>
          영향 예약{" "}
          {preview.affectedReservations
            .map((r) => `#${r.reservationId}`)
            .join(", ")}
        </p>
      )}
      {preview.unresolvedReservationIds.length > 0 && (
        <p>
          미해결 예약{" "}
          {preview.unresolvedReservationIds.map((r) => `#${r}`).join(", ")}
        </p>
      )}
      <p className="footnote">
        미리보기는 현재 상태의 계산 결과입니다. 재배정 시작 시 다시 계산합니다.
      </p>
    </div>
  );
}
