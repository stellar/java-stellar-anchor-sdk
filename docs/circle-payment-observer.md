# Circle Payment Observer

[Circle API notifications](https://developers.circle.com/docs/notifications-data-models) is an async webhook notifier
implemented by Circle that can notify our server whenever a change is detected by them. It allows observing a variety of
Circle events, although we will only be observing [`transfer` events](https://developers.circle.com/docs/notifications-data-models#transfer-flow),
since they are the only ones that can contain information about incoming and outgoing stellar transfers.

## Configuration

In order to use Circle in the SEP-31 flow, a few basic steps are needed:
1. Make sure you register to Circle and generate an API key.
2. Register the Platform `{HOST}/circle-observer` endpoint to the Circle subscription service.
3. Generate the SEP-31 transactions using a memo provided by Circle.

### Circle API Key

Start by getting a sandbox account at <https://my-sandbox.circle.com/signup> and generating an API key. You'll need that
to authenticate your Circle requests.

At some point, you'll want to get a production key. In order to do so, go to <https://www.circle.com/> and create a new
Circle account. The account creation screening process takes around 3 weeks.
   
### Subscribing to Receive Webhook Requests

Here are the steps to subscribe to Circle events:
1. Make sure your server is up and running and that it can be reached from an external IP.
2. Execute a `POST /v1/notifications/subscriptions` request using the `{HOST}/circle-observer` provided by the Platform.
   * You can use the [Circle API reference](https://developers.circle.com/reference/listsubscriptions) website to do so.
3. If the request went through successful, Circle will send a `"SubscriptionConfirmation"` notification to your instance
of the Platform.
4. Upon receiving the `"SubscriptionConfirmation"` notification, the Platform will automatically reach back to Circle to
confirm the subscription. After receiving that confirmation, Circle will start notifying the Platform through the
`{HOST}/circle-observer` endpoint.    

You can check Circle's official documentation [here](https://developers.circle.com/docs/notifications-quickstart#2-subscribe-to-payments-status-notifications).

### Generating SEP-31 Transactions Using Circle's Memo

In order to connect incoming Circle payments with a SEP-31 transfer, SEP-31 actually needs to use Circle to generate the
SEP-31 memo. To do so, make sure you configure `sep31` in the yaml file with `depositInfoGeneratorType: circle`.  
