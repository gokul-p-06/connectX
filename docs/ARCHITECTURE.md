# ConnectX — System Architecture

## 1. Architecture Overview

ConnectX follows a **layered monolithic architecture** using Spring Boot.

The frontend communicates with the backend through REST APIs and WebSocket connections. The backend handles authentication, business logic, message processing, and database operations.

MySQL provides persistent storage.

## 2. High-Level Architecture

```text
             CLIENT
       HTML / CSS / JavaScript
                 |
        +--------+--------+
        |                 |
      REST API         WebSocket
        |              STOMP
        |                 |
        +--------+--------+
                 |
          SPRING BOOT
                 |
       +---------+---------+
       |                   |
  Controller Layer    WebSocket Layer
       |                   |
       +---------+---------+
                 |
           Service Layer
                 |
          Repository Layer
                 |
             Hibernate
                 |
             MySQL
```

## 3. Backend Layers

### Controller Layer

Handles incoming HTTP requests and returns appropriate responses.

Examples:

* AuthController
* UserController
* MessageController

### Service Layer

Contains application business logic.

Responsibilities:

* User registration and authentication workflows.
* Message creation and retrieval.
* Conversation management.
* Unread message counting.
* Read-status updates.
* Pagination validation.

### Repository Layer

Uses Spring Data JPA to communicate with MySQL.

Responsibilities:

* User database operations.
* Message persistence.
* Conversation queries.
* Unread message queries.
* Paginated message retrieval.

### Entity Layer

Represents database entities.

Examples:

* User
* Message

### DTO Layer

Transfers request and response data between the frontend and backend.

Examples:

* SendMessageRequest
* MessageResponse
* ConversationResponse

### Configuration Layer

Contains application configuration, including Spring Security, JWT authentication, and WebSocket configuration.

## 4. Authentication Flow

```text
User
 |
 | Registration / Login
 v
AuthController
 |
 v
AuthService
 |
 v
UserRepository
 |
 v
MySQL

Login successful
 |
 v
JWT Generated
 |
 v
Client stores JWT
 |
 v
Subsequent API requests
 |
 v
JwtAuthenticationFilter
 |
 v
Spring Security
 |
 v
Authorized Controller
```

Passwords are hashed using BCrypt. JWT is used for stateless authentication.

## 5. REST Messaging Flow

```text
Client
  |
  | POST /api/messages
  v
MessageController
  |
  v
MessageService
  |
  v
MessageRepository
  |
  v
MySQL
```

The message is saved in the database and returned as a response DTO.

## 6. Real-Time Messaging Flow

```text
Sender Browser
      |
      | STOMP Message
      v
WebSocket Connection
      |
      v
Spring Boot WebSocket Handler
      |
      v
Message Processing
      |
      v
Database Persistence
      |
      v
Recipient Subscription
      |
      v
Recipient Browser
```

WebSocket with STOMP enables real-time communication between connected users.

## 7. Message History and Pagination

Conversation history is retrieved using a paginated REST endpoint.

```text
GET /api/messages/conversation/{otherUserId}?page=0&size=30
```

Pagination behavior:

* Default page: 0.
* Default page size: 30.
* Maximum page size: 100.
* Messages are ordered by creation time and ID.
* Older messages can be loaded on demand.
* Frontend preserves the scroll position while loading older messages.

## 8. Unread Message Tracking

ConnectX tracks unread messages for each conversation.

The application supports:

* Retrieving unread message counts.
* Marking messages as read.
* Updating the unread indicator in the frontend.

## 9. Database Architecture

MySQL stores persistent application data.

Main entities:

### User

Represents a registered application user.

### Message

Represents a message sent from one user to another.

Messages reference their sender and receiver through database relationships.

## 10. Technology Stack

| Layer       | Technology                   |
| ----------- | ---------------------------- |
| Frontend    | HTML, CSS, JavaScript        |
| Backend     | Java 21, Spring Boot         |
| API         | Spring Web                   |
| Security    | Spring Security, JWT, BCrypt |
| Real-time   | WebSocket, STOMP             |
| ORM         | Hibernate                    |
| Persistence | Spring Data JPA              |
| Database    | MySQL 8                      |
| Build       | Maven                        |
| Testing     | JUnit, Mockito, MockMvc      |

## 11. Architectural Decisions

### Why Monolithic Architecture?

* Simpler development and maintenance.
* Easier debugging.
* Single deployment unit.
* Suitable for the current project scope.
* Avoids unnecessary microservice complexity.

### Why WebSocket?

* Enables real-time communication.
* Supports persistent connections.
* Avoids repeatedly polling the server for new messages.

### Why JWT?

* Stateless authentication.
* Token-based API authorization.
* Suitable for frontend-backend communication.

## 12. Current Project Scope

The current implementation focuses on secure one-to-one messaging, real-time delivery, persistent message history, pagination, and unread-status tracking.

Group messaging, file sharing, voice/video calls, and microservices are outside the current implementation scope.
