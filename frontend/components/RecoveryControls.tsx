import Link from "next/link";
import ImpactPreview from "@/components/ImpactPreview";
import type { Calculation, Reservation, Room } from "@/lib/types";

type Props = {
  id: string;
  referenceDate: string;
  busy: boolean;
  savedSession: string;
  checkingSession: boolean;
  sessionCheckFailed: boolean;
  preview: Calculation | null;
  reservations: Reservation[] | null;
  rooms: Room[];
  onDateChange: (date: string) => void;
  onPreview: () => void;
  onStart: () => void;
  onCheckSession: () => void;
};

export default function RecoveryControls({
  id,
  referenceDate,
  busy,
  savedSession,
  checkingSession,
  sessionCheckFailed,
  preview,
  reservations,
  rooms,
  onDateChange,
  onPreview,
  onStart,
  onCheckSession,
}: Props) {
  const startDisabled =
    busy ||
    !referenceDate ||
    checkingSession ||
    sessionCheckFailed ||
    !preview ||
    preview.placements.length === 0;

  const reason = busy ? "처리 중입니다."
    : !referenceDate ? "재배정 기준일을 선택하세요."
    : checkingSession ? "진행 중인 세션을 확인하고 있습니다."
    : sessionCheckFailed ? "진행 중인 세션을 다시 확인하세요."
    : !preview ? "먼저 변경이 필요한 예약을 찾으세요."
    : preview.affectedReservations.length === 0 ? "기준일부터 객실 변경이 필요한 예약이 없습니다."
    : preview.placements.length === 0 ? "남은 숙박 기간에 배정할 수 있는 대체 객실을 찾지 못했습니다."
    : "";

  return (
    <section className="card">
      <div className="section-heading">
        <div>
          <h2>영향 확인 및 재배정</h2>
        </div>
      </div>
      <div className="preview-controls">
        <label>
          재배정 기준일
          <input
            type="date"
            disabled={busy}
            value={referenceDate}
            onChange={(event) => onDateChange(event.target.value)}
          />
        </label>
        <button
          className="secondary"
          disabled={busy || !referenceDate}
          onClick={onPreview}
        >
          변경이 필요한 예약 찾기
        </button>
        {savedSession ? (
          <Link
            className="button-link"
            href={`/accommodations/${id}/outages/recovery/${encodeURIComponent(savedSession)}`}
          >
            이어서 작업
          </Link>
        ) : (
          <button disabled={startDisabled} onClick={onStart}>
            재배정 제안 검토 시작
          </button>
        )}
      </div>
      <p className="footnote">이 날짜부터 남은 숙박의 대체 객실을 계산합니다.</p>
      {!savedSession && reason && <p className="footnote" role="status">{reason}</p>}
      {sessionCheckFailed && (
        <p role="alert" className="error">
          저장된 세션을 확인하지 못했습니다.{" "}
          <button className="text-button" onClick={onCheckSession}>
            다시 확인
          </button>
        </p>
      )}
      {savedSession && (
        <p className="footnote">
          첫 제안은 세션 생성 시점의 상태를 기준으로 계산합니다. 수락 시 새
          장애와 겹치면 배정안을 다시 계산합니다.
        </p>
      )}
      {preview && <ImpactPreview preview={preview} reservations={reservations} rooms={rooms} />}
    </section>
  );
}
