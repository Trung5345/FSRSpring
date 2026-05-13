import type { Metadata } from "next";
import Script from "next/script";
import "./globals.css";
import { ToastProvider } from "@/components/ui/toast";

export const metadata: Metadata = {
  title: "FSRSpring Vocabulary",
  description: "Smart vocabulary learning with FSRS scheduling"
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="vi" data-scroll-behavior="smooth" suppressHydrationWarning>
      <body suppressHydrationWarning>
        <Script
          id="remove-extension-hydration-attrs"
          strategy="beforeInteractive"
          dangerouslySetInnerHTML={{
            __html: `
              (function () {
                var extensionAttrs = [
                  'bis_skin_checked',
                  'data-new-gr-c-s-check-loaded',
                  'data-gr-ext-installed'
                ];
                function clean() {
                  document.querySelectorAll('[bis_skin_checked], [data-new-gr-c-s-check-loaded], [data-gr-ext-installed]').forEach(function (node) {
                    extensionAttrs.forEach(function (attr) {
                      node.removeAttribute(attr);
                    });
                  });
                }
                clean();
                new MutationObserver(clean).observe(document.documentElement, {
                  attributes: true,
                  subtree: true,
                  attributeFilter: extensionAttrs
                });
              })();
            `
          }}
        />
        <ToastProvider>
          {children}
        </ToastProvider>
      </body>
    </html>
  );
}
