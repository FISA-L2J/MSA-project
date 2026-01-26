provider "google" {
  project = var.project_id
  region  = var.region
  zone    = var.zone
}

# 1. Artifact Registry (Store Docker Images)
# Keep this as is
resource "google_artifact_registry_repository" "msa_repo" {
  location      = var.region
  repository_id = "msa-repo"
  description   = "Docker repository for MSA project"
  format        = "DOCKER"
}

# 2. GKE Cluster
resource "google_container_cluster" "primary" {
  name     = "msa-cluster"
  location = var.zone  # Zonal cluster is cheaper than Regional

  # We can't create a cluster with no node pool defined, but we want to only use separately managed node pools.
  # So we create the smallest possible default node pool and immediately delete it.
  remove_default_node_pool = true
  initial_node_count       = 1
}

# 3. Managed Node Pool
resource "google_container_node_pool" "primary_nodes" {
  name       = "msa-node-pool"
  location   = var.zone
  cluster    = google_container_cluster.primary.name
  node_count = 2 # 2 Nodes for High Availability (minimum)

  node_config {
    preemptible  = true # Cheaper, good for learning/testing
    machine_type = "e2-standard-2" # 2 vCPU, 8GB RAM

    # Google recommends custom service accounts that have cloud-platform scope and permissions granted via IAM Roles.
    oauth_scopes = [
      "https://www.googleapis.com/auth/cloud-platform"
    ]
  }
}

# 4. Output for K8s Connection
output "get_credentials_command" {
  value = "gcloud container clusters get-credentials ${google_container_cluster.primary.name} --zone ${var.zone} --project ${var.project_id}"
  description = "Command to configure kubectl"
}
