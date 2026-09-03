# Terraform plan

Terraform is intentionally **not required for Stage 1**.

The local conference demo must remain deterministic and runnable without cloud credentials or Wi-Fi.

For Stage 3, this directory will become the optional real-cloud deployment layer:

- AWS: containerized provider service + managed data store
- Azure: containerized provider service + managed data store
- GCP: containerized provider service + managed data store
- Shared configuration: outputs used by the sync orchestrator

Recommended repository layout later:

```text
infra/terraform/
  modules/
    aws-provider-endpoint/
    azure-provider-endpoint/
    gcp-provider-endpoint/
  environments/
    demo/
      main.tf
      variables.tf
      outputs.tf
```

Terraform should provision infrastructure. It should **not** contain synchronization/business logic.
That logic stays in Kotlin/Spring Boot.

A useful separation is:

```text
Kotlin/Spring Boot -> behavior
Docker            -> packaging
Terraform         -> infrastructure
GitHub Actions    -> CI/CD
Kafka/Redpanda    -> event transport
```
