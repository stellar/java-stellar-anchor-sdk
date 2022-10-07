package org.stellar.anchor.platform.event;

import static org.apache.kafka.clients.producer.ProducerConfig.*;

import java.util.Properties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.stellar.anchor.platform.config.MskConfig;
import org.stellar.anchor.util.Log;

public class MskEventPublisher extends KafkaEventPublisher {
  public MskEventPublisher(MskConfig mskConfig) {
    Log.debugF("MskConfig: {}", mskConfig);
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, mskConfig.getBootstrapServer());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    props.put(CLIENT_ID_CONFIG, mskConfig.getClientId());
    props.put(RETRIES_CONFIG, mskConfig.getRetries());
    props.put(LINGER_MS_CONFIG, mskConfig.getLingerMs());
    props.put(BATCH_SIZE_CONFIG, mskConfig.getBatchSize());

    if (mskConfig.isUseIAM()) {
      props.put("security.protocol", "SASL_SSL");
      props.put("sasl.mechanism", "AWS_MSK_IAM");
      props.put("sasl.jaas.config", "software.amazon.msk.auth.iam.IAMLoginModule");
      props.put(
          "sasl.client.callback.handler.class",
          "software.amazon.msk.auth.iam.IAMClientCallbackHandler");
    }
    createPublisher(props);
  }
}
