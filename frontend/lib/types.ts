export type Accommodation = { id: number; name: string };
export type Room = { id: number; name: string; roomTypeName: string; capacity: number; nightlyRate: number };
export type Segment = { roomId: number; startDate: string; endDate: string };
export type Reservation = {
  id: number;
  startDate: string;
  endDate: string;
  status: "CONFIRMED" | "CHECKED_IN" | "CANCELLED";
  roomTypeName: string;
  guestCount: number;
  assignments: Segment[];
};
export type Outage = Segment & { id: number; reason: string };
export type Calculation = {
  referenceDate: string;
  affectedReservations: {
    reservationId: number;
    startDate: string;
    endDate: string;
    status: string;
  }[];
  placements: Placement[];
  estimatedAdditionalCost: number;
  unresolvedReservationIds: number[];
  urgentReservationIds: number[];
};
export type Placement = {
  reservationId: number;
  segments: Segment[];
  additionalCost: number;
  moves: number;
};
export type Move = { date: string; fromRoomId: number; toRoomId: number };
export type Offer = {
  id: number;
  reservationId: number;
  initialRoomId: number | null;
  segments: Segment[];
  plannedMoves: Move[];
  decision: "ACCEPTED" | "CANCEL_REQUESTED" | null;
};
export type Session = {
  finishBlockedReason: string | null;
  sessionId: string;
  accommodationId: number;
  referenceDate: string;
  plan: {
    placements: Placement[];
    unresolvedReservationIds: number[];
    urgentReservationIds: number[];
    score: {
      urgentServed: number;
      served: number;
      additionalCost: number;
      moves: number;
    };
  } | null;
  offers: Offer[];
  moveHistory: (Move & { reservationId: number })[];
};
