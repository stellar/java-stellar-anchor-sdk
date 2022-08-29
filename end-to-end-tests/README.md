# Anchor Platform End-to-End Tests

end_to_end_tests.py is a Python CLI tool used to test end-to-end Anchor Platform + Anchor Server data flows

## Available Tests

### sep31_flow
1) (End-to-End Test) Get the SEP endpoints/keys from the Platform Server TOML file (*<PLATFORM_SERVER_ENDPOINT>/.well-known/stellar.toml*) 
2) (End-to-End Test) Get a token from the *GET <PLATFORM_SERVER_ENDPOINT>/<WEB_AUTH_ENDPOINT>* endpoint to authenticate all further requests
3) (End-to-End Test) Make a *PUT <PLATFORM_SERVER_ENDPOINT>/<KYC_SERVER>/customer* request to create a customer
4) (End-to-End Test) Create a transaction using the *POST <SEP_31_ENDPOINT>/transactions* endpoint
5) (End-to-End Test) Send the Asset on the Stellar Network to the *stellar_account_id* address that was returned in step 4
6) The Platform Server's Payment Observer detects the payment on the Stellar Network and publishes a message to the event queue
7) The Anchor Reference Server detects the published event, "processes" the event and makes a PATCH request back to the Platform Server to update the transaction's status as "complete"
8) (End-to-End Test) Polls the Platform Server for the transaction to be marked as "complete"

### sep31_flow_with_sep38
Same as **sep31_flow** but a SEP38 quote is requested before step 4 and the "quote_id" is passed into the transaction creation step.

### sep38_create_quote
Request a SEP38 quote from the Anchor Platform

### omnibus_allowlist
Test the omnibus allowlist feature in SEP-10. If the supplied account is not in the `omnibusAllowList`, the request 
should be rejected with a 403 error. 

### all
Run all the available tests

## How To Run
```shell
python anchor_platform_functional_test.py --domain <PLATFORM_SERVER_ENDPOINT> --tests <TESTS> --secret <STELLAR_SECRET_KEY>
```
## Examples:
```shell
python anchor_platform_functional_test.py --domain localhost:8080 --tests sep31_flow_with_sep38 --secret <STELLAR_SECRET_KEY> 
```
```shell
python anchor_platform_functional_test.py --domain anchor-sep-server-dev.stellar.org --tests sep31_flow --secret <STELLAR_SECRET_KEY>
```


Note: The Stellar account derived from  <STELLAR_SECRET_KEY> should have own assets used in the tests (USDC, JPYC)