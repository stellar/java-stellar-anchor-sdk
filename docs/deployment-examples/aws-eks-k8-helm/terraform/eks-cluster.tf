resource "aws_iam_role" "eks_cluster" {
  # The name of the role
  name = "eks-cluster"

  # The policy that grants an entity permission to assume the role.
  # Used to access AWS resources that you might not normally have access to.
  # The role that Amazon EKS will use to create AWS resources for Kubernetes clusters
  assume_role_policy = <<POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "eks.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
POLICY
}

resource "aws_iam_policy" "elb_controller_policy" {
  name        = "aws_load_balancer_controller"
  description = "aws-load-balancer-controller"
  policy = jsonencode(
    {
   "Version":"2012-10-17",
   "Statement":[
      {
         "Effect":"Allow",
         "Action":[
            "iam:CreateServiceLinkedRole"
         ],
         "Resource":"*",
         "Condition":{
            "StringEquals":{
               "iam:AWSServiceName":"elasticloadbalancing.amazonaws.com"
            }
         }
      },
      {
         "Effect":"Allow",
         "Action":[
            "ec2:DescribeAccountAttributes",
            "ec2:DescribeAddresses",
            "ec2:DescribeAvailabilityZones",
            "ec2:DescribeInternetGateways",
            "ec2:DescribeVpcs",
            "ec2:DescribeVpcPeeringConnections",
            "ec2:DescribeSubnets",
            "ec2:DescribeSecurityGroups",
            "ec2:DescribeInstances",
            "ec2:DescribeNetworkInterfaces",
            "ec2:DescribeTags",
            "ec2:GetCoipPoolUsage",
            "ec2:DescribeCoipPools",
            "elasticloadbalancing:DescribeLoadBalancers",
            "elasticloadbalancing:DescribeLoadBalancerAttributes",
            "elasticloadbalancing:DescribeListeners",
            "elasticloadbalancing:DescribeListenerCertificates",
            "elasticloadbalancing:DescribeSSLPolicies",
            "elasticloadbalancing:DescribeRules",
            "elasticloadbalancing:DescribeTargetGroups",
            "elasticloadbalancing:DescribeTargetGroupAttributes",
            "elasticloadbalancing:DescribeTargetHealth",
            "elasticloadbalancing:DescribeTags"
         ],
         "Resource":"*"
      },
      {
         "Effect":"Allow",
         "Action":[
            "cognito-idp:DescribeUserPoolClient",
            "acm:ListCertificates",
            "acm:DescribeCertificate",
            "iam:ListServerCertificates",
            "iam:GetServerCertificate",
            "waf-regional:GetWebACL",
            "waf-regional:GetWebACLForResource",
            "waf-regional:AssociateWebACL",
            "waf-regional:DisassociateWebACL",
            "wafv2:GetWebACL",
            "wafv2:GetWebACLForResource",
            "wafv2:AssociateWebACL",
            "wafv2:DisassociateWebACL",
            "shield:GetSubscriptionState",
            "shield:DescribeProtection",
            "shield:CreateProtection",
            "shield:DeleteProtection"
         ],
         "Resource":"*"
      },
      {
         "Effect":"Allow",
         "Action":[
            "ec2:AuthorizeSecurityGroupIngress",
            "ec2:RevokeSecurityGroupIngress"
         ],
         "Resource":"*"
      },
      {
         "Effect":"Allow",
         "Action":[
            "ec2:CreateSecurityGroup"
         ],
         "Resource":"*"
      },
      {
         "Effect":"Allow",
         "Action":[
            "ec2:CreateTags"
         ],
         "Resource":"arn:aws:ec2:*:*:security-group/*",
         "Condition":{
            "StringEquals":{
               "ec2:CreateAction":"CreateSecurityGroup"
            },
            "Null":{
               "aws:RequestTag/elbv2.k8s.aws/cluster":"false"
            }
         }
      },
      {
         "Effect":"Allow",
         "Action":[
            "ec2:CreateTags",
            "ec2:DeleteTags"
         ],
         "Resource":"arn:aws:ec2:*:*:security-group/*",
         "Condition":{
            "Null":{
               "aws:RequestTag/elbv2.k8s.aws/cluster":"true",
               "aws:ResourceTag/elbv2.k8s.aws/cluster":"false"
            }
         }
      },
      {
         "Effect":"Allow",
         "Action":[
            "ec2:AuthorizeSecurityGroupIngress",
            "ec2:RevokeSecurityGroupIngress",
            "ec2:DeleteSecurityGroup"
         ],
         "Resource":"*",
         "Condition":{
            "Null":{
               "aws:ResourceTag/elbv2.k8s.aws/cluster":"false"
            }
         }
      },
      {
         "Effect":"Allow",
         "Action":[
            "elasticloadbalancing:CreateLoadBalancer",
            "elasticloadbalancing:CreateTargetGroup"
         ],
         "Resource":"*",
         "Condition":{
            "Null":{
               "aws:RequestTag/elbv2.k8s.aws/cluster":"false"
            }
         }
      },
      {
         "Effect":"Allow",
         "Action":[
            "elasticloadbalancing:CreateListener",
            "elasticloadbalancing:DeleteListener",
            "elasticloadbalancing:CreateRule",
            "elasticloadbalancing:DeleteRule"
         ],
         "Resource":"*"
      },
      {
         "Effect":"Allow",
         "Action":[
            "elasticloadbalancing:AddTags",
            "elasticloadbalancing:RemoveTags"
         ],
         "Resource":[
            "arn:aws:elasticloadbalancing:*:*:targetgroup/*/*",
            "arn:aws:elasticloadbalancing:*:*:loadbalancer/net/*/*",
            "arn:aws:elasticloadbalancing:*:*:loadbalancer/app/*/*"
         ],
         "Condition":{
            "Null":{
               "aws:RequestTag/elbv2.k8s.aws/cluster":"true",
               "aws:ResourceTag/elbv2.k8s.aws/cluster":"false"
            }
         }
      },
      {
         "Effect":"Allow",
         "Action":[
            "elasticloadbalancing:AddTags",
            "elasticloadbalancing:RemoveTags"
         ],
         "Resource":[
            "arn:aws:elasticloadbalancing:*:*:listener/net/*/*/*",
            "arn:aws:elasticloadbalancing:*:*:listener/app/*/*/*",
            "arn:aws:elasticloadbalancing:*:*:listener-rule/net/*/*/*",
            "arn:aws:elasticloadbalancing:*:*:listener-rule/app/*/*/*"
         ]
      },
      {
         "Effect":"Allow",
         "Action":[
            "elasticloadbalancing:ModifyLoadBalancerAttributes",
            "elasticloadbalancing:SetIpAddressType",
            "elasticloadbalancing:SetSecurityGroups",
            "elasticloadbalancing:SetSubnets",
            "elasticloadbalancing:DeleteLoadBalancer",
            "elasticloadbalancing:ModifyTargetGroup",
            "elasticloadbalancing:ModifyTargetGroupAttributes",
            "elasticloadbalancing:DeleteTargetGroup"
         ],
         "Resource":"*",
         "Condition":{
            "Null":{
               "aws:ResourceTag/elbv2.k8s.aws/cluster":"false"
            }
         }
      },
      {
         "Effect":"Allow",
         "Action":[
            "elasticloadbalancing:RegisterTargets",
            "elasticloadbalancing:DeregisterTargets"
         ],
         "Resource":"arn:aws:elasticloadbalancing:*:*:targetgroup/*/*"
      },
      {
         "Effect":"Allow",
         "Action":[
            "elasticloadbalancing:SetWebAcl",
            "elasticloadbalancing:ModifyListener",
            "elasticloadbalancing:AddListenerCertificates",
            "elasticloadbalancing:RemoveListenerCertificates",
            "elasticloadbalancing:ModifyRule"
         ],
         "Resource":"*"
      }
   ]
}
)  
}

resource "aws_iam_role_policy_attachment" "amazon_eks_cluster_policy" {
  role = aws_iam_role.eks_cluster.name
  policy_arn = aws_iam_policy.elb_controller_policy.arn
}



module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "18.20.5"

  cluster_name    = "${local.cluster_name}"
  cluster_endpoint_private_access = true
  cluster_endpoint_public_access  = true
  cluster_addons = {
    coredns = {
      resolve_conflicts = "OVERWRITE"
    }
    kube-proxy = {}
    vpc-cni = {
      resolve_conflicts = "OVERWRITE"
    }

  }
  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  cluster_security_group_additional_rules = {
    egress_nodes_ephemeral_ports_tcp = {
      description                = "To node 1025-65535"
      protocol                   = "tcp"
      from_port                  = 1025
      to_port                    = 65535
      type                       = "egress"
      source_node_security_group = true
    }
  }

  # Extend node-to-node security group rules
  node_security_group_additional_rules = {
    ingress_self_all = {
      description = "Node to node all ports/protocols"
      protocol    = "-1"
      from_port   = 0
      to_port     = 0
      type        = "ingress"
      self        = true
    }
    egress_all = {
      description      = "Node all egress"
      protocol         = "-1"
      from_port        = 0
      to_port          = 0
      type             = "egress"
      cidr_blocks      = ["0.0.0.0/0"]
      ipv6_cidr_blocks = ["::/0"]
    }
    ingress_allow_access_from_control_plane = {
      type                          = "ingress"
      protocol                      = "tcp"
      from_port                     = 9443
      to_port                       = 9443
      source_cluster_security_group = true
      description                   = "Allow access from control plane to webhook port of AWS load balancer controller"
    }
  }

  # EKS Managed Node Group(s)
  eks_managed_node_group_defaults = {
    instance_types = ["t3.nano", "t3.micro"]
    update_launch_template_default_version = true
    #iam_role_additional_policies = [
    #  "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
    #]
  }

  eks_managed_node_groups = {
    
    common = {
      
      capacity_type = "SPOT"
      instance_types = ["t3.nano", "t3.micro", "t3.small"]
      desired_size = 6
      min_size     = 6
      max_size     = 7

        tags = {
          Environment = "dev"
          Terraform   = "true"
        }
    }
  }
}

data "aws_eks_cluster" "cluster" {
  name = module.eks.cluster_id
}

data "aws_eks_cluster_auth" "cluster" {
  name = module.eks.cluster_id
}

data "aws_eks_cluster_auth" "cluster-auth" {
  depends_on = [module.eks.cluster_id]
  name       = module.eks.cluster_id
}

resource "aws_iam_role_policy_attachment" "additional" {
   for_each = module.eks.eks_managed_node_groups
   policy_arn = aws_iam_policy.elb_controller_policy.arn
   role = each.value.iam_role_name
}


