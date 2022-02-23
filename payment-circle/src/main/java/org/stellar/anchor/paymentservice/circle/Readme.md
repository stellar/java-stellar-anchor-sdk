# Circle Payment Service

The Circle payment service implements the [PaymentService] interface using the [Circle network].

## Setup

In order to properly utilize the Circle payment service you need to create a circle account and configure it properly:

1. Go to <https://www.circle.com/> and create a new Circle account.
   - Alternatively, you can skip the long registration process and get your hands on the code quickly by creating a
     simpler sandbox account at <https://my-sandbox.circle.com/signup>.
2. Make sure you get an API key.
3. If you need to receive bank wire transfers, please refer to the [Wire Payments Quickstart] for instructions on how to
   register your bank account that will be used for incoming payments.
   - Please be aware that all received funds go directly to Circle merchant account (what we call the distribution
   account). Circle does not assign a bank account to a specific internal account so all bank payments go to the
   distribution account.
4. If you need to send bank wire transfers, please refer to the [Wire Payouts Quickstart] for instructions on how to
   register the third-party bank accounts that will be receiving this outgoing payments, a.k.a. payouts. Any internal
   Circle account can make a payout given they have enough funds.

### Creating a Bank Wire Account

In order to send or receive Wire transfers, Circle requires the Bank Wire accounts to be registered in their platform.
This part is not covered by the `PaymentService` interface, so you'll need to send an API request directly to Circle.
For more details, please refer to the [Wire Payments Quickstart] or [Wire Payouts Quickstart] sections of the Circle
documentation.

#### Bank Account Verification

Due to regulatory obligations, Circle systems automatically compare the bank account (number and routing) and sender
(first and last name) information provided at the time of bank account creation (within Circle systems) with the
information received on the wire transfer details (what the sender bank transmits). When there are mismatches, Circle
is obliged to fail the payment and return the funds.

To avoid returns, make sure you build a user experience that makes that clear for end users, so that they provide you
with the correct bank account and account holder / beneficiary information.

#### Unsupported Bank Accounts

Circle currently cannot support wire payments originating from bank accounts that require For Further Credit (FFC)
instructions. In a For Further Credit (FFC) payment, the money is sent to the final beneficiary via an intermediary bank
which obfuscates the bank account sender information. Examples of these bank accounts are brokerage and investment
accounts, accounts that leverage third party payment processors, accounts with money transfer services, and challenger
banks without their own banking license.

#### Supported Countries

You can find an updated list of the supported countries [here](https://developers.circle.com/docs/supported-countries#wire-transfer-payments--payouts).

Note that even if wire transfers are technically supported for a country on the Circle Payments API, your Circle account
might end up being configured to only accept payments from a subset of those countries.

Also note that countries might have capital controls in place which might in practice limit your ability to process wire
transfers from / to some countries.

## Usage

The CirclePaymentService does not implement all Circle capabilities, just a subset of the ones that are important for
most of the Stellar-related use cases, being limited to `CircleWallet<>CircleWallet`, `CircleWallet<>Stellar` and
`CircleWallet<>BankWire` integrations. Credit cards, ACH, SEPA, BTC, ETH and other networks supported by Circle are not
covered by this integration.

You can find how to use the Circle payment service with the supported networks below:

### `ping()`

Allows users to check if the network is up and running.

### `getDistributionAccountAddress()`

Returns the address string of the distribution account, also called **merchant account** within the Circle context.

### `getAccount(String accountId)`

Returns the circle account info and its balance.

> Note: only works with Circle accounts, not stellar nor bank wire accounts.

### `createAccount(String accountId)`

Allows the creation of a new Circle account. The `accountId` isn't needed for Circle, but if provided it will be added
to the account description, which is not unique.

### `getAccountPaymentHistory(String accountID, String afterCursor)`

Returns a paginated history of the account-related transactions, which includes:
- CircleWallet<>Circle
- CircleWallet<>Stellar
- CircleWallet->BankWire

> Note: BankWire->CircleWallet is still missing.

### `sendPayment(Account sourceAccount, Account destinationAccount, String currencyName, BigDecimal amount)`

Allows sending payments to internal and external accounts, including:
- CircleWallet->CircleWallet
- CircleWallet->Stellar
- CircleWallet->BankWire
   - ATTENTION: in order to send a payment to a wire account you first need to create this account using the Circle API.
     We provide a helper method to do that that's not part of the PaymentService interface, but can be used directly.
   - Please refer to the [Creating a Bank Wire Account](#creating-a-bank-wire-account) section.

### `getDepositInstructions(DepositRequirements config)`

Used to get the instructions to make a deposit into the desired account using the native Circle network or an
intermediary network (medium) such as Stellar or BankWire.

## Circle Documentation

To get more info on the circle API and how to properly configure and use it directly, please refer to [Circle docs] and
[Circle API reference].

[PaymentService]: ../../../../../../../../../core/src/main/java/org/stellar/anchor/paymentservice/PaymentService.java
[Circle network]: https://developers.circle.com/reference
[Circle docs]: https://developers.circle.com/docs/
[Circle API reference]: https://developers.circle.com/reference
[Wire Payments Quickstart]: https://developers.circle.com/docs/wire-payments-quickstart#3-create-the-bank-account-you-will-accept-a-payment-from
[Wire Payouts Quickstart]: https://developers.circle.com/docs/payouts-quickstart#4-create-the-bank-account-you-will-send-the-payout-to
