# ConnectX — Real-Time Messaging Application

## Project Overview

ConnectX is a real-time messaging application built using Java 21 and Spring Boot.

The application enables users to register, authenticate securely, communicate through one-to-one conversations, and receive messages in real time.

It uses REST APIs for conventional operations and WebSocket with STOMP for real-time communication.

The project follows a layered monolithic architecture with MySQL database persistence.

## Features

* User registration and login.
* JWT-based authentication.
* Password encryption using BCrypt.
* Spring Security authorization.
* User listing.
* One-to-one messaging.
* Real-time message delivery using WebSocket and STOMP.
* Persistent message storage using MySQL.
* Conversation history.
* Paginated message retrieval.
* Load older messages without losing scroll position.
* Unread message counts.
* Message read-status tracking.
* Global exception handling.
* Automated backend tests.

## Technology Stack

### Backend

* Java 21
* Spring Boot
* Spring Web
* Spring Security
* Spring WebSocket
* STOMP
* Spring Data JPA
* Hibernate
* JWT
* Bean Validation
* Maven

### Database

* MySQL 8
* SQL

### Frontend

* HTML5
* CSS3
* JavaScript

### Testing

* JUnit
* Mockito
* Spring Boot Test
* MockMvc

### Development Tools

* IntelliJ IDEA
* Visual Studio Code
* Git
* GitHub
* MySQL Workbench
* Postman

## System Architecture

ConnectX follows a layered monolithic architecture.

The backend is organized into the following layers:

1. Controller Layer — Handles incoming HTTP requests.
2. Service Layer — Contains business logic.
3. Repository Layer — Handles database operations.
4. Entity Layer — Represents database entities.
5. DTO Layer — Transfers data between application layers.
6. Configuration Layer — Manages security and application configuration.

### Communication Architecture

**REST API**

* Registration and authentication.
* User management.
* Message history.
* Conversation retrieval.
* Unread counts and read-status updates.

**WebSocket with STOMP**

* Real-time message delivery.
* Live communication between connected users.

**Database**

* MySQL stores user and message information.

## Authentication and Security

ConnectX uses:

* Spring Security for endpoint protection.
* JWT for stateless authentication.
* BCrypt for password hashing.
* Authentication filters for JWT validation.
* Authorization checks for protected messaging operations.

## Message Pagination

Conversation history supports pagination.

* Default page size: 30 messages.
* Older messages can be loaded on demand.
* Messages are ordered using creation time and ID.
* Scroll position is preserved when older messages are loaded.

## Project Setup

### Prerequisites

Install:

* Java 21
* MySQL 8
* Git

### 1. Clone the Repository

```bash
git clone <your-github-repository-url>
cd connectX
```

### 2. Create the Database

```sql
CREATE DATABASE connectx;
```

Configure your MySQL database credentials.

### 3. Configure Environment Variables

Set the following environment variables:

```text
DB_PASSWORD=your_mysql_password
JWT_SECRET=your_secure_jwt_secret
```

Never commit actual passwords or JWT secrets to GitHub.

### 4. Run the Application

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On Linux/macOS:

```bash
./mvnw spring-boot:run
```

### 5. Open the Application

```text
http://localhost:8080/
```

## Running Tests

Run the complete automated test suite:

Windows:

```powershell
.\mvnw.cmd clean test
```

Linux/macOS:

```bash
./mvnw clean test
```

## Development Status

**Core application implementation completed.**

Implemented modules include authentication, authorization, user management, one-to-one messaging, real-time communication, message persistence, pagination, unread tracking, and exception handling.

Core messaging functionality has been manually verified using two user accounts.

Automated backend tests have passed in the development environment.

Final documentation and project preparation are ongoing.

## Future Enhancements

Possible future extensions include:

* Group messaging.
* File and image sharing.
* Message delivery receipts.
* Typing indicators.
* Push notifications.
* Deployment to a cloud platform.

These are future enhancements, not currently claimed as implemented.

## Learning Objectives

This project demonstrates practical experience with:

* Java backend development.
* Spring Boot application development.
* REST API design.
* Spring Security and JWT authentication.
* Relational database management.
* WebSocket communication.
* Layered monolithic architecture.
* Automated testing.
* Git version control.
