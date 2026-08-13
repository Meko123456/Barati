# Barati 🗂️

**ბარათი** (*barati* — Georgian for "card") — a spaced-repetition flashcards app
built with **Kotlin Multiplatform Mobile (KMM)**.

The interesting part is the architecture. Rather than sharing UI, Barati shares
only the **business logic in Kotlin** and keeps each platform's UI **fully
native**:

- 🧠 **Shared Kotlin** (`shared/`) — flashcard/deck models, the SM-2 spaced-
  repetition engine, decks, and the study session logic. One source of truth,
  unit-tested once.
- 🤖 **Android** — native **Jetpack Compose** UI.
- 🍎 **iOS** — native **SwiftUI / Swift** UI, consuming the shared framework.

This deliberately shows two things at once: real multiplatform code sharing
*and* idiomatic, native UI on each platform (no shared-UI compromise).

## Structure

```
shared/      Kotlin Multiplatform module (commonMain domain + androidMain/iosMain)
androidApp/  Android app — Jetpack Compose
iosApp/      iOS app — SwiftUI (links the shared framework)
```

## Why native UI over shared UI?

Shared UI (Compose Multiplatform) is great, but native UI proves you can deliver
the look-and-feel each platform expects — SwiftUI navigation, gestures, and
system integration on iOS; Material 3 on Android — while still sharing the hard
part (domain logic, persistence, algorithms) exactly once.

## Status

🚧 Day 1 — README-first. See [issues](../../issues) for the roadmap.

## License

[MIT](LICENSE)
