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
- [ ] Cut a branch for the new release, following the gitflow pattern `release/0.1.0`.
- [ ] Update the code to use this version number.
- [ ] Update the CHANGELOG.md file with the new version number and release notes.
- [ ] Run tests and linting. Not only CI/CD tests, but also manual tests to make sure the release is up and running, and that it's stable!
- [ ] Make all changes necessarry to make sure the release is ready to be published. If new issues are found on manual tests, improve the CI so it can automatically detect them next time.
- [ ] When the team is confident the release is stable, you'll need to create two pull requests:
  - [ ] `release/0.1.0 -> develop`: this should be merged by your reviewer.
  - [ ] `release/0.1.0 -> main`: this should require two approvals.
- [ ] Create a new release on Github with the name `0.1.0` and the changes from the CHANGELOG.md file.
  - The release should trigger the release process on Github that will publish a new version of the SDK to jitpack and automatically upload the jar file to the GH release.
