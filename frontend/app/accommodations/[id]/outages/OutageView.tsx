"use client";
import { useCallback, useEffect, useState } from "react";
import ConfirmDialog from "@/components/ConfirmDialog";
import OutageList from "@/components/OutageList";
import RecoveryControls from "@/components/RecoveryControls";
import OutageForm from "@/components/OutageForm";
import useReservations from "@/components/useReservations";
import ReservationContext from "@/components/ReservationContext";
import OutageTabs from "@/components/OutageTabs";
import AccommodationHeader from "@/components/AccommodationHeader";
import { useRouter } from "next/navigation";
import { api, ApiError, base, roomName, today, sessionKey } from "@/lib/api";
import type { Accommodation, Room, Outage, Calculation } from "@/lib/types";

const blank = { roomId: "", startDate: "", endDate: "", reason: "" };

export default function OutageView({ id }: { id: string }) {
  const router = useRouter();
  const booking = useReservations(id);
  const [name, setName] = useState("");
  const [rooms, setRooms] = useState<Room[]>([]);
  const [outages, setOutages] = useState<Outage[] | null>(null);

  const [error, setError] = useState("");
  const [readError, setReadError] = useState(false);
  const [busy, setBusy] = useState(false);

  const [selectedOutage, setSelectedOutage] = useState<Outage | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Outage | null>(null);
  const [editing, setEditing] = useState<number | null>(null);
  const [form, setForm] = useState(blank);
  const [showForm, setShowForm] = useState(false);
  const [formError, setFormError] = useState("");

  const [referenceDate, setReferenceDate] = useState(today);
  const [preview, setPreview] = useState<Calculation | null>(null);
  const [savedSession, setSavedSession] = useState("");
  const [checkingSession, setCheckingSession] = useState(true);
  const [sessionCheckFailed, setSessionCheckFailed] = useState(false);

  const load = useCallback(async () => {
    setError("");
    setReadError(false);
    try {
      const [accommodations, roomData, outageData] = await Promise.all([
        api<Accommodation[]>("/accommodations"),
        api<Room[]>(`${base(id)}/rooms`),
        api<Outage[]>(`${base(id)}/unavailabilities`),
      ]);
      setName(
        accommodations.find((accommodation) => String(accommodation.id) === id)
          ?.name || `숙소 ${id}`,
      );
      setRooms(roomData);
      setOutages(outageData);
    } catch (error) {
      setReadError(true);
      setError((error as Error).message);
    }
  }, [id]);

  const checkSession = useCallback(async () => {
    const stored = localStorage.getItem(sessionKey(id));
    setSessionCheckFailed(false);
    if (!stored) {
      setCheckingSession(false);
      return;
    }
    setCheckingSession(true);
    try {
      await api(`${base(id)}/recovery-sessions/${encodeURIComponent(stored)}`);
      setSavedSession(stored);
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        if (localStorage.getItem(sessionKey(id)) === stored) {
          localStorage.removeItem(sessionKey(id));
        }
        setSavedSession("");
      } else {
        setSessionCheckFailed(true);
      }
    } finally {
      setCheckingSession(false);
    }
  }, [id]);

  useEffect(() => {
    void load();
    void checkSession();
  }, [load, checkSession]);

  async function save(event: React.FormEvent) {
    event.preventDefault();
    if (busy) {
      return;
    }
    setFormError("");
    setBusy(true);
    setError("");
    setReadError(false);
    try {
      const saved = await api<Outage>(
        `${base(id)}/unavailabilities${editing === null ? "" : `/${editing}`}`,
        {
          method: editing === null ? "POST" : "PUT",
          body: JSON.stringify({ ...form, roomId: Number(form.roomId) }),
        },
      );
      setForm(blank);
      setEditing(null);
      setShowForm(false);
      setPreview(null);
      setSelectedOutage(saved);
      await load();
    } catch (error) {
      setFormError((error as Error).message);
    } finally {
      setBusy(false);
    }
  }

  async function remove(outage: Outage) {
    if (busy) {
      return;
    }
    setBusy(true);
    setError("");
    setReadError(false);
    try {
      await api(`${base(id)}/unavailabilities/${outage.id}`, {
        method: "DELETE",
      });
      setPreview(null);
      setSelectedOutage(null);
      await load();
    } catch (error) {
      setError((error as Error).message);
    } finally {
      setBusy(false);
    }
  }

  async function previewImpact() {
    if (busy || !referenceDate) {
      return;
    }
    setPreview(null);
    setBusy(true);
    setError("");
    setReadError(false);
    try {
      const result = await api<Calculation>(
        `${base(id)}/recovery-calculations`,
        {
          method: "POST",
          body: JSON.stringify({ referenceDate }),
        },
      );
      setPreview(result);
    } catch (error) {
      setError((error as Error).message);
    } finally {
      setBusy(false);
    }
  }

  async function startRecovery() {
    if (busy || !referenceDate || !preview || preview.placements.length === 0) {
      return;
    }
    setBusy(true);
    setError("");
    setReadError(false);
    try {
      const session = await api<{ sessionId: string }>(
        `${base(id)}/recovery-sessions`,
        {
          method: "POST",
          body: JSON.stringify({ referenceDate }),
        },
      );
      localStorage.setItem(sessionKey(id), session.sessionId);
      router.push(
        `/accommodations/${id}/outages/recovery/${encodeURIComponent(session.sessionId)}`,
      );
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setError(
          `이미 진행 중인 세션이 있습니다. 저장된 세션이 있다면 이어보기를 선택해 주세요. ${error.message}`,
        );
      } else {
        setError((error as Error).message);
      }
    } finally {
      setBusy(false);
    }
  }

  function changeReferenceDate(date: string) {
    setReferenceDate(date);
    setPreview(null);
  }

  function editOutage(outage: Outage) {
    setFormError("");
    setSelectedOutage(null);
    setEditing(outage.id);
    setForm({
      roomId: String(outage.roomId),
      startDate: outage.startDate,
      endDate: outage.endDate,
      reason: outage.reason,
    });
    setShowForm(true);
  }

  function toggleForm() {
    setEditing(null);
    setFormError("");
    setForm(blank);
    setShowForm(!showForm);
  }

  function updateForm(value: typeof form) {
    setFormError("");
    setForm(value);
    setSelectedOutage(null);
  }

  function confirmDelete() {
    if (!deleteTarget) {
      return;
    }
    const target = deleteTarget;
    setDeleteTarget(null);
    void remove(target);
  }

  return (
    <div className="shell">
      {deleteTarget && (
        <ConfirmDialog
          title="객실 장애 삭제"
          message={`${roomName(rooms, deleteTarget.roomId)}의 장애를 삭제하시겠습니까?`}
          confirmLabel="삭제"
          onClose={() => setDeleteTarget(null)}
          onConfirm={confirmDelete}
        />
      )}
      <AccommodationHeader id={id} name={name} tab="outages" />
      <OutageTabs id={id} />
      {error && (
        <div role="alert" className="notice error">
          {error}{" "}
          {readError && (
            <button className="text-button" onClick={load}>
              다시 불러오기
            </button>
          )}
        </div>
      )}
      <div className="operations-layout">
        <div>
          <section className="card">
            <div className="section-heading">
              <div>
                <h2>객실 장애</h2>
                <p>이용할 수 없는 객실과 기간을 등록하세요.</p>
              </div>
              {!showForm && <button onClick={toggleForm}>장애 등록</button>}
            </div>
            {showForm && editing === null && (
              <OutageForm
                key={editing ?? "new"}
                form={form}
                setForm={updateForm}
                serverError={formError}
                rooms={rooms}
                busy={busy}
                editing={editing}
                save={save}
                close={() => setShowForm(false)}
              />
            )}
            <OutageList
              outages={outages}
              rooms={rooms}
              busy={busy}
              error={error}
              editing={editing}
              selectedId={selectedOutage?.id ?? null}
              editForm={showForm && editing !== null ? (
                <OutageForm key={editing} form={form} setForm={updateForm} serverError={formError}
                  rooms={rooms} busy={busy} editing={editing} save={save}
                  close={() => { setShowForm(false); setEditing(null); }} />
              ) : null}
              onEdit={editOutage}
              onDelete={setDeleteTarget}
              onViewReservations={(outage) => {
                setShowForm(false);
                setEditing(null);
                setSelectedOutage({ ...outage });
                document.getElementById("reservation-context")?.scrollIntoView({ behavior: "smooth" });
              }}
            />
          </section>
          <RecoveryControls
            id={id}
            referenceDate={referenceDate}
            busy={busy}
            savedSession={savedSession}
            checkingSession={checkingSession}
            sessionCheckFailed={sessionCheckFailed}
            preview={preview}
            reservations={booking.reservations}
            rooms={rooms}
            onDateChange={changeReferenceDate}
            onPreview={previewImpact}
            onStart={startRecovery}
            onCheckSession={checkSession}
          />
        </div>
        <ReservationContext
          rooms={rooms}
          reservations={booking.reservations}
          error={booking.error}
          onReload={booking.load}
          outages={outages || []}
          selected={selectedOutage}
          draft={showForm && form.roomId && form.startDate && form.endDate && form.startDate < form.endDate
            ? { roomId: Number(form.roomId), startDate: form.startDate, endDate: form.endDate } : null}
        />
      </div>
    </div>
  );
}
