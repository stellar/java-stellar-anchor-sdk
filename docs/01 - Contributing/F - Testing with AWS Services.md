# Testing Anchor Platform with AWS Services

Anchor Platform can be configured to work with the following AWS Services:
- RDS - Aurora Postgres Database (with IAM support)
- MSK - Managed Streaming for Kafka (with IAM support)
- SQS - Simple Queue Service


## Aurora Postgres (with IAM)

### AWS Configuration:
Create an Aurora Progres Database in AWS with default settings and `IAM DB authentication` enabled.  
(https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/Aurora.CreateInstance.html)

### Grant IAM authentication permissions to user in the Postgres Database:
Connect to the Postgres database (you can use the pgAdmin tool) using the `postgres` username/password 
(created during db creation) and execute the following queries to create a user called `anchorplatform1` 
and grant the `rds_iam` permission for this user. 
```text
CREATE USER anchorplatform1; 
GRANT rds_iam TO anchorplatform1;
```  

### IAM Settings:
Create an IAM policy that allows your database user `anchorplatform1` to be used to connect to the database. Attach this
policy to the IAM User you plan to use for Anchor Platform. Replace `db-TY6R4WIFG2MTFWQAUCVOX5UQA4` with your database's
Resource ID
(https://docs.aws.amazon.com/IAM/latest/UserGuide/access_policies_manage-attach-detach.html)
```text
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "VisualEditor0",
            "Effect": "Allow",
            "Action": [
                "rds-db:connect",
            ],
            "Resource": [
                "arn:aws:rds-db:us-east-1:760238374005:dbuser:db-TY6R4WIFG2MTFWQAUCVOX5UQA4/anchorplatform1",
            ]
        }
    ]
}
```

### Anchor Platform Configuration:
Refer to the example `data-spring-jdbc-aws-aurora-postgres` configuration in `anchor-config-defaults.yaml` 

NOTE: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY and AWS_REGION environment variables need to be set


## MSK - Managed Streaming for Kafka (with IAM)

### MSK Configuration:
1) Under `Cluster Configuration`, set `auto.create.topics.enable=true`
2) Under `Security Settings`, uncheck `Unauthorized access`
3) Under `Security Settings`, check `IAM role-based authentication`
4) After the cluster has been created, you can enable `Public access` under the `Network Settings` if you want Anchor 
Platform to be able to connect over the internet (eg: you're developing/testing Anchor Platform on your local machine)
### IAM Settings:
Create an IAM policy that allows a user to connect to the Kafka cluster. Attach this
policy to the IAM User you plan to use for Anchor Platform
```text
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "VisualEditor0",
            "Effect": "Allow",
            "Action": [
                "kafka-cluster:*"
            ],
            "Resource": [
                "arn:aws:kafka:us-east-1:760238374005:*/demo-cluster-1/*"
            ]
        }
    ]
}
```

### Anchor Platform Configuration:
Refer to the example `kafka.publisher` configuration in `anchor-config-defaults.yaml`. `useIAM` needs to be set to `true`

NOTE: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY and AWS_REGION environment variables need to be set


## SQS - Simple Queue Service (TODO update when IAM support is added)