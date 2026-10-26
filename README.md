\# ConnectX — Reliable Real-Time Chat Application



ConnectX is a real-time chat application built with Java 21 and Spring Boot.



The project focuses on reliable messaging, secure authentication, database persistence, and backend engineering best practices.



\## Project Goals



\* Implement user registration and authentication.

\* Secure endpoints using Spring Security and JWT.

\* Create private chatrooms and manage room membership.

\* Exchange messages in real time using WebSocket and STOMP.

\* Persist messages in MySQL.

\* Retrieve message history with pagination.

\* Recover missed messages after reconnection.

\* Prevent duplicate messages using idempotency.

\* Track message delivery and read status.

\* Write automated tests for APIs, business logic, and reliability features.



\## Technology Stack



\### Backend



\* Java 21

\* Spring Boot

\* Spring Web

\* Spring WebSocket

\* STOMP

\* Spring Security

\* JWT

\* Spring Data JPA

\* Hibernate

\* Maven

\* Bean Validation



\### Database



\* MySQL

\* SQL



\### Frontend



\* HTML

\* CSS

\* JavaScript



\### Testing



\* JUnit

\* Mockito

\* Spring Boot Test

\* Postman



\### Development Tools



\* IntelliJ IDEA

\* Git

\* GitHub

\* MySQL Workbench

\* VS Code



\## Architecture



The application will initially use a modular Spring Boot monolith.



The backend will separate controllers, services, repositories, entities, DTOs, and configuration.



REST APIs will handle conventional operations such as authentication and message history retrieval. WebSocket with STOMP will support real-time messaging.



MySQL will store users, chatrooms, memberships, and messages.



Microservices will be considered only after the initial application is implemented and tested.



\## Development Status



\*\*Current phase:\*\* Project planning and repository setup.



The application features listed above are planned. They are not yet implemented.



\## Planned Development Phases



1\. Project planning and GitHub setup

2\. Spring Boot project setup

3\. Database design and JPA

4\. User authentication

5\. Chatroom functionality

6\. Real-time messaging

7\. Message persistence and reliability

8\. Automated testing

9\. Microservices migration

10\. Documentation and deployment



\## Learning Objectives



This project is intended to develop practical skills in Java, Spring Boot, REST API development, SQL, authentication, WebSocket messaging, testing, Git, and software architecture.



