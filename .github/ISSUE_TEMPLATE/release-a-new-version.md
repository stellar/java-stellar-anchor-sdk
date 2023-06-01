---
name: Release a New Version
about: Prepare a release to be launched
title: ''
labels: ''
assignees: ''

---
<!-- Please Follow this checklist before making your release. Thanks! -->

### Release Checklist

- [ ] Decide on a version number based on the current version number and the common rules defined in [Semantic Versioning](https://semver.org). E.g. `2.0.x`.
- [ ] Update this ticket name to reflect the new version number, following the pattern "Release `2.0.x`".
- [ ] Update `version` string (Eg: `2.0.x`) attribute in the `build.gradle.kts`
- [ ] Cut a branch for the new release out of the `develop` branch, following the Gitflow naming pattern `release/2.0.x`.
- [ ] Update the code to use this version number.
- [ ] Update the badges versions in [docs/00 - Stellar Anchor Platform.md].
- [ ] Make all changes necessary to make sure the release is ready to be published. If new issues are found during the manual tests, create new tickets aiming at improving the automated tests so these issues can be automatically detected next time.
- [ ] DO NOT RELEASE before holidays or weekends! Mondays and Tuesdays are preferred.
- [ ] Create a new release on GitHub with the name `2.0.x` and tag: `2.0.x` (without the `release-` prefix).
  - [ ] Check the docker image of the release automatically published to [Docker Hub](https://hub.docker.com/r/stellar/anchor-platform).
  - [ ] Update the badges versions in [docs/00 - Stellar Anchor Platform.md].
- [ ] Create two pull requests:
  - [ ] `release/2.0.x -> main`: this should require two approvals.
  - [ ] `release/2.0.x -> develop`: ideally, this should be merged after the `main` branch is merged.
- [ ] (Optional) You'll need to manually publish a new version of the SDK to [Maven Central](https://search.maven.org/search?q=g:org.stellar.anchor-sdk).
- [ ] (Optional) You'll need to manually upload the jar file from [Maven Central](https://search.maven.org/search?q=g:org.stellar.anchor-sdk) to the GH release.
- [ ] (Optional) If necessary, open a PR for stellar/helm-charts and [update with the latest helm chart](https://docs.google.com/document/d/10ujUQZvBCMUyciObQPouxjtlnOdI5OpAz2Pk1LFdDDE) to publish
- [ ] (Optional) Bump helm chart version.
