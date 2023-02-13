import argparse
import os
import requests
import json
import base64
import time
from datetime import datetime, timedelta
from pprint import pprint

from stellar_sdk import (
    Asset,
    Keypair,
    Network,
    TransactionEnvelope,
    TransactionBuilder,
    Server,
)
from stellar_sdk.sep.federation import fetch_stellar_toml

STELLAR_TESTNET_NETWORK_PASSPHRASE = "Test SDF Network ; September 2015"
HORIZON_URI = "https://horizon-testnet.stellar.org"
FRIENDBOT_URI = "https://friendbot.stellar.org"

TRANSACTION_STATUS_COMPLETE = "completed"


class Endpoints:
    def __init__(self, domain, use_http=True):
        toml = fetch_stellar_toml(domain, use_http=use_http)
        self.ANCHOR_PLATFORM_AUTH_ENDPOINT = toml.get("WEB_AUTH_ENDPOINT")
        self.ANCHOR_PLATFORM_SEP12_CUSTOMER_ENDPOINT = toml.get("KYC_SERVER") + "/customer"
        self.ANCHOR_PLATFORM_SEP38_QUOTE_ENDPOINT = toml.get("ANCHOR_QUOTE_SERVER") + "/quote"
        self.ANCHOR_PLATFORM_SEP31_TRANSACTIONS_ENDPOINT = toml.get("DIRECT_PAYMENT_SERVER") + "/transactions"


def get_anchor_platform_token(endpoints, public_key, secret_key):
    print("============= Getting Token from Anchor Platform ========================")
    response = requests.get(endpoints.ANCHOR_PLATFORM_AUTH_ENDPOINT, {"account": public_key})
    content = response.json()

    envelope_xdr = content["transaction"]
    envelope = TransactionEnvelope.from_xdr(
        envelope_xdr, network_passphrase=STELLAR_TESTNET_NETWORK_PASSPHRASE
    )
    envelope.sign(secret_key)
    response = requests.post(
        endpoints.ANCHOR_PLATFORM_AUTH_ENDPOINT,
        data={"transaction": envelope.to_xdr()},
    )
    print(response)
    content = json.loads(response.content)
    return content["token"]


def create_anchor_test_quote(endpoints, headers, payload):
    print("===================== Creating SEP-38 Quote ====================================")
    print("Request Payload:")
    pprint(payload)
    expire = (datetime.utcnow() + timedelta(days=2)).strftime('%Y-%m-%dT%H:%M:%S.%fZ')
    payload["expire_after"] = expire
    create_quote = requests.post(endpoints.ANCHOR_PLATFORM_SEP38_QUOTE_ENDPOINT, data=json.dumps(payload),
                                 headers=headers)
    print("Quote:")
    quote = json.loads(create_quote.content)
    pprint(quote)
    return quote


def get_transaction(endpoints, headers, transaction_id):
    transaction = requests.get(endpoints.ANCHOR_PLATFORM_SEP31_TRANSACTIONS_ENDPOINT + "/" + transaction_id,
                               headers=headers)
    res = json.loads(transaction.content)
    return res


SENDING_CLIENT_PAYLOAD = {
    "first_name": "Allie",
    "last_name": "Grater",
    "email_address": "allie@email.com"
}

RECEIVING_CLIENT_PAYLOAD = {
    "first_name": "John",
    "last_name": "Doe",
    "address": "123 Washington Street",
    "city": "San Francisco",
    "state_or_province": "CA",
    "address_country_code": "US",
    "clabe_number": "1234",
    "bank_number": "abcd",
    "bank_account_number": "1234",
    "bank_account_type": "checking"
}


def create_anchor_test_customer(endpoints, headers, payload):
    print("============= Creating Customer in Anchor Platform ======================")
    create_customer = requests.put(endpoints.ANCHOR_PLATFORM_SEP12_CUSTOMER_ENDPOINT,
                                   data=json.dumps(payload), headers=headers)
    res = json.loads(create_customer.content)
    return res["id"]


def create_anchor_test_transaction(endpoints, headers, payload, quote_id=None):
    print("============= Creating Transaction in Anchor Platform ===================")

    if quote_id:
        payload["quote_id"] = quote_id
    create_transaction = requests.post(endpoints.ANCHOR_PLATFORM_SEP31_TRANSACTIONS_ENDPOINT,
                                       data=json.dumps(payload), headers=headers)
    res = json.loads(create_transaction.content)
    pprint(res)
    return res


def send_asset(asset, source_secret_key, receiver_public_key, memo_hash):
    print("============= Send Asset on Stellar Network =============================")

    source_keypair = Keypair.from_secret(source_secret_key)
    source_public_key = source_keypair.public_key
    server = Server(horizon_url="https://horizon-testnet.stellar.org")

    source_account = server.load_account(source_public_key)

    base_fee = 100

    transaction = (
        TransactionBuilder(
            source_account=source_account,
            network_passphrase=Network.TESTNET_NETWORK_PASSPHRASE,
            base_fee=base_fee,
        )
            .add_hash_memo(memo_hash)
            .append_payment_op(receiver_public_key, asset, "10")
            .set_timeout(30)
            .build()
    )

    transaction.sign(source_keypair)

    print(transaction.to_xdr())

    response = server.submit_transaction(transaction)


def poll_transaction_status(endpoints, headers, transaction_id, status=TRANSACTION_STATUS_COMPLETE, timeout=120,
                        poll_interval=2):
    """
    :param endpoints: Anchor Platform endpoints
    :param headers: Request headers
    :param transaction_id: Transaction ID to poll for completion
    :param status: The desired status to poll for
    :param timeout: Time (in seconds) to poll for the desired status
    :param poll_interval: Time (in seconds) between each poll attempt
    :return:
    """
    attempt = 1
    print("============= Polling Transaction Status from Anchor Platform ===========")
    while True and attempt*poll_interval <= timeout:
        transaction_status = get_transaction(endpoints, headers, transaction_id)["transaction"]["status"]
        print(f"attempt #{attempt} - transaction - {transaction_id} status is {transaction_status}")
        if transaction_status == status:
            break
        attempt += 1
        time.sleep(poll_interval)
    else:
        raise Exception("error: timed out while polling transaction status")
    print("=========================================================================")

def test_sep_31_flow(endpoints, keypair, transaction_payload, sep38_payload=None):
    """
    :param endpoints: Anchor Platform endpoints
    :param keypair: Stellar Account keypair
    :param transaction_payload: Payload to create a transaction in the Anchor Platform
    :param sep38_payload: Payload to request a quote from the Anchor Platform and use that quote in the sep31 request
    :return:
    """
    public_key = keypair.public_key
    secret_key = keypair.secret

    token = get_anchor_platform_token(endpoints, public_key, secret_key)

    headers = {"Authorization": f"Bearer {token}", 'Content-Type': 'application/json'}

    sender_id = create_anchor_test_customer(endpoints, headers, SENDING_CLIENT_PAYLOAD)
    transaction_payload["sender_id"] = sender_id
    receiver_id = create_anchor_test_customer(endpoints, headers, RECEIVING_CLIENT_PAYLOAD)
    transaction_payload["receiver_id"] = receiver_id

    if sep38_payload:
        quote = create_anchor_test_quote(endpoints, headers, sep38_payload)
        transaction = create_anchor_test_transaction(endpoints, headers, transaction_payload, quote["id"])
    else:
        transaction = create_anchor_test_transaction(endpoints, headers, transaction_payload)


    memo_hash = transaction["stellar_memo"]
    memo_hash = base64.b64decode(memo_hash)

    asset = Asset(transaction_payload["asset_code"], transaction_payload["asset_issuer"])
    send_asset(asset, secret_key, transaction["stellar_account_id"], memo_hash)

    try:
        poll_transaction_status(endpoints, headers, transaction["id"])
    except Exception as e:
        print(e)
        exit(1)

def test_sep38_create_quote(endpoints, keypair, payload):
    token = get_anchor_platform_token(endpoints, keypair.public_key, keypair.secret)

    headers = {"Authorization": f"Bearer {token}", 'Content-Type': 'application/json'}
    #res = requests.get("http://localhost:8080/sep38/prices?sell_asset=iso4217:USD&sell_amount=10", headers=headers)
    #print(res.content)
    quote = create_anchor_test_quote(endpoints, headers, payload)
    return quote

def wait_for_anchor_platform_ready(domain, poll_interval=3, timeout=180):
    print(f"polling {domain}/.well-known/stellar.toml to check for readiness")
    attempt = 1
    while attempt*poll_interval <= timeout:
        try:
            toml = fetch_stellar_toml(domain, use_http=True)
            print("anchor platform is ready")
            return
        except Exception as e:
            print(e)
        attempt += 1
        time.sleep(poll_interval)
    else:
        print("error: timed out while polling for readiness")
        exit(1)


if __name__ == "__main__":
    TESTS = [
        "sep31_flow",
        "sep31_flow_with_sep38",
        "sep38_create_quote",
        "omnibus_allowlist"
    ]

    parser = argparse.ArgumentParser()
    #parser.add_argument('--verbose', '-v', help="verbose mode", type=bool, default=False) TODO
    #parser.add_argument('--load-size', "-ls", help="number of tests to execute (multithreaded)", type=int, default=1)
    parser.add_argument('--tests', "-t", nargs="*", help=f"names of tests to execute: {TESTS}", default=TESTS)
    parser.add_argument('--domain', "-d", help="The Anchor Platform endpoint", default="http://localhost:8000")
    parser.add_argument('--secret', "-s", help="The secret key used for transactions", default=os.environ.get('E2E_SECRET'))
    parser.add_argument('--delay', help="Seconds to delay before running the tests", default=0)

    args = parser.parse_args()

    if args.delay:
        print(f"delaying start by {args.delay} seconds")
        time.sleep(int(args.delay))

    domain = args.domain

    wait_for_anchor_platform_ready(domain)

    endpoints = Endpoints(args.domain)
    keypair = Keypair.from_secret(args.secret)

    tests = TESTS if args.tests[0] == "all" else args.tests

    for test in tests:
        if test == "sep31_flow":
            print("####################### Testing SEP-31 Send Flow #######################")
            TRANSACTION_PAYLOAD = {
                "amount": "10.0",
                "asset_code": "USDC",
                "asset_issuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
                "fields": {
                    "transaction": {
                        "receiver_routing_number": "r0123",
                        "receiver_account_number": "a0456",
                        "type": "SWIFT"
                    }
                }
            }
            test_sep_31_flow(endpoints, keypair, TRANSACTION_PAYLOAD)
        elif test == "sep31_flow_with_sep38":
            QUOTE_PAYLOAD_USDC_TO_JPYC = {
                "sell_asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
                "sell_amount": "10",
                "buy_asset": "stellar:JPYC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
                "context": "sep31"
            }

            TRANSACTION_PAYLOAD_USDC_TO_JPYC = {
                "amount": "10.0",
                "asset_code": "USDC",
                "asset_issuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
                "fields": {
                    "transaction": {
                        "receiver_routing_number": "r0123",
                        "receiver_account_number": "a0456",
                        "type": "SWIFT"
                    }
                }
            }

            QUOTE_PAYLOAD_JPYC_TO_USD = {
                "sell_asset": "stellar:JPYC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
                "sell_amount": "10",
                "buy_asset": "iso4217:USD",
                "context": "sep31"
            }

            TRANSACTION_PAYLOAD_JPYC_TO_USD = {
                "amount": "10.0",
                "asset_code": "JPYC",
                "asset_issuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
                "fields": {
                    "transaction": {
                        "receiver_routing_number": "r0123",
                        "receiver_account_number": "a0456",
                        "type": "SWIFT"
                    }
                }
            }
            print("####################### Testing SEP-31/38 USDC to JPYC Flow #######################")
            test_sep_31_flow(endpoints, keypair, TRANSACTION_PAYLOAD_USDC_TO_JPYC,
                             sep38_payload=QUOTE_PAYLOAD_USDC_TO_JPYC)
            print()
            print()
            print("####################### Testing SEP-31/38 JPYC to USD Flow #######################")
            test_sep_31_flow(endpoints, keypair, TRANSACTION_PAYLOAD_JPYC_TO_USD,
                             sep38_payload=QUOTE_PAYLOAD_JPYC_TO_USD)
        elif test == "sep38_create_quote":
            print("####################### Testing POST Quote #######################")
            QUOTE_PAYLOAD_USDC_TO_JPYC = {
                "sell_asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
                "sell_amount": "10",
                "buy_asset": "stellar:JPYC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
                "context": "sep31"
            }
            test_sep38_create_quote(endpoints, keypair, QUOTE_PAYLOAD_USDC_TO_JPYC)
        elif test == "omnibus_allowlist":
            print("####################### Testing Omnibus Allowlist #######################")
            print(f"Omnibus Allowlist - testing with allowed key: {keypair.public_key}")
            response = requests.get(endpoints.ANCHOR_PLATFORM_AUTH_ENDPOINT, {"account": keypair.public_key})
            assert response.status_code == 200, f"return code should be 200, got: {response.status_code}"
            print(f"Omnibus Allowlist - testing with allowed key: {keypair.public_key} - success")

            random_kp = Keypair.random()
            print(f"Omnibus Allowlist - testing with disallowed (random) key: {random_kp.public_key}")
            response = requests.get(endpoints.ANCHOR_PLATFORM_AUTH_ENDPOINT, {"account": random_kp.public_key})
            assert response.status_code == 403, f"return code should be 403, got: {response.status_code}"
            print(f"Omnibus Allowlist - testing with disallowed (random) key: {random_kp.public_key} expecting 403 error - success")

        else:
            exit(f"Error: unknown test {test}")
