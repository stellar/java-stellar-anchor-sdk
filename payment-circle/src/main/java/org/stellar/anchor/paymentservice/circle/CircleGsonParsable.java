package org.stellar.anchor.paymentservice.circle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.stellar.anchor.paymentservice.circle.model.CirclePayout;
import org.stellar.anchor.paymentservice.circle.model.CircleTransfer;

public interface CircleGsonParsable {
  Gson gson =
      new GsonBuilder()
          .registerTypeAdapter(CircleTransfer.class, new CircleTransfer.Serialization())
          .registerTypeAdapter(CirclePayout.class, new CirclePayout.Deserializer())
          .create();
}
