import ReservationView from "./ReservationView";
export default async function Page({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return <ReservationView id={id} />;
}
