import Script from "next/script";

export default function GoogleAdSenseScript() {
  return (
    <Script
      id="google-adsense"
      async
      src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-8033378933696766"
      crossOrigin="anonymous"
      strategy="afterInteractive"
    />
  );
}
