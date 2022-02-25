# Circle Payment Service

The Circle payment service implements the [PaymentService] interface using the [Circle network].

## Setup

In order to properly utilize the Circle payment service you need to create a Circle account and configure it properly:

1. Go to <https://www.circle.com/> and create a new Circle account.
   - Alternatively, you can skip the long registration process and get your hands on the code quickly by creating a
     simpler sandbox account at <https://my-sandbox.circle.com/signup>.
2. Make sure you get an API key.
3. If you need to receive bank wire transfers, please refer to the [Wire Payments Quickstart] for instructions on how to
   register your bank account that will be used for incoming payments.
   - Please be aware that all received funds go directly to Circle merchant account (what we call the distribution
   account). Circle does not assign a bank account to a specific internal account so all bank payments go to the
   distribution account balance.
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

The CirclePaymentService does not implement all Circle capabilities, just a subset of the ones that are relevant for
most of the Stellar-related use cases, being limited to `CircleWallet<>CircleWallet`, `CircleWallet<>Stellar` and
`CircleWallet<>BankWire` integrations. Credit cards, ACH, SEPA, BTC, ETH and other networks supported by Circle are not
covered by this integration.

All methods are async and return a `reactor.core.publisher.Mono`. For more information, please refer to [Project Reactor].
In most of the examples below we won't be handling the throwable errors, and we will be using the methods synchronously,
but you can also use the library asynchronous features at your choice, here is a quick example to help you get started:

```java
CirclePaymentService service = CirclePaymentService(config);

// Sync usage:
String distributionAccountId = service.getDistributionAccountAddress().block();
System.out.println(distributionAccountId);

// Async usage:
service.getDistributionAccountAddress().then(distributionAccountId -> {
  System.out.println(distributionAccountId);
});
```

You can find more details on how to use the CirclePaymentService in the subsections below:

### `ping()`

Allows users to check if the network is up and running. Usage:

```java
try {
  service.ping().block();
} catch (Exception ex) {
  ex.printStackTrace();
  // TODO: handle service being offline
}
```

### `getDistributionAccountAddress()`

Returns the address string of the distribution account, also called **merchant account** within the Circle context. Usage:

```java
String distributionAccountId = service.getDistributionAccountAddress().block();
System.out.println(distributionAccountId);
```

### `getAccount(String accountId)`

Returns the circle account info and its balance. Usage:

```java
Account account = service.getAccount("<account-id>").block();
System.out.println(account);
```

> Note: only works with Circle accounts, not stellar nor bank wire accounts.

### `createAccount(String accountId)`

Allows the creation of a new Circle account. The `accountId` isn't needed for Circle, but if provided it will be added
to the account description, which is not unique. Usage:

```java
Account newAccount = service.createAccount(null).block();
System.out.println(newAccount);
```

### `getAccountPaymentHistory(String accountID, String afterCursor, String beforeCursor)`

Returns the paginated history of the account-related transactions, which includes:
- CircleWallet<>Circle
- CircleWallet<>Stellar
- CircleWallet->BankWire

Usage:

```java
PaymentHistory history = service.getAccountPaymentHistory("<account-id>", null).block();
System.out.println(history);

PaymentHistory nextPageHistory = service.getAccountPaymentHistory("<account-id>", null, history.afterCursor).block();
System.out.println(nextPageHistory);
```

> Note: CircleWallet<-BankWire is still missing.

### `sendPayment(Account sourceAccount, Account destinationAccount, String currencyName, BigDecimal amount)`

Allows sending payments to internal and external accounts, including:
- CircleWallet->CircleWallet
- CircleWallet->Stellar
- CircleWallet->BankWire
   - ATTENTION: in order to send a payment to a wire account you first need to create this wire account using the Circle
     API. Please refer to the [Creating a Bank Wire Account](#creating-a-bank-wire-account) section for more info.

Usage:

```java
// CircleWallet->CircleWallet
Account source = new Account(PaymentNetwork.CIRCLE, "1000066041", Account.Capabilities(PaymentNetwork.CIRCLE, PaymentNetwork.STELLAR));
Account destination = new Account(PaymentNetwork.CIRCLE, "1000067536", Account.Capabilities(PaymentNetwork.CIRCLE, PaymentNetwork.STELLAR));
String currencyName = CircleAsset.circleUSD();
Payment payment = service.sendPayment(source, destination, currencyName, BigDecimal.valueOf(0.91)).block();
System.out.println(payment);

// CircleWallet->Stellar
Account source = new Account(PaymentNetwork.CIRCLE, "1000066041", Account.Capabilities(PaymentNetwork.CIRCLE, PaymentNetwork.STELLAR));
Account destination = new Account(PaymentNetwork.STELLAR, "GBG7VGZFH4TU2GS7WL5LMPYFNP64ZFR23XEGAV7GPEEXKWOR2DKCYPCK", "<memo here>", Account.Capabilities());
String currencyName = "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5";
Payment payment = service.sendPayment(source, destination, currencyName, BigDecimal.valueOf(0.91)).block();
System.out.println(payment);

// CircleWallet->BankWire
Account source = new Account(PaymentNetwork.CIRCLE, "1000066041", Account.Capabilities(PaymentNetwork.CIRCLE, PaymentNetwork.STELLAR));
// The bank wire account should have been created in advance directly in Circle
Account destination = new Account(PaymentNetwork.BANK_WIRE, "6c87da10-feb8-484f-822c-2083ed762d25", "test@mail.com", Account.Capabilities());
String currencyName = CircleAsset.fiatUSD();
Payment payment = service.sendPayment(source, destination, currencyName, BigDecimal.valueOf(0.91)).block();
System.out.println(payment);
```

### `getDepositInstructions(DepositRequirements config)`

Used to get the instructions to make a deposit into the desired account using the native Circle network or an
intermediary network (medium) such as Stellar or BankWire.

Usage:

```java
// Deposit requirements to receive CircleWallet<-CircleWallet payments
DepositRequirements config = DepositRequirements("1000066041", PaymentNetwork.CIRCLE, CircleAsset.circleUSD());
DepositInstructions instructions = service.getDepositInstructions(config).block();
System.out.println(instructions);

// Deposit requirements to receive CircleWallet<-Stellar payments
DepositRequirements config = DepositRequirements("1000066041", PaymentNetwork.STELLAR, CircleAsset.circleUSD());
DepositInstructions instructions = service.getDepositInstructions(config).block();
System.out.println(instructions);

// Deposit requirements to receive CircleWallet<-BankWire payments
DepositRequirements config = DepositRequirements("1000066041", null, PaymentNetwork.BANK_WIRE, "a4e76642-81c5-47ca-9229-ebd64efd74a7", CircleAsset.circleUSD());
DepositInstructions instructions = service.getDepositInstructions(config).block();
System.out.println(instructions);
```

## Circle Documentation

To get more info on the circle API and how to properly configure and use it directly, please refer to [Circle docs] and
[Circle API reference].

[PaymentService]: ../core/src/main/java/org/stellar/anchor/paymentservice/PaymentService.java
[Circle network]: https://developers.circle.com/reference
[Circle docs]: https://developers.circle.com/docs/
[Circle API reference]: https://developers.circle.com/reference
[Wire Payments Quickstart]: https://developers.circle.com/docs/wire-payments-quickstart#3-create-the-bank-account-you-will-accept-a-payment-from
[Wire Payouts Quickstart]: https://developers.circle.com/docs/payouts-quickstart#4-create-the-bank-account-you-will-send-the-payout-to
[Project Reactor]: https://projectreactor.io/docs/core/release/reference/