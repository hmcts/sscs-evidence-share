output "microserviceName" {
  value = "${var.component}"
}

output "pdf_service_base_url" {
  value = "${data.azurerm_key_vault_secret.pdf_service_base_url.value}rs/render"
}

output "pdf_service_access_key" {
  value = "${data.azurerm_key_vault_secret.pdf_service_access_key.value}"
}

output "pdf_service_health_url" {
  value = "${data.azurerm_key_vault_secret.pdf_service_base_url.value}rs/health"
}

output "consulDnsIp" {
  value = "${data.azurerm_lb.consul_dns.private_ip_address}"
}
