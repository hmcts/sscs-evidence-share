variable "product" {
}

variable "component" {
}

variable "location" {
  default = "UK South"
}

variable "env" {
}

variable "subscription" {}

variable "common_tags" {
  type = map(string)
}

variable "send_grid_subscription" {
  default = "1c4f0704-a29e-403d-b719-b90c34ef14c9"
}
