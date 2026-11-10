"use client";
import { useId, useRef, useState } from "react";
import type { Room } from "@/lib/types";
export type OutageFormValue = {
  roomId: string;
  startDate: string;
  endDate: string;
  reason: string;
};
export default function OutageForm({
  form,
  setForm,
  rooms,
  busy,
  editing,
  save,
  close,
  serverError,
}: {
  form: OutageFormValue;
  setForm: (value: OutageFormValue) => void;
  rooms: Room[];
  busy: boolean;
  editing: number | null;
  save: (event: React.FormEvent) => void;
  close: () => void;
  serverError: string;
}) {
  const [dateError, setDateError] = useState("");
  const endDateRef = useRef<HTMLInputElement>(null);
  const dateErrorId = useId();
  function submit(event: React.FormEvent) {
    if (form.startDate >= form.endDate) {
      event.preventDefault();
      setDateError("종료일은 시작일보다 늦어야 합니다.");
      endDateRef.current?.focus();
      return;
    }
    save(event);
  }
  return (
    <form className="form-grid" onSubmit={submit}>
      <label>
        객실
        <select
          required
          value={form.roomId}
          onChange={(e) => setForm({ ...form, roomId: e.target.value })}
        >
          <option value="">객실 선택</option>
          {rooms.map((r) => (
            <option key={r.id} value={r.id}>
              {r.name}
            </option>
          ))}
        </select>
      </label>
      <label>
        시작일
        <input
          required
          type="date"
          value={form.startDate}
          onChange={(e) => {
            setDateError("");
            setForm({ ...form, startDate: e.target.value });
          }}
        />
      </label>
      <label>
        종료일 (미포함)
        <input
          required
          type="date"
          ref={endDateRef}
          value={form.endDate}
          aria-invalid={!!dateError}
          aria-describedby={dateError ? dateErrorId : undefined}
          onChange={(e) => {
            setDateError("");
            setForm({ ...form, endDate: e.target.value });
          }}
        />
        {dateError && (
          <span id={dateErrorId} className="field-error" role="alert">
            {dateError}
          </span>
        )}
      </label>
      <label className="wide">
        사유
        <input
          required
          maxLength={500}
          value={form.reason}
          onChange={(e) => setForm({ ...form, reason: e.target.value })}
          placeholder="장애 사유를 입력해 주세요"
        />
      </label>
      {serverError && (
        <p className="field-error wide" role="alert">
          {serverError}
        </p>
      )}
      <div className="form-actions">
        <button disabled={busy}>
          {busy ? "처리 중" : editing === null ? "등록하기" : "변경 저장"}
        </button>
        <button type="button" className="secondary" onClick={close}>
          취소
        </button>
      </div>
    </form>
  );
}
