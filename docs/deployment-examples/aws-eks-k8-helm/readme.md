# Terraform Cloud, EKS, Kubernetes, Helm with Kafka Event Queue (AWS MSK)
This documentation will configure AWS Infrastructure, Anchor Platform and a sample Receiving Anchor Application. Once deployed you can run the Anchor Validator tool to verify SEP-31 Compliance.
- Network
  - VPC
  - Public and Private Subnets
  - Security Groups
  - Route53 Hosted Zone, CNAMES
- Reverse Proxy
  - Nginx Public Ingress Controller (ELB for SEP Server)
  - Kubernetes CertManager, CertIssuer, and LetsEncrypt Signed SSL Certificate
- Kubernetes
  - Amazon EKS
  - Helm Charts for Anchor Platform Kubernetes (Sep Server) deployment
  - Helm Charts for Reference Server (Receiving Anchor Sample Application) 
  - AWS Load Balancer Controller Add-on (internal ELB for Reference Server) 
- Event Queue
  - Amazon MSK Cluster for event notification 

# Steps
1. Pre-requisites
   1. Clone the [stellar-java-anchor-sdk](https://github.com/stellar/java-stellar-anchor-sdk_) github repository
   2. Create AWS Account and IAM account with deployment permissions
   3. Create DNS Hosted Zone for Anchor Platform with public DNS
   4. Configure Terraform Cloud Account 
      1. Create a Terraform Cloud account. 
      2. Create a Terraform Cloud work-space tied to `stellar-anchor-platform SDK repository`
      3. Setup Workspace variables `AWS_SECRET_ACCESS_KEY` and `AWS_ACCESS_KEY_ID` variables for IAM user in step 1.
2. Add Workspace variable of DNS Hosted Zone ARN
3. Deploy Anchor Platform Example
   1. Run Terraform Plan
   2. Run Terraform Apply


# Current Known Issues
- [Issue 259](https://github.com/stellar/java-stellar-anchor-sdk/issues/259) Project Destruction fails intermittently. Work-around to run destruction repeatedly and/or use AWS console.
-  DNS Hosted Zone needs to be created manually and t