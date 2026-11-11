"use client";
import { useCallback, useEffect, useState } from "react";
import AccommodationHeader from "@/components/AccommodationHeader";
import { api, base, money } from "@/lib/api";
import type { Accommodation, Room } from "@/lib/types";

export default function RoomView({ id }: { id: string }) {
  const [name, setName] = useState("");
  const [rooms, setRooms] = useState<Room[] | null>(null);
  const [error, setError] = useState("");
  const load = useCallback(async () => {
    setError("");
    try {
      const [accommodations, data] = await Promise.all([
        api<Accommodation[]>("/accommodations"),
        api<Room[]>(`${base(id)}/rooms`),
      ]);
      setName(accommodations.find((a) => String(a.id) === id)?.name || `숙소 ${id}`);
      setRooms(data);
    } catch (e) {
      setError((e as Error).message);
    }
  }, [id]);
  useEffect(() => { void load(); }, [load]);

  return (
    <div className="shell">
      <AccommodationHeader id={id} name={name} tab="rooms" />
      <section className="card">
        <div className="section-heading"><h2>객실 목록</h2></div>
        {error ? (
          <p className="message error" role="alert">
            {error} <button className="text-button" onClick={load}>다시 불러오기</button>
          </p>
        ) : rooms === null ? (
          <p className="message">객실을 불러오는 중입니다.</p>
        ) : rooms.length === 0 ? (
          <p className="message">등록된 객실이 없습니다.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>객실</th><th>객실 유형</th><th>수용 인원</th><th>1박 기준가</th></tr></thead>
              <tbody>
                {rooms.map((room) => (
                  <tr key={room.id}>
                    <td>{room.name}</td>
                    <td>{room.roomTypeName}</td>
                    <td>{room.capacity}명</td>
                    <td>{money(room.nightlyRate)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
