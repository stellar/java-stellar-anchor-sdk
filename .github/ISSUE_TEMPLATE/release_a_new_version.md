---
name: Release a New Version!
about: Prepare a release to be launched
title: ''
labels: release
---

<!-- Please Follow this checklist before making your release. Thanks! -->

### Release Checklist

> Attention: the examples below use the version `0.1.0` but you should update them to use the version you're releasing.

- [ ] Decide on a version number based on the current version number and the common rules defined in [Semantic Versioning](https://semver.org). E.g. `0.1.0`.
- [ ] Update this ticket name to reflect the new version number, following the pattern "Release `0.1.0`".
- [ ] Cut a branch for the new release out of the `develop` branch, following the gitflow naming pattern `release/0.1.0`.
- [ ] Update the code to use this version number.
- [ ] Update the [CHANGELOG.md] file with the new version number and release notes.
- [ ] Run tests and linting. **Not only CI/CD tests, but also manual tests to make sure the release is up and running, and that it's stable!**
- [ ] Make all changes necessary to make sure the release is ready to be published. If new issues are found during the manual tests, create new tickets aiming at improving the automated tests so these issues can be automatically detected next time.
- [ ] DO NOT RELEASE before holidays or weekends! Mondays and Tuesdays are preferred.
- [ ] When the team is confident the release is stable, you'll need to create two pull requests:
  - [ ] `release/0.1.0 -> main`: this should require two approvals.
  - [ ] `release/0.1.0 -> develop`: ideally, this should be merged after the `main` branch is merged.
- [ ] Create a new release on GitHub with the name `0.1.0` and the changes from the [CHANGELOG.md] file.
  - [ ] The release will automatically publish a new version of the docker image to Docker Hub.
  - [ ] You'll need to manually publish a new version of the SDK to jitpack and
  - [ ] You'll need to manually upload the jar file from jitpack to the GH release.

[CHANGELOG.md]: ../../CHANGELOG.md