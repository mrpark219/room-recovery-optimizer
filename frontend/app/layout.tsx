import "./globals.css";
import Link from "next/link";
export const metadata = {
  title: "Room Recovery",
  description: "객실 운영 관리",
};
export default function Layout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ko">
      <body>
        <header className="top">
          <Link href="/" className="brand">
            Room Recovery
          </Link>
        </header>
        <main>{children}</main>
      </body>
    </html>
  );
}
