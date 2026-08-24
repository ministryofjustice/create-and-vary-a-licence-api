# How the system handles merged offender records

> This document explains, in plain language, how the system handles merged offender
> records. It is aimed at non-technical readers.
>
> This document is checked automatically for staleness: if a developer changes the
> underlying prisoner merge logic in the code, an automated test will fail until someone
> reviews this document and confirms it's still accurate (or updates it). See
> `docs-change-tracking.yaml` at the root of the repository if you want to understand
> how that check works.

## What this is for

Sometimes two prison records that were created for the same person are found to be
duplicates, and the prison system (NOMIS) merges them into a single record. When this
happens, one of the two prison identifiers (the "NOMIS ID") is retired and all future
activity continues under the other, surviving NOMIS ID, usually alongside a new "booking"
(NOMIS's term for a specific period spent in custody). The system needs to keep any
licences it holds in step with this change, so that:

- licences that were created against the identifier that's been retired end up filed
  under the surviving identifier, with up-to-date personal details, and
- licences that are no longer relevant because they belonged to an older, now-superseded
  period in custody are switched off rather than left active by mistake.

This is triggered by a "prisoner merged" notification sent by the prison system, which is
only acted on if merge handling has been switched on via a
setting [^1]

## Key terms

- **NOMIS ID**: the identifier the prison system uses for a person. Also referred to as
  a "noms number".
- **Old NOMIS ID**: the identifier that has been retired as part of the merge (the
  "removed" one).
- **New NOMIS ID**: the identifier that survives the merge and is used going forward.
- **Booking ID**: an identifier for a specific period a person has spent in custody. A
  new booking ID usually accompanies a merge.
- **CRO number**: a Criminal Records Office reference number used to identify offenders
  across police and prison records.
- **PNC number**: a Police National Computer reference number, another identifier used
  to cross-reference offenders.

## What happens when a merge notification is received

When the notification arrives, the system looks at every licence it holds for the old
NOMIS ID [^2].
If there are none, nothing further happens.

If licences are found, they are split into two groups depending on whether they belong
to the same booking that the merge notification says is now the current one:

- licences that belong to a **different, older booking** are treated as no longer
  relevant to the person's current time in custody, and
- licences that already belong to the **new, current booking** are treated as still
  relevant and are simply updated with the person's latest details [^3].

### Licences on an old booking are switched off

Any licence found on a different, older booking is deactivated, so it's no longer
treated as a live licence. This is recorded as a system action, with a note explaining
it was deactivated because of a prisoner merge [^4].

### Licences on the current booking are refreshed with the latest details

Any licence that already belongs to the new, current booking is kept active, but is
updated so that its details match the surviving identity. This includes:

- swapping the old NOMIS ID for the new one,
- refreshing the person's first name, middle name(s), surname and date of birth from
  the prison system,
- refreshing which prison the person is held at,
- refreshing the CRO number, and
- refreshing the PNC number from probation records [^5].

Before and after values for every one of these fields are recorded in an audit trail, so
there's a clear record of exactly what changed on the licence as a result of the merge
[^6].

## Where the CRO number comes from

When refreshing a licence's CRO number, the system prefers the CRO number held by
probation records over the one held by the prison system, but only if it looks like a
genuinely valid CRO number; if neither source has a valid-looking value, the CRO number
on the licence is cleared [^7].

## How it all comes together

A single merge notification can affect several licences at once, if the same person had
more than one licence recorded against their old identifier. Each affected licence is
routed independently: it's either deactivated (if it belongs to a booking that's now
been superseded) or refreshed with the latest personal details (if it belongs to the
booking the person is currently on). This means a person's licence history stays intact
under their surviving identifier, while any licences tied to the retired identity that
are no longer relevant are cleanly switched off rather than left active under
out-of-date details.

## References

[^1]. [PrisonerMergedHandler.kt:25-30](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L25-L30) — merge handling only runs if switched on via the `prisoner.merged.handler.enabled` setting
[^2]. [PrisonerMergedHandler.kt:48-49](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L48-L49) — licences are looked up by the old (removed) NOMIS ID
[^3]. [PrisonerMergedHandler.kt:51-57](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L51-L57) — licences are split into "old booking" and "new booking" groups and handled differently
[^4]. [PrisonerMergedHandler.kt:61-65](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L61-L65) — licences on an old booking are deactivated with a reason recorded
[^5]. [PrisonerMergedHandler.kt:67-104](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L67-L104) — licences on the new booking have their NOMIS ID and personal details refreshed
[6^]. [PrisonerMergedHandler.kt:75-99](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L75-L99) — before/after values for each field are recorded for audit purposes
7. [PrisonerUpdatedHandler.kt:456-464](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerUpdatedHandler.kt#L456-L464) — the rule for choosing between the probation and prison CRO numbers
