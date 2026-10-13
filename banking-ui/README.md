# banking-ui

Web-Oberfläche des Banking Systems: React 19, TypeScript, Vite und Tailwind CSS.

```bash
npm install
npm run dev     # Entwicklungsserver auf http://localhost:5173
npm run build   # Produktions-Build nach dist/
npm run lint    # ESLint
```

Alle Anfragen an `/api` werden vom Vite-Entwicklungsserver an das API-Gateway (`http://localhost:8080`) weitergeleitet (siehe `vite.config.ts`). Das Backend muss dafür laufen.

| Ordner | Inhalt |
|---|---|
| `src/api/` | API-Client und Typen der Backend-DTOs |
| `src/components/` | Seiten (Überweisung, Kontoabfrage, Konto eröffnen) und UI-Bausteine |
| `src/hooks/` | `useSavedAccounts`: im Browser eröffnete Konten zur Schnellauswahl |
| `src/utils/` | Formatierung von Beträgen, Datum und Kontonummern |

Weitere Informationen stehen in der [Haupt-README](../README.md).
