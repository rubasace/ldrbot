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

Builds run on **Drone** at `drone.nasvigo.com`, repo `rubasace/ldrbot`. Use the
`drone-ci` skill to query build status, read logs, and promote.

| Event | Result |
|---|---|
| push to `main` | image `rubasace/ldrbot:beta` |
| `promote` (target = version) | sets the version, builds, pushes `latest` + `<version>`, pushes a git tag |
| `tag` | GitHub Release with the jar attached |

The repo is **protected** in Drone: `.drone.yml` carries a `kind: signature`
HMAC block that is validated on every build. Any edit to the pipeline must be
re-signed (`POST /api/repos/rubasace/ldrbot/sign`) or all subsequent builds are
rejected.

## Versioning

`pom.xml` on `main` stays at `1.0.0-SNAPSHOT` forever. The real version comes
from the Drone promote target — `mvn versions:set` runs inside the release
pipeline. **Never bump the pom by hand**; it is not the source of truth.

Versions are semver, tags are bare (`0.11.0`, no `v` prefix).

## Cutting a release

Do not skip steps, and do not report a step as done without checking its actual
source. A green pipeline is necessary, not sufficient — verify the artifacts.

1. **Preconditions.** `main` is green in Drone, and every PR meant to be in this
   release is merged. Find the head build of `main`; it must be `success`.
   Promoting a build from any other branch is forbidden: `set-version-release`
   is gated on `branch: main` but `bake-release` is not, so it would publish
   `latest` built from an unversioned pom.
2. **Draft the release notes** from the commit range `<last-tag>..main` and show
   them to the user *before* promoting. This is the point of doing it here — the
   pipeline's own note is the placeholder string `Release for <tag>`.
3. **Promote** the head build of `main` with the version as the target.
4. **Follow the new build** to a final status. On failure, pull the failed
   step's logs and report before doing anything else.
5. **Verify the four artifacts** independently:
   - `rubasace/ldrbot:<version>` on Docker Hub
   - `latest` repointed to it
   - git tag `<version>` on GitHub
   - the GitHub Release (created by the follow-up `tag` build, with the jar)
6. **Replace the notes**: `gh release edit <version> --notes-file …` with the
   text approved in step 2.
7. **Deploy**: the running instance is defined in the `rubasace/homelab` repo.
   Follow the deploy instructions in that repo's `CLAUDE.md`. Open a PR there —
   never merge it.
