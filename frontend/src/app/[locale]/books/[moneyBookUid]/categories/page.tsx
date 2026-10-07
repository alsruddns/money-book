import { notFound } from "next/navigation";
import CategoryList from "@/category/components/CategoryList";
import { parseMoneyBookUid } from "@/moneybook/parseMoneyBookUid";

export default async function CategoriesPage({ params }: { params: Promise<{ moneyBookUid: string }> }) {
  const { moneyBookUid } = await params;
  const uid = parseMoneyBookUid(moneyBookUid);
  if (uid === null) notFound();
  return <CategoryList moneyBookUid={uid} />;
}
