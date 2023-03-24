# Changelog

## Unreleased

## 1.0.12

### Updates
* The java-stellar-anchor-sdk currently supports SEP-24 Protocol Release [V3.0.0](https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0024.md#changelog)
* Please note, optional features may not be implemented by your service provider using this SDK. Please consult with your service provider to review any optional features that you may require. 
* Please subscribe to this reposistory on the `core-release-1.0` branch to be notified of any updates.  Consult with your service provider to determine which version of the SDK the currently support.
* Support for accepting `refund_memo` and `refund_memo_type` parameters in SEP-24 `POST /transactions/withdraw/interactive` requests
    * Add the following methods to `org.stellar.anchor.model.Sep24Transaction` interface:
        * `String getRefundMemo()`
        * `void setRefundMemo(String refundMemo)`
        * `String getRefundMemoType()`
        * `void setRefundMemoType(String refundMemoType)`
    * `Sep24Service.withdraw()` accepts the `refund_memo` & `refund_memo_type` parameters and assigns the values using the `setRefundMemo()` and `setRefundMemoType()` methods.
