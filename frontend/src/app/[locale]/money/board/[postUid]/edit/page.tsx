import { notFound } from "next/navigation";
import { PostFormView } from "@/board/components/BoardViews";
export default async function EditBoardPostPage({ params }: { params: Promise<{ postUid: string }> }) { const { postUid } = await params; const uid = Number(postUid); if (!Number.isSafeInteger(uid) || uid < 1) notFound(); return <PostFormView uid={uid} />; }
