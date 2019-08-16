provider "azurerm" {
  version = "1.22.1"
}

locals {
  ase_name               = "${data.terraform_remote_state.core_apps_compute.ase_name[0]}"
  azureVaultName         = "sscs-${var.env}"
  s2sCnpUrl              = "http://rpe-service-auth-provider-${var.env}.service.${local.ase_name}.internal"

  shared_app_service_plan     = "${var.product}-${var.env}"
  non_shared_app_service_plan = "${var.product}-${var.component}-${var.env}"
  app_service_plan            = "${(var.env == "saat" || var.env == "sandbox") ?  local.shared_app_service_plan : local.non_shared_app_service_plan}"

  documentStore               = "http://dm-store-${var.env}.service.${local.ase_name}.internal"
  ccdApi                      = "http://ccd-data-store-api-${var.env}.service.${local.ase_name}.internal"
  send_letter_service_baseurl = "http://rpe-send-letter-service-${var.env}.service.core-compute-${var.env}.internal"
}

resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = "${var.location_app}"

  tags = "${merge(var.common_tags,
    map("lastUpdated", "${timestamp()}")
    )}"
}

data "azurerm_key_vault" "sscs_key_vault" {
  name                = "${local.azureVaultName}"
  resource_group_name = "${local.azureVaultName}"
}

data "azurerm_key_vault_secret" "idam_api" {
  name      = "idam-api"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "sscs_s2s_secret" {
  name      = "sscs-s2s-secret"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "idam_sscs_systemupdate_user" {
  name      = "idam-sscs-systemupdate-user"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "idam_sscs_systemupdate_password" {
  name      = "idam-sscs-systemupdate-password"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "idam_oauth2_client_secret" {
  name      = "idam-sscs-oauth2-client-secret"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "sscs_asb_primary_send_and_listen_shared_access_key" {
  name      = "sscs-asb-primary-send-and-listen-shared-access-key"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "pdf_service_base_url" {
  name      = "docmosis-endpoint"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "pdf_service_access_key" {
  name      = "docmosis-api-key"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_lb" "consul_dns" {
  name                = "consul-server_dns"
  resource_group_name = "${var.consul_dns_resource_group_name}"
}

data "azurerm_key_vault_secret" "robotics_email_from" {
  name      = "robotics-email-from"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "robotics_email_to" {
  name      = "robotics-email-to"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "robotics_email_scottish_to" {
  name      = "robotics-email-scottish-to"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "smtp_host" {
  name      = "smtp-host"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "smtp_port" {
  name      = "smtp-port"
  vault_uri = "${data.azurerm_key_vault.sscs_key_vault.vault_uri}"
}

module "sscs-evidence-share" {
  source              = "git@github.com:hmcts/moj-module-webapp?ref=master"
  product             = "${var.product}-${var.component}"
  location            = "${var.location_app}"
  env                 = "${var.env}"
  ilbIp               = "${var.ilbIp}"
  resource_group_name = "${azurerm_resource_group.rg.name}"
  subscription        = "${var.subscription}"
  capacity            = "${var.capacity}"
  common_tags         = "${var.common_tags}"

  appinsights_instrumentation_key = "${var.appinsights_instrumentation_key}"

  app_settings = {

    SEND_LETTER_SERVICE_BASEURL = "${local.send_letter_service_baseurl}"
    SEND_LETTER_SERVICE_ENABLED = "${var.send_letter_service_enabled}"

    BUNDLE_STITCHING_ENABLED    = "${var.bundling_stitching_enabled}"

    PDF_SERVICE_BASE_URL        = "${data.azurerm_key_vault_secret.pdf_service_base_url.value}rs/render"
    PDF_SERVICE_ACCESS_KEY      = "${data.azurerm_key_vault_secret.pdf_service_access_key.value}"
    PDF_SERVICE_HEALTH_URL      = "${data.azurerm_key_vault_secret.pdf_service_base_url.value}rs/status"

    IDAM_API_URL = "${data.azurerm_key_vault_secret.idam_api.value}"

    IDAM_S2S_AUTH_TOTP_SECRET  = "${data.azurerm_key_vault_secret.sscs_s2s_secret.value}"
    IDAM_S2S_AUTH              = "${local.s2sCnpUrl}"
    IDAM_S2S_AUTH_MICROSERVICE = "${var.ccd_idam_s2s_auth_microservice}"

    IDAM_SSCS_SYSTEMUPDATE_USER     = "${data.azurerm_key_vault_secret.idam_sscs_systemupdate_user.value}"
    IDAM_SSCS_SYSTEMUPDATE_PASSWORD = "${data.azurerm_key_vault_secret.idam_sscs_systemupdate_password.value}"

    IDAM_OAUTH2_CLIENT_ID     = "${var.idam_oauth2_client_id}"
    IDAM_OAUTH2_CLIENT_SECRET = "${data.azurerm_key_vault_secret.idam_oauth2_client_secret.value}"
    IDAM_OAUTH2_REDIRECT_URL  = "${var.idam_redirect_url}"

    AMQP_HOST         = "sscs-servicebus-${var.env}.servicebus.windows.net"
    // In Azure Service bus, rulename/key is used as username/password
    AMQP_USERNAME     = "SendAndListenSharedAccessKey"
    AMQP_PASSWORD     = "${data.azurerm_key_vault_secret.sscs_asb_primary_send_and_listen_shared_access_key.value}"
    TOPIC_NAME        = "sscs-evidenceshare-topic-${var.env}"
    SUBSCRIPTION_NAME = "sscs-evidenceshare-subscription-${var.env}"

    TRUST_ALL_CERTS         = "${var.trust_all_certs}"

    DOCUMENT_MANAGEMENT_URL = "${local.documentStore}"

    CORE_CASE_DATA_API_URL  = "${local.ccdApi}"
    CORE_CASE_DATA_JURISDICTION_ID = "${var.core_case_data_jurisdiction_id}"
    CORE_CASE_DATA_CASE_TYPE_ID    = "${var.core_case_data_case_type_id}"

    WEBSITE_DNS_SERVER = "${data.azurerm_lb.consul_dns.private_ip_address}"

    ROBOTICS_EMAIL_FROM    = "${data.azurerm_key_vault_secret.robotics_email_from.value}"
    ROBOTICS_EMAIL_TO      = "${data.azurerm_key_vault_secret.robotics_email_to.value}"
    ROBOTICS_EMAIL_SCOTTISH_TO      = "${data.azurerm_key_vault_secret.robotics_email_scottish_to.value}"
    ROBOTICS_EMAIL_SUBJECT = "${var.robotics_email_subject}"
    ROBOTICS_EMAIL_MESSAGE = "${var.robotics_email_message}"

    EMAIL_SERVER_HOST      = "${data.azurerm_key_vault_secret.smtp_host.value}"
    EMAIL_SERVER_PORT      = "${data.azurerm_key_vault_secret.smtp_port.value}"
    EMAIL_SMTP_TLS_ENABLED = "${var.appeal_email_smtp_tls_enabled}"
    EMAIL_SMTP_SSL_TRUST   = "${var.appeal_email_smtp_ssl_trust}"

  }
}
