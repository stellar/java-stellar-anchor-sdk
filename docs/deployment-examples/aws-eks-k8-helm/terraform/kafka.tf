resource "aws_security_group" "sg" {
  vpc_id = module.vpc.vpc_id
   ingress {
    description      = "kafka broker"
    from_port        = 9092
    to_port          = 9092
    protocol         = "tcp"
  }
}

resource "aws_msk_cluster" "anchor_kafka_msk" {
  cluster_name           = "anchorkafka"
  kafka_version          = "2.8.0"
  number_of_broker_nodes = 3

  broker_node_group_info {
    instance_type   = "kafka.t3.small"
    ebs_volume_size = 1000
    client_subnets = module.vpc.private_subnets
    security_groups = [aws_security_group.sg.id]
  }

  #encryption_info {
  #  encryption_at_rest_kms_key_arn = aws_kms_key.kms.arn
  #}
  encryption_info {
 
    encryption_in_transit {
      client_broker = "TLS_PLAINTEXT"
    }
  }

  open_monitoring {
    prometheus {
      jmx_exporter {
        enabled_in_broker = false
      }
      node_exporter {
        enabled_in_broker = false
      }
    }
  }

  logging_info {
    broker_logs {
      cloudwatch_logs {
        enabled   = false
        #log_group = aws_cloudwatch_log_group.test.name
      }
      firehose {
        enabled         = false
        #delivery_stream = aws_kinesis_firehose_delivery_stream.test_stream.name
      }
      s3 {
        enabled = false
        #bucket  = aws_s3_bucket.bucket.id
        #prefix  = "logs/msk-"
      }
    }
  }

  tags = {
    foo = "bar"
  }

  configuration_info {
    revision = resource.aws_msk_configuration.anchor_kafka_config.latest_revision
    arn = resource.aws_msk_configuration.anchor_kafka_config.arn
  }
}

resource "aws_msk_configuration" "anchor_kafka_config" {
  kafka_versions = ["2.1.0"]
  name           = "anchor-demo"

  server_properties = <<PROPERTIES
auto.create.topics.enable = true
delete.topic.enable = true
PROPERTIES
}

data "aws_msk_cluster" "anchor_msk" {
  cluster_name = resource.aws_msk_cluster.anchor_kafka_msk.cluster_name
}



#value = ["${split(",", azurerm_app_service.testap.outbound_ip_addresses)}"]