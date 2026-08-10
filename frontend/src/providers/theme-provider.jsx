import { ThemeProvider as NextThemesProvider } from "next-themes";

/**
 * Global theme provider — wraps the app to enable light/dark/system mode.
 * Uses the `class` attribute strategy (adds `.dark` to <html>) which
 * is what our CSS `@custom-variant dark` expects.
 */
export function ThemeProvider({ children, ...props }) {
  return (
    <NextThemesProvider
      attribute="class"
      defaultTheme="system"
      enableSystem
      disableTransitionOnChange
      {...props}
    >
      {children}
    </NextThemesProvider>
  );
}
