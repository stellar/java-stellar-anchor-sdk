# Circle Payment Service

The Circle payment service implements the [PaymentService] interface using the [Circle network].

## Setup

In order to properly utilize the Circle payment service you need to create a circle account and configure it properly
the: following way:

1. Go to <https://www.circle.com/> and create a new Circle account.
   - Alternatively, you can skip the long registration process and get your hands on the code quickly by creating a
   simpler sandbox account at <https://my-sandbox.circle.com/signup>.
2. Make sure you get an API key.
3. If you need to receive bank wire transfers, please refer to the [Wire Payments Quickstart] for instructions on how to
register your bank account that will be used for incoming payments.
4. If you need to send bank wire transfers, please refer to the [Wire Payouts Quickstart] for instructions on how to
register the third-party bank accounts that will be receiving this outgoing payments, a.k.a. payouts. 

## Usage

The CirclePaymentService does not implement all Circle capabilities, just a subset of the ones that are important for
most Stellar-related use cases, being limited to `CircleWallet<>CircleWallet`, `CircleWallet<>Stellar` and
`CircleWallet<>BankWire` integrations. Credit cards, ACH, SEPA, BTC,ETH and other networks supported by Circle are not
supported of this integration.

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
