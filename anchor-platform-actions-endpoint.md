# Anchor Platform RPC API

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

```js
[
  {
    "jsonrpc": "2.0",
    "method": "notify_interactive_flow_complete",
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

### request_offchain_funds

Updates the transaction with the information needed for the user / sender to send funds off-chain. This is relevant for SEP-6 & 24

```js
[
  {
    "jsonrpc": "2.0",
    "method": "request_offchain_funds",
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

### request_stellar_funds

Updates the transaction with information needed for the user / sender to send funds on-chain. This is relavant to all SEPs.

```js
[
  {
    "jsonrpc": "2.0",
    "method": "request_stellar_funds",
    "params": {
      "transaction_id": "59014132454",
      "message": "make a payment to the provided Stellar account & memo",
      "stellar_account": "G...", // only required if not configured to generate / fetch from custodian
      "memo": "1840104232" // only required if not configured to generate / fetch from custodian
    }
  }
]
```

### notify_offchain_funds_received

Update the user / sender that funds have been received off-chain. 

The business may also update the amounts using this action, to support the case where the user / sender sent an amount different than was originally specified. 

If SEP-38 quotes were used, changing amounts is not accepted. In this case the business should cancel/expire/error the transaction and request the user initiate a new transaction with the updated amounts or refund the amount received.

```js
[
  {
    "jsonrpc": "2.0",
    "method": "notify_offchain_funds_received",
    "params": {
      "transaction_id": "59014132454",
      "funds_received_at": "<UTC datetime>",
      "external_transaction_id": "18012412399",
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

### notify_stellar_funds_received

Updates the user / sender that funds have been received on Stellar. If the Anchor Platform has been configured to detect inbound payments, this action is unnecessary unless the business wants to provide a more specific status message.

The business may also update the amounts using this action, to support the case where the user / sender sent an amount different than was originally specified. 

If SEP-38 quotes were used, changing amounts is not accepted. In this case the business should cancel/expire/error the transaction and request the user initiate a new transaction with the updated amounts or refund the amount received.

```js
[
  {
    "jsonrpc": "2.0",
    "method": "notify_stellar_funds_received",
    "params": {
      "transaction_id": "59014132454",
      "stellar_transaction_id": "0dac12813c0dca12ce3...",
      "funds_received_at": "<UTC datetime>",
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

### notify_offchain_funds_delivered

Updates the transaction to notify the user / sender that the business has initiated or completed the off-chain payment.

If the non-Stellar payment rails used by the business can provide information on whether the funds have been delivered, then it is preferred that the business update the payment as “pending_external”. If the fails used do not provide this information, or at a later time the funds are delivered, the business should update the transaction as delivered.

```js
[
  {
    "jsonrpc": "2.0",
    "method": "notify_funds_sent",
    "params": {
      "transaction_id": "59014132454",
      "funds_delivered_at": "<UTC datetime>",
      "external_transaction_id": "18012412399",
      "message": "an ACH transfer has been initiated and your funds should be deposited into your  account within 3-5 business days",
    }
  }
]
```

### notify_stellar_funds_delivered

Updates the transaction to notify the user / sender that the business has sent the on-chain payment. Only necessary if the Anchor Platform is not configured to send Stellar payments. Use `make_stellar_payment` to send Stellar payments using the Platform.

```js
[
  {
    "jsonrpc": "2.0",
    "method": "notify_stellar_funds_delivered",
    "params": {
      "transaction_id": "59014132454",
      "stellar_transaction_id": "0dac12813c0dca12ce3...",
      "funds_sent_at": "<UTC datetime>",
      "message": "an ACH transfer has been initiated and your funds should be deposited into your  account within 3-5 business days",
    }
  }
]
```

### notify_funds_available

Used when the business does not deliver funds and instead requires the user / recipient to pick up or collect funds off-chain.

```js
[
  {
    "jsonrpc": "2.0",
    "method": "notify_offchain_funds_available",
    "params": {
      "transaction_id": "59014132454",
      "funds_made_available_at": "<UTC datetime>",
      "external_transaction_id": "18012412399",
      "message": "pick up cash at 123 Address",
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

```js
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

```js
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

### notify_stellar_refund_delivered

```js
[
  {
    "jsonrpc": "2.0",
    "method": "notify_stellar_refund_delivered",
    "params": {
      "transaction_id": "17402749012",
      "refund": {
        "id": "0dac12813c0dca12ce3...",
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

```js
[
  {
    "jsonrpc": "2.0",
    "method": "cancel_stellar_payment",
    "params": {
      "transaction_id": "184018401"
    }
  }
]
```

### cancel_stellar_refund

Payment cancellations can be requested but are not guaranteed. If the Anchor Platform has submitted a Stellar transaction to Horizon or queued the payment through the configured custodian before detecting the cancellation request, the payment may still occur.

If a request to horizon or the configured custodian has not occurred, the Anchor Platform will remove the payment from its queue and update the transaction’s state accordingly.

```js
[
  {
    "jsonrpc": "2.0",
    "method": "cancel_stellar_refund",
    "params": {
      "transaction_id": "184018401",
      "refund_id": "502033"
    }
  }
]
```

## Example: SEP-24 deposit, using Anchor Platform for payments

Lets say a _deposit_ interactive flow was served, the user completed the flow, and the business is ready to receive funds. Note that its possible to notify the platform that the interactive flow is complete without being ready to receive funds. In this case you would send the 2nd object included in the payload below at a later time.

```js
[
  {
    "jsonrpc": "2.0",
    "method": "notify_interactive_flow_complete",
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
      }
    }
  },
  {
    "jsonrpc": "2.0",
    "method": "request_offchain_funds",
    "params": {
      "transaction_id": "1840201241",
      "message": "Make an ACH or wire transfer to The Company with address 123 Address & reference number 123"
    }
  }
]
```

Ok, now the user has send the ACH transfer to the correct bank account, with the correct reference number, and for the expected amount. Lets notify the wallet and send funds to the user.

```js
[
  {
    "jsonrpc": "2.0",
    "method": "notify_offchain_funds_received",
    "params": {
      "transaction_id": "1840201241",
      "funds_received_at": "<UTC datetime>",
      "external_transaction_id": "18012412399",
      "message": "funds have been received"
    } 
  },
  {
    "jsonrpc": "2.0",
    "method": "make_stellar_payment",
    "params": {
      "transaction_id": "1840201241"
    }
  }
]
```

Thats it! Done.

## Example: SEP-24 withdraw, not using Anchor Platform for payments, refund occurs

Lets say a _withdraw_ interactive flow was served, the user completed the flow, and the business is ready to receive funds on Stellar. Note that its possible to notify the platform that the interactive flow is complete without being ready to receive funds. In this case you would send the 2nd object included in the payload below at a later time.

Also, if the business wanted the anchor platform to generate the receive address & memo, they would just omit those attributes and make sure the Anchor Platform is configured to connect to their custody service.

```js
[
  {
    "jsonrpc": "2.0",
    "method": "notify_interactive_flow_complete",
    "params": {
      "transaction_id": "1840201241",
      "amount_in": {
        "amount": "105",
        "asset": "stellar:USDC:G..."
      },
      "amount_fee": {
        "amount": "5",
        "asset": "stellar:USDC:G..."
      },
      "amount_out": {
        "amount": "100",
        "asset": "iso4217:USD"
      }
    }
  },
  {
    "jsonrpc": "2.0",
    "method": "request_stellar_funds",
    "params": {
      "transaction_id": "1840201241",
      "message": "make a payment to the provided Stellar account & memo",
      "stellar_account": "G...",
      "memo": "175001348"
    }
  }
]
```

Now lets say the user sends the Stellar payment. Remember, the business uses another system to detect inbound payments and doesn't use the Anchor Platform's payment observer. So the business should update the Platform with that information. 

```js
[
  {
    "jsonrpc": "2.0",
    "method": "notify_stellar_funds_received",
    "params": {
      "transaction_id": "1840201241",
      "stellar_transaction_id": "0dac12813c0dca12ce3...",
      "funds_received_at": "<UTC datetime>",
      "message": "funds have been received, processing ACH deposit"
    }
  }
]
```

And then the business initiates the ACH deposit. The business knows it will take some time to deliver, but won't be able to know when it is officially delivered, so it updates as complete on their end.

```js
[
  {
    "jsonrpc": "2.0",
    "method": "notify_offchain_funds_delivered",
    "params": {
      "transaction_id": "1840201241",
      "funds_delivered_at": "<UTC datetime>",
      "external_transaction_id": "18012412399",
      "message": "an ACH transfer has been initiated and your funds should be deposited into your  account within 3-5 business days",
    }
  }
]
```

But wait! An error occured trying to send the ACH deposit. The business sends a refund payment over Stellar using some other system, and updates the Anchor Platform.

```js
[
  {
    "jsonrpc": "2.0",
    "method": "notify_stellar_refund_delivered",
    "params": {
      "transaction_id": "17402749012",
      "refund": {
        "id": "0dac12813c0dca12ce3...",
        "amount": "10.0",
        "fee": "2.0"
      }
    }
  }
]
```
