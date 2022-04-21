package org.stellar.anchor.paymentobserver;

import java.util.Map;
import org.stellar.anchor.config.CirclePaymentObserverConfig;
import org.stellar.anchor.util.Log;

public class CirclePaymentObserverService {
  private final CirclePaymentObserverConfig circlePaymentObserverConfig;

  public CirclePaymentObserverService(CirclePaymentObserverConfig circlePaymentObserverConfig) {
    this.circlePaymentObserverConfig = circlePaymentObserverConfig;
  }

  public void handleCircleNotification(Map<String, Object> requestBody) {
    String notificationType = (String) requestBody.getOrDefault("notificationType", "");
    if (notificationType.isEmpty()) {
      Log.error("Missing \"notificationType\" field in notification body.");
      return;
    }

    switch (notificationType) {
      case "SubscriptionConfirmation":
        handleSubscriptionConfirmationNotification(requestBody);
        break;

      case "transfers":
        handleTransferNotification(requestBody);
        break;

      default:
        Log.warn("Not handling notification of type "+notificationType);
        break;
    }
  }

  public void handleSubscriptionConfirmationNotification(Map<String, Object> requestBody) {
    // TODO: handle subscription notification
    System.out.println("Subscription notification:");
    System.out.println(requestBody);
//    {
//      "Type" : "SubscriptionConfirmation",
//      "MessageId" : "ddbdcdcf-d36a-45b5-927c-da25b9b009ae",
//      "Token" : "2336412f37fb687f5d51e6e2425f004aed7b7526d5fae41bc257a0d80532a6820258bf77eb25b90453b863450713a2a5a4250696d725a306ef39962b5b543752c9003e0841c0e61253fd6c517a94edebe44f36c5fe4ba131c8ea5f6f42a43f97f6e1865505e2f29f79a62f89e18f97e03a0dd5d982a7578c8d6e21154163f2d6aae523cff25557f9bc21b2503d413006",
//      "TopicArn" : "arn:aws:sns:us-west-2:908968368384:sandbox_platform-notifications-topic",
//      "Message" : "You have chosen to subscribe to the topic arn:aws:sns:us-west-2:908968368384:sandbox_platform-notifications-topic.\nTo confirm the subscription, visit the SubscribeURL included in this message.",
//      "SubscribeURL" : "https://sns.us-west-2.amazonaws.com/?Action=ConfirmSubscription&TopicArn=arn:aws:sns:us-west-2:908968368384:sandbox_platform-notifications-topic&Token=2336412f37fb687f5d51e6e2425f004aed7b7526d5fae41bc257a0d80532a6820258bf77eb25b90453b863450713a2a5a4250696d725a306ef39962b5b543752c9003e0841c0e61253fd6c517a94edebe44f36c5fe4ba131c8ea5f6f42a43f97f6e1865505e2f29f79a62f89e18f97e03a0dd5d982a7578c8d6e21154163f2d6aae523cff25557f9bc21b2503d413006",
//      "Timestamp" : "2020-04-11T20:50:16.324Z",
//      "SignatureVersion" : "1",
//      "Signature" : "kBr9z/ysQrr0ldowHY4lThkOA+dwyjcsyx7NwkbTkgEKG4N61BSSEA+43aYQEB/Ml09hclybvyjyRKWYOjaxQgbUXWmyWrCQ7vY93WYhuGvOqZxAMPiDiILxLs6/KtOxneKVvzfpK4abLrYyTTA+z/dQ52h9L8eoiSKSW81e4clfYBTJkGmuAPKFC08FvEAVT89VikPp68mBf4CctPv3Em0b4J1VvDhAB21B2LekgUmwUE0aE7fUbsF3XsKGQd/fDshLOJasQEuXSqdB5X7LITBA8r24FY+wCjwm8oR3VI9IMy21fUC6wMgoFIVZHW1KxzpEkMCSe7R1ySdNIru8SQ==",
//      "SigningCertURL" : "https://sns.us-west-2.amazonaws.com/SimpleNotificationService-a86cb10b4e1f29c941702d737128f7b6.pem"
//  }
  }

  public void handleTransferNotification(Map<String, Object> requestBody) {
    Object transferBody = requestBody.get("transfer");
    if (transferBody == null) {
      Log.error("Missing \"transfer\" value in notification of type \"transfers\".");
      return;
    }

    // TODO: handle transfer body
    System.out.println("Transfer notification:");
    System.out.println(transferBody);
  }
}
