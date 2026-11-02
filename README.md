# PeopleFlow — Modern Employee & Leave Management System

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

> **Sprint 1 Deliverable**: Production-ready Employee Management and Department Management backend with persistent MySQL storage, clean layered architecture, validation, pagination, filtering, global exception handling, and 100% automated test coverage.

---

## 1. Architectural Overview & Request Flow

PeopleFlow adheres strictly to the **Layered Modular Monolith Architecture**, enforcing clean separation of concerns and data boundaries:

```
                  ┌───────────────────────────────┐
                  │    HTTP Client (Web/Postman)  │
                  └──────────────┬────────────────┘
                                 │ HTTP Requests (JSON)
                                 ▼
                  ┌───────────────────────────────┐
                  │          Controller           │
                  │   (Validation, HTTP Status)   │
                  └──────────────┬────────────────┘
                                 │ Request DTOs
                                 ▼
                  ┌───────────────────────────────┐
                  │            Service            │
                  │ (Business Rules, Transactions)│
                  └──────────────┬────────────────┘
                                 │ Domain Entities
                                 ▼
                  ┌───────────────────────────────┐
                  │          Repository           │
                  │     (Spring Data JPA / HQL)   │
                  └──────────────┬────────────────┘
                                 │ SQL
                                 ▼
                  ┌───────────────────────────────┐
                  │         MySQL 8 Engine        │
                  └───────────────────────────────┘
```

### Layer Responsibilities
- **Controller Layer** (`com.peopleflow.controller`): Ingests HTTP requests, validates request DTOs via Jakarta Validation, maps HTTP status codes (`201 Created`, `200 OK`, `204 No Content`), and delegates business execution.
- **Service Layer** (`com.peopleflow.service`): Implements domain business logic, enforces business invariants (e.g. duplicate email/name checks, referential integrity guards), manages transactional boundaries (`@Transactional`), and maps entities to response DTOs.
- **Repository Layer** (`com.peopleflow.repository`): Interfaces with Spring Data JPA for persistent CRUD operations, custom query derivation, and optimized pagination/sorting.
- **Entity Layer** (`com.peopleflow.entity`): Defines JPA entities, table constraints, indices, and JPA auditing timestamps (`createdAt`, `updatedAt`).
- **DTO Layer** (`com.peopleflow.dto`): Decouples internal database entities from external API contracts to prevent data leakage and Jackson circular references.
- **Exception Layer** (`com.peopleflow.exception`): Captures domain and platform exceptions with `@RestControllerAdvice`, returning uniform RFC-compliant JSON errors without exposing internal database stack traces.

---

## 2. Database Schema & Relational Design

### Database Name: `peopleflow_db`

#### Table: `departments`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique department identifier |
| `name` | VARCHAR(100) | NOT NULL, UNIQUE (`uk_department_name`) | Department name |
| `description` | VARCHAR(255) | NULLABLE | Department scope and duties |
| `created_at` | DATETIME | NOT NULL | Audit timestamp of creation |
| `updated_at` | DATETIME | NOT NULL | Audit timestamp of last update |

#### Table: `employees`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique employee identifier |
| `first_name` | VARCHAR(100) | NOT NULL | Employee given name |
| `last_name` | VARCHAR(100) | NOT NULL | Employee family name |
| `email` | VARCHAR(150) | NOT NULL, UNIQUE (`uk_employee_email`), INDEX | Corporate email |
| `phone` | VARCHAR(20) | NULLABLE | Contact telephone |
| `job_title` | VARCHAR(100) | NOT NULL | Current organizational role |
| `joining_date` | DATE | NOT NULL | Date joined company |
| `salary` | DECIMAL(12,2) | NOT NULL | Monthly/annual compensation |
| `department_id` | BIGINT | NOT NULL, FOREIGN KEY (`fk_employee_department`), INDEX | Foreign key referencing `departments(id)` |
| `created_at` | DATETIME | NOT NULL | Audit timestamp of creation |
| `updated_at` | DATETIME | NOT NULL | Audit timestamp of last update |

### Relationship Design: 1 Department ⟷ Many Employees
- **FetchType.LAZY**: The `@ManyToOne` relationship from `Employee` to `Department` is explicitly configured as `LAZY`. In standard JPA, `@ManyToOne` defaults to `EAGER`, which introduces the N+1 query problem on list operations.
- **Referential Integrity Defense**: Deletion of a department with active assigned employees is strictly intercepted and blocked (`HTTP 409 Conflict`), avoiding dangling employee records or cascading deletes.
- **Infinite Recursion Defense**: JPA entities are never serialized directly to HTTP responses; they are converted to DTOs in the service layer.

---

## 3. Technology Stack

- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.3.5
- **Modules**:
  - `spring-boot-starter-web` (REST APIs, Jackson, Tomcat)
  - `spring-boot-starter-data-jpa` (Hibernate ORM 6, Spring Data)
  - `spring-boot-starter-validation` (Jakarta Validation)
  - `mysql-connector-j` (MySQL 8 runtime driver)
- **Testing**:
  - JUnit 5 & Mockito
  - Spring Boot Test & MockMvc
  - H2 Database (in-memory mode for CI/test isolation)

---

## 4. Getting Started & Setup Instructions

### Prerequisites
1. **Java 21** or later installed (`java -version`).
2. **Apache Maven 3.9+** installed (`mvn -version`).
3. **MySQL Server 8.0+** running locally.

### Step 1: Start MySQL and Create the Database
Ensure your MySQL service is started (e.g., via `services.msc` or `net start MYSQL80`).
Connect to your MySQL server and create the database (though Spring Boot's connection string has `createDatabaseIfNotExist=true` enabled):
```sql
CREATE DATABASE IF NOT EXISTS peopleflow_db;
```

### Step 2: Configure Environment or application.properties
The application supports standard environment variables for zero-code configuration:
| Property | Environment Variable | Default Value |
|---|---|---|
| Database Host | `DB_HOST` | `localhost` |
| Database Port | `DB_PORT` | `3306` |
| Database Name | `DB_NAME` | `peopleflow_db` |
| Username | `DB_USERNAME` | `root` |
| Password | `DB_PASSWORD` | `root` |

### Step 3: Run the Application
Execute from the project root:
```bash
mvn spring-boot:run
```
The server will start on `http://localhost:8080`.

### Step 4: Run the Automated Test Suite
Execute:
```bash
mvn test
```
The build runs all 42 automated tests (unit tests and slice controller tests) against the in-memory H2 database.

---

## 5. API Reference & cURL Examples

Base URL: `http://localhost:8080`

### Department Management (`/api/v1/departments`)

#### 1. Create Department
- **Endpoint**: `POST /api/v1/departments`
- **Request Body**:
```json
{
  "name": "Engineering",
  "description": "Software development and engineering team"
}
```
- **cURL**:
```bash
curl -X POST http://localhost:8080/api/v1/departments \
  -H "Content-Type: application/json" \
  -d '{"name": "Engineering", "description": "Software development and engineering team"}'
```
- **Response**: `201 Created`

#### 2. Get All Departments (Paginated)
- **Endpoint**: `GET /api/v1/departments?page=0&size=10&sortBy=name&direction=asc`
- **cURL**:
```bash
curl -X GET "http://localhost:8080/api/v1/departments?page=0&size=10&sortBy=name&direction=asc"
```
- **Response**: `200 OK`

#### 3. Get Department by ID
- **Endpoint**: `GET /api/v1/departments/{id}`
- **cURL**:
```bash
curl -X GET http://localhost:8080/api/v1/departments/1
```
- **Response**: `200 OK` (includes active `employeeCount`)

#### 4. Update Department
- **Endpoint**: `PUT /api/v1/departments/{id}`
- **Request Body**:
```json
{
  "name": "Platform Engineering",
  "description": "Core platform and infrastructure team"
}
```
- **cURL**:
```bash
curl -X PUT http://localhost:8080/api/v1/departments/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "Platform Engineering", "description": "Core platform and infrastructure team"}'
```
- **Response**: `200 OK`

#### 5. Delete Department
- **Endpoint**: `DELETE /api/v1/departments/{id}`
- **cURL**:
```bash
curl -X DELETE http://localhost:8080/api/v1/departments/1
```
- **Response**: `204 No Content` (Returns `409 Conflict` if employees are assigned)

---

### Employee Management (`/api/v1/employees`)

#### 1. Create Employee
- **Endpoint**: `POST /api/v1/employees`
- **Request Body**:
```json
{
  "firstName": "Rahul",
  "lastName": "Sharma",
  "email": "rahul@example.com",
  "phone": "9876543210",
  "jobTitle": "Software Engineer",
  "joiningDate": "2026-11-01",
  "salary": 65000.00,
  "departmentId": 1
}
```
- **cURL**:
```bash
curl -X POST http://localhost:8080/api/v1/employees \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Rahul",
    "lastName": "Sharma",
    "email": "rahul@example.com",
    "phone": "9876543210",
    "jobTitle": "Software Engineer",
    "joiningDate": "2026-11-01",
    "salary": 65000.00,
    "departmentId": 1
  }'
```
- **Response**: `201 Created`

#### 2. Get All Employees (Pagination, Sorting, Filtering & Search)
- **Endpoint**: `GET /api/v1/employees`
- **Query Parameters**:
  - `page`: Page index (default `0`)
  - `size`: Page size (default `10`)
  - `sortBy`: Sorting field (default `id`, e.g. `firstName`, `salary`, `joiningDate`)
  - `direction`: `asc` or `desc` (default `asc`)
  - `departmentId`: Optional filter by department ID (e.g. `?departmentId=1`)
  - `search`: Case-insensitive search keyword across first name, last name, and email (e.g. `?search=rahul`)
- **cURL**:
```bash
curl -X GET "http://localhost:8080/api/v1/employees?page=0&size=10&sortBy=firstName&direction=asc&departmentId=1&search=rahul"
```
- **Response**: `200 OK`

#### 3. Get Employee by ID
- **Endpoint**: `GET /api/v1/employees/{id}`
- **cURL**:
```bash
curl -X GET http://localhost:8080/api/v1/employees/1
```
- **Response**: `200 OK`

#### 4. Update Employee
- **Endpoint**: `PUT /api/v1/employees/{id}`
- **cURL**:
```bash
curl -X PUT http://localhost:8080/api/v1/employees/1 \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Rahul",
    "lastName": "Sharma",
    "email": "rahul@example.com",
    "phone": "9876543210",
    "jobTitle": "Senior Software Engineer",
    "joiningDate": "2026-11-01",
    "salary": 85000.00,
    "departmentId": 1
  }'
```
- **Response**: `200 OK`

#### 5. Delete Employee
- **Endpoint**: `DELETE /api/v1/employees/{id}`
- **cURL**:
```bash
curl -X DELETE http://localhost:8080/api/v1/employees/1
```
- **Response**: `204 No Content`

---

## 6. Standardized Error Handling

All error responses follow a uniform, RFC-compliant JSON contract:

```json
{
  "timestamp": "2026-11-03T03:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Employee not found with id: 99",
  "path": "/api/v1/employees/99"
}
```

When input validation fails (`400 Bad Request`), detailed field errors are included:
```json
{
  "timestamp": "2026-11-03T03:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Input validation failed. Please check the 'validationErrors' field for details.",
  "path": "/api/v1/employees",
  "validationErrors": {
    "email": "Invalid email format",
    "salary": "Salary must be greater than zero"
  }
}
```

---

## 7. Postman Collection

Import `PeopleFlow_Sprint1_Postman_Collection.json` into Postman:
1. Open Postman ➔ Click **Import**.
2. Select the `PeopleFlow_Sprint1_Postman_Collection.json` file located in the project root.
3. The collection is pre-configured with a `{{baseUrl}}` variable defaulting to `http://localhost:8080` covering all CRUD operations.

---

## 8. Senior Engineering Decisions & Interview Preparation

### Q1: Why should `spring.jpa.hibernate.ddl-auto=update` not be used in production?
- **Answer**: `update` only adds new tables and columns; it does not drop columns, rename fields, or reliably migrate data. Furthermore, automatic schema alterations on application startup can lock production tables, introduce race conditions in clustered deployments, and bypass database change review controls. In enterprise production environments, versioned database migration frameworks like **Flyway** or **Liquibase** are mandatory.

### Q2: Why did we place `@EnableJpaAuditing` in a separate `JpaConfig` class instead of `PeopleFlowApplication`?
- **Answer**: When `@EnableJpaAuditing` is on the main application class, `@WebMvcTest` slice tests attempt to construct `JpaAuditingHandler`. Because `@WebMvcTest` disables JPA entities and repositories to keep web tests fast, this results in a `NoSuchBeanDefinitionException: jpaMappingContext`. Placing it in a dedicated `@Configuration` keeps slice tests decoupled and blazing fast.

### Q3: Why is constructor injection preferred over `@Autowired` field injection?
- **Answer**: 
  1. **Immutability**: Dependencies can be declared `final`.
  2. **Testability**: Unit tests can instantiate classes and inject mocks using `new MyService(mockRepo)` without requiring a Spring Container or reflection hacks.
  3. **Fail-fast compile checks**: Prevents `NullPointerException` at runtime due to uninitialized dependencies.

---

## 9. Branching Strategy & Git History

- `main`: Production release branch.
- `develop`: Ongoing integration branch.
- `feature/department-management`: Department CRUD, validation, and test suite.
- `feature/employee-management`: Employee CRUD, filters, pagination, and test suite.