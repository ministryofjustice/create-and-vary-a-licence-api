# How the system handles a prisoner record merge

> This document explains, in plain language, how the system handles merged offenders. It is
> aimed at non-technical readers.
>
> This document is checked automatically for staleness: if a developer changes the
> underlying prisoner-merge logic in the code, an automated test will fail until someone
> reviews this document and confirms it's still accurate (or updates it). See
> `docs-change-tracking.yaml` at the root of the repository if you want to understand
> how that check works.

## What this is for

Sometimes the prison system discovers that two prison records actually belong to the same
person - for example, someone was booked in under two different identities, or a data entry
error created a duplicate record. When this happens, the prison system "merges" the two
records: one identity (the "old" one) is retired, and everything is consolidated under the
"new" identity going forward.[1](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L29-L33)

When this merge happens, this service is told about it, so that any licences linked to the
old identity are moved across to the new one, and the person's personal details on those
licences are refreshed to match the latest confirmed record. Without this, a licence could
be left pointing at an identity that no longer exists, or could show out-of-date personal
details.

## The merge event, and the on/off switch

The prison system sends a "prisoner merged" event containing:

- the **old prison number** (Nomis ID) that has been retired
- the **new prison number** (Nomis ID) that replaces it
- the **new booking ID** - the identifier for the specific prison "booking" (a period in
  custody) that the new prison number is currently associated with[2](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerUpdatedHandler.kt#L164-L169)

This handling can be switched off entirely via a configuration setting. If it is switched
off, the event is simply logged and ignored, and no licences are changed.[3](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L25-L38)

## What happens when a merge event is received

The system looks up every licence currently held under the old prison number.[4](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L47-L49) If there
are none, nothing further happens. If there are licences, they are split into two groups
depending on whether they belong to the same booking as the new prison number, or a
different (older) booking:[5](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L50-L56)

- **Licences on a different, older booking** - these are deactivated (see below).
- **Licences on the same booking as the new prison number** - these have their offender
  details updated to match the new, confirmed record (see below).

### Deactivating licences on the old booking

Any licence that relates to an older booking, rather than the specific booking now
associated with the new prison number, is automatically deactivated as part of the merge,
with a note recorded against it explaining that it was deactivated because of the prisoner
merge.[6](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L58-L62) This mirrors how the system deactivates any other licence that is no longer
current: its status is set to inactive, an audit trail entry and a licence history entry
are created, and other parts of the system are notified that the licence is now
inactive.[7](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/LicenceService.kt#L566-L599)

### Updating licence details for the current booking

For any licence that is on the same booking as the new prison number, the system fetches
the latest confirmed record for that person - both from the prison system and from the
probation system (Delius) - and then updates the licence with:[8](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L64-L105)

- the new prison number, replacing the old one
- first name, middle name(s) and surname
- date of birth
- the prison the person is currently held at
- a criminal record office (CRO) number
- a police national computer (PNC) number, taken from the probation record

Before every change is applied, the system records an audit entry showing the old and new
value of each of these fields, so there is a clear trail of exactly what changed as a
result of the merge.[9](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L75-L95)

**How the CRO number is chosen**: the system prefers the CRO number held by probation, but
only if it is in a valid CRO format; if that isn't available or valid, it falls back to
the CRO number held by the prison system, again only if that is in a valid format;
otherwise it is left blank rather than saving something that doesn't look like a genuine
CRO number.[10](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/LicenceFactory.kt#L446-L464)

## A couple of important edge cases

- If the merge event message itself cannot be understood (for example, it's missing
  expected information), the failure is logged and the event is not processed further -
  no licences are changed as a result of a message the system couldn't make sense of.[3](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L25-L38)
- If a person has no licences at all under their old prison number, the merge event is
  simply logged and no further action is taken - there is nothing to move across.[4](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L47-L49)

## References

1. [PrisonerMergedHandler.kt:29-33](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L29-L33) — the event carries the old and new Nomis IDs and the new booking ID
2. [PrisonerUpdatedHandler.kt:164-169](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerUpdatedHandler.kt#L164-L169) — the shape of the prisoner merged event's additional information
3. [PrisonerMergedHandler.kt:25-38](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L25-L38) — the enabled/disabled check, and handling of unparseable messages
4. [PrisonerMergedHandler.kt:47-49](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L47-L49) — looking up licences by the old Nomis ID
5. [PrisonerMergedHandler.kt:50-56](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L50-L56) — splitting licences by booking ID
6. [PrisonerMergedHandler.kt:58-62](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L58-L62) — deactivating licences on the old booking with a merge-specific reason
7. [LicenceService.kt:566-599](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/LicenceService.kt#L566-L599) — what deactivating a licence does (status, audit, licence history, domain event)
8. [PrisonerMergedHandler.kt:64-105](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L64-L105) — fetching the latest prison and probation records and updating licence fields
9. [PrisonerMergedHandler.kt:75-95](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/domainEvents/PrisonerMergedHandler.kt#L75-L95) — recording the before/after changes as an audit event
10. [LicenceFactory.kt:446-464](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/LicenceFactory.kt#L446-L464) — how the CRO number is chosen between the probation and prison records
