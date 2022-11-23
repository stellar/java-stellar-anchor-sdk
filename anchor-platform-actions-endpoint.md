# Anchor Platform API - POST /actions

## Background

The existing `PATCH /transactions` is used to update various fields on the Anchor Platform's transaction. These changes are reflected in the relevant SEP's `GET /transaction(s)` endpoint. This is largely in-line with what RESTful `PATCH` endpoints should do -- update a record's information.

However, there are two primary reasons why `PATCH /transactions` is a less-than-ideal solution for this purpose.

1. The endpoint does more than update a record's information -- it also creates side effects, such as sending a deposit or refund payment
2. The transaction information that should, or can, be updated depends on the current state of the transaction

An example pertaining to the first reason: if a remittance payment cannot be delivered off-chain, the business should refund the on-chain payment to the sender. Today, this can be done by making a `PATCH /transactions` call updating the transaction's `status` value to `refunded` and providing a `refund` object containing the amounts to be refunded. The Anchor Platform would not only update the transaction's `status` and `refunds` object, but would send the payment back to the sender.

An example pertaining to the second reason: if a remittance payment has not been sent on Stellar to the business, then it of course does not make sense for the business to update the transaction's `status` to `refunded`. The Platform should reject this request, even though the same request would be considered valid if the transaction was in a different state.

For the above reasons, the Anchor Platform should provide businesses with an interface with clearly defined semantics for when particiar sets of information can be updated, and for requesting particular actions to be taken.

## POST /actions

A [JSON RPC](https://www.jsonrpc.org/specification) endpoint is a great standard for this type of interface. Instead of the RESTful pattern of peforming CRUD operations on resources, RPC APIs allow clients to call functions (or procedures) executed on remote servers. These functions/procedures each accept different sets of parameters and have explicitly defined results.

The Anchor Platform can provide an endpoint for accepting these RPC requests.

### notify_interactive_flow_complete

Updates the transaction with the amounts & fees collected in the interactive flow. Accepts an optional message to communicate to the user. This message is only necessary if the business does not intend to take the request_funds action in the same API request to the platform. 

This action is only relevant for SEP-24, because SEP-6 & SEP-31 receive the amounts in their respective transaction initiation endpoints, and the platform requests fee information immediately.

```json
[
  {
    "jsonrpc": "2.0",
    "method": "complete_interactive_flow",
    "params": {
      "transaction_id": "1840201241",
      "amount_in": {
        "amount": "105",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "100",
        "asset": "stellar:USDC:G..."
      },
      "message": "reviewing KYC information" 
    }
  }
]
```

### request_funds

Updates the transaction with the information needed for the user / sender to send funds. This is relevant to all SEPs.

Currently, SEP-31 support in the Anchor Platform assumes the business is ready to receive funds if the customers have passed KYC in SEP-12. The Platform requests this information in the GET /unique_address endpoint.

However, we’ve learned that partners may have transaction-specific information to review (like AML compliance) per transaction. So in the future (or for initial SEP-24/SEP-6 support), the Platform should support the business not returning the information required for the user to send funds immediately, and support this action for providing that information later.

Deposit case:

```json
[
  {
    "jsonrpc": "2.0",
    "method": "request_funds",
    "params": {
      "transaction_id": "59014132454",
      "message": "Make an ACH or wire transfer to The Company with address 123 Address & reference number 123",
      "extra": { // SEP-6 supports providing extra info in a structured manner
        "business_name": "The Company",
        "business_address": "123 Address",
        "reference_number": "123"
      }
    }
  }
]
```

Withdraw / send case:

```json
[
  {
    "jsonrpc": "2.0",
    "method": "request_funds",
    "params": {
      "transaction_id": "59014132454",
      "message": "make a payment to the provided Stellar account & memo",
      "stellar_account": "G...", // only required if not configured to generate / fetch from custodian
      "memo": "1840104232" // only required if not configured to generate / fetch from custodian
    }
  }
]
```

### notify_funds_received

Update the user / sender that funds have been received either off-chain or on-chain. If the Anchor Platform has been configured to detect inbound payments, this action is unnecessary unless the business wants to provide a more specific status message.

The business may also update the amounts using this action, to support the case where the user / sender sent an amount different than was originally specified. 

If SEP-38 quotes were used, changing amounts is not accepted. In this case the business should cancel/expire/error the transaction and request the user initiate a new transaction with the updated amounts or refund the amount received.

```json
[
  {
    "jsonrpc": "2.0",
    "method": "notify_funds_received",
    "params": {
      "transaction_id": "59014132454",
      "stellar_transaction_id": "0dac12813c0dca12ce3...", // if withdraw / send & AP not configured to detect payments
      "funds_received_at": "<UTC datetime>", // if AP not configured to detect payments
      "external_transaction_id": "18012412399", // if deposit
      "amount_in": { // amounts only passed if update needed
        "amount": "105",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "100",
        "asset": "stellar:USDC:G..."
      },
      "message": "funds have been received"
    }
  }
]
```

### make_stellar_payment

Requests the Anchor Platform to submit an on-chain payment associated with a transaction. All information to perform the payment must be provided in prior update_transaction_data actions, including amounts,  fees, and memos. Only one make_stellar_payment request can be sent per transaction. 

Possible errors:
payment already requested
account doesn’t exist and account creation is not supported

The Anchor Platform will queue the payment to be made. If the account exists but does not have a trustline and claimable balances are supported, a claimable balance will be sent. If claimable balances are not supported the transaction will be placed in “pending_trust” status.

The Platform will wait until a trustline is established or the payment is canceled by the business using a cancel_stellar_payment action. update_transaction_status and update_transaction_data actions should also be taken to document the off-chain refund.

The Platform will automatically update transaction statuses and make status callbacks as necessary throughout this process.

```json
[
  {
    "jsonrpc": "2.0",
    "method": "make_stellar_payment",
    "params": {
      "transaction_id": "1840178401"
    }
  }
]
```

### make_stellar_refund

Requests the Anchor Platform to submit an on-chain refund payment associated with the transaction. Off-chain refund information should be communicated via update_transaction_data and update_transaction_status actions.

Account existence and trustline handling is the same as described in make_stellar_payment. Refund payments can also be canceled using cancel_stellar_payment.

Refunds and any fees charged will always be denominated in units of the asset originally received.

Because the business could make multiple refund payments, an idempotency key is required.  JSON RPC uses the “id” attribute for idempotency.

```json
[
  {
    "jsonrpc": "2.0",
    "method": "make_stellar_refund",
    "id": "17402749012",
    "params": {
      "transaction_id": "184501284",
      "refund": {
        "amount": "10.0",
        "fee": "2.0"
      }
    }
  }
]
```

### cancel_stellar_payment

Payment cancellations can be requested but are not guaranteed. If the Anchor Platform has submitted a Stellar transaction to Horizon or queued the payment through the configured custodian before detecting the cancellation request, the payment may still occur.

If a request to horizon or the configured custodian has not occurred, the Anchor Platform will remove the payment from its queue and update the transaction’s state accordingly.

```json
[
  {
    "jsonrpc": "2.0",
    "method": "cancel_stellar_payment",
    "params": {
      "transaction_id": "184018401",
      "refund_id": null
    }
  }
]
```
