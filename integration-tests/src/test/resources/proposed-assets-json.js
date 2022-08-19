var proposedAssetsJson = {
    "assets": [
    {
        "schema": "stellar",
        "code": "USDC",
        "issuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",

        //move to assets: config
        // "distribution_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",

        "significant_decimals": 2,
        "sep24" : {
            "deposit" : {
                "min_amount": 1,
                "max_amount": 1000000
            },
            "withdraw": {
                "min_amount": 1,
                "max_amount": 1000000
            }
        },
        "sep31" : {
            "quotes_supported": true,
            "quotes_required": false,
            "min_amount": 1,
            "max_amount": 1000000,
            "sep12": {
                "sender": {
                    "types": {
                        "sep31-sender": {
                            "description": "U.S. citizens limited to sending payments of less than $10,000 in value"
                        },
                        "sep31-large-sender": {
                            "description": "U.S. citizens that do not have sending limits"
                        },
                        "sep31-foreign-sender": {
                            "description": "non-U.S. citizens sending payments of less than $10,000 in value"
                        }
                    }
                },
                "receiver": {
                    "types": {
                        "sep31-receiver": {
                            "description": "U.S. citizens receiving USD"
                        },
                        "sep31-foreign-receiver": {
                            "description": "non-U.S. citizens receiving USD"
                        }
                    }
                }
            },
            "fields": {
                "transaction": {
                    "receiver_routing_number": {
                        "description": "routing number of the destination bank account"
                    },
                    "receiver_account_number": {
                        "description": "bank account number of the destination"
                    },
                    "type": {
                        "description": "type of deposit to make",
                        "choices": [
                            "SEPA",
                            "SWIFT"
                        ]
                    }
                }
            }
        },
        "sep38": {
            "exchangeable_assets": [
                "iso4217:USD"
            ]
        }
    },
    {
        "schema": "iso4217",
        "code": "USD",
        "significant_decimals": 2,
        "sep24": {
            "deposit" : {
                "min_amount": 0,
                "max_amount": 10000
            },
            "withdraw": {
                "min_amount": 0,
                "max_amount": 10000
            }
        },
        "sep38": {
            "exchangeable_assets": [
                "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
            ],
            "country_codes": ["USA"],
            "sell_delivery_methods": [
                {
                    "name": "WIRE",
                    "description": "Send USD directly to the Anchor's bank account."
                }
            ],
            "buy_delivery_methods": [
                {
                    "name": "WIRE",
                    "description": "Have USD sent directly to your bank account."
                }
            ]
        }
    }
]
}


