---
name: document-business-logic
description: How to write a new non-technical business-logic doc under /docs (in the style of docs/eligibility.md and docs/licence-start-date.md), optionally with an opt-in reference list (footnote-style markdown links back to source file/line ranges), and register/update it in docs-change-tracking.yaml so DocsChangeTrackingTest stays green. Use when asked to document business rules/logic for a service or class, add a doc to /docs, add code citations/references to a business-logic doc, or when DocsChangeTrackingTest fails because source code drifted from its doc.
metadata:
  author: create-and-vary-a-licence-api
  version: "1.0"
---

# Documenting business logic for non-technical readers

Some services in this codebase implement business/policy rules that are significant enough to warrant a
plain-English explanation for non-technical readers (policy leads, product, delivery managers, auditors) — not
just code comments. Two existing examples to use as templates:

- `docs/eligibility.md` — documents `EligibilityService.kt`.
- `docs/licence-start-date.md` — documents `ReleaseDateService.kt`.

Freshness of these docs is enforced automatically by `DocsChangeTrackingTest`
(`src/test/kotlin/.../docs/DocsChangeTrackingTest.kt`) against the registry in `docs-change-tracking.yaml` — see
step 5 below. **Never skip that step**: a new or updated doc without a matching, correctly computed hash will
either fail CI immediately (wrong hash) or silently fail to catch future drift (missing entry).

## 1. Decide what to document and read the source first

Identify the class/service (or whole package) that implements the business rule, e.g.
`uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService`. Read it fully — don't
paraphrase from memory or from tests. Look especially for:

- Named constants/config values that represent business rules (e.g. "3 working days", "28 days") — quote the
  actual default values, and note that they're configurable if driven by `@Value`.
- Branching logic that represents distinct real-world scenarios (different licence kinds, eligibility routes,
  edge cases) — each branch is usually worth its own section.
- Anything that falls back/defaults when data is missing, and anything logged as a "data quality issue" — these
  are important enough to call out explicitly, as they affect how confidently a reader can trust the outcome.
- Helper/private functions that encode a sub-rule (e.g. "move to last working day") — worth their own short
  section since they're reused across multiple top-level rules.

## 2. Ask whether a reference list is wanted

Before writing, ask the user (every time this skill runs — for a brand-new doc and for a refresh of an existing
one alike): **"Would you like a reference list linking statements in this doc back to the source code?"**
Briefly explain what that means: inline footnote markers (e.g. `[^1]`) added at the end of key sentences/bullets,
each backed by an entry in a `## References` section at the bottom of the doc linking to the exact file and line
range in the code that the statement was derived from. Don't add this unless the user opts in — plain docs
without references remain the default, matching the existing style of `eligibility.md` and
`licence-start-date.md`.

## 3. Write the doc in `/docs`

Create `docs/<kebab-case-topic>.md` (match the source concept, not the class name, e.g. `licence-start-date.md`
not `release-date-service.md`). Follow this structure, matching the tone of the existing docs exactly:

### Required opening callout

Start every doc with a blockquote exactly in this style (adjust the first sentence to the topic):

```markdown
# <Doc title in sentence case>

> This document explains, in plain language, how the system <does the thing>. It is
> aimed at non-technical readers.
>
> This document is checked automatically for staleness: if a developer changes the
> underlying <topic> logic in the code, an automated test will fail until someone
> reviews this document and confirms it's still accurate (or updates it). See
> `docs-change-tracking.yaml` at the root of the repository if you want to understand
> how that check works.
```

### Body sections

- **"What this is for"** — one short paragraph/list framing why this logic exists and what decision it drives.
  Plain-English restate the "so what", not just "what".
- **A glossary of key terms/dates** — as a bullet list, one term per bullet, bold the term, then a plain-English
  definition. Do this for every domain term/date field a reader would otherwise need to look up (e.g.
  "conditional release date", "post recall release date").
- **One section per rule/scenario** — usually one per enum value, licence kind, or decision branch in the code.
  Use `##`/`###` headings matching the code's own branching (e.g. one `###` per licence kind under a `##` for
  "how the date is calculated"). State conditions as bullet lists of "all of the following must be true"
  wherever the code is an `&&`/`when` chain — this mirrors the code structure and is easier to verify against it.
- **Edge cases / defaults for missing data** — a closing section (e.g. "A couple of important edge cases") that
  explicitly calls out fallback behaviour and anything logged as a data-quality issue, and states what the
  system defaults to and why (usually "defaults to not blocking the person, to avoid wrongly penalising them for
  missing data" — but confirm against the actual code, don't assume this default).
- **A "how it all comes together" section**, if the logic has multiple independent routes/checks that combine
  into one final outcome (see `eligibility.md`'s "How the final decision is made") — explain the priority order
  if one exists.

### Tone rules (the most important part)

- Write for someone with zero familiarity with the codebase, Kotlin, or Spring — no class names, method names,
  variable names, or code snippets anywhere in the prose.
- Prefer plain nouns/verbs over jargon: "the date someone is due to be released", not "CRD field". Introduce the
  common abbreviation (e.g. "conditional release date (CRD)") once, in the glossary, then it's fine to reuse the
  abbreviation sparingly afterward if the source docs do — otherwise prefer the full term throughout, as
  `licence-start-date.md` mostly does.
- Use short paragraphs and bullet lists over long prose paragraphs; use tables where there's a fixed enumerable
  set of options (see the licence-kind table in `eligibility.md`).
- State rules as plain conditions ("if X and Y, then Z") rather than describing control flow ("the function
  checks whether...").
- Don't editorialise or add caveats not present in the code (no "this seems inefficient" or similar) — the doc
  should be a faithful, neutral plain-English mirror of the current logic, not a critique or a proposal.
- Bold sparingly, only for genuinely key terms/dates/outcomes on first use in a section — don't bold whole
  sentences.
- Avoid absolute claims not actually guaranteed by the code (e.g. don't say "always" if there's a config flag
  that can disable it — say "unless switched off via a setting" as `eligibility.md` does for HDC).

## 4. Add a reference list (only if the user opted in at step 2)

If the user asked for a reference list, add it now, after the doc's prose is otherwise finished:

- As you identify the source location backing each key claim, add an inline footnote marker (`[^1]`, `[^2]`,
  ...) at the end of the sentence or bullet it belongs to. Use one marker per distinct claim/section, not one per
  word — usually a handful per doc, similar in density to how many `##`/`###` sections it has.
- At the very end of the doc (after any "how it all comes together" section), add a `## References` heading
  followed by one footnote definition per marker used. Each definition must be a **markdown link**, not a plain
  code span, using a repo-relative path (from the repo root, with a leading `/`) and a GitHub-style `#L<start>-
  L<end>` line-range anchor, followed by a short description of what the reference backs:

  ```markdown
  ## References

  [^1]: [ReleaseDateService.kt:120-135](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/dates/ReleaseDateService.kt#L120-L135) — how the CRD adjustment for non-working days is calculated
  [^2]: [ReleaseDateService.kt:210-225](/src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/dates/ReleaseDateService.kt#L210-L225) — the fallback used when the PRRD is missing
  ```

  This form renders as a clickable link on GitHub (which highlights the given line range when opening the file)
  and resolves in most local editors too, without depending on a commit SHA or branch that could go stale.
- The `## References` section is metadata, not prose: it's the one place in the doc that's **exempt** from the
  "no class names, method names, or code snippets" tone rule above — file names and line ranges belong here.
- Don't reference every line of every method — only the specific lines that most directly evidence the claim
  (e.g. the branch condition, the fallback default, the constant), so each reference stays a fast, precise jump
  target rather than "the whole file".

## 5. Register the doc in `docs-change-tracking.yaml` (mandatory)

Every doc referenced by this process must have an entry in `docs-change-tracking.yaml` at the repo root, or the
test that's supposed to catch drift will never run for it. Add an entry:

```yaml
  - id: <kebab-case-id>                       # short, unique, e.g. "release-date-service"
    description: "<one-line description of the business logic covered>"
    doc: docs/<your-doc>.md
    sources:
      - src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/<Path>.kt
    hash: <computed-below>
```

`sources` can list one or more file paths, or a directory (in which case every `*.kt` file under it, recursively,
sorted by path, is included) — use a directory if the logic spans several files in one package.

### Computing the hash

The `hash` **must** be computed the same way `DocsChangeTrackingTest` computes it — an MD5 digest built by, for
each resolved source file (sorted by repo-relative path): hashing the UTF-8 bytes of its repo-relative path, then
a single null byte, then the raw file bytes. Do not hand-type or guess this value. Compute it with:

```bash
python3 -c "
import hashlib
paths = [
    'src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/<Path>.kt',
]
d = hashlib.md5()
for p in sorted(paths):
    d.update(p.encode('utf-8'))
    d.update(b'\x00')
    with open(p, 'rb') as f:
        d.update(f.read())
print(d.hexdigest())
"
```

For a `sources` entry that's a directory, extend the `paths` list with every `*.kt` file under it (sorted), not
just the directory path itself.

Alternatively, put in a deliberately wrong hash, run the test (below), and copy the correct hash from the
failure message — but computing it directly is faster and avoids a throwaway test run.

## 6. Verify

Run the doc-tracking test to confirm the new/updated entry passes (and that you haven't broken any existing
entry):

```bash
./gradlew test --tests "*DocsChangeTrackingTest*"
```

Every entry in the registry runs as its own dynamic test (named after its `id`) — check the new one appears and
passes, alongside all pre-existing entries.

## 7. Keeping docs from going stale later (for anyone changing the source)

If you instead change the *source* of an already-documented rule (e.g. edit `ReleaseDateService.kt`), the same
test will fail with the newly computed hash. When that happens:

1. Re-read the changed source and update the corresponding `docs/*.md` file to reflect the new behaviour, using
   the same tone rules above.
2. Update only the `hash` field for that entry in `docs-change-tracking.yaml` to the value reported in the test
   failure message (or recompute it as in step 3).
3. Re-run `./gradlew test --tests "*DocsChangeTrackingTest*"` to confirm it's green.
4. **If the doc has a `## References` section**, re-check every reference's line range against the changed
   source and update any that have shifted or no longer point at the right lines. Reference links are **not**
   part of the hash (the hash only covers whether the source content changed at all, not specific line numbers),
   so they can silently go stale even though the doc otherwise still passes `DocsChangeTrackingTest` — this
   manual re-check when the hash changes is the only safeguard, so don't skip it for docs with references.

Never update the `hash` without actually reviewing/updating the doc content first — that defeats the purpose of
the check.
