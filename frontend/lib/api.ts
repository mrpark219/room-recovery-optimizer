import type { Room } from "@/lib/types";

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
  }
}

export async function api<T>(path: string, options?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`/api${path}`, {
      ...options,
      headers: { "Content-Type": "application/json", ...options?.headers },
      cache: "no-store",
    });
  } catch {
    throw new Error("서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.");
  }

  if (!response.ok) {
    let message = "요청을 처리하지 못했습니다.";
    try {
      const body = await response.json();
      message = body.error || body.message || message;
    } catch {}
    throw new ApiError(response.status, message);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return response.json();
}

export const base = (id: string) => `/accommodations/${id}`;
export const roomName = (rooms: Room[], id: number | null) =>
  id === null
    ? "미배정"
    : rooms.find((room) => room.id === id)?.name || `객실 ${id}`;
export const money = (amount: number) => `${amount.toLocaleString("ko-KR")}원`;
export const today = () => {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
};
export const statusName = (status: string) =>
  ({ CONFIRMED: "예약 확정", CHECKED_IN: "투숙 중", CANCELLED: "취소" })[
    status
  ] || status;
export const sessionKey = (id: string) => `recovery-session:${id}`;
