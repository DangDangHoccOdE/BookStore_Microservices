# 📚 BookStore Microservices

## 📌 Giới thiệu

Đây là project mô phỏng hệ thống bán sách theo kiến trúc **Microservices** sử dụng Java Spring Boot.

Project được xây dựng để học và thực hành các công nghệ backend hiện đại như:

* Spring Boot
* Spring Cloud Gateway
* RabbitMQ
* Keycloak OAuth2/JWT
* PostgreSQL
* Docker
* Grafana + Prometheus
* CI/CD GitHub Actions

---

# 🏗️ Kiến trúc hệ thống

Hệ thống gồm nhiều service nhỏ, mỗi service phụ trách một chức năng riêng.

## Các service chính

| Service              | Chức năng                   |
| -------------------- | --------------------------- |
| API Gateway          | Cổng vào chung của hệ thống |
| Catalog Service      | Quản lý sách                |
| Order Service        | Quản lý đơn hàng            |
| User Service         | Quản lý người dùng          |
| Notification Service | Gửi email thông báo         |

---

# 🔄 Flow hoạt động

```text
Client
   ↓
API Gateway
   ↓
Các Microservices
   ↓
Database / RabbitMQ
```

Ví dụ khi tạo đơn hàng:

```text
Client tạo order
    ↓
Order Service xử lý
    ↓
Publish event qua RabbitMQ
    ↓
Notification Service nhận event
    ↓
Gửi email thông báo
```

---

# 🛠️ Công nghệ sử dụng

## Backend

* Java 21
* Spring Boot
* Spring Security
* Spring Cloud Gateway
* Spring Data JPA

## Database

* PostgreSQL

## Message Queue

* RabbitMQ

## Authentication

* Keycloak
* JWT OAuth2

## Monitoring

* Prometheus
* Grafana
* Loki
* Tempo

## DevOps

* Docker
* Docker Compose
* GitHub Actions

---

# 🔐 Authentication

Project sử dụng:

* Keycloak để login
* JWT token để xác thực
* API Gateway kiểm tra token trước khi request vào service

---

# 📦 Event-Driven Architecture

Project sử dụng RabbitMQ để giao tiếp bất đồng bộ giữa các service.

Ví dụ:

```text
Order Service
   ↓ publish event
RabbitMQ
   ↓ consume
Notification Service
```

Giúp:

* Giảm phụ thuộc giữa service
* Dễ scale
* Retry khi lỗi

☠️ Dead Letter Queue (DLQ)

# Project có triển khai:

* Retry Queue
* Dead Letter Queue (DLQ)

Khi message xử lý thất bại:

* Message được retry nhiều lần
* Nếu vẫn lỗi → chuyển vào DLQ
* Developer có thể kiểm tra và xử lý thủ công

Ví dụ:

Order Service
    ↓
RabbitMQ Exchange
    ↓
Notification Queue
    ↓ lỗi
Retry Queue
    ↓ vẫn lỗi
Dead Letter Queue (DLQ)

## Giúp:

* Không mất message
* Tránh crash consumer
* Dễ debug production issue

---

# 💾 Database

Mỗi service có database riêng:

| Service              | Database         |
| -------------------- | ---------------- |
| Catalog Service      | catalog-db       |
| Order Service        | orders-db        |
| User Service         | users-db         |
| Notification Service | notifications-db |

---

# 🚀 Chạy project

## 1. Clone source

```bash
git clone https://github.com/DangDangHoccOdE/BookStore_Microservices.git
cd BookStore_Microservices
```

---

## 2. Build project

```bash
./mvnw clean package -DskipTests
```

---

## 3. Chạy infrastructure

```bash
docker-compose -f deployment/docker-compose/infra.yml up -d
```

Infrastructure gồm:

* PostgreSQL
* RabbitMQ
* Keycloak
* Redis

---

## 4. Chạy các service

```bash
docker-compose -f deployment/docker-compose/apps.yml up -d
```

# 🚀 Hướng dẫn chạy project

Project sử dụng **Taskfile** để đơn giản hoá việc build và chạy hệ thống.

---

# 📋 Yêu cầu trước khi chạy

Cần cài đặt:

| Tool           | Version |
| -------------- | ------- |
| Java           | 21+     |
| Maven          | 3.8+    |
| Docker         | Latest  |
| Docker Compose | Latest  |
| Task           | Latest  |

---

# ⚙️ Cài đặt Task

## Windows

```bash id="52e0bk"
winget install Task.Task
```

Hoặc:

```bash id="bf4a9n"
choco install go-task
```

---

## MacOS

```bash id="3fmu0v"
brew install go-task
```

---

## Linux

```bash id="3h14r7"
sudo snap install task --classic
```

---

# 📦 Build project

## Format source code

```bash id="3zx2cr"
task format
```

Sử dụng Spotless để format code.

---

## Run test

```bash id="e7u1rk"
task test
```

Bao gồm:

* Unit Test
* Integration Test
* Verify project

---

## Build Docker images

```bash id="gmxj2v"
task build
```

Build image cho:

* catalog-service
* order-service
* notification-service
* api-gateway
* user-service

---

# 🐳 Chạy hệ thống

## 1. Chạy infrastructure

```bash id="7odt9v"
task start_infra
```

Infrastructure gồm:

* PostgreSQL
* RabbitMQ
* Keycloak
* Redis

---

## 2. Chạy monitoring stack

```bash id="mdrb4r"
task start_monitoring
```

Monitoring gồm:

* Grafana
* Prometheus
* Loki
* Tempo

---

## 3. Chạy toàn bộ application

```bash id="0qgq3y"
task start
```

Lệnh này sẽ:

1. Build Docker images
2. Start infrastructure
3. Start tất cả microservices

---

## 4. Chạy full hệ thống

```bash id="t5mcf0"
task start_full_apps
```

Bao gồm:

* Infrastructure
* Applications
* Monitoring

---

# 🔄 Restart hệ thống

## Restart infrastructure

```bash id="vw9duf"
task restart_infra
```

---

## Restart monitoring

```bash id="5l3z2e"
task restart_monitoring
```

---

## Restart applications

```bash id="xv9m4m"
task restart
```

---

# 🛑 Stop hệ thống

## Stop infrastructure

```bash id="vg04wa"
task stop_infra
```

---

## Stop monitoring

```bash id="s7u1i8"
task stop_monitoring
```

---

## Stop applications

```bash id="6l4kgn"
task stop
```

---

# 👤 Tài khoản mặc định

| Service  | Username | Password |
| -------- | -------- | -------- |
| Keycloak | admin    | admin    |
| RabbitMQ | guest    | guest    |
| Grafana  | admin    | admin123 |

---

# 🌐 Các URL quan trọng

| Service     | URL                                   |
| ----------- | ------------------------------------- |
| API Gateway | http://localhost:8989                 |
| Swagger UI  | http://localhost:8989/swagger-ui.html |
| RabbitMQ    | http://localhost:15672                |
| Grafana     | http://localhost:3000                 |
| Keycloak    | http://localhost:9191                 |

---

# 📚 Các kiến thức học được

Project này giúp học:

* Microservices Architecture
* API Gateway Pattern
* OAuth2 JWT Authentication
* RabbitMQ Messaging
* Event-Driven Architecture
* Docker & Docker Compose
* Monitoring & Logging
* CI/CD Pipeline
* Distributed Tracing
* Resilience4J

---

# 📁 Cấu trúc project

```text
BookStore_Microservices
│
├── api-gateway
├── catalog-service
├── order-service
├── user-service
├── notification-service
├── deployment
└── docker-compose
```

---

# 👨‍💻 Mục tiêu project

Project được xây dựng để:

* Học microservices thực tế
* Thực hành Spring ecosystem
* Hiểu cách các service giao tiếp
* Làm portfolio backend developer
