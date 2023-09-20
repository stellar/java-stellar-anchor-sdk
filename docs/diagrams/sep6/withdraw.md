### Withdraw

This diagram illustrates the withdraw flow. The flow starts after the user has authenticated with the anchor via SEP-10 and provided some basic information about themselves. The example here shows the user withdrawing funds to their bank account, but the flow is similar for other withdrawal methods. The `withdraw-exchange` flow works similarly, but the Platform will additionally verify the quote requested or make a call to the Fee integration to update amounts.

For more information on the withdraw flow, see the [SEP-6](https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0006.md) specification.

```mermaid
sequenceDiagram
  participant User
  participant Wallet
  participant Platform
  participant Stellar
  participant Anchor
  participant Bank

  Wallet->>+Platform: GET /withdraw
  Platform->>Platform: Creates withdraw transaction with id abcd-1234 with status incomplete
  Platform-->>-Wallet: Returns withdraw response with id abcd-1234
  Platform-)Anchor: Sends an AnchorEvent with type transaction_created and transaction id abcd-1234
  loop until status is pending_user_transfer_start
    Anchor->>Anchor: Evaluates whether additional KYC/financial account information is required
    alt requires additional KYC or financial account information
      Anchor->>+Platform: PATCH /transaction abcd-1234 with status pending_customer_info_update and required_customer_info_updates fields
      Platform-->>-Anchor: Returns success response
      Platform-)Wallet: Sends an AnchorEvent with type transaction_status_changed
      Wallet->>User: Prompts user to update customer fields
      Wallet->>+Platform: PUT [SEP-12]/customer to provide updated customer fields
      Platform-->>-Wallet: Returns success response
      Platform-)Anchor: Sends an AnchorEvent with type customer_updated
    else no additional KYC or financial account information required
      Anchor->>+Platform: PATCH /transaction abcd-1234 with status pending_user_transfer_start
      Platform-->>-Anchor: Returns success response
    end
  end
  Platform-)Wallet: Sends an AnchorEvent with type transaction_status_changed
  Wallet->>+Stellar: Submits payment transaction to the Anchor's Stellar account
  Stellar-->>-Wallet: Returns success response
  Stellar-)Platform: Receives a payment transaction from the user
  Platform->>Platform: Patches the transaction with status pending_anchor
  Platform-)Anchor: Sends an AnchorEvent with type transaction_status_changed
  Anchor->>+Bank: Sends a payment transaction to the user's bank account
  Bank-->>-Anchor: Returns success response
  Anchor->>+Platform: PATCH /transaction abcd-1234 with status complete
  Platform-->>-Anchor: Returns success response
  Platform-)Wallet: Sends an AnchorEvent with type transaction_status_changed
  Wallet->>User: Notifies user that withdraw is complete
```
