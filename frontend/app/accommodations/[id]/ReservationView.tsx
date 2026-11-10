"use client";
import { useCallback, useEffect, useState } from "react";
import AccommodationHeader from "@/components/AccommodationHeader";
import ReservationTable from "@/components/ReservationTable";
import { api, base } from "@/lib/api";
import type { Accommodation, Room, Reservation } from "@/lib/types";
export default function ReservationView({ id }: { id: string }) {
  const [name, setName] = useState("");
  const [rooms, setRooms] = useState<Room[]>([]);
  const [items, setItems] = useState<Reservation[] | null>(null);
  const [error, setError] = useState("");
  const load = useCallback(async () => {
    setError("");
    setItems(null);
    try {
      const [accommodations, roomData, reservations] = await Promise.all([
        api<Accommodation[]>("/accommodations"),
        api<Room[]>(`${base(id)}/rooms`),
        api<Reservation[]>(`${base(id)}/reservations`),
      ]);
      setName(
        accommodations.find((a) => String(a.id) === id)?.name || `숙소 ${id}`,
      );
      setRooms(roomData);
      setItems(reservations);
    } catch (e) {
      setError((e as Error).message);
    }
  }, [id]);
  useEffect(() => {
    void load();
  }, [load]);
  return (
    <div className="shell">
      <AccommodationHeader id={id} name={name} tab="reservations" />
      {error && (
        <div className="notice error" role="alert">
          {error}{" "}
          <button className="text-button" onClick={load}>
            다시 불러오기
          </button>
        </div>
      )}
      <section className="card reservation-card">
        {items === null ? (
          <p className="message">
            {error
              ? "예약을 불러오지 못했습니다."
              : "예약을 불러오는 중입니다."}
          </p>
        ) : items.length === 0 ? (
          <p className="message">예약 내역이 없습니다.</p>
        ) : (
          <ReservationTable reservations={items} rooms={rooms} />
        )}
        <p className="footnote">숙박 기간의 종료일은 포함되지 않습니다.</p>
      </section>
    </div>
  );
}
