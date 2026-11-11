import HistoryView from "./HistoryView";

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <HistoryView id={id} />;
}
