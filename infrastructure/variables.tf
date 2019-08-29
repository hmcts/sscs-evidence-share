variable "product" {
  type    = "string"
}

variable "component" {
  type = "string"
}

variable "location_app" {
  type    = "string"
  default = "UK South"
}

variable "env" {
  type = "string"
}

variable "ilbIp" {}

variable "subscription" {}

variable "capacity" {
  default = "1"
}

variable "common_tags" {
  type = "map"
}

variable "ccd_idam_s2s_auth_microservice" {
  default = "sscs"
}

variable "idam_oauth2_client_id" {
  default = "sscs"
}

variable "idam_redirect_url" {
  default = "https://sscs-case-loader-sandbox.service.core-compute-sandbox.internal"
}

variable "trust_all_certs" {
  default = false
}

variable "logback_require_alert_level" {
  default = false
}

variable "logback_require_error_code" {
  default = false
}

variable "send_letter_service_enabled" {
  default = true
}

variable "bundling_stitching_enabled" {
  default = false
}

variable "core_case_data_jurisdiction_id" {
  default = "SSCS"
}

variable "core_case_data_case_type_id" {
  default = "Benefit"
}

variable "consul_dns_resource_group_name" {
  type = "string"
}

variable "appinsights_instrumentation_key" {
  description = "Instrumentation key of the App Insights instance this webapp should use. Module will create own App Insights resource if this is not provided"
  default     = ""
}

variable "robotics_email_message" {
  type    = "string"
  default = "Please find attached the robotics json file \nPlease do not respond to this email"
}

variable "appeal_email_smtp_tls_enabled" {
  type    = "string"
  default = "true"
}

variable "appeal_email_smtp_ssl_trust" {
  type    = "string"
  default = "*"
}
