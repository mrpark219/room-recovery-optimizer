"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import type { Accommodation } from "@/lib/types";
export default function Home() {
  const [items, setItems] = useState<Accommodation[] | null>(null);
  const [error, setError] = useState("");
  function load() {
    setError("");
    setItems(null);
    api<Accommodation[]>("/accommodations")
      .then(setItems)
      .catch((e) => setError(e.message));
  }
  useEffect(() => {
    load();
  }, []);
  return (
    <div className="shell">
      <h1>숙소 목록</h1>
      <p className="lead">관리할 숙소를 선택해 주세요.</p>
      <section className="card list-card" aria-label="숙소 목록">
        {error ? (
          <div className="message error" role="alert">
            {error}{" "}
            <button className="text-button" onClick={load}>
              다시 시도
            </button>
          </div>
        ) : items === null ? (
          <p className="message">숙소를 불러오는 중입니다.</p>
        ) : items.length === 0 ? (
          <p className="message">등록된 숙소가 없습니다.</p>
        ) : (
          items.map((item) => (
            <Link
              className="action-row"
              key={item.id}
              href={`/accommodations/${item.id}`}
            >
              <span className="row-icon" aria-hidden="true">
                ⌂
              </span>
              <strong>{item.name}</strong>
              <span className="chevron" aria-hidden="true">
                ›
              </span>
            </Link>
          ))
        )}
      </section>
    </div>
  );
}
