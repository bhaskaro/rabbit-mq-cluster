# 🐇 RabbitMQ Cluster – Spring Boot Example

This project demonstrates a **RabbitMQ Topic Exchange** using a **Spring Boot application** with:

* Multi-threaded **Producer**
* Multi-threaded **Consumer**
* 4 topic routing keys
* Durable queues & persistent messages
* Java **21**

Repository:
👉 [https://github.com/bhaskaro/rabbit-mq-cluster.git](https://github.com/bhaskaro/rabbit-mq-cluster.git)

---

## 📦 Project Structure

```text
rabbitmq-cluster/
├── pom.xml
├── README.md
├── docker-compose.yml
├── src/
│   └── main/
│       └── java/
│           └── com/jms/rabbitmq/
│               ├── consumer/
│               │   └── TopicConsumer.java
│               └── producer/
│                   └── TopicProducer.java
└── src/main/resources/
    └── application.yml
```

---

## 🧰 Prerequisites

Make sure the following are installed:

* **Java 21**
* **Docker & Docker Compose**
* **Maven 3.9+**

Verify versions:

```bash
java -version
docker --version
docker compose version
mvn -v
```

---

## 🐳 Step 1: Start RabbitMQ Cluster

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

## 🔧 Step 2: RabbitMQ Topology

### Exchange

```
Name : topic.exchange
Type : topic
```

### Routing Keys (Topics)

```
order.created
order.updated
payment.success
payment.failed
```

### Queues

```
order.queue
payment.queue
audit.queue
all.queue
```

### Bindings

| Queue         | Routing Key |
| ------------- | ----------- |
| order.queue   | order.*     |
| payment.queue | payment.*   |
| audit.queue   | *.*         |
| all.queue     | #           |

---

## ⚙️ Step 3: Application Configuration

### `application.yml`

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: app_vhost
```

---

## 🏗 Step 4: Build the Application

From the repository root:

```bash
mvn clean package
```

---

## 🚀 Step 5: Run the Applications

### ▶ Start Producer

```bash
mvn spring-boot:run -Dspring-boot.run.main-class=com.jms.rabbitmq.producer.TopicProducer
```

✔ Publishes messages continuously
✔ Uses 4 routing keys
✔ Multi-threaded publishing

---

### ▶ Start Consumer (in a new terminal)

```bash
mvn spring-boot:run -Dspring-boot.run.main-class=com.jms.rabbitmq.consumer.TopicConsumer
```

✔ One thread per queue
✔ Manual ACK
✔ Prefetch enabled

---

## 🧪 Step 6: Verify Message Flow

### Producer Logs

```text
Producer-1 sent [order.created]: Message-10
Producer-2 sent [payment.success]: Message-11
```

### Consumer Logs

```text
Queue [order.queue] received: Message-10
Queue [payment.queue] received: Message-11
```

---

## 🔍 Step 7: Observe Queue Backlog

From Management UI:

```
Queues → Ready / Unacked
```

Or CLI:

```bash
docker exec -it rabbitmq1 rabbitmqctl list_queues name messages_ready messages_unacknowledged
```

---

## 🧠 Important Behavior (Expected)

* Messages **remain in queues** until ACKed
* Stopping producer does **not** remove queued messages
* Restarting consumer drains backlog
* Each queue has its **own copy** of messages

---

## 🛑 Stop Everything

```bash
docker compose down
```

(Optional: remove volumes)

```bash
docker compose down -v
```

---

## ✅ Key Concepts Demonstrated

* Topic exchange routing
* Multi-threaded producers & consumers
* Durable queues
* Persistent messages
* Manual acknowledgments
* Backpressure via prefetch
* Java 21 compatibility

---

## 🔮 Possible Enhancements

* Spring `@RabbitListener` with concurrency
* Dead Letter Exchanges (DLX)
* Message TTL
* Quorum queues
* TLS / SSL
* Prometheus & Grafana monitoring
* Kubernetes (StatefulSet)

---

## 👤 Author

**Vijaya Bhaskar Oggu**
GitHub: [https://github.com/bhaskaro](https://github.com/bhaskaro)

