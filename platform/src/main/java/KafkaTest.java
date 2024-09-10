import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

public class KafkaTest {

  private static final String SECRET_DIR =
      "C:\\projects\\java-stellar-anchor-sdk\\service-runner\\src\\main\\resources\\dev-tools\\secrets";

  public static Properties getKafkaProperties() {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
    props.put(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.StringSerializer");
    props.put(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.StringSerializer");
    props.put(
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.StringDeserializer");
    props.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.StringDeserializer");
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "your-consumer-group");

    // SASL/SSL Configurations
    props.put("security.protocol", "SASL_SSL");
    props.put("sasl.mechanism", "PLAIN");
    props.put(
        "sasl.jaas.config",
        "org.apache.kafka.common.security.plain.PlainLoginModule required username='admin' password='admin-secret';");
    props.put("ssl.endpoint.identification.algorithm", ""); // Disable hostname verification
    props.put("ssl.engine.factory.class", InsecureSslEngineFactory.class);

    return props;
  }

  public static KafkaProducer<String, String> createProducer() {
    Properties props = getKafkaProperties();
    return new KafkaProducer<>(props);
  }

  public static void createKafkaTopic(
      String topicName, int numPartitions, short replicationFactor) {
    Properties props = getKafkaProperties();

    // Create Kafka Admin Client
    try (AdminClient adminClient = AdminClient.create(props)) {
      NewTopic newTopic = new NewTopic(topicName, numPartitions, replicationFactor);

      // Create the topic
      adminClient.createTopics(Arrays.asList(newTopic)).all().get();
      System.out.println("Topic " + topicName + " created successfully");
    } catch (ExecutionException e) {
      System.out.println("Topic " + topicName + " already exists");
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Failed to create topic: " + e.getMessage());
    }
  }

  public static void sendMessages(KafkaProducer<String, String> producer, String topic) {
    ProducerRecord<String, String> record = new ProducerRecord<>(topic, "key", "value");
    producer.send(
        record,
        (metadata, exception) -> {
          if (exception == null) {
            System.out.println(
                "Sent message to "
                    + metadata.topic()
                    + " partition "
                    + metadata.partition()
                    + " offset "
                    + metadata.offset());
          } else {
            System.out.println("Error sending message: " + exception.getMessage());
          }
        });
    producer.close();
  }

  public static KafkaConsumer<String, String> createConsumer() {
    Properties props = getKafkaProperties();
    return new KafkaConsumer<>(props);
  }

  public static void consumeMessages(KafkaConsumer<String, String> consumer, String topic) {
    consumer.subscribe(Arrays.asList(topic));
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(10000));
    records.forEach(
        record -> {
          System.out.println(
              "Received message: (key: "
                  + record.key()
                  + ", value: "
                  + record.value()
                  + ", partition: "
                  + record.partition()
                  + ", offset: "
                  + record.offset()
                  + ")");
        });
    consumer.close();
  }

  public static void main(String[] args) {
    String topic = "your-topic-name-1";
    KafkaProducer<String, String> producer = createProducer();
    sendMessages(producer, topic);

    KafkaConsumer<String, String> consumer = createConsumer();
    consumeMessages(consumer, topic);
  }
}
