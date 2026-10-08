import { Suspense } from "react";
import { BoardListView } from "@/board/components/BoardViews";
export default function BoardPage() { return <Suspense fallback={<p role="status">게시판을 불러오는 중...</p>}><BoardListView /></Suspense>; }
