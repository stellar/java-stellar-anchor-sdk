# Changelog
## 1.2.7
* Add database reconnection with backoff timer. [#774](https://github.com/stellar/java-stellar-anchor-sdk/pull/774)

## 1.2.6
* Add bank_account_type to SEP-9 and customer field [#750](https://github.com/stellar/java-stellar-anchor-sdk/pull/750)

## 1.2.5
* Fix DETELE endpoint to take customer types into account [#734](https://github.com/stellar/java-stellar-anchor-sdk/pull/734)

## 1.2.4
* Fix customer type not being properly resolved (when multiple types are configured) [#721](https://github.com/stellar/java-stellar-anchor-sdk/pull/721)
* Add cbu_alias and cbu_number fields to core and platform [#728](https://github.com/stellar/java-stellar-anchor-sdk/pull/728)

## 1.2.2
* Detects and handle silent and errored SSEStream. [#632](https://github.com/stellar/java-stellar-anchor-sdk/issues/632)
* When the health status is RED, set the status code to 500.

## 1.2.1
* Fix gson serialization error of the Refunds object. [#626](https://github.com/stellar/java-stellar-anchor-sdk/issues/626) 

## 1.2.0
* Add Stellar observer retries with exponential back-off timer [#607](https://github.com/stellar/java-stellar-anchor-sdk/pull/607)
* Add health check endpoint to the Stellar observer [#602](https://github.com/stellar/java-stellar-anchor-sdk/pull/602)

## 1.1.1

Update the version of Helm Chart.

## 1.1.0

* SDK support for [SEP-1], [SEP-10], [SEP-12], [SEP-31] & [SEP-38].
* API support for [SEP-1], [SEP-10], [SEP-12], [SEP-31] & [SEP-38].
* Database support for H2, SQLite, Postgres & Aurora Postgres.
* Queue Publishing support for Kafka and SQS.
* Stellar network monitoring for incoming [SEP-31] payments.
* End-to-end tests through docker-compose.
* Updated documentation.
* Deployment examples with k8s, helm-charts, and fargate. 

## Unreleased

- Add the [Release Checklist](.github/ISSUE_TEMPLATE/release_a_new_version.md).

[SEP-1]: https://stellar.org/protocol/sep-1
[SEP-10]: https://stellar.org/protocol/sep-10
[SEP-12]: https://stellar.org/protocol/sep-12
[SEP-31]: https://stellar.org/protocol/sep-31
[SEP-38]: https://stellar.org/protocol/sep-38