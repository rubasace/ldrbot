# CLAUDE.md — ldrbot

Spring Boot Telegram bot that tracks LinkedIn games results. Screenshots are
parsed with Tesseract, so the build needs the native Tesseract libraries.

## Build

```bash
mvn package -Dtesseract.data-path=$(pwd)/tessdata -Dtesseract.lib-path=/usr/local/lib
```

Adjust `tesseract.lib-path` to wherever `libtesseract.so` lives locally. CI
stages the libraries from the `franky1/tesseract:5.5.0` image; see `.drone.yml`.

## CI

Builds run on **Drone**. Use the `drone-ci` skill to query build status, read
logs, and promote.

| Event | Result |
|---|---|
| push to `main` | image `rubasace/ldrbot:beta` |
| `promote` (target = version) | sets the version, builds, pushes `latest` + `<version>`, pushes a git tag |
| `tag` | GitHub Release with the jar attached |

The repo is **protected** in Drone: `.drone.yml` carries a `kind: signature`
HMAC block validated on every build. Any edit to the pipeline must be re-signed
(`POST /api/repos/{repo}/sign`) or all subsequent builds are rejected.

There is no path filter, so even a docs-only push to `main` rebuilds and
republishes `beta`.

## Versioning

`pom.xml` on `main` stays at `1.0.0-SNAPSHOT` forever. The real version comes
from the Drone promote target — `mvn versions:set` runs inside the release
pipeline. **Never bump the pom by hand**; it is not the source of truth.

Versions are semver, tags are bare (`0.11.0`, no `v` prefix).

## Release notes

Write them without asking the user. The format is established by releases
0.7.x–0.8.0 — reuse it exactly, omitting a section that would be empty:

```markdown
### ✨ Features

- Support new score formats on mobile

### 🐛 Fixes

- Give preference to isolated timer over one with message when parsing duration
```

Derive the entries from the commit range `<last-tag>..main`: `feat:` goes under
Features, `fix:` under Fixes. Rewrite each subject as a user-facing sentence —
drop the conventional-commit prefix, the issue scope, and the trailing `(#NN)`.
Sentence case, no trailing period. Describe what changed for someone using the
bot, not what changed in the code.

Releases 0.9.0 through 0.11.0 have the placeholder body `Release for <tag>`
because that string is hardcoded in the pipeline's `github-release` step. The
pipeline is not the place to fix this; overwrite the body afterwards with
`gh release edit`.

## Cutting a release

Do not skip steps, and do not report a step as done without checking its actual
source. A green pipeline is necessary, not sufficient — verify the artifacts.

1. **Preconditions.** `main` is green in Drone and every PR meant to be in this
   release is merged. Find the head build of `main`; it must be `success`.
   Promoting a build from any other branch is forbidden: `set-version-release`
   is gated on `branch: main` but `bake-release` is not, so it would publish
   `latest` built from an unversioned pom.
2. **Promote** that build with the version as the target.
3. **Follow the new build** to a final status. On failure, pull the failed
   step's logs and report before doing anything else.
4. **Verify the four artifacts** independently:
   - `rubasace/ldrbot:<version>` on Docker Hub
   - `latest` repointed to it
   - git tag `<version>` on GitHub
   - the GitHub Release, created by the follow-up `tag` build, with the jar
5. **Write the notes** onto that release (see above).

## Deploying

Releasing and deploying are **separate requests**. Cutting a release publishes
artifacts; it does not touch anything running.

The live instance is managed by GitOps from a separate private repository,
which is deliberately not named here. That repo carries its own instructions
for bumping a deployed image — follow those, not these. The mapping from this
project to its deployment lives in the operator's local agent config
(`~/.agent/config/deploys.json`); if it is missing, ask rather than guess.
