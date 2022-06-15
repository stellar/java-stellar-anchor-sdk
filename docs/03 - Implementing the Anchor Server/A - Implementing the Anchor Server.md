# Implementing the Anchor Server

- [Implementing the Anchor Server](#implementing-the-anchor-server)
  - [Pre-requisites](#pre-requisites)
  - [Testing Tools](#testing-tools)
  - [Step-by-step Anchor Server Implementation](#step-by-step-anchor-server-implementation)
    - [Step 1 - Run the project with the Anchor Reference Server](#step-1---run-the-project-with-the-anchor-reference-server)
      - [Configuring Step 1](#configuring-step-1)
      - [Running Step 1](#running-step-1)
      - [Testing Step 1](#testing-step-1)
    - [Step 2 - Implement Authentication](#step-2---implement-authentication)
      - [Configuring Step 2](#configuring-step-2)
      - [Running Step 2](#running-step-2)
      - [Testing Step 2](#testing-step-2)
    - [Step 3 - Implement KYC](#step-3---implement-kyc)
    - [Step 4 - Implement The Remmitances Receiving Party Without Quotes](#step-4---implement-the-remmitances-receiving-party-without-quotes)
    - [Step 5 - Implement RFR - Request for Quotation](#step-5---implement-rfr---request-for-quotation)
    - [Step 6 - Implement The Remmitances Receiving Party With Quotes](#step-6---implement-the-remmitances-receiving-party-with-quotes)
    - [Step 7 - Make Sure All Tests Pass](#step-7---make-sure-all-tests-pass)
    - [Step 8 - Experiment with the Public Network](#step-8---experiment-with-the-public-network)

In this section, we will cover how to implement the Anchor Server that interacts with the Anchor Platform.

Before proceeding with this document, please make sure you understand the project architecture and the definitions that are available on the [README.md](/README.md).

## Pre-requisites

* Properly [configure the Anchor Platform](/docs/01%20-%20Running%20%26%20Configuring%20the%20Application/A%20-%20Running%20%26%20Configuring%20the%20Application.md).
* Supply the services that will be used by the anchor platform, which include:
	* **Queue service**: the only supported ones as of this date are Kafka and SQS.
	* **Database**: this product has been tested with SQLite, Postgres and MySQL.
* Make sure the Platform points to the endpoint of your Anchor server.

## Testing Tools

* [`anchor-tests`], a.k.a. Anchor Validator:
    * These are smoke tests that verify if the basics of the [SEPs] (interoperability protocols) are working correctly.
    * It can be run from the web interface (https://anchor-tests.stellar.org) or the [command line](https://github.com/stellar/java-stellar-anchor-sdk/blob/6a3db93ee0fb30dda3abaab79957c516bc9aa605/.github/workflows/build_and_test.yml#L90).
    * These tests depend on a successful integration between the Anchor Platform and your Anchor Server. This project shipped with an [Anchor Reference Server](/anchor-reference-server) that can be used for test purposes, but Anchors will be deploying their own Anchor Server containing their core business logic.
* Demo Wallet (https://demo-wallet.stellar.org):
	* The demo wallet can be used to test how a wallet (or Sending Anchor) will interact with your infrastructure.
	* It can be used to test the end-to-end [SEP-31] flow, where the Demo Wallet works like a Sending Anchor while the Platform is the Receiving Anchor.
	* Please be aware that the Demo Wallet does not use [SEP-38] quotes. To test them, please refer to the Python scripts below.
* [Python scripts](/end-to-end-tests/end_to_end_tests.py):
    * They contain end-to-end tests for [SEP-31] with and without [SEP-38].
    * The script takes the role of the Sending Anchor, testing the Platform role as a Receiving Anchor.

## Step-by-step Anchor Server Implementation

In this section, we cover the steps to build your Anchor Server, integrate it with the Platform and test the integration. We follow an incremental approach, where you start by deploying the Anchor Reference Server and running the tests with Platform + Anchor Reference Server, and then you will start developing your own server with your own business logic that will be used instead of the Anchor Reference Server.

For each new increment in functionality you add to your Anchor Server, there are apropriate configurations and tests that make sure your integration is compliant with the Platform and the [SEPs].

In terms of configuration, there will be 4 main sources:
- The configuration file that can be defined by and defaults to [`anchor-config-defaults.yaml`].
- The `stellar.toml` file, that is referenced in the configuration file and defaults to [`stellar-wks.toml`].
- The environment variables, the only ones that need to be kept secret and safe. You can find a description of them at [`example.env`].
- The [`assets-test.json`] file, that's used to define the list of supported assets. Currently, it's only used in the [SEP-31] flow.

> Note: please consider the steps below are all on testnet. We’ll only explicitly suggest Public network in the latest step.

### Step 1 - Run the project with the Anchor Reference Server

This step is meant to guarantee you have the pre-requisites in place to start developing your own Anchor Server.

#### Configuring Step 1

This step will use the default configuration provided in this project, which are in the files [`anchor-config-defaults.yaml`] and [`anchor-reference-server.yaml`]. With that in mind, you don't need to modify anything there if you're deploying the system in tour local machine.

#### Running Step 1

Please follow the steps in [01.A - Running & Configuring the Application](/docs/01%20-%20Running%20%26%20Configuring%20the%20Application/A%20-%20Running%20%26%20Configuring%20the%20Application.md#running-the-application-from-source-code) to run the project with the Anchor Reference Server.

#### Testing Step 1

Proceed to test the project with the [`anchor-tests`] command line tool by running:

```shell
export HOME_DOMAIN = "http://localhost:8000"  # Platform Server endpoint
export SEP_CONFIG = "platform/src/test/resources/stellar-anchor-tests-sep-config.json" # SEP configuration file. This file is in the project root but you can use your own.
stellar-anchor-tests --home-domain $HOME_DOMAIN --seps 1 10 12 31 38 --sep-config $SEP_CONFIG
```

### Step 2 - Implement Authentication

This step introduces the authentication mechanism that will be used by the Anchor Platform. It is the implementation of [SEP-10], where clients need to sign a no-op Stellar transaction to prove they are indeed the owner of the Stellar account that will interact with the Platform.

This step does not require any implementation, so you don't need to deploy an Anchor Server just yet. All you need is to configure the appropriate configuration files.

#### Configuring Step 2

1. At the config file (ex. [`anchor-config-defaults.yaml`]) you need to define the `app-config.sep10` section.
2. Expose the `SEP10_SIGNING_SEED` variable containing the Stellar private key to the account your Anchor will use to handshake. It's recommended you have a separate account just for authentication that doesn't hold any funds. Further explanation can be found at [`example.env`].
3. Make sure you configure your `stellar.toml` with:
   - `WEB_AUTH_ENDPOINT`, the endpoint clients will reach to authenticate. For the Platform, make sure to use `{PLATFORM_HOST}/auth`
   - `SIGNING_KEY`, which is the public key that forms a pair with the private key in `SEP10_SIGNING_SEED`.
   - Please refer to [`stellar-wks.toml`] to check how this information is set in the default configuration.

#### Running Step 2

At this point, you don't even need to have an Anchor Server, just run the Anchor Platform using the configuration files you've configured.

#### Testing Step 2

Proceed to test the project with the [`anchor-tests`] command line tool by running:

```shell
export HOME_DOMAIN = "http://localhost:8000"  # Platform Server endpoint
stellar-anchor-tests --home-domain $HOME_DOMAIN --seps 1 10
```

### Step 3 - Implement KYC

[SEP-12].
    
### Step 4 - Implement The Remmitances Receiving Party Without Quotes

[SEP-31]
    
If you need quotes, skip to step 5
    
### Step 5 - Implement RFR - Request for Quotation

[SEP-38]
    
### Step 6 - Implement The Remmitances Receiving Party With Quotes

[SEP-31] + [SEP-38]
    
### Step 7 - Make Sure All Tests Pass
    
### Step 8 - Experiment with the Public Network

[`anchor-tests`]: https://www.npmjs.com/package/@stellar/anchor-tests
[`anchor-config-defaults.yaml`]: /platform/src/main/resources/anchor-config-defaults.yaml
[`anchor-reference-server.yaml`]: /anchor-reference-server/src/main/resources/anchor-reference-server.yaml
[`stellar-wks.toml`]: /platform/src/main/resources/sep1/stellar-wks.toml
[`example.env`]: /platform/src/main/resources/example.env
[`assets-test.json`]: /platform/src/main/resources/assets-test.json
[SEPs]: https://github.com/stellar/stellar-protocol/tree/master/ecosystem
[SEP-10]: https://stellar.org/protocol/sep-10
[SEP-12]: https://stellar.org/protocol/sep-12
[SEP-24]: https://stellar.org/protocol/sep-24
[SEP-31]: https://stellar.org/protocol/sep-31
[SEP-38]: https://stellar.org/protocol/sep-38