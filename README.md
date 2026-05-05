# RabbitMQ 4-Node Cluster Demo with Spring Boot, Prometheus, and Grafana

This repository contains:

- A Spring Boot 4.0.1 application using Spring AMQP
- Four RabbitMQ containers with management UI enabled
- Prometheus scraping RabbitMQ metrics
- Grafana for dashboards

The Spring Boot app both publishes and consumes messages:

- `TopicProducer` publishes to `topic.exchange` on a configurable schedule
- `TopicConsumer` listens on four queues and manually acknowledges messages

## Verified Compose Summary

The current [`docker-compose.yml`](/scratch/voggu/docker/rabbitmq-cluster/docker-compose.yml) defines these services:

| Service | Purpose | Host Ports |
| --- | --- | --- |
| `rabbitmq1` | Cluster seed node + management UI + metrics | `5672`, `15672`, `15692` |
| `rabbitmq2` | Cluster node 2 + management UI + metrics | `5673`, `15673`, `15693` |
| `rabbitmq3` | Cluster node 3 + management UI + metrics | `5674`, `15674`, `15694` |
| `rabbitmq4` | Cluster node 4 + management UI + metrics | `5675`, `15675`, `15695` |
| `prometheus` | Prometheus server | `9090` |
| `grafana` | Grafana UI | `3000` |

Named Docker volumes:

- `rabbitmq1_data`
- `rabbitmq2_data`
- `rabbitmq3_data`
- `rabbitmq4_data`

Files mounted from the repo:

- [`prometheus.yml`](/scratch/voggu/docker/rabbitmq-cluster/prometheus.yml)
- `./data/grafana`

## Important Verification Notes

- `docker compose config` succeeds.
- The old Compose `version` key was obsolete and has been removed.
- Grafana was not on the same Docker network as Prometheus; that has been fixed so Grafana can reach Prometheus.
- A backup of the previous Compose file was created at [`docker-compose.yml.bak-2026-05-04`](/scratch/voggu/docker/rabbitmq-cluster/docker-compose.yml.bak-2026-05-04).
- `rabbitmq2`, `rabbitmq3`, and `rabbitmq4` now start locally, reset their node state, and join `rabbitmq1` as a real RabbitMQ cluster.

## Application Topology

The Spring app declares the RabbitMQ topology in [`RabbitConfig.java`](/scratch/voggu/docker/rabbitmq-cluster/src/main/java/com/oggu/jms/rabbitmq/config/RabbitConfig.java):

- Exchange: `topic.exchange`
- Queues: `order.queue`, `payment.queue`, `audit.queue`, `all.queue`
- Bindings:
  - `order.queue` -> `order.*`
  - `payment.queue` -> `payment.*`
  - `audit.queue` -> `*.*`
  - `all.queue` -> `#`

The consumer implementation is in [`TopicConsumer.java`](/scratch/voggu/docker/rabbitmq-cluster/src/main/java/com/oggu/jms/rabbitmq/consumer/TopicConsumer.java). It uses manual acknowledgements.

The scheduled publisher is in [`TopicProducer.java`](/scratch/voggu/docker/rabbitmq-cluster/src/main/java/com/oggu/jms/rabbitmq/producer/TopicProducer.java). It rotates through:

- `order.created`
- `order.updated`
- `payment.success`
- `payment.failed`

## Prerequisites

- Java 21
- Docker with Compose
- Maven 3.9+

Verify locally:

```bash
java -version
docker --version
docker compose version
mvn -v
```

## Start the Stack

From the repository root:

```bash
docker compose up -d
```

Useful endpoints:

- RabbitMQ 1 UI: `http://localhost:15672`
- RabbitMQ 2 UI: `http://localhost:15673`
- RabbitMQ 3 UI: `http://localhost:15674`
- RabbitMQ 4 UI: `http://localhost:15675`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

RabbitMQ credentials from Compose:

- Username: `admin`
- Password: `admin`

## Prometheus Configuration

[`prometheus.yml`](/scratch/voggu/docker/rabbitmq-cluster/prometheus.yml) scrapes:

- `prometheus:9090`
- `rabbitmq1:15692`
- `rabbitmq2:15692`
- `rabbitmq3:15692`
- `rabbitmq4:15692`

## Spring Boot Configuration

[`application.yml`](/scratch/voggu/docker/rabbitmq-cluster/src/main/resources/application.yml) is now aligned with the local 4-node cluster:

```yaml
spring:
  rabbitmq:
    addresses: localhost:5672,localhost:5673,localhost:5674,localhost:5675
    username: guest
    password: guest
    virtual-host: app_vhost

app:
  messaging:
    producer:
      fixed-rate-ms: 500
```

What this means:

- The app can connect to any of the four exposed RabbitMQ nodes.
- The default credentials match the preserved live cluster state.
- The producer schedule is configurable from YAML without changing Java code.

## Build and Run the App

Build:

```bash
mvn clean package
```

Run:

```bash
mvn spring-boot:run
```

On startup the app will:

- Declare the exchange, queues, and bindings
- Start four `@RabbitListener` consumers
- Publish test messages on a fixed schedule

To change the producer interval, update:

```yaml
app:
  messaging:
    producer:
      fixed-rate-ms: 1000
```

## App Utility Script

Use the local helper script to manage the Spring Boot process:

```bash
bash scripts/springboot-app.sh start
bash scripts/springboot-app.sh status
bash scripts/springboot-app.sh logs
bash scripts/springboot-app.sh stop
bash scripts/springboot-app.sh restart
```

Notes:

- The script sets `JAVA_HOME` to `/scratch/voggu/softwares/jdk-25.0.1` by default.
- It stores the PID file and application log under `./.run/`.
- On this workspace, invoking via `bash scripts/springboot-app.sh ...` is more reliable than executing the file directly.

## Verifying Messaging

Once the app is running, expected routing is:

| Routing Key | Queues |
| --- | --- |
| `order.created` | `order.queue`, `audit.queue`, `all.queue` |
| `order.updated` | `order.queue`, `audit.queue`, `all.queue` |
| `payment.success` | `payment.queue`, `audit.queue`, `all.queue` |
| `payment.failed` | `payment.queue`, `audit.queue`, `all.queue` |

Example log output:

```text
Sent -> order.created : Message at 2026-05-04T20:00:00Z
[order.queue] received: Message at 2026-05-04T20:00:00Z
[audit.queue] received: Message at 2026-05-04T20:00:00Z
[all.queue] received: Message at 2026-05-04T20:00:00Z
```

You can also inspect queues from the first broker:

```bash
docker exec -it rabbitmq1 rabbitmqctl list_queues name messages_ready messages_unacknowledged
```

## Operational Notes

- Only `rabbitmq1` defines default credentials in Compose.
- `rabbitmq2`, `rabbitmq3`, and `rabbitmq4` join the cluster via `rabbitmqctl join_cluster rabbit@rabbitmq1`.
- The follower nodes use idempotent startup logic so preserved cluster state is not re-reset on every restart.
- RabbitMQ metrics are expected on port `15692` inside each RabbitMQ container.
- Grafana persistence is stored in `./data/grafana`.

## Verify Cluster Membership

After startup, verify that all four nodes are in the same cluster:

```bash
docker exec -it rabbitmq1 rabbitmqctl cluster_status
```

Expected node names:

- `rabbit@rabbitmq1`
- `rabbit@rabbitmq2`
- `rabbit@rabbitmq3`
- `rabbit@rabbitmq4`

## Stop the Environment

```bash
docker compose down
```

To remove volumes as well:

```bash
docker compose down -v
```
