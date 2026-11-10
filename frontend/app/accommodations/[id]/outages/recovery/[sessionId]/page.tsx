import Recovery from "./RecoveryView";
export default async function Page({
  params,
}: {
  params: Promise<{ id: string; sessionId: string }>;
}) {
  const { id, sessionId } = await params;
  return <Recovery id={id} sessionId={sessionId} />;
}
