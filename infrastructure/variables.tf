variable "product" {
  type = "string"
}

variable "component" {
  type = "string"
}

variable "location" {
  type    = "string"
  default = "UK South"
}

variable "env" {
  type = "string"
}

variable "subscription" {}

variable "common_tags" {
  type = "map"
}
variable "idam_redirect_url" {
  default = "https://sscs-case-loader-sandbox.service.core-compute-sandbox.internal"
}

variable "send_letter_service_enabled" {
  default = true
}

variable "bundling_stitching_enabled" {
  default = false
}

variable "ready_to_list_robotics_enabled" {
  default = false
}
variable "consul_dns_resource_group_name" {
  type = "string"
}
