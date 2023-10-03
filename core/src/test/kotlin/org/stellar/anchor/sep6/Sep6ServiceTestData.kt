package org.stellar.anchor.sep6

class Sep6ServiceTestData {
  companion object {
    val infoJson =
      """
        {
            "deposit": {
                "USDC": {
                    "enabled": true,
                    "authentication_required": true,
                    "min_amount": 1,
                    "max_amount": 10000,
                    "fields": {
                        "type": {
                            "description": "type of deposit to make",
                            "choices": [
                                "SEPA",
                                "SWIFT"
                            ],
                            "optional": false
                        }
                    }
                }
            },
            "deposit-exchange": {
                "USDC": {
                    "enabled": true,
                    "authentication_required": true,
                    "min_amount": 1,
                    "max_amount": 10000,
                    "fields": {
                        "type": {
                            "description": "type of deposit to make",
                            "choices": [
                                "SEPA",
                                "SWIFT"
                            ],
                            "optional": false
                        }
                    }
                }
            },
            "withdraw": {
                "USDC": {
                    "enabled": true,
                    "authentication_required": true,
                    "min_amount": 1,
                    "max_amount": 10000,
                    "types": {
                        "cash": {
                            "fields": {}
                        },
                        "bank_account": {
                            "fields": {}
                        }
                    }
                }
            },
            "withdraw-exchange": {
                "USDC": {
                    "enabled": true,
                    "authentication_required": true,
                    "min_amount": 1,
                    "max_amount": 10000,
                    "types": {
                        "cash": {
                            "fields": {}
                        },
                        "bank_account": {
                            "fields": {}
                        }
                    }
                }
            },
            "fee": {
                "enabled": false,
                "description": "Fee endpoint is not supported."
            },
            "transactions": {
                "enabled": true,
                "authentication_required": true
            },
            "transaction": {
                "enabled": true,
                "authentication_required": true
            },
            "features": {
                "account_creation": false,
                "claimable_balances": false
            }
        }
      """
        .trimIndent()

    val transactionsJson =
      """
      {
          "transactions": [
              {
                  "id": "2cb630d3-030b-4a0e-9d9d-f26b1df25d12",
                  "kind": "deposit",
                  "status": "complete",
                  "status_eta": 5,
                  "more_info_url": "https://example.com/more_info",
                  "amount_in": "100",
                  "amount_in_asset": "USD",
                  "amount_out": "98",
                  "amount_out_asset": "stellar:USDC:GABCD",
                  "amount_fee": "2",
                  "from": "GABCD",
                  "to": "GABCD",
                  "deposit_memo": "some memo",
                  "deposit_memo_type": "text",
                  "started_at": "2023-08-01T16:53:20Z",
                  "updated_at": "2023-08-01T16:53:20Z",
                  "completed_at": "2023-08-01T16:53:20Z",
                  "stellar_transaction_id": "stellar-id",
                  "external_transaction_id": "external-id",
                  "message": "some message",
                  "refunds": {
                      "amount_refunded": {
                          "amount": "100",
                          "asset": "USD"
                      },
                      "amount_fee": {
                          "amount": "0",
                          "asset": "USD"
                      },
                      "payments": [
                          {
                              "id": "refund-payment-id",
                              "id_type": "external",
                              "amount": {
                                  "amount": "100",
                                  "asset": "USD"
                              },
                              "fee": {
                                  "amount": "0",
                                  "asset": "USD"
                              }
                          }
                      ]
                  },
                  "required_info_message": "some info message",
                  "required_info_updates": ["first_name", "last_name"]
              }
          ]
      }
    """
        .trimIndent()

    val depositResJson =
      """
      {
          "how": "Check the transaction for more information about how to deposit."
      }
    """
        .trimIndent()

    val depositTxnJson =
      """
      {
          "status": "incomplete",
          "kind": "deposit",
          "type": "bank_account",
          "requestAssetCode": "USDC",
          "requestAssetIssuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "amountExpected": "100",
          "sep10Account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
          "toAccount": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO"
      }
    """
        .trimIndent()

    val depositTxnEventJson =
      """
      {
          "type": "transaction_created",
          "sep": "6",
          "transaction": {
              "sep": "6",
              "kind": "deposit",
              "status": "incomplete",
              "amount_expected": {
                  "amount": "100",
                  "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
              },
              "destination_account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO"
          }
      }
    """
        .trimIndent()

    val depositTxnJsonWithoutAmountOrType =
      """
      {
          "status": "incomplete",
          "kind": "deposit",
          "requestAssetCode": "USDC",
          "requestAssetIssuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "sep10Account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
          "toAccount": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO"
      }
    """
        .trimIndent()

    val depositTxnEventWithoutAmountOrTypeJson =
      """
      {
          "type": "transaction_created",
          "sep": "6",
          "transaction": {
              "sep": "6",
              "kind": "deposit",
              "status": "incomplete",
              "destination_account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO"
          }
      }
    """
        .trimIndent()

    val depositExchangeTxnJson =
      """
      {
          "status": "incomplete",
          "kind": "deposit-exchange",
          "type": "SWIFT",
          "requestAssetCode": "USDC",
          "requestAssetIssuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "amountIn": "100",
          "amountInAsset": "iso4217:USD",
          "amountOut": "98",
          "amountOutAsset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "amountFee": "2",
          "amountFeeAsset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "amountExpected": "100",
          "sep10Account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
          "toAccount": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
          "quoteId": "test-quote-id"
      }
    """
        .trimIndent()

    val depositExchangeTxnEventJson =
      """
      {
          "type": "transaction_created",
          "sep": "6",
          "transaction": {
              "sep": "6",
              "kind": "deposit-exchange",
              "status": "incomplete",
              "amount_expected": {
                  "amount": "100",
                  "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
              },
              "amount_in": {
                  "amount": "100",
                  "asset": "iso4217:USD"
              },
              "amount_out": {
                  "amount": "98",
                  "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
              },
              "amount_fee": {
                  "amount": "2",
                  "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
              },
              "quote_id": "test-quote-id",
              "destination_account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO"
          }
      }
    """
        .trimIndent()

    val depositExchangeTxnWithoutQuoteJson =
      """
      {
          "status": "incomplete",
          "kind": "deposit-exchange",
          "type": "SWIFT",
          "requestAssetCode": "USDC",
          "requestAssetIssuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "amountIn": "100",
          "amountInAsset": "iso4217:USD",
          "amountOut": "98",
          "amountOutAsset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "amountFee": "2",
          "amountFeeAsset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "amountExpected": "100",
          "sep10Account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
          "toAccount": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO"
      }
    """
        .trimIndent()

    val depositExchangeTxnEventWithoutQuoteJson =
      """
      {
          "type": "transaction_created",
          "sep": "6",
          "transaction": {
              "sep": "6",
              "kind": "deposit-exchange",
              "status": "incomplete",
              "amount_expected": {
                  "amount": "100",
                  "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
              },
              "amount_in": {
                  "amount": "100",
                  "asset": "iso4217:USD"
              },
              "amount_out": {
                  "amount": "98",
                  "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
              },
              "amount_fee": {
                  "amount": "2",
                  "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
              },
              "destination_account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO"
          }
      }
    """
        .trimIndent()

    val withdrawResJson =
      """
      {
          "account_id": "GA7FYRB5VREZKOBIIKHG5AVTPFGWUBPOBF7LTYG4GTMFVIOOD2DWAL7I",
          "memo_type": "hash"
      }
    """
        .trimIndent()

    val withdrawTxnJson =
      """
      {
          "status": "incomplete",
          "kind": "withdrawal",
          "type": "bank_account",
          "requestAssetCode": "USDC",
          "requestAssetIssuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "amountExpected": "100",
          "sep10Account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
          "withdrawAnchorAccount": "GA7FYRB5VREZKOBIIKHG5AVTPFGWUBPOBF7LTYG4GTMFVIOOD2DWAL7I",
          "fromAccount": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
          "toAccount": "GA7FYRB5VREZKOBIIKHG5AVTPFGWUBPOBF7LTYG4GTMFVIOOD2DWAL7I",
          "memoType": "hash",
          "refundMemo": "some text",
          "refundMemoType": "text"
      }
    """
        .trimIndent()

    val withdrawTxnEventJson =
      """
      {
          "type": "transaction_created",
          "sep": "6",
          "transaction": {
              "sep": "6",
              "kind": "withdrawal",
              "status": "incomplete",
              "amount_expected": {
                  "amount": "100",
                  "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
              },
              "source_account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
              "destination_account": "GA7FYRB5VREZKOBIIKHG5AVTPFGWUBPOBF7LTYG4GTMFVIOOD2DWAL7I",
              "memo_type": "hash",
              "refund_memo": "some text",
              "refund_memo_type": "text"
          }
      }
    """
        .trimIndent()

    val withdrawTxnWithoutAmountOrTypeJson =
      """
      {
          "status": "incomplete",
          "kind": "withdrawal",
          "requestAssetCode": "USDC",
          "requestAssetIssuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "sep10Account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
          "withdrawAnchorAccount": "GA7FYRB5VREZKOBIIKHG5AVTPFGWUBPOBF7LTYG4GTMFVIOOD2DWAL7I",
          "fromAccount": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
          "toAccount": "GA7FYRB5VREZKOBIIKHG5AVTPFGWUBPOBF7LTYG4GTMFVIOOD2DWAL7I",
          "memoType": "hash",
          "refundMemo": "some text",
          "refundMemoType": "text"
      }
    """
        .trimIndent()

    val withdrawTxnEventWithoutAmountOrTypeJson =
      """
      {
          "type": "transaction_created",
          "sep": "6",
          "transaction": {
              "sep": "6",
              "kind": "withdrawal",
              "status": "incomplete",
              "source_account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
              "destination_account": "GA7FYRB5VREZKOBIIKHG5AVTPFGWUBPOBF7LTYG4GTMFVIOOD2DWAL7I",
              "memo_type": "hash",
              "refund_memo": "some text",
              "refund_memo_type": "text"
          }
      }
    """
        .trimIndent()

    val withdrawExchangeTxnJson =
      """
      {
          "status": "incomplete",
          "kind": "withdrawal-exchange",
          "type": "bank_account",
          "requestAssetCode": "USDC",
          "requestAssetIssuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "amountIn": "100",
          "amountInAsset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "amountOut": "98",
          "amountOutAsset": "iso4217:USD",
          "amountFee": "2",
          "amountFeeAsset": "iso4217:USD",
          "amountExpected": "100",
          "sep10Account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
          "withdrawAnchorAccount": "GA7FYRB5VREZKOBIIKHG5AVTPFGWUBPOBF7LTYG4GTMFVIOOD2DWAL7I",
          "fromAccount": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
          "memoType": "hash",
          "quoteId": "test-quote-id",
          "refundMemo": "some text",
          "refundMemoType": "text"
      }
    """
        .trimIndent()

    val withdrawExchangeTxnEventJson =
      """
      {
          "type": "transaction_created",
          "sep": "6",
          "transaction": {
              "sep": "6",
              "kind": "withdrawal-exchange",
              "status": "incomplete",
              "amount_expected": {
                  "amount": "100",
                  "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
              },
              "amount_in": {
                  "amount": "100",
                  "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
              },
              "amount_out": {
                  "amount": "98",
                  "asset": "iso4217:USD"
              },
              "amount_fee": {
                  "amount": "2",
                  "asset": "iso4217:USD"
              },
              "quote_id": "test-quote-id",
              "source_account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
              "memo_type": "hash",
              "refund_memo": "some text",
              "refund_memo_type": "text"
          }
      }
    """
        .trimIndent()

    val withdrawExchangeTxnWithoutQuoteJson =
      """
      {
          "status": "incomplete",
          "kind": "withdrawal-exchange",
          "type": "bank_account",
          "requestAssetCode": "USDC",
          "requestAssetIssuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "amountIn": "100",
          "amountInAsset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "amountOut": "98",
          "amountOutAsset": "iso4217:USD",
          "amountFee": "2",
          "amountFeeAsset": "iso4217:USD",
          "amountExpected": "100",
          "sep10Account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
          "withdrawAnchorAccount": "GA7FYRB5VREZKOBIIKHG5AVTPFGWUBPOBF7LTYG4GTMFVIOOD2DWAL7I",
          "fromAccount": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
          "memoType": "hash",
          "refundMemo": "some text",
          "refundMemoType": "text"
      }
    """
        .trimIndent()

    val withdrawExchangeTxnWithoutQuoteEventJson =
      """
      {
          "type": "transaction_created",
          "sep": "6",
          "transaction": {
              "sep": "6",
              "kind": "withdrawal-exchange",
              "status": "incomplete",
              "amount_expected": {
                  "amount": "100",
                  "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
              },
              "amount_in": {
                  "amount": "100",
                  "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
              },
              "amount_out": {
                  "amount": "98",
                  "asset": "iso4217:USD"
              },
              "amount_fee": {
                  "amount": "2",
                  "asset": "iso4217:USD"
              },
              "source_account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
              "memo_type": "hash",
              "refund_memo": "some text",
              "refund_memo_type": "text"
          }
      }
    """
        .trimIndent()
  }
}
