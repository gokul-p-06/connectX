# ConnectX — API Documentation

## 1. Base URL

```text
http://localhost:8080/api
```

## 2. Authentication APIs

Base path: `/auth`

| Method | Endpoint         | Description                        | Success |
| ------ | ---------------- | ---------------------------------- | ------- |
| POST   | `/auth/register` | Register a new user                | 201     |
| POST   | `/auth/login`    | Authenticate user and generate JWT | 200     |

### Register

Request:

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "your_password"
}
```

Response:

```json
{
  "message": "User registered successfully",
  "id": 1,
  "username": "alice",
  "email": "alice@example.com"
}
```

### Login

Request:

```json
{
  "email": "alice@example.com",
  "password": "your_password"
}
```

Response:

```json
{
  "message": "Login successful",
  "id": 1,
  "username": "alice",
  "email": "alice@example.com",
  "token": "<JWT_TOKEN>"
}
```

## 3. User APIs

Base path: `/users`

| Method | Endpoint    | Description                        |
| ------ | ----------- | ---------------------------------- |
| GET    | `/users/me` | Get authenticated user information |
| GET    | `/users`    | Retrieve other registered users    |

### Current User

Response:

```json
{
  "message": "Authenticated successfully",
  "userId": "1"
}
```

### List Users

Returns a list of users excluding the authenticated user.

Example response:

```json
[
  {
    "id": 2,
    "username": "bob"
  }
]
```

## 4. Message APIs

Base path: `/messages`

| Method | Endpoint                               | Description                             | Success |
| ------ | -------------------------------------- | --------------------------------------- | ------- |
| POST   | `/messages`                            | Send a message                          | 201     |
| GET    | `/messages/sent`                       | Retrieve sent messages                  | 200     |
| GET    | `/messages/received`                   | Retrieve received messages              | 200     |
| GET    | `/messages/conversations`              | Retrieve conversation list              | 200     |
| GET    | `/messages/conversation/{otherUserId}` | Retrieve paginated conversation history | 200     |
| GET    | `/messages/unread/{senderId}`          | Get unread message count                | 200     |
| PUT    | `/messages/read/{senderId}`            | Mark messages as read                   | 200     |

### Send Message

Request:

```json
{
  "receiverId": 2,
  "content": "Hello!"
}
```

The authenticated user's ID is used as the sender ID.

### Retrieve Conversation History

Example:

```http
GET /api/messages/conversation/2?page=0&size=30
```

Query parameters:

| Parameter | Default | Description                 |
| --------- | ------: | --------------------------- |
| page      |       0 | Page number                 |
| size      |      30 | Number of messages per page |

Maximum page size: 100.

The response uses Spring Data's `Page<MessageResponse>` structure.

### Get Unread Count

```http
GET /api/messages/unread/2
```

Returns the unread message count for the authenticated user from sender ID 2.

### Mark Messages as Read

```http
PUT /api/messages/read/2
```

Returns the number of messages marked as read.

## 5. Health API

| Method | Endpoint  | Description              |
| ------ | --------- | ------------------------ |
| GET    | `/health` | Check application health |

Example response:

```json
{
  "status": "UP",
  "application": "ConnectX"
}
```

## 6. Authentication

Authenticated endpoints use JWT authentication.

Include the token in the HTTP Authorization header:

```http
Authorization: Bearer <JWT_TOKEN>
```

Registration and login are the endpoints used to obtain authentication credentials.

## 7. HTTP Status Codes

| Status | Meaning                            |
| ------ | ---------------------------------- |
| 200    | Request successful                 |
| 201    | Resource created                   |
| 400    | Invalid request                    |
| 401    | Authentication required or invalid |
| 403    | Access forbidden                   |
| 404    | Resource not found                 |
| 500    | Internal server error              |

Actual error responses are handled by the application's exception-handling configuration.

## 8. Real-Time Communication

ConnectX also supports real-time messaging through WebSocket and STOMP.

The exact connection URL, subscription destinations, and message destinations are defined by the application's WebSocket configuration.
