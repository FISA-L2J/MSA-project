provider "google" {
  project = var.project_id
  region  = var.region
  zone    = var.zone
}

# 1. Artifact Registry (Store Docker Images)
resource "google_artifact_registry_repository" "msa_repo" {
  location      = var.region
  repository_id = "msa-repo" // Repository Name
  description   = "Docker repository for MSA project"
  format        = "DOCKER"
}

# 2. Firewall Rules
resource "google_compute_firewall" "msa_firewall" {
  name    = "allow-msa-ports"
  network = "default"

  allow {
    protocol = "tcp"
    ports    = ["22", "8080", "8081", "8082", "9411"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["msa-server"]
}

# 3. Compute Engine Instance (VM)
resource "google_compute_instance" "msa_server" {
  name         = "msa-server"
  machine_type = "e2-standard-2" # 2 vCPU, 8GB RAM (Adjust as needed)
  tags         = ["msa-server", "http-server", "https-server"]

  boot_disk {
    initialize_params {
      image = "ubuntu-os-cloud/ubuntu-2204-lts"
      size  = 20
    }
  }

  network_interface {
    network = "default"
    access_config {
      # Allocate a public IP
    }
  }

  service_account {
    # Allow the VM to pull images from Artifact Registry
    scopes = ["cloud-platform"]
  }

  metadata_startup_script = <<-EOF
    #!/bin/bash
    # Update and install Docker
    apt-get update
    apt-get install -y ca-certificates curl gnupg
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg

    echo \
      "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | \
      tee /etc/apt/sources.list.d/docker.list > /dev/null
    
    apt-get update
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

    # Enable Docker service
    systemctl enable docker
    systemctl start docker
    
    # Allow current user to run docker commands (optional, helpful for debugging)
    usermod -aG docker ubuntu
  EOF
}

output "vm_public_ip" {
  value = google_compute_instance.msa_server.network_interface[0].access_config[0].nat_ip
}
