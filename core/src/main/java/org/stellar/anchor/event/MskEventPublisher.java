package org.stellar.anchor.event;

import java.util.Properties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.stellar.anchor.config.event.MskConfig;
import org.stellar.anchor.util.Log;

public class MskEventPublisher extends KafkaEventPublisher {
  public MskEventPublisher(MskConfig mskConfig) {
    super(mskConfig);

    Log.debugF("MskConfig: {}", mskConfig);
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, mskConfig.getBootstrapServers());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    if (mskConfig.isUseIAM()) {
      props.put("security.protocol", "SASL_SSL");
      props.put("sasl.mechanism", "AWS_MSK_IAM");
      props.put("sasl.jaas.config", "software.amazon.msk.auth.iam.IAMLoginModule required;");
      props.put(
          "sasl.client.callback.handler.class",
          "software.amazon.msk.auth.iam.IAMClientCallbackHandler");
    }
    createPublisher(props);
  }
}
