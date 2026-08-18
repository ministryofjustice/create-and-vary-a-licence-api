# Scheduled background jobs

> This document explains, in plain language, what each of CVL's (Create and Vary a
> Licence) automatic background jobs does. These are tasks that run on a timer, rather
> than being triggered by someone using the service, to keep licences up to date and to
> nudge people to act when needed. It is aimed at non-technical readers.
>
> This document is checked automatically for staleness: if a developer changes the
> underlying job logic in the code, an automated test will fail until someone reviews
> this document and confirms it's still accurate (or updates it). See
> `docs-change-tracking.yaml` at the root of the repository if you want to understand
> how that check works.

## What this is for

A licence's situation can change even when nobody is actively working on it — someone's
release date arrives, a deadline passes, or a colleague forgets to come back to
something. CVL runs a set of jobs on a recurring schedule to react to exactly these
kinds of things automatically, without needing a person to notice and act first. Each
job does one specific thing, such as: activating a licence on release day, clearing up
licences that are no longer needed, or sending a reminder email.

## At a glance

| Job                                             | Runs                                | What it does, in a sentence                                                                                     |
|--------------------------------------------------|---------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| Prompt licence creation                          | 9am every Monday                       | Emails probation practitioners about upcoming releases they need to create a licence for.                        |
| Deactivate HDC licences                          | 3am every day                          | Cancels draft HDC licences for people who are no longer realistically going to be released on HDC.               |
| Activate licences                                | 3am, 9am, 12pm, 3pm and 6pm every day  | Switches approved licences on when someone is actually released, and cancels the ones made redundant by HDC.      |
| Deactivate licences past release date            | 2am, Monday to Friday                  | Cancels draft/in-progress licences for people who've already been released without one being finished in time.   |
| Expire licences                                  | 4am every day                          | Switches off active licences once their licence (or top-up supervision) period has ended.                        |
| Warn licence review overdue                      | 8am, Monday to Friday                  | Reminds probation practitioners to review certain licences that were activated without their usual involvement.  |
| Time out licences                                | 1:30am, Monday to Friday               | Catches licences that have quietly entered their "hard stop" period and times them out.                          |
| Notify probation of unapproved licences          | 2am every day                          | Reminds probation practitioners about approved licences that were edited but never re-approved before release.   |
| Deactivate progression licences                  | 3:30am, once a year on 1 December      | Cancels old-policy-version licences that should now be recreated under the newer policy.                          |

## Prompt licence creation

**Runs**: 9am every Monday.

This job looks ahead to everyone due for release in the **next four weeks**, and works
out who among them still needs a licence created. It excludes anyone who:

- Already has a licence in progress or further along, or
- Is not eligible for a CVL licence, or
- Has no probation practitioner recorded against their case (or no email address for
  them), or
- Already falls inside their "hard stop" period (see `docs/licence-start-date.md`) — by
  this point, the usual "please create a licence" prompt is no longer relevant, since
  prison staff take over instead.

Everyone left is grouped by their probation practitioner, and each practitioner is sent
a single email listing all their upcoming cases that need a licence creating, sorted by
release date — giving them advance notice, rather than everyone finding out separately
at the last minute.

## Deactivate HDC licences

**Runs**: 3am every day.

This job looks for **draft HDC (Home Detention Curfew) licences** — ones still being
prepared, submitted, or approved but not yet active — where the person's conditional
release date is **due within the next 9 days, or has already passed**. Since HDC release
is meant to happen *before* someone's standard release date, a conditional release date
this close (or already gone) means an HDC release is no longer realistically going to
happen. These draft HDC licences are automatically deactivated so they don't linger as
stale, incorrect drafts.

## Activate licences

**Runs**: 3am, 9am, 12pm, 3pm and 6pm every day.

This job looks for **approved licences** whose licence start date is **today or
earlier** — i.e. release day has arrived or passed — and haven't yet been switched on.
For each one, it checks the person's current HDC (Home Detention Curfew) status, then:

- **Activates** the licence once it's clear the person has genuinely been released on
  this basis — for IS91/extradition cases, once their licence start date has passed;
  for everyone else, once their licence start date has passed *and* the prison's own
  records show them as released.
- **Deactivates** an approved standard licence instead, if it turns out the person has
  since been approved for release on HDC — in that case, the HDC licence takes over,
  and the standard one is no longer needed.
- Leaves alone anyone whose HDC status is still undecided, to avoid activating or
  cancelling the wrong licence too early.

## Deactivate licences past release date

**Runs**: 2am, Monday to Friday.

This job looks for licences that are **still being drafted, submitted, or have already
timed out** (excluding HDC and "time served" licences, which are handled differently)
where the **licence start date has already passed**. If a licence hasn't been finished
by the time someone is released, it's no longer useful in that form, so it's
automatically deactivated.

## Expire licences

**Runs**: 4am every day.

This job looks for **active** licences that have now run past the end of their licence
period:

- Standard licences, once they're past their **licence expiry date**.
- Licences that include a top-up supervision period, once they're past their **top-up
  supervision expiry date** instead (this is the later of the two dates for those
  licence types, so it's used as the true end point).

These licences are automatically switched off, since the person is no longer subject to
licence conditions at that point.

## Warn licence review overdue

**Runs**: 8am, Monday to Friday.

Some licences (hard stop licences, and "time served" licences) are created without
probation's usual full involvement beforehand, so they're expected to be reviewed by the
responsible probation practitioner shortly after the person is released. This job checks
for licences of this kind that were **activated exactly 5 days ago** and still haven't
been reviewed, and sends the practitioner a reminder email to do so.

## Time out licences

**Runs**: 1:30am, Monday to Friday (it does nothing at all on weekends or bank
holidays).

Licences can move into their "hard stop" period at any time — for example, as a side
effect of someone's dates changing (see `docs/sentence-date-updates.md`). This job acts
as a daily safety net: it looks at standard and recall licences that are still **in
progress**, with a licence start date due within the next two weeks, and — of those —
finds any that have now genuinely entered their hard stop period. Any it finds are
**timed out**, just as if this had been caught immediately when the date change
happened.

## Notify probation of unapproved licences

**Runs**: 2am every day.

If a licence was previously **approved**, but has since been **edited** (for example,
because something changed) and hasn't been **re-approved** by the time the person is
due for release, this job sends the responsible probation practitioner a reminder email.
This exists to make sure an edited-but-unapproved licence doesn't slip through
unnoticed right up to release day.

## Deactivate progression licences

**Runs**: 3:30am, once a year, on 1 December.

CVL introduced a newer version of its licence conditions ("policy version 4"), which
applies from a configured go-live date onwards. This job looks for licences that are
**still on an older policy version** (one of the four versions before policy version 4)
and are **still in progress, submitted, approved, or timed out**, but whose **licence
start date now falls on or after** the go-live date. Since these cases should really be
recreated under the current policy version, the old-version licences are automatically
deactivated. If the responsible probation practitioner is known, and the licence start
date falls within a configured notification window, they're also emailed to explain that
this has happened. (If the go-live date hasn't been configured at all, this job does
nothing.)

This is effectively the mirror image of the "policy version safeguard" described in
`docs/sentence-date-updates.md` — that one catches newer-policy-version licences moving
to start *before* the go-live date; this one catches older-policy-version licences that
are still around once their start date has moved to *on or after* it.

## A note on other endpoints in the same part of the codebase

Two further endpoints exist alongside these scheduled jobs but are **not** run on a
timer — they're triggered on demand instead: one migrates standard licence conditions
to a newer wording, and one manually recalculates licence start dates for a given set of
cases. As they aren't part of the automatic schedule, they're out of scope for this
document.
