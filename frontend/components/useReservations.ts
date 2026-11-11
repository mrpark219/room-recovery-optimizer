"use client";
import { useCallback, useEffect, useState } from "react";
import { api, base } from "@/lib/api";
import type { Reservation } from "@/lib/types";

export default function useReservations(id: string, revision = 0) {
  const [reservations, setReservations] = useState<Reservation[] | null>(null);
  const [error, setError] = useState("");
  const load = useCallback(async () => {
    setError("");
    try {
      setReservations(await api<Reservation[]>(`${base(id)}/reservations`));
    } catch (e) {
      setError((e as Error).message);
    }
  }, [id]);
  useEffect(() => { void load(); }, [load, revision]);
  return { reservations, error, load };
}
