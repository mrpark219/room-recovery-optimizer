import RoomView from "./RoomView";
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <RoomView id={id} />;
}
