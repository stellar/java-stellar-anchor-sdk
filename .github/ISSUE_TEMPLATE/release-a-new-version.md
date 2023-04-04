---
name: Release a New Version
about: Prepare a release to be launched
title: ''
labels: ''
assignees: ''

---

<!-- Please Follow this checklist before making your release. Thanks! -->

### Release Checklist

> Attention: the examples below use the version `0.1.0` but you should update them to use the version you're releasing.

- [ ] Decide on a version number based on the current version number and the common rules defined in [Semantic Versioning](https://semver.org). E.g. `1.2.x`.
- [ ] Update this ticket name to reflect the new version number, following the pattern "Release `1.2.x`".
- [ ] Cut a branch for the new release out of the `develop` branch, following the Gitflow naming pattern `release/0.1.0`.
- [ ] Update the code to use this version number.
- [ ] Update the badges versions in [docs/00 - Stellar Anchor Platform.md].
- [ ] Run tests and linting. **Not only CI/CD tests, but also manual tests to make sure the release is up and running, and that it's stable!**
- [ ] Make all changes necessary to make sure the release is ready to be published. If new issues are found during the manual tests, create new tickets aiming at improving the automated tests so these issues can be automatically detected next time.
- [ ] DO NOT RELEASE before holidays or weekends! Mondays and Tuesdays are preferred.
- [ ] When the team is confident the release is stable, you'll need to create two pull requests:
  - [ ] `release/1.2.x -> main`: this should require two approvals.
  - [ ] `release/1.2.x -> develop`: ideally, this should be merged after the `main` branch is merged.
- [ ] After the release(e.g. `0.1.0`) branch is merged to `main`, create a new release on GitHub with the name `1.2.x` and the changes from the [docs/00 - Stellar Anchor Platform.md] file.
  - [ ] The release will automatically publish a new version of the docker image to Docker Hub.
  - [ ] Update the badges versions in [docs/00 - Stellar Anchor Platform.md].
  - [ ] (Optional) You'll need to manually publish a new version of the SDK to [Maven Central](https://search.maven.org/search?q=g:org.stellar.anchor-sdk).
  - [ ] (Optional) You'll need to manually upload the jar file from [Maven Central](https://search.maven.org/search?q=g:org.stellar.anchor-sdk) to the GH release.
- [ ] If necessary, open a PR for stellar/helm-charts and [update with the latest helm chart](https://docs.google.com/document/d/10ujUQZvBCMUyciObQPouxjtlnOdI5OpAz2Pk1LFdDDE) to publish
  - [ ] (Optional) Bump helm chart version.
