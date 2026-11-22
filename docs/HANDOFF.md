# Short summary (`spyhound-local-handoff`)

A small JSON summary of one check that a person can share with another app through Android's share sheet
("Share report" → "Short summary"). Format proposed by Damadori ([@IA-DAMA](https://github.com/IA-DAMA)) in
[issue #3](https://github.com/BEKO2210/spyhound/issues/3). Code: `app/src/main/kotlin/de/belkis/spyhound/scan/ReportHandoff.kt`.

```json
{
  "format": "spyhound-local-handoff",
  "format_version": 1,
  "spyhound_version": "0.3.17",
  "checked_at_utc": "2026-11-19T12:00:00Z",
  "check_depth": "basic",
  "counts": { "findings": 1, "leads": 2, "gaps": 2 },
  "findings": [
    { "indicator_name": "BadBazaar", "indicator_source": "eset-gref" }
  ],
  "gaps": ["apps.needs_permission_files", "deep_check.needs_shizuku"],
  "notice": "Unsigned user-shared snapshot, not device attestation. No findings never means a clean device."
}
```

*Example values, not a real check.*

## Fields

| Field | Meaning |
|---|---|
| `check_depth` | `basic` (no special access), `extended` (Shizuku or built-in ADB), `root` |
| `checked_at_utc` | Start of the check, from the phone's own clock (not trusted time) |
| `counts` | Findings, leads and gaps, counted separately and before de-duplication |
| `findings` | One entry per published label: the family name the source gives, and the source id from `sources.json`. Two matches of the same app (package name and certificate) are one entry |
| `gaps` | Stable codes `<check>.<reason>`: `needs_…` names what would close the gap (`needs_shizuku`, `needs_root`, `needs_permission_files` …). Missing, outdated or damaged indicator sources use `indicators.needs_update_indicators`. Without a fix, the reason names the gap kind (`large_files`, `read_failed` …) |

## What it never contains

The installed apps, the device model, paths, logs, SMS senders, lead texts, or the matched value itself (package
name, domain, hash). A gap is a code, never its text, because a gap text can name a folder.

## Limits a receiver must respect

- **User action only.** Spyhound never sends it on its own: no background transfer, no server, no account.
- **Untrusted by design.** It is plain, unsigned text: it can be edited, replayed or made up. Treat it as advice,
  never as proof that a check ran or what it found.
- **Advisory only.** A receiver may warn and show the gaps; it must never call a phone safe because `findings` is
  empty, and never decide on the person's behalf.
