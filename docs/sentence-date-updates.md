# What happens when someone's sentence dates change

> This document explains, in plain language, what CVL (Create and Vary a Licence) does
> when the prison system reports updated sentence or release dates for someone who
> already has a licence in progress. It is aimed at non-technical readers.
>
> This document is checked automatically for staleness: if a developer changes the
> underlying date-update logic in the code, an automated test will fail until someone
> reviews this document and confirms it's still accurate (or updates it). See
> `docs-change-tracking.yaml` at the root of the repository if you want to understand
> how that check works.

## What this is for

Sentence and release dates aren't fixed once a licence has started being prepared —
they can change at any time, for all sorts of reasons (a court appearance changes a
sentence, a recall is added or removed, an administrative correction is made, and so
on). Whenever the prison system reports a change to someone's dates, and that person
already has a CVL licence being progressed, CVL needs to react sensibly. This covers
four things:

1. **Recalculating the licence start date** and refreshing all the other dates held
   against the licence, so they match what the prison system now says.
2. **Working out which changes actually matter** enough to be worth telling anyone
   about.
3. **Telling the responsible probation practitioner (COM)** about meaningful changes,
   so they're not caught out.
4. **Automatically protecting the licence process** — moving a licence into or out of
   the "hard stop" period, or (in some cases) stopping the licence altogether — if the
   date change means the existing licence can no longer safely be used.

## Key terms

- **Licence start date (LSD)** — the date the licence period begins (see
  `docs/licence-start-date.md` for how this is calculated).
- **Conditional release date, confirmed release date, post recall release date** — see
  `docs/licence-start-date.md` for what these mean.
- **Licence expiry date (LED)**, **sentence end date (SED)**, **sentence start date**,
  **top-up supervision start/end dates** — the other key sentence dates held against a
  licence, alongside the HDC-specific dates (HDC actual date, HDC end date, HDC
  eligibility date) for people on a Home Detention Curfew licence.
- **Responsible probation practitioner (COM)** — the probation officer currently
  assigned to the case, who is notified about changes that affect them.
- **Hard stop period** — the short window before someone's release where the licence
  can no longer be edited by probation (see `docs/licence-start-date.md`).

## Step 1: Recalculating and refreshing the dates

Whenever this process runs for a licence:

- The licence's **eligibility and licence kind are re-checked** against the latest
  prison data (for example, someone might now be a recall case, or now eligible for
  HDC, when they weren't before). If the person is no longer eligible for a licence at
  all, no kind change is made — instead, the licence start date ends up unset, which
  causes the case to appear on the prison's "attention needed" list, prompting someone
  to correct the prison data.
- The **licence start date is recalculated** from scratch using the latest data.
- **Every other date held on the licence** (conditional release date, confirmed release
  date, licence expiry date, sentence start/end dates, top-up supervision dates, post
  recall release date, and — for HDC licences only — the HDC actual/end/eligibility
  dates) is compared against the newly reported values and updated to match.
- The licence's **status may also be adjusted**: for example, an already-**active**
  licence is automatically marked **inactive** if the person's release date has moved
  into the future (they're no longer being released as previously thought), unless it's
  a top-up-supervision-only licence being affected by a later post recall release date.

## Step 2: Working out which changes actually matter

Not every date change is significant enough to act on. Each date type is individually
classed as either:

- **Notifiable** — worth telling the probation practitioner about if it changes (most
  date types), or
- **Not notifiable** — changes are still recorded, but don't by themselves trigger a
  notification (the conditional release date, confirmed release date, and sentence
  start date fall into this category, since they're often stepping stones to other
  dates changing, rather than something the practitioner needs to react to directly).

A change is considered **material** (significant enough to potentially notify someone
about and to check the automatic protections in Step 4) if any notifiable date has
changed — **with one exception**: a change to the **sentence end date** on its own is
only treated as material if the licence has already reached **approved** status; before
that point, a sentence end date change alone isn't treated as significant.

HDC-specific dates (HDC actual/end/eligibility dates) are only ever considered relevant
for people on an HDC licence.

## Step 3: Telling the responsible probation practitioner

If the changes are material, the responsible probation practitioner is sent an email
listing what's changed — **unless**:

- There's **no responsible practitioner assigned** to the case yet (nothing to notify,
  though this is still logged), or
- The person **is not on an HDC licence**, but **is now approved for HDC release** —
  in this situation, the change isn't notified, on the basis that a more relevant HDC
  conversation is about to happen instead.

## Step 4: Automatically protecting the licence process

Two independent safeguards can automatically change what happens to the licence,
depending on how the dates have shifted:

### Moving into the hard stop period unexpectedly

If a licence is currently still being worked on by probation (**in progress**) and
the date change means it has now unexpectedly entered its **hard stop period**, the
licence is automatically **timed out** — probation can no longer edit it, and it falls
to prison staff to finalise it in time for release, in the same way as if it had
naturally reached the hard stop period.

### Moving back out of the hard stop period

If a licence was previously in its hard stop period, but the date change means it's now
**no longer** in that period (e.g. the release date has moved further away again), the
in-flight licence for that person may need to be scrapped so a fresh one can be created
under normal (non-hard-stop) rules:

- If this behaviour is switched on, the case is instead flagged as a **"potential hard
  stop case"** and left alone for now. A separate scheduled check comes back **8 hours
  later** and only deactivates the in-flight licences at that point if the case is
  *still* not back in its hard stop period (this avoids reacting to a date change that
  gets reversed shortly afterwards).
- If that flagging behaviour is switched off, the in-flight licences are deactivated
  **immediately**.

### Policy version safeguard

Separately, if a licence was created under a specific older policy version (version
4.0) and is still at a pre-release stage, and the date change causes the licence start
date to move to before a configured cut-off date, the licence is automatically **deactivated**, so that the case can be
re-created under the correct, earlier policy version instead. In this case, the
responsible practitioner is notified separately that this has happened, rather than
being sent the usual "dates changed" notification.

## A couple of important edge cases

- If someone becomes **ineligible** for a licence as a result of the date change, no
  licence kind change is applied, and the licence start date is left unset — this is a
  deliberate signal for prison staff to investigate and correct the underlying data,
  rather than the system guessing what to do.
- For **post recall release date (PRRD) licences**, if a post recall release date is
  newly added where there wasn't one before, or is removed entirely, this is logged
  explicitly, since it usually reflects a recall being added to, or removed from,
  someone's case.
- Every date change (even ones that aren't "material") is recorded as an audit event
  against the licence, so there's always a history of what changed and when — but only
  when at least one date has actually changed value.
