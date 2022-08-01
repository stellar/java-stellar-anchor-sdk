package org.stellar.anchor.platform.payment.observer.circle;

import com.google.gson.Gson;
import org.stellar.anchor.platform.payment.observer.circle.model.CirclePayment;
import org.stellar.anchor.platform.payment.observer.circle.model.CirclePayout;
import org.stellar.anchor.platform.payment.observer.circle.model.CircleTransfer;
import org.stellar.anchor.util.GsonUtils;

public interface CircleGsonParsable {
  Gson gson =
      GsonUtils.builder()
          .registerTypeAdapter(CircleTransfer.class, new CircleTransfer.Serialization())
          .registerTypeAdapter(CirclePayout.class, new CirclePayout.Deserializer())
          .registerTypeAdapter(CirclePayment.class, new CirclePayment.Deserializer())
          .create();
}
