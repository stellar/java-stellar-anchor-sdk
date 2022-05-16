# Anchor Platform Example Deployment Documentation: Terraform Cloud, EKS, Kubernetes, Helm, MSK (Kafka)
This documentation will configure AWS Infrastructure, Anchor Platform and a sample Receiving Anchor Application. Once deployed you can run the Anchor Validator tool to verify SEP-31 Compliance.
- VPC
  - Public and Private Subnets across three availability zones
- Amazon EKS
- Amazon MSK Cluster for event notification 
- Helm Charts for Anchor Platform Kubernetes (Sep Server) deployment
- Helm Charts for Reference Server (Receiving Anchor Sample Application) Kubernetes Deployment
- Nginx Public Ingress Controller (ELB for SEP Server)
- AWS Internal (Private) Internal Load Balancer Controller Add-on (ELB for Reference Server) 
- AWS MSK Kafka Cluster for Event Notifications
- Helm Charts for Anchor Platform and Receiving Anchor Application (Reference Server)
- Helm Release for Nginx Ingress and AWS Load Balancer Controller Addon
- LetsEncrypt TLS using Kubernetes CertManager, CertIssuer, and SSL Certificate for Public Load Balancer
- Validation using Anchor Stellar Tests SEP-31 Validator
  
# Steps
1. AWS and IAM Account
   1. Create an AWS account and IAM user with API credentials and the following permissions:
   2. [todo add all privs here]
2. Terraform Cloud Account 
   1. Create a Terraform Cloud account. A single-user free account will suffice.
3. Create a Terraform Cloud work-space tied to `stellar-anchor-platform SDK repository` add `AWS_SECRET_ACCESS_KEY` and `AWS_ACCESS_KEY_ID` variables for IAM user in step 1.
