variable "map_users" {
  description = "additional iam users to add to the aws-auth configmap"
  type = list(object({
    userarn = string
    username = string
    groups = list(string)
  }))
  
  default = [
    {
      userarn  = "arn:aws:iam::245943599471:user/reece"
      username = "reece"
      groups   = ["system:masters"]
    },
 
  ]
}
