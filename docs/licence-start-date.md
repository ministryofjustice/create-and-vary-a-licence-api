# Licence start date (LSD)

> This document explains, in plain language, how the system works out the **licence
> start date** — the date someone's licence period actually begins — along with the
> related "hard stop" dates that control when the licence stops being editable ahead of
> release. It is aimed at non-technical readers.
>
> This document is checked automatically for staleness: if a developer changes the
> underlying licence start date logic in the code, an automated test will fail until
> someone reviews this document and confirms it's still accurate (or updates it). See
> `docs-change-tracking.yaml` at the root of the repository if you want to understand
> how that check works.

## What this is for

Once someone has been found eligible for a licence (see `docs/eligibility.md`), CVL
needs to work out exactly **which date their licence starts on**. This is called the
**licence start date (LSD)**.

The LSD matters because:

- It's the date the licence conditions actually take effect.
- It drives a "hard stop" period shortly beforehand, during which the licence can no
  longer be edited by probation and is instead finalised by prison staff, to make sure
  a licence is always ready in time for release.
- It's used to work out whether someone is being released "early" (e.g. because their
  proper release date falls on a weekend) and whether extra warnings need to be sent to
  probation about late allocation of a licence.

How the LSD is calculated depends on **which type of licence** the person is getting.

## Key dates used in the calculation

- **Conditional release date (CRD)** — the date someone is due to be released from
  custody under normal conditions.
- **Confirmed release date** — the actual release date recorded by the prison, once
  known. This can differ slightly from the CRD (for example, if it's been adjusted).
- **Post recall release date (PRRD)** — the date someone who was recalled to prison is
  due to be released again.
- **Home Detention Curfew (HDC) actual date / eligibility date** — the date someone was
  actually approved for release on HDC, or (if that's not yet known) the date they
  become eligible to be considered for it.
- **Sentence start date** — the date someone's current sentence began.

## How the licence start date is calculated, by licence type

Across all three routes below, whenever the confirmed release date ends up being used
directly, it's used exactly as recorded — **even if it falls on a weekend or bank
holiday**. So if the reference date (conditional release date, or post recall release
date) and the confirmed release date are the same non-working day, that day is used as
the licence start date as-is, rather than being pulled back to the last working day
beforehand.

### Standard release licences (based on the conditional release date)

For most people, the licence start date is based on their **conditional release date**,
compared against their **confirmed release date**:

- If there's no confirmed release date yet, or the confirmed release date is *later*
  than the conditional release date, the licence start date is the conditional release
  date itself — but adjusted to fall on a working day if it lands on a weekend or bank
  holiday (see "Moving dates onto a working day" below).
- Otherwise (the confirmed release date is known and is on or before the conditional
  release date), the licence start date is simply the **confirmed release date**.

**A special case applies for people in one of these situations**, because for them a
confirmed release date can't always be trusted as the true release date:

- They're recorded as an **immigration detainee**, on **remand**, or **convicted but not
  yet sentenced** (based on their legal status), or
- They're identified as an **IS91 or extradition case** (broadly: people held for
  immigration/deportation reasons, or awaiting extradition), based on separate
  information held about their case.

  > **A note on remand cases**: although the calculation below already accounts for
  > remand cases, CVL does not currently support remand cases end-to-end elsewhere in
  > the system (for example, people on remand don't typically have a conditional release
  > date yet, since their sentence hasn't been determined) — so in practice this part of
  > the calculation is the beginnings of future support, rather than something currently
  > relied on.

For these people, the calculation is slightly different:

- If there's no confirmed release date, or the confirmed release date falls *before* the
  (working-day-adjusted) conditional release date, or *after* the conditional release
  date itself, the licence start date is the conditional release date, adjusted onto a
  working day.
- Otherwise, the licence start date is the confirmed release date.

### Recall licences — standard or fixed-term (based on the post recall release date)

For people on a recall licence, the same style of comparison is made, but using the
**post recall release date** instead of the conditional release date:

- If there's no confirmed release date, or the confirmed release date is *later* than
  the post recall release date, or the confirmed release date falls on or before the
  conditional release date, the licence start date is the post recall release date,
  adjusted onto a working day.
- Otherwise, the licence start date is the confirmed release date.

### HDC (Home Detention Curfew) licences

For HDC licences, the licence start date is simply:

- The **HDC actual date**, if it's known, or
- If not, the **HDC eligibility date** instead.

No working-day adjustment is applied for HDC dates — these dates already reflect a
planned/curfew release date rather than a standard custodial release date.

## Moving dates onto a working day

Prisons don't release people on weekends or bank holidays. So whenever a calculated
licence start date would otherwise fall on a non-working day, the system moves it back
to the **last working day before that date**. This "last working day" adjustment is
applied consistently across the standard release and recall calculations described
above, and is also the first step used when working out the hard stop date below.

## Is this an "early" release?

Someone's release is treated as **early** if their licence start date falls on a
**Friday, Saturday, or Sunday** (Friday is deliberately included here, alongside the
weekend, because it can shift the practical release timing), or on any other day the
system recognises as a **non-working day** (such as a bank holiday).

This "early release" flag is used, alongside the number of working days configured for
early release (**3 working days by default**), to work out the **earliest possible
release date** someone could actually be released on — except this doesn't apply to
someone being released on HDC, where the actual HDC date is always used instead.

When counting back those working days, **Fridays are skipped as well** as weekends and
bank holidays — consistent with Friday releases being treated as "early" above. This is
a different (stricter) count than the plain working-day countback used for the hard stop
dates below, which does treat Friday as a normal working day.

## The "hard stop" period

Shortly before someone's licence start date, CVL enters a **hard stop period**. During
this window, probation staff can no longer make changes to the licence — instead, the
licence is finalised using the details already in place, to guarantee something is
ready in time for release. (Note: this doesn't apply to "time served" licences, where a
different process handles it.)

- The **hard stop date** — when the hard stop period begins — is calculated as
  **2 working days before** the licence start date. If the licence start date itself
  falls on a non-working day, it's first pulled back to the working day before it,
  and *then* the 2-working-day countback happens from there.
- The hard stop period runs from the **hard stop date** up to and including the
  **licence start date** itself.
- A **hard stop warning date** is also calculated, as a further **2 working days before**
  the hard stop date — this gives probation an early heads-up that the hard stop period
  is approaching.

## Late allocation warning

If a licence hasn't been allocated to a probation practitioner with enough lead time
before release, this can cause problems. To help catch this, the system works out a
**late allocation warning date**: **5 working days before** the release date (by
default), using the same Friday-skipping working-day count described above. Once
today's date reaches or passes that warning date — and the release date itself hasn't
already passed — the case is flagged as needing a late allocation warning.

## "Time served" cases

Some people are released on the very day their sentence starts (for example, where time
spent on remand already covers their sentence). This is identified as a **time served**
case when:

- Their **sentence start date** is the same as their **conditional release date**, and
- That conditional release date is **recent** — specifically, within a configurable
  number of days before today (28 days, by default).

Time served cases are handled as their own licence kind, and are exempt from the hard
stop period described above.
