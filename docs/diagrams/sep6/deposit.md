### Deposit

This diagram illustrates how Anchors can provide deposit instructions to a user asynchronously. The flow starts from after the user has authenticated with the anchor via SEP-10 and provided some basic information about themselves. The example here requires the user deposits fund to the Anchor's bank account, but the flow is similar for other deposit methods. The `deposit-exchange` flow works similarly, but the Platform will additionally verify the quote requested or make a call to the Fee integration to update amounts.

For more information on the deposit flow, see the [SEP-6](https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0006.md) specification.

```mermaid
sequenceDiagram
  participant Bank
  participant User
  participant Wallet
  participant Platform
  participant Stellar
  participant Anchor

  Wallet->>Platform: GET /deposit
  Platform-->>Platform: Creates deposit transaction with id abcd-1234 with status incomplete
  Platform-->Wallet: Returns deposit response with id abcd-1234 and deposit instructions omitted
  Platform--)Anchor: Sends an AnchorEvent with type transaction_created and transaction id abcd-1234
  loop until timeout or status is pending_user_transfer_start
    Wallet->>Platform: GET /transaction abcd-1234
    Anchor--)Anchor: Evaluates whether additional KYC is required whenever KYC is updated (customer_updated events or PUT /customer callback)
    alt requires additional KYC
      Note over Anchor,Platform: PATCH is shown before returning the transaction response for clarity
      Anchor->>Platform: PATCH /transaction abcd-1234 with status pending_customer_info_update and required_customer_info_updates fields
      Platform-->>Anchor: Returns success response
      Platform-->Wallet: Returns transaction response
      Wallet-->User: Prompts user to update customer fields
      Wallet->>Platform: PUT [SEP-12]/customer to provide updated customer fields
      Platform-->>Wallet: Returns success response
      Platform--)Anchor: Sends an AnchorEvent with type customer_updated
    else no additional KYC required
      Note over Anchor,Platform: PATCH is shown before returning the transaction response for clarity
      Anchor->>Platform: PATCH /transaction abcd-1234 with status pending_user_transfer_start and deposit instructions
      Platform-->>Anchor: Returns success response
      Platform-->Wallet: Returns transaction response
    end
  end
  Wallet->>Platform: GET /transaction abcd-1234
  Platform-->>Wallet: Returns transaction response with status pending_user_transfer_start and deposit instructions
  Wallet->>User: Prompts user to send funds using deposit instructions
  User->>Bank: Sends off-chain funds to Anchor's bank account
  loop until timeout or funds received
    Anchor->>Bank: Polls bank account for funds
    Bank-->>Anchor: Returns whether funds were received
  end
  Anchor->>Stellar: Submits payment transaction to the user's account
  Stellar-->>Anchor: Returns success response
  Anchor->>Platform: PATCH /transaction abcd-1234 with status completed
  Platform-->>Anchor: Returns success response
  Wallet->>Platform: GET /transaction abcd-1234
  Platform-->>Wallet: Returns transaction response with status completed
  Wallet->>User: Notifies user that deposit is complete
```
