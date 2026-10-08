import { notFound } from "next/navigation";
import { PostDetailView } from "@/board/components/BoardViews";
export default async function BoardPostPage({ params }: { params: Promise<{ postUid: string }> }) { const { postUid } = await params; const uid = Number(postUid); if (!Number.isSafeInteger(uid) || uid < 1) notFound(); return <PostDetailView uid={uid} />; }
