# Releasing NNPPSS

The release workflow is defined in `.github/workflows/release.yml`. It runs only
for tags matching `vMAJOR.MINOR.PATCH`.

## Version rules

- The **tag is the source of truth**: `v2.0.0` becomes version `2.0.0` in Maven,
  the JAR manifest, `jpackage`, the installers and `update-manifest.json`.
- Use SemVer: `MAJOR.MINOR.PATCH`; pre-releases use `-beta.N`/`-rc.N`.
- Never move or reuse a published tag. Fix a bad release with a new patch tag.
- The development `pom.xml` stays on `2.0.0-SNAPSHOT`; CI sets the release
  version with `mvn versions:set`.

## Before tagging

1. Add a `## [X.Y.Z]` section to `CHANGELOG.md` (the workflow requires it).
2. Run `mvn clean verify` locally.
3. Confirm `git status` is clean and CI is green.

## Create the draft

```bash
git tag vX.Y.Z
git push origin vX.Y.Z
```

The workflow:

- validates tag/CHANGELOG;
- runs tests on Windows, macOS and Linux;
- builds MSI, DMG, DEB and portable archives;
- generates `SHA256SUMS.txt` and `update-manifest.json`;
- creates a **draft** GitHub release.

It does not publish automatically.

## Review and publish

```bash
gh release view vX.Y.Z --json isDraft,assets
gh release download vX.Y.Z --pattern update-manifest.json
```

Check:

- all expected assets are present;
- `SHA256SUMS.txt` matches `update-manifest.json`;
- installers open/install on clean machines;
- the version shown in Settings is `X.Y.Z`;
- no checksum mismatch.

Then publish the draft in GitHub. Publishing makes the release visible to:

```text
https://github.com/allyssonmoscoso/NNPPSS/releases/latest/download/update-manifest.json
```

The app checks that URL at most once every 24 hours and only opens the release
page. It never installs silently.

## Unsigned builds

2.0.0 has no Windows/macOS signing certificates. Users may see SmartScreen or
Gatekeeper warnings; this is expected and must be documented in the release
notes. Do not describe unsigned builds as silently updatable.

## Local validation with act

```bash
act -l
act -W .github/workflows/release.yml -j validate -e .act-tag-event.json
act -n -W .github/workflows/release.yml -j build --matrix os:ubuntu-latest
```

Use a tag event payload containing `refs/tags/v2.0.0`. The Linux build can be
run fully with act (DEB + app image); MSI and DMG require GitHub runners or
native machines.

## Rollback

- Bad asset: delete the draft, fix the workflow, create a new patch tag.
- Bad published release: publish a fixed patch; never rewrite the tag.
- If the update manifest is wrong, replace the asset in a **new** release rather
  than pointing users at a mutable URL.
