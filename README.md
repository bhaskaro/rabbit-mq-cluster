# 🐇 RabbitMQ Cluster – Spring Boot Topic Messaging (Java 21)

This project demonstrates **RabbitMQ Topic Exchange messaging** using a **Spring Boot application** with **Spring AMQP**.

The application acts as a **consumer**, while messages can be published from:

* RabbitMQ Management UI
* CLI (`rabbitmqadmin`)
* Any external AMQP client (Java, Python, Node, etc.)

---

## 📌 Repository

**GitHub:**
[https://github.com/bhaskaro/rabbit-mq-cluster.git](https://github.com/bhaskaro/rabbit-mq-cluster.git)

---

## 🧰 Tech Stack

* **Java:** 21
* **Spring Boot**
* **Spring AMQP**
* **RabbitMQ (Docker)**
* **Maven**

---

## 📦 Project Structure

```text
rabbitmq-cluster/
├── pom.xml
├── README.md
├── docker-compose.yml
├── src/
│   └── main/
│       ├── java/
│       │   └── com/jms/rabbitmq/
│       │       ├── config/
│       │       │   └── RabbitConfig.java
│       │       ├── consumer/
│       │       │   └── TopicConsumer.java
│       │       └── producer/
│       │           └── TopicProducer.java
│       └── resources/
│           └── application.yml
```

---

## 🧰 Prerequisites

Make sure you have:

* **Java 21**
* **Docker & Docker Compose**
* **Maven 3.9+**

Verify:

```bash
java -version
docker --version
docker compose version
mvn -v
```

---

## 🐳 Step 1: Start RabbitMQ (Docker)

From the project root:

```bash
docker compose up -d
```

### RabbitMQ Management UI

```
http://localhost:15672
```

**Credentials**

```
username: guest
password: guest
```

---

## 🏗 Step 2: RabbitMQ Concepts Used

### Exchange

```
Name   : topic.exchange
Type   : topic
Durable: true
```

### Queues

```
order.queue
payment.queue
audit.queue
all.queue
```

### Topics (Routing Keys)

> Topics are **logical routing keys**, not physical resources.

```
order.created
order.updated
payment.success
payment.failed
```

---

## 🔗 Step 3: Exchange Bindings (Topic Patterns)

| Queue         | Binding Pattern | Matches Topics                  |
| ------------- | --------------- | ------------------------------- |
| order.queue   | order.*         | order.created, order.updated    |
| payment.queue | payment.*       | payment.success, payment.failed |
| audit.queue   | *.*             | all two-level topics            |
| all.queue     | #               | all topics                      |

---

## ⚙️ Step 4: Resource Creation (Automatic)

RabbitMQ resources are **automatically created by Spring Boot** at application startup via `RabbitConfig`.

✔ No manual creation required
✔ Idempotent
✔ Safe for restarts

> If the exchange or queues already exist, Spring Boot will reuse them.

---

## ⚙️ Step 5: Application Configuration

### `application.yml`

```yaml
spring:
  application:
    name: rabbitmq-cluster

  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: app_vhost

    listener:
      simple:
        acknowledge-mode: manual
        concurrency: 4
        max-concurrency: 8
        prefetch: 10
```

---

## 🏗 Step 6: Build the Application

```bash
mvn clean package
```

---

## ▶ Step 7: Start the Spring Boot Application

Run the consumer application:

```bash
mvn spring-boot:run
```

On startup, Spring Boot will:

* Connect to RabbitMQ
* Declare exchange, queues, and bindings
* Start multiple consumer threads

---

## 🧪 Step 8: Publish Messages (Topic Creation & Testing)

> Topics are created **implicitly** when a message is published with a routing key.

---

### ▶ Option A: RabbitMQ Management UI (Recommended)

1. Open

   ```
   http://localhost:15672
   ```
2. Go to **Exchanges → topic.exchange**
3. Scroll to **Publish message**
4. Enter:

```
Routing key  : order.created
Payload      : {"orderId":101,"status":"CREATED"}
Delivery mode: Persistent
```

5. Click **Publish message**

Expected delivery:

* `order.queue`
* `audit.queue`
* `all.queue`

---

### ▶ Option B: CLI (`rabbitmqadmin`)

```bash
docker exec -it rabbitmq1 rabbitmqadmin \
  -V app_vhost publish \
  exchange=topic.exchange \
  routing_key=payment.failed \
  payload="Payment failed for order 101"
```

Expected delivery:

* `payment.queue`
* `audit.queue`
* `all.queue`

---

### ▶ Option C: Publish a Non-matching Topic

```bash
docker exec -it rabbitmq1 rabbitmqadmin \
  -V app_vhost publish \
  exchange=topic.exchange \
  routing_key=shipment.created \
  payload="Shipment created"
```

Expected behavior:

* ❌ Not delivered to `order.queue`
* ❌ Not delivered to `payment.queue`
* ✔ Delivered to `audit.queue`
* ✔ Delivered to `all.queue`

---

## 🔍 Step 9: Verify Message Consumption

### Application Logs

```text
[order.queue] received: {"orderId":101,"status":"CREATED"}
[payment.queue] received: Payment failed for order 101
```

---

## 🔎 Step 10: Monitor Queues

### Management UI

```
Queues → Ready / Unacked
```

### CLI

```bash
docker exec -it rabbitmq1 rabbitmqctl list_queues name messages_ready messages_unacknowledged
```

---

## 🧠 Expected Behavior (Important)

* Messages remain in queues until **ACKed**
* Stopping the producer does **not** remove queued messages
* Restarting the consumer drains the backlog
* Each queue has its **own copy** of messages

---

## 🛑 Step 11: Stop Everything

```bash
docker compose down
```

(Optional: remove volumes)

```bash
docker compose down -v
```

---

## ✅ What This Project Demonstrates

* Topic exchange routing
* Spring-managed RabbitMQ topology
* `@RabbitListener` consumers
* Manual acknowledgments
* Multi-threaded consumption
* External client interoperability
* Java 21 compatibility

---

## 🔮 Future Enhancements

* Dead Letter Exchanges (DLQ)
* Retry policies
* JSON serialization/deserialization
* Metrics with Micrometer
* Testcontainers integration tests
* Spring Cloud Stream implementation

---

## 👤 Author

**Vijaya Bhaskar Oggu**
GitHub: [https://github.com/bhaskaro](https://github.com/bhaskaro)

---

### ✅ Final Note

> Topics are **routing keys**, not entities.
> If routing keys and bindings match, messaging works — regardless of client language.
