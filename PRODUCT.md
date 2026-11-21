# Product

<!-- impeccable:product-schema 1 -->

## Platform

adaptive

One shared Compose Multiplatform UI (Material 3 components) for Android, iOS and Desktop. The
owner's main device is an iPhone.

## Users

Lifters who train with free weights and machines in a gym, logging between sets with one hand,
often sweaty, glancing at the phone for a few seconds. The owner is the first user.

## Product Purpose

An offline-first strength-training log. You log **per exercise, not per set**: do all your sets,
then save them together in a "Batch Log Sheet" that is already filled in from last time. Success
is logging a whole session in a few taps without breaking focus, and seeing progress (PRs,
charts, streaks) afterwards.

## Positioning

Batch logging with pre-fill: one tap (or one swipe) logs "same as last time"; the sheet is only
for changes. Other trackers make you tick every set.

## Operating Context

Gym floor (bright light, glare, sometimes dark mode at night), rest periods of 1–3 minutes,
routines that rotate, a rest timer, plate maths for barbell work. History and charts are read at
home.

## Capabilities and Constraints

Exercise library (129 bundled exercises with images, form cues), active workout, Batch Log
Sheet, PRs, routines, history, progress charts, Home streak / heatmap / weekly goal, sets per
muscle, rest timer, plate calculator, warm-ups, supersets, body measurements. Offline only for
now; backend-ready sync schema. Dynamic color on Android 12+, dark mode, adaptive layouts
(bar / rail / drawer).

## Product Principles

1. Fewest taps wins: the common path (log same as last time) is one gesture.
2. Glanceable mid-set: big numbers, high contrast, nothing to read twice.
3. Offline and honest: the local data is the truth; never fake progress.
4. Accessible by default: labelled controls, screen-reader actions for every gesture, 56 dp
   primary actions.

## Accessibility & Inclusion

Every icon button labelled, headings marked, swipe/drag gestures mirrored as accessibility
actions or menu items, primary actions at least 56 dp.

<!-- Inferred from README.md, DECISIONS.md and CLAUDE.md; not yet confirmed in an interview. -->
