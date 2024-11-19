# Changelog

## Unreleased

## 1.0.13
* Upgrade to JDK-17
* Migrate `javax` to `jakarta` packages


## 1.0.12

### Updates

* Support for accepting `refund_memo` and `refund_memo_type` parameters in SEP-24 `POST /transactions/withdraw/interactive` requests
    * Add the following methods to `org.stellar.anchor.model.Sep24Transaction` interface:
        * `String getRefundMemo()`
        * `void setRefundMemo(String refundMemo)`
        * `String getRefundMemoType()`
        * `void setRefundMemoType(String refundMemoType)`
    * `Sep24Service.withdraw()` accepts the `refund_memo` & `refund_memo_type` parameters and assigns the values using the `setRefundMemo()` and `setRefundMemoType()` methods.
