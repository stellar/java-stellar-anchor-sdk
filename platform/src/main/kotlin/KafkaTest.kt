import java.security.cert.X509Certificate
import java.time.Duration
import java.util.*
import java.util.concurrent.ExecutionException
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord

val SECRET_DIR =
  "C:\\projects\\java-stellar-anchor-sdk\\service-runner\\src\\main\\resources\\dev-tools\\secrets"

fun getKafkaProperties(): Properties {
  val props = Properties()
  props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "kafka:9092"
  props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] =
    "org.apache.kafka.common.serialization.StringSerializer"
  props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] =
    "org.apache.kafka.common.serialization.StringSerializer"
  props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] =
    "org.apache.kafka.common.serialization.StringDeserializer"
  props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
    "org.apache.kafka.common.serialization.StringDeserializer"
  props[ConsumerConfig.GROUP_ID_CONFIG] = "your-consumer-group"

  // SASL/SSL Configurations
  props["security.protocol"] = "SASL_SSL"
  //  props["security.protocol"] = "SSL"
  props["sasl.mechanism"] = "PLAIN"
  props["sasl.jaas.config"] =
    "org.apache.kafka.common.security.plain.PlainLoginModule required username='admin' password='admin-secret';"
  //  props["ssl.truststore.location"] = "${SECRET_DIR}\\keystore.jks"
  //  props["ssl.truststore.password"] = "test123"
  //  props["ssl.keystore.location"] = "${SECRET_DIR}\\keystore.jks"
  //  props["ssl.keystore.password"] = "test123"
  props["ssl.endpoint.identification.algorithm"] = ""

  //  props["ssl.key.password"] = "test123"

  return props
}

fun configureTrustAllCerts() {
  try {
    // Create a trust manager that does not validate certificate chains
    val trustAllCerts =
      arrayOf<TrustManager>(
        object : X509TrustManager {
          override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate>? {
            return null
          }

          override fun checkClientTrusted(
            certs: Array<java.security.cert.X509Certificate>?,
            authType: String?
          ) {
            // Trust all clients
            println("Trusting client")
          }

          override fun checkServerTrusted(
            certs: Array<java.security.cert.X509Certificate>?,
            authType: String?
          ) {
            // Trust all servers
            println("Trusting server")
          }
        }
      )

    // Install the all-trusting trust manager
    val sslContext = SSLContext.getInstance("SSL")
    sslContext.init(null, trustAllCerts, java.security.SecureRandom())

    // Set the default SSLContext to the one that trusts all certs
    HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
  } catch (e: Exception) {
    e.printStackTrace()
  }
}

fun createProducer(): KafkaProducer<String, String> {
  val props = getKafkaProperties()
  configureTrustAllCerts()
  return KafkaProducer(props)
}

fun createKafkaTopic(topicName: String, numPartitions: Int, replicationFactor: Short) {
  // Configuration for Kafka Admin Client
  val props = getKafkaProperties()

  // Create Kafka Admin Client
  val adminClient = AdminClient.create(props)

  try {
    // Define the new topic with the specified replication factor and number of partitions
    val newTopic = NewTopic(topicName, numPartitions, replicationFactor)

    // Create the topic
    adminClient.createTopics(listOf(newTopic)).all().get()
    println("Topic $topicName created successfully")
  } catch (e: ExecutionException) {
    println("Topic $topicName already exists")
  } catch (e: Exception) {
    e.printStackTrace()
    println("Failed to create topic: ${e.message}")
  } finally {
    adminClient.close()
  }
}

fun sendMessages(producer: KafkaProducer<String, String>, topic: String) {
  val record = ProducerRecord(topic, "key", "value")
  producer.send(record) { metadata, exception ->
    if (exception == null) {
      println(
        "Sent message to ${metadata.topic()} partition ${metadata.partition()} offset ${metadata.offset()}"
      )
    } else {
      println("Error sending message: ${exception.message}")
    }
  }
  producer.close()
}

fun createConsumer(): KafkaConsumer<String, String> {
  val props = getKafkaProperties()
  configureTrustAllCerts()
  return KafkaConsumer<String, String>(props)
}

fun consumeMessages(consumer: KafkaConsumer<String, String>, topic: String) {
  consumer.subscribe(listOf(topic))
  val records = consumer.poll(Duration.ofMillis(10000))
  for (record in records) {
    println(
      "Received message: (key: ${record.key()}, value: ${record.value()}, partition: ${record.partition()}, offset: ${record.offset()})"
    )
  }
  consumer.close()
}

fun main() {
  configureTrustAllCerts()

  val topic = "your-topic-name-1"
  //  createKafkaTopic(topic, 1, 1)
  val producer = createProducer()
  sendMessages(producer, topic)

  val consumer = createConsumer()
  consumeMessages(consumer, topic)
}
