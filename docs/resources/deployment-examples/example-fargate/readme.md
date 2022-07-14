# Deployment of Anchor Platform and Receiving Anchor Example Reference Server with Terraform Cloud, AWS ECS/Fargate, SQS
This documentation will configure AWS Infrastructure, Anchor Platform and a sample Receiving Anchor Application using AWS Fargate. Once deployed you can run the Anchor Validator tool to verify SEP-31 Compliance. The following will be deployed via Terraform:
- Network
  - [AWS VPC, Public/Private Subnets] (terraform/vpc.tf)
  - [AWS ECS Fargate](terraform/ecs.tf)
  - [Route53 and AWS ACM Certificates](terraform/route53.tf), including Public and Internal CNAMES
  - [Amazon SQS](terraform/sqs.tf) Event Queue

# Steps
1. Pre-requisites
   1. Fork the [stellar-java-anchor-sdk](https://github.com/stellar/java-stellar-anchor-sdk_) github repository
   2. Create AWS Account and IAM account with deployment permissions
   3. Create DNS Hosted Zone for Anchor Platform with public DNS
   4. Configure Terraform Cloud Account 
      1. Create a Terraform Cloud account. 
      2. Create a Terraform Cloud work-space tied to `stellar-anchor-platform SDK repository`
      3. Setup Workspace variables `AWS_SECRET_ACCESS_KEY` and `AWS_ACCESS_KEY_ID` variables for IAM user in step 1.
2. Add Terraform Workspace variables
3. Deploy Anchor Platform Example
   1. Run Terraform Plan
   2. Run Terraform Apply
4. Run [Stellar Anchor Validation Tool][https://anchor-tests.stellar.org/) to verify your deployment.

# Terraform User Policy
Terraform user used to deploy requires the following policy:
```
{
    "Version": "2012-10-17",
    "Statement": [
       {
            "Effect": "Allow",
            "Action": [

            ],
            "Resource": "*"
        }
```


# Example Detail
## ECS Cluster

## Task Definitions
