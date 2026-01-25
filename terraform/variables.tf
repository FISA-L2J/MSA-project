variable "project_id" {
  description = "The ID of the Google Cloud project"
  type        = string
  default     = "msa-project-prod"
}

variable "region" {
  description = "The region to deploy resources in"
  type        = string
  default     = "asia-northeast3" # Seoul
}

variable "zone" {
  description = "The zone to deploy resources in"
  type        = string
  default     = "asia-northeast3-a"
}
