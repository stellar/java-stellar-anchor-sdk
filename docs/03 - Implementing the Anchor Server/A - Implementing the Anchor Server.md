# Implementing the Anchor Server

- [Implementing the Anchor Server](#implementing-the-anchor-server)
  - [Pre-requisites](#pre-requisites)
  - [Testing Tools](#testing-tools)
  - [`Anchor Platform <> Anchor Server` Communication](#anchor-platform--anchor-server-communication)
  - [Anchor Server Implementation Resources](#anchor-server-implementation-resources)
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
      - [Configuring Step 3](#configuring-step-3)
      - [Running Step 3](#running-step-3)
      - [Testing Step 3](#testing-step-3)
    - [Step 4 - Implement The Remmitances Receiving Party Without Quotes](#step-4---implement-the-remmitances-receiving-party-without-quotes)
      - [Configuring Step 4](#configuring-step-4)
      - [Running Step 4](#running-step-4)
      - [Testing Step 4](#testing-step-4)
    - [Step 5 - Implement RFR - Request for Quotation](#step-5---implement-rfr---request-for-quotation)
      - [Configuring Step 5](#configuring-step-5)
      - [Running Step 5](#running-step-5)
      - [Testing Step 5](#testing-step-5)
    - [Step 6 - Implement The Remmitances Receiving Party With Quotes](#step-6---implement-the-remmitances-receiving-party-with-quotes)
      - [Configuring Step 6](#configuring-step-6)
      - [Running Step 6](#running-step-6)
      - [Testing Step 6](#testing-step-6)
    - [Step 7 - Experiment with the Public Network](#step-7---experiment-with-the-public-network)

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
* [Python scripts]:
    * They contain end-to-end tests for [SEP-31] with and without [SEP-38].
    * The script takes the role of the Sending Anchor, testing the Platform role as a Receiving Anchor.

## `Anchor Platform <> Anchor Server` Communication

While the Anchor Platform implements the interoperability part of the [SEPs], it relies on the Anchor Server to implement the business logic, and the communication between these two servers occur in three different ways:
- **Callback API (`Sync Platform->Anchor`)**: a syncronous API that the Platform will use to gather a business-specific data from the Anchor Server, in order to perform a SEP-compliant operation (like exchange rate or user registration, for instance)
- **Events Queue (`Async Platform->Anchor`)**: an asyncronous communication venue that the Platform will use to notify the Anchor Server about a pending action, like an incoming payment that needs to be processed.
- **Platform API (`Sync Anchor->Platform`)**: a syncronous API that the Anchor can use to fetch information (e.g. transactions or quotes) and also update the data of transactions stored in the Platform database.

## Anchor Server Implementation Resources

In the Step-by-step section below, we will require you to implement the following features:
- Make available (or deploy) a queue service, such as Kafka or SQS, for instance.
- Make available a database, such as SQLite, Postgres or MySQL, for instance.
- Implement the [Callback API] endpoints. 
- Connect the Anchor Server with the queue service so it can listen to async [Events].
- Implement calls to the [Platform API], so the Anchor can update the state of transactions stored in the Platform database.

## Step-by-step Anchor Server Implementation

In this section, we cover the steps to build your Anchor Server, integrate it with the Platform and test the integration. We follow an incremental approach, where you start by deploying the Anchor Reference Server and running the tests with Platform + Anchor Reference Server, and then you will start developing your own server with your own business logic that will be used instead of the Anchor Reference Server.

For each new increment in functionality you add to your Anchor Server, there are apropriate configurations and tests that make sure your integration is compliant with the Platform and the [SEPs].

In terms of configuration, there will be 4 main sources:
- The configuration file that can be defined by the environment variable `STELLAR_ANCHOR_CONFIG` and defaults to [`anchor-config-defaults.yaml`].
- The `stellar.toml` file, that is referenced in the configuration file at `app-config.sep1.stellarFile` and defaults to [`stellar-wks.toml`]. It contains the public facing characteristics of the Platform that are essential for interoperability.
- The environment variables, the only ones that need to be kept secret and safe. You can find a description of them at [`example.env`].
- The assets file, that's defined in the config file at `app-config.app.assets` and defaults to [`assets-test.json`]. It's used to define the list of supported assets and their characteristics. Currently, it's only used in the [SEP-31] and [SEP-38] flows.

> Note: please consider the steps below are all on testnet. We’ll only explicitly suggest Public network in the latest step.

### Step 1 - Run the project with the Anchor Reference Server

This step is meant to guarantee you have the pre-requisites in place to start developing your own Anchor Server.

#### Configuring Step 1

In this we use the default configuration provided in this project, which are in the files [`anchor-config-defaults.yaml`] and [`anchor-reference-server.yaml`]. With that in mind, you don't need to modify anything there if you're deploying the system in tour local machine.

#### Running Step 1

Please follow the steps in [01.A - Running & Configuring the Application](/docs/01%20-%20Running%20%26%20Configuring%20the%20Application/A%20-%20Running%20%26%20Configuring%20the%20Application.md#running-the-application-from-source-code) to run the project with the Anchor Reference Server.

#### Testing Step 1

Proceed to test the project with the [`anchor-tests`] command line tool by running:

```shell
export HOME_DOMAIN = "http://localhost:8080"  # Platform Server endpoint
export SEP_CONFIG = "platform/src/test/resources/stellar-anchor-tests-sep-config.json" # SEP configuration file. This file is in the project root but you can use your own.
stellar-anchor-tests --home-domain $HOME_DOMAIN --seps 1 10 12 31 38 --sep-config $SEP_CONFIG
```

### Step 2 - Implement Authentication

This step introduces the authentication mechanism that will be used by the Anchor Platform. It is the implementation of [SEP-10], where clients need to sign a no-op Stellar transaction to prove they are indeed the owner of the Stellar account that will interact with the Platform.

This step does not require any implementation, so you don't need to deploy an Anchor Server just yet. All you need is to configure the appropriate configuration files.

#### Configuring Step 2

At this point, you should start your own configuration files, based off the default ones:

1. At the config file (default found at [`anchor-config-defaults.yaml`]) you need to define the `app-config.sep1` and `app-config.sep10` sections.
2. Expose the `SEP10_SIGNING_SEED` variable containing the Stellar private key to the account your Anchor will use to handshake. It's recommended you have a separate account just for authentication that doesn't hold any funds. Further explanation can be found at [`example.env`].
3. Make sure you configure your `stellar.toml` with:
   - `WEB_AUTH_ENDPOINT`, the endpoint clients will reach to authenticate. For the Platform, make sure to use `{PLATFORM_HOST}/auth`
   - `SIGNING_KEY`, which is the public key that forms a pair with the private key in `SEP10_SIGNING_SEED`.
   - The `[[CURRENCIES]]` section.
   - Please refer to [`stellar-wks.toml`] to check how this information is set in the default configuration.

#### Running Step 2

At this point, you don't even need to have an Anchor Server, just run the Anchor Platform using the configuration files you've configured.

#### Testing Step 2

Proceed to test the project with the [`anchor-tests`] command line tool by running:

```shell
export HOME_DOMAIN = "http://localhost:8080"  # Platform Server endpoint
stellar-anchor-tests --home-domain $HOME_DOMAIN --seps 1 10
```

### Step 3 - Implement KYC

This step introduces customer registration and KYC. It is the implementation of [SEP-12], where customers need to be registered and KYCed before some operations can be performed. It is currently mandatory for remittance operations ([SEP-31]).

This step requires both configuration of the Platform and the implementation of the [Callback API] `GET /customer` endpoint in the Anchor Server, where the Anchor should handle the registration and KYC of the customer.

#### Configuring Step 3

1. Update `app-config.sep12` section in your config file (default found at [`anchor-config-defaults.yaml`]) and make sure `app-config.sep12.customerIntegrationEndpoint` points to the [Callback API] in your Anchor Server.
2. Configure the `stellar.toml` file with `KYC_SERVER={PLATFORM_HOST}/sep12`. Wallets and Sender Anchors will use this endpoint to register and KYC customers and the Platform will pre-process the request and forward it to your Anchor Server.

#### Running Step 3

Here, you'll need to deploy your Anchor Server and run the Anchor Platform using the configuration files you've configured. Remember, your Anchor server should be available at the same address you configured at `app-config.sep12.customerIntegrationEndpoint` and it should expose the [Callback API] `GET /customer` endpoint.

#### Testing Step 3

Proceed to test the project with the [`anchor-tests`] command line tool by running:

```shell
export HOME_DOMAIN = "http://localhost:8080"  # Platform Server endpoint
export SEP_CONFIG = ".../sep-config.json"     # SEP configuration file needed for SEP-12 tests
stellar-anchor-tests --home-domain $HOME_DOMAIN --seps 1 10 12 --sep-config $SEP_CONFIG
```

> Note: this repo provides a `SEP_CONFIG` file at [stellar-anchor-tests-sep-config.json](/platform/src/test/resources/stellar-anchor-tests-sep-config.json) that you can use for your tests, but keep in mind that if your KYC is more restrictive than the default one, you'll need to update the `SEP_CONFIG` file accordingly.

### Step 4 - Implement The Remmitances Receiving Party Without Quotes

Now we introduce the Platform's Remittances flow ([SEP-31]) for Receiving Anchors that's part of the flow `Sending Client -> Sending Anchor -> Receiving Anchor -> Receiving Client`. The Platform implements all the API standards that [SEP-31] riquires to be interoperable with any [SEP-31] Sending Anchor, and it relies on the Anchor Server to implement the core business logic of the flow, which includes:
- Registering into the [Events] queue service to watch for incoming `TransactionsEvents` containing updates in the transaction status. Keep in mind that you need to provide/deploy a queue service for this to work.
- Implementing the [Callback API] `GET /fee` endpoint to return the fee for the transaction.

The platform will also need access to the queue service and a database.

> Note: if you need quotes for your use-case, please skip to step 5.

#### Configuring Step 4

1. Update `app-config.sep31` section in your config file (default found at [`anchor-config-defaults.yaml`]):
   1. Make sure `app-config.sep31.feeIntegrationEndPoint` points to the [Callback API] in your Anchor Server.
   2. Decide on how you will configure the `app-config.sep31.paymentType` field.
   3. Decide on your `app-config.sep31.depositInfoGeneratorType`. If you're using a custodial tool such as [circle.com](circle.com) to receive your payments, please refer to [01.B - Circle Payment Observer].
2. Configure the `app-config.event` section, as well as the the correspondent publisher configuration. For instance, if you've set `app-config.event.publisherType = kafka`, you'll need to configure `kafka.publisher` accordingly.
3. Configure the database by setting the `stellar.anchor.data-access` section, as well as the corresponding `data-spring-jdbc-*` sections.
4. Configure the `stellar.toml` file with `DIRECT_PAYMENT_SERVER={PLATFORM_HOST}/sep31`. Sending Anchors will use this endpoint to create transactions and initiate the remittances along with the Sending Clients.

#### Running Step 4

Here, you'll need to deploy a few services:
1. The queue service you've configured (ex. Kafka).
2. The database you've configured (ex. Postgres).
3. Your Anchor Server, which should be doing all the following:
   1. Implement the [Callback API] `GET /fee` endpoint.
   2. Listen to events comming from the queue service.
   3. Be able to update the Platform with the status of the transaction by reaching the [Platform API] when the transaction status changes from `pending_receiver` to something else.
4. Run the Anchor Platform using the configuration files you've configured. Remember, your Anchor Server should be available at the same address you configured at `app-config.sep31.feeIntegrationEndpoint` and it should expose the [Callback API] `GET /fee` endpoint.

#### Testing Step 4

Proceed to test the project with the [`anchor-tests`] command line tool by running:

```shell
export HOME_DOMAIN = "http://localhost:8080"  # Platform Server endpoint
export SEP_CONFIG = ".../sep-config.json"     # SEP configuration file needed for SEP-12 and SEP-31 tests
stellar-anchor-tests --home-domain $HOME_DOMAIN --seps 1 10 12 31 --sep-config $SEP_CONFIG
```

Run the Python end-to-end `sep31_flow` test. You'll need to manually update the python script payloads with assets and fields compliant with your use-case requirements:

```shell
export HOME_DOMAIN = "http://localhost:8080"  # Platform Server endpoint
export PRIVATE_KEY = "S..."                   # A private key whose public key holds some of the asset used in the SEP-31 test.
python3 end_to_end_tests.py --domain $HOME_DOMAIN --secret $PRIVATE_KEY --tests sep31_flow
```

Use Stellar Demo Wallet (https://demo-wallet.stellar.org) to manually test the SEP-31 flow.
    
### Step 5 - Implement RFR - Request for Quotation

RFR (Request for Quotation) is a mecanism where you can consult an Anchor for the conversion price between two assets. It is implemented in the [SEP-38] protocol and it requires the Anchor Server to implement the [Callback API] `GET /rate` endpoint, and to listen for quotes [Events] coming from the queue service.

You will only need this part if your use-case supports quotes, which is recommended for converting non-equivalent assets such as `USDC <> fiat EURO`.

#### Configuring Step 5

1. Update `app-config.sep38` section in your config file (default found at [`anchor-config-defaults.yaml`]) and make sure `app-config.sep38.quoteIntegrationEndPoint` points to the [Callback API] in your Anchor Server.
2. Configure the `stellar.toml` file with `ANCHOR_QUOTE_SERVER={PLATFORM_HOST}/sep38`. Wallets and Sender Anchors will use this endpoint to consult on conversion prices and also to register [firm quotes](https://www.investopedia.com/terms/f/firmquote.asp).

#### Running Step 5

The running steps here are very similar to the step 4, you'l need:
1. The queue service you've configured (ex. Kafka).
2. The database you've configured (ex. Postgres).
3. Your Anchor Server, which should be doing all the following:
   1. Implement the [Callback API] `GET /rate` endpoint.
   2. Listen to events comming from the queue service. Whenever a quote is created, the Anchor should receive a a Rate [Event].
   3. Be able to relate a quote with a transaction, when a transaction contains a `quote_id`.
4. Run the Anchor Platform using the configuration files you've configured. Remember, your Anchor Server should be available at the same address you configured at `app-config.sep38.quoteIntegrationEndPoint` and it should expose the [Callback API] `GET /rate` endpoint.

#### Testing Step 5

Proceed to test the project with the [`anchor-tests`] command line tool by running:

```shell
export HOME_DOMAIN = "http://localhost:8080"  # Platform Server endpoint
export SEP_CONFIG = ".../sep-config.json"     # SEP configuration file needed for SEP-12 and SEP-38 tests
stellar-anchor-tests --home-domain $HOME_DOMAIN --seps 1 10 12 38 --sep-config $SEP_CONFIG
```

Run the Python end-to-end `sep38_create_quote` test. You'll need to manually update the python script payloads with assets and fields compliant with your use-case requirements:

```shell
export HOME_DOMAIN = "http://localhost:8080"  # Platform Server endpoint
export PRIVATE_KEY = "S..."                   # A private key whose public key holds some of the asset used in the SEP-38 test.
python3 end_to_end_tests.py --domain $HOME_DOMAIN --secret $PRIVATE_KEY --tests sep38_create_quote
```

### Step 6 - Implement The Remmitances Receiving Party With Quotes

This step introduces the Platform's Remittances flow ([SEP-31]) with quotes ([SEP-38]). It is basically the result of a composition between Steps 4 and 5.

This needs the Anchor to configure the assets in [`assets-test.json`] with `quotes_supported: true` and optionally `quotes_required: true`. If quotes are marked as required, the Anchor does not need to implement the [Callback API] `GET /fee` endpoint.

#### Configuring Step 6

To configure the Anchor to receive remittances with quotes, you need to complete both the [Configuring Step 4](#configuring-step-4) and [Configuring Step 5](#configuring-step-5) sections.

#### Running Step 6

This is also a combination of [Running Step 4](#running-step-4) and [Running Step 5](#running-step-5), with one caveat: if all your assets have `quotes_required: true`, you don't need to implement the [Callback API] `GET /fee` endpoint.

#### Testing Step 6

Proceed to test the project with the [`anchor-tests`] command line tool by running:

```shell
export HOME_DOMAIN = "http://localhost:8080"  # Platform Server endpoint
export SEP_CONFIG = ".../sep-config.json"     # SEP configuration file needed for SEP-12, SEP-31 and SEP-38 tests.
stellar-anchor-tests --home-domain $HOME_DOMAIN --seps 1 10 12 31 38 --sep-config $SEP_CONFIG
```

Run the Python end-to-end `sep31_flow_with_sep38` test. You'll need to manually update the python script payloads with assets and fields compliant with your use-case requirements:

```shell
export HOME_DOMAIN = "http://localhost:8080"  # Platform Server endpoint
export PRIVATE_KEY = "S..."                   # A private key whose public key holds some of the assets used in the SEP-31 and the SEP-38 tests.
python3 end_to_end_tests.py --domain $HOME_DOMAIN --secret $PRIVATE_KEY --tests sep31_flow_with_sep38
```

### Step 7 - Experiment with the Public Network

Both the [`anchor-tests`] and [Demo Wallet] can be used to test the production environment.

[`anchor-tests`]: https://www.npmjs.com/package/@stellar/anchor-tests
[Demo Wallet]: https://demo-wallet.stellar.org
[Python scripts]: /end-to-end-tests/end_to_end_tests.py
[`anchor-config-defaults.yaml`]: /platform/src/main/resources/anchor-config-defaults.yaml
[`anchor-reference-server.yaml`]: /anchor-reference-server/src/main/resources/anchor-reference-server.yaml
[`stellar-wks.toml`]: /platform/src/main/resources/sep1/stellar-wks.toml
[`example.env`]: /platform/src/main/resources/example.env
[`assets-test.json`]: /platform/src/main/resources/assets-test.json
[Callback API]: /docs/03%20-%20Implementing%20the%20Anchor%20Server/Communication/Callbacks%20API.yml
[Events]: /docs/03%20-%20Implementing%20the%20Anchor%20Server/Communication/Events%20Schema.yml
[Platform API]: /docs/03%20-%20Implementing%20the%20Anchor%20Server/Communication/Platform%20API.yml
[01.B - Circle Payment Observer]: /docs/01%20-%20Running%20%26%20Configuring%20the%20Application/B%20-%20Circle%20Payment%20Observer.md
[SEPs]: https://github.com/stellar/stellar-protocol/tree/master/ecosystem
[SEP-10]: https://stellar.org/protocol/sep-10
[SEP-12]: https://stellar.org/protocol/sep-12
[SEP-24]: https://stellar.org/protocol/sep-24
[SEP-31]: https://stellar.org/protocol/sep-31
[SEP-38]: https://stellar.org/protocol/sep-38