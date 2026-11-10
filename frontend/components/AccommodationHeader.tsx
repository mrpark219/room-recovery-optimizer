import Link from "next/link";
export default function AccommodationHeader({
  id,
  name,
  tab,
}: {
  id: string;
  name: string;
  tab: "reservations" | "outages";
}) {
  return (
    <>
      <Link className="back" href="/">
        ‹ &nbsp;숙소 목록
      </Link>
      <h1>{name || "숙소 관리"}</h1>
      <nav className="tabs" aria-label="숙소 관리">
        <Link
          className={tab === "reservations" ? "active" : ""}
          aria-current={tab === "reservations" ? "page" : undefined}
          href={`/accommodations/${id}`}
        >
          예약 내역
        </Link>
        <Link
          className={tab === "outages" ? "active" : ""}
          aria-current={tab === "outages" ? "page" : undefined}
          href={`/accommodations/${id}/outages`}
        >
          객실 장애 관리
        </Link>
      </nav>
    </>
  );
}
