# Licence eligibility rules

> This document explains, in plain language, how the system decides whether someone in
> prison is eligible for a licence to be created in CVL (Create and Vary a Licence), and
> which type of licence applies. It is aimed at non-technical readers.
>
> This document is checked automatically for staleness: if a developer changes the
> underlying eligibility logic in the code, an automated test will fail until someone
> reviews this document and confirms it's still accurate (or updates it). See
> `docs-change-tracking.yaml` at the root of the repository if you want to understand
> how that check works.

## What this is for

Before a licence can be created for someone leaving prison, CVL needs to work out two
things automatically:

1. **Is this person eligible** to have a licence created at all?
2. If so, **what type of licence** applies to them?

There are four possible kinds of licence:

| Licence kind                    | Plain English meaning                                                                     |
|---------------------------------|-------------------------------------------------------------------------------------------|
| **Standard release**            | A normal licence tied to their conditional release date                                   |
| **Home Detention Curfew (HDC)** | An early release licence, typically involving an electronic tag/curfew                    |
| **Standard recall**             | The person was recalled to prison and is now due for re-release under a standard recall   |
| **Fixed-term recall**           | The person was recalled to prison and is due for re-release after a fixed, shorter period |

If none of these apply, the person is **not eligible** and no licence is created
automatically — a case worker would need to handle it manually, if at all.

## Key dates used in the decision

- **Conditional release date** — the date someone is due to be released from custody
  under normal conditions.
- **Post recall release date** — the date someone who was recalled to prison is due to
  be released again.
- **Parole eligibility date** — the date from which someone becomes eligible to apply
  for parole (relevant for certain longer sentence types).
- **Actual parole date** — the date someone was actually released on parole, if that
  happened.
- **Licence expiry date** and **top-up supervision expiry date** — the dates their
  licence conditions, and any follow-on supervision, come to an end.

## People who are never eligible for a licence

Regardless of anything else, someone is **not eligible for any licence type** if any of
these apply:

- They are **still eligible to apply for parole** in the future — parole cases follow a
  separate process, so this isn't handled here.
- They have **died** while in custody.
- They are serving an **indeterminate sentence** (e.g. a life sentence) — these follow a
  different, separate release process.
- Their **current prison status isn't one that supports release** (for example, they're
  not currently an active prisoner, or a recognised restricted patient).
- They are being held for a **breach of their top-up supervision** conditions — this is
  handled as a separate process, not a new licence.
- They are on a licence type that **only covers a top-up supervision period**, with no
  ordinary licence period — CVL no longer supports creating these.

## When someone is eligible for a standard release licence

Someone can get a standard release licence if all of the following are true:

- They have a **conditional release date** set.
- That date is **today or in the future** — or, if it's already passed, the case still
  qualifies as a recognised "time served" release.
- If they're on a sentence type where parole is a factor (see "extended sentences"
  below), they also need to pass that additional check.
- They are **not a recall case** (see "How a recall is identified" below).
- They are **not due to be released on HDC** instead — if they are, HDC takes priority
  (see the HDC section).

### Extra rule for certain longer/extended sentences

For people serving certain longer sentence types with a parole element, two extra checks
apply:

- If we know both their actual release date and their conditional release date, the
  actual release date must fall within a short window ending on the conditional release
  date (a few days' grace is allowed for weekends and bank holidays). If it falls outside
  that window, they're not eligible via this route.
- If they were released on parole at an earlier point in their sentence, they're
  considered to have already had their "successful parole" release, and so aren't
  eligible for a standard release licence via this route.

People **not** on this type of sentence automatically pass this extra check — it only
applies to the relevant sentence types.

## When someone is eligible for a recall licence

Someone can get a recall licence (standard or fixed-term) if all of the following are
true:

- They have a **post recall release date** set.
- That date is **today or in the future**.
- Their calculated release date under this route doesn't **land on the exact day their
  licence would otherwise expire** — if it does, a different process applies instead.
- If they're on a sentence type where parole is a factor, and they were released on
  parole at an earlier point, they're not eligible for a recall licence via this route
  (there's no "grace window" check for recalls, unlike the standard release route).
- They are **not due to be released on HDC** instead.

### How a recall is identified

The system works out whether someone should be treated as "on recall" using this logic:

1. If they have a conditional release date but **no** post recall release date, they are
   **not** treated as a recall case.
2. If they have both a conditional release date and a post recall release date, they're
   only treated as a recall case if the **post recall date is later** than the
   conditional release date.
3. If neither date is available, the system falls back to a recall flag recorded in the
   prison system. (If that flag itself is missing, this is logged as a data quality
   issue and, to avoid wrongly blocking someone, they default to **not** being treated as
   a recall.)

### Double-checking recall cases

Because the dates alone don't always tell the full story, anyone identified as a
possible recall case is double-checked against sentence and recall records held by the
prison system, to confirm what kind of recall it actually is:

- If there's **no matching sentence record at all**, they're treated as **not
  eligible** — recorded as "does not have any active sentences".
- If the record confirms it **is** a standard or fixed-term recall, the original
  decision stands, and this also determines whether they get a "standard recall" or
  "fixed-term recall" licence.
- If the record shows some **other type of recall** that CVL doesn't support, they're
  treated as **not eligible** — recorded as "on an unidentified recall type".

This extra check exists because dates alone can't always reliably distinguish a genuine
standard/fixed-term recall from other recall situations that CVL isn't designed to
handle.

## When someone is eligible for an HDC licence

HDC licence creation can be switched on or off as a whole via a system setting. If it's
switched off, **nobody** is eligible for an HDC licence, and this is recorded as "HDC
licence creation not currently supported".

If it's switched on, someone is eligible for an HDC licence if all of the following are
true:

- They have a **conditional release date** set.
- That date is **at least 10 days away** — this gives enough lead time to prepare an HDC
  licence (e.g. curfew address checks) before release.
- They **are** expected to be released on HDC, according to the prison system's HDC
  status information.

## How the final decision is made

Putting it all together, for each person:

- They must first pass **all** of the "never eligible" checks above.
- Then, they need to pass **at least one** of: the standard release checks, the recall
  checks, or the HDC checks.
- If they pass more than one, the system picks **one** overall outcome, in this priority
  order:
    1. **Standard release** licence, if eligible via that route.
    2. **HDC** licence, if not eligible for standard release but eligible for HDC.
    3. **Recall** licence (standard or fixed-term, as determined by the recall
       double-check above), if not eligible via either of the above.
- If none of the routes apply, the person is recorded as **not eligible**, along with
  all the specific reasons why (e.g. "has died", "is a recall case", "HDC licence
  creation not currently supported").

## A couple of important edge cases

- Anyone with **no active prison booking recorded** is immediately marked as not
  eligible ("no active booking") — none of the other checks are run for them.
- If certain data (like whether someone is on an indeterminate sentence, or the recall
  flag) is missing from the prison system, this is logged as a data quality issue for
  investigation, and the system defaults to **not** blocking someone's eligibility
  because of missing data.
