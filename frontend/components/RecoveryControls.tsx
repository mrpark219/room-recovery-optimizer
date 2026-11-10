import Link from "next/link";
import ImpactPreview from "@/components/ImpactPreview";
import type { Calculation } from "@/lib/types";

type Props = {
  id: string;
  referenceDate: string;
  busy: boolean;
  savedSession: string;
  checkingSession: boolean;
  sessionCheckFailed: boolean;
  preview: Calculation | null;
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

  return (
    <section className="card">
      <div className="section-heading">
        <div>
          <h2>영향 확인 및 재배정</h2>
        </div>
      </div>
      <div className="preview-controls">
        <label>
          기준일
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
          영향 미리보기
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
            재배정 시작
          </button>
        )}
      </div>
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
          진행 중인 세션은 생성 시점의 상태를 사용하므로 이후 장애 변경은
          반영되지 않습니다.
        </p>
      )}
      {preview && <ImpactPreview preview={preview} />}
    </section>
  );
}
