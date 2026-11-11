import Link from "next/link";

export default function OutageTabs({ id, history = false }: { id: string; history?: boolean }) {
  const path = `/accommodations/${id}/outages`;
  return (
    <nav className="tabs" aria-label="객실 장애 관리">
      <Link className={history ? "" : "active"} href={path} aria-current={history ? undefined : "page"}>
        장애 대응
      </Link>
      <Link className={history ? "active" : ""} href={`${path}/history`} aria-current={history ? "page" : undefined}>
        복구 이력
      </Link>
    </nav>
  );
}
