# tracking-service
Service for consuming kafka messages, storing shipment data and calling notify service

## Technologies
Java 21, Spring Boot 3, PostgreSQL, Lombok, Mapstruct, Spring Kafka, Micrometer, OpenApi, Protobuf

## Implementation details
Async communication with Kafka, Protobuf shared model used, poison pill message pushing to dedicated topic, 
Retry and Circuit breaker pattern used with communication with notification service. 
Model shared with Open API schema.
Metrics published using OTL registry sidecar and published to AWS Prometheus -> AWS Grafana

## CI, CD, Cloud technologies
AWS ECS, AWS ECR, AWS Parameter Store, AWS CloudWatch, AWS Managed Prometheus, AWS Managed Grafana, Git Actions, 
Git Packages, Docker, Confluent Cloud Schema Registry, Confluent Cloud Kafka, OpenAPI