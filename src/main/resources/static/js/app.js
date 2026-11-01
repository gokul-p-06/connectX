"use strict";

const API_BASE = "/api";

const loginForm = document.getElementById("login-form");
const registerForm = document.getElementById("register-form");
const formTitle = document.getElementById("form-title");
const formSubtitle = document.getElementById("form-subtitle");
const switchText = document.getElementById("switch-text");
const switchModeButton = document.getElementById("switch-mode");
const statusMessage = document.getElementById("status-message");
const dashboard = document.getElementById("dashboard");
const authContainer = document.querySelector(".app-container");
const currentUsername = document.getElementById("current-username");
const logoutButton = document.getElementById("logout-button");
const userSearchInput = document.getElementById("user-search");
const userList = document.getElementById("user-list");
const conversationList = document.getElementById("conversation-list");
const conversationStatus = document.getElementById("conversation-status");
const conversationTitle = document.getElementById("conversation-title");
const conversationSubtitle = document.getElementById("conversation-subtitle");
const messageList = document.getElementById("message-list");
const messageForm = document.getElementById("message-form");
const messageContent = document.getElementById("message-content");
const sendButton = document.getElementById("send-button");
const connectionStatus = document.getElementById("connection-status");

let currentUser = null;
let selectedReceiverId = null;  
const usernameMap = new Map();
let allUsers = [];
let stompClient = null;

function connectWebSocket() {
    const token = sessionStorage.getItem("connectx_token");

    if (!token) {
        return;
    }

    const socket = new SockJS("/ws");

    stompClient = StompJs.Stomp.over(socket);

    stompClient.connect(
        {
            Authorization: `Bearer ${token}`
        },
        () => {
            console.log("WebSocket connected successfully.");
            connectionStatus.textContent = "Connected";
connectionStatus.classList.add("connected");

            stompClient.subscribe(
                "/user/queue/messages",
                (frame) => {
                    const message = JSON.parse(frame.body);
                    loadConversations();

                    if (
                        selectedReceiverId &&
                        (
                            message.senderId === selectedReceiverId ||
                            message.receiverId === selectedReceiverId
                        )
                    ) {
                        loadConversation(selectedReceiverId);
                    }
                }
            );
        },
        (error) => {
    console.error("WebSocket connection failed:", error);

    connectionStatus.textContent = "Disconnected";
    connectionStatus.classList.remove("connected");
}
    );
}

let currentMode = "login";

function showMessage(message, type = "error") {
    statusMessage.textContent = message;
    statusMessage.className = `status-message ${type}`;
    statusMessage.hidden = false;
}

function clearMessage() {
    statusMessage.textContent = "";
    statusMessage.hidden = true;
    statusMessage.className = "status-message";
}

function setLoading(form, loading, defaultText) {
    const button = form.querySelector('button[type="submit"]');

    button.disabled = loading;
    button.textContent = loading ? "Please wait..." : defaultText;
}

function switchMode() {
    clearMessage();

    if (currentMode === "login") {
        currentMode = "register";
        loginForm.hidden = true;
        registerForm.hidden = false;

        formTitle.textContent = "Create your account";
        formSubtitle.textContent = "Join ConnectX and start connecting.";
        switchText.textContent = "Already have an account?";
        switchModeButton.textContent = "Sign in";
    } else {
        currentMode = "login";
        registerForm.hidden = true;
        loginForm.hidden = false;

        formTitle.textContent = "Welcome back";
        formSubtitle.textContent = "Sign in to continue to ConnectX.";
        switchText.textContent = "Don't have an account?";
        switchModeButton.textContent = "Create account";
    }
}

switchModeButton.addEventListener("click", switchMode);
logoutButton.addEventListener("click", () => {

    if (stompClient && stompClient.connected) {
        stompClient.disconnect(() => {
            console.log("WebSocket disconnected.");
        });
    }

    stompClient = null;
    currentUser = null;
    selectedReceiverId = null;

    sessionStorage.removeItem("connectx_token");
    sessionStorage.removeItem("connectx_user");

    dashboard.hidden = true;
    authContainer.hidden = false;

    loginForm.reset();
    registerForm.reset();
    clearMessage();

    currentMode = "login";
    loginForm.hidden = false;
    registerForm.hidden = true;

    formTitle.textContent = "Welcome back";
    formSubtitle.textContent = "Sign in to continue to ConnectX.";
    switchText.textContent = "Don't have an account?";
    switchModeButton.textContent = "Create account";

    console.log("Logged out successfully.");
});
async function apiRequest(endpoint, payload) {
    let response;

    try {
        response = await fetch(`${API_BASE}${endpoint}`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json"
            },
            body: JSON.stringify(payload)
        });
    } catch {
        throw new Error(
            "Cannot connect to the server. Please check that ConnectX is running."
        );
    }

    const responseText = await response.text();
    let data = {};

    if (responseText) {
        try {
            data = JSON.parse(responseText);
        } catch {
            data = {};
        }
    }

    if (!response.ok) {
        const message =
            data.message ||
            data.detail ||
            data.error ||
            (response.status === 400
                ? "Please check your details and try again."
                : response.status === 401
                    ? "Invalid email or password."
                    : response.status === 409
                        ? "This account may already exist."
                        : `Request failed (${response.status}).`);

        throw new Error(message);
    }

        return data;
}

async function authenticatedGet(endpoint) {
const token = sessionStorage.getItem("connectx_token");


if (!token) {
    throw new Error("Your session has expired. Please sign in again.");
}

const response = await fetch(`${API_BASE}${endpoint}`, {
    method: "GET",
    headers: {
        "Accept": "application/json",
        "Authorization": `Bearer ${token}`
    }
});

const data = await response.json().catch(() => ({}));

if (!response.ok) {
    throw new Error(
        data.message || `Request failed (${response.status}).`
    );
}

return data;

}

async function loadUsers() {
try {
const users = await authenticatedGet("/users");

    allUsers = users;
    usernameMap.clear();

    users.forEach(user => {
        usernameMap.set(user.id, user.username);
    });

    renderUserList(allUsers);

} catch (error) {
    userList.textContent = "Unable to load users.";
    console.error("User loading failed:", error);
}

}


async function loadConversations() {
    try {
        const conversations = await authenticatedGet("/messages/conversations");

        conversationList.replaceChildren();

        if (conversations.length === 0) {
            conversationList.textContent = "No conversations yet.";
            return;
        }

        conversations.forEach(conversation => {
            const item = document.createElement("button");
            item.type = "button";
            item.className = "conversation-item";
            item.dataset.userId = conversation.userId;

            const username = document.createElement("strong");
            username.textContent = conversation.username;

            const preview = document.createElement("p");
            preview.textContent = conversation.lastMessage;

            const timestamp = document.createElement("small");
            timestamp.textContent = new Date(
                conversation.lastMessageAt
            ).toLocaleString();

            item.append(username, preview, timestamp);

            // Display unread message count
            if (conversation.unreadCount > 0) {
                const badge = document.createElement("span");
                badge.className = "unread-badge";
                badge.textContent = conversation.unreadCount;
                item.appendChild(badge);
            }

            item.addEventListener("click", () => {
                openConversation({
                    id: conversation.userId,
                    username: conversation.username
                });
            });

            conversationList.appendChild(item);
        });

    } catch (error) {
        conversationList.textContent = "Unable to load conversations.";
        console.error("Conversation loading failed:", error);
    }
}



function renderUserList(users) {
    userList.replaceChildren();

    if (users.length === 0) {
        userList.textContent = "No users found.";
        return;
    }

    users.forEach(user => {
        const userButton = document.createElement("button");

        userButton.type = "button";
        userButton.className = "user-item";
        userButton.textContent = user.username;
        userButton.dataset.userId = user.id;

        userButton.addEventListener("click", () => {
            openConversation(user);
        });

        userList.appendChild(userButton);
    });
}
userSearchInput.addEventListener("input", () => {
    const searchText = userSearchInput.value
        .trim()
        .toLowerCase();

    const filteredUsers = allUsers.filter(user =>
        user.username.toLowerCase().includes(searchText)
    );

    renderUserList(filteredUsers);
});

async function openConversation(user) {
    selectedReceiverId = user.id;

    document.querySelectorAll(".user-item").forEach(item => {
        item.classList.remove("active");
    });

    const selectedUserButton = document.querySelector(
        `.user-item[data-user-id="${user.id}"]`
    );

    if (selectedUserButton) {
        selectedUserButton.classList.add("active");
    }

    conversationTitle.textContent = user.username;
    conversationSubtitle.textContent = "Loading messages...";
    conversationStatus.textContent = "";

    messageList.replaceChildren();
    messageContent.disabled = true;
    sendButton.disabled = true;

    try {
    // Load messages while unread status is still available
    await loadConversation(user.id);

    // Mark incoming messages as read
    await authenticatedPut(`/messages/read/${user.id}`);

    // Refresh unread counts
    await loadConversations();

    conversationSubtitle.textContent =
        "Conversation with " + user.username;

} catch (error) {
    conversationStatus.textContent = error.message;
    conversationSubtitle.textContent =
        "Unable to load conversation.";

} finally {
    messageContent.disabled = false;
    sendButton.disabled = false;
}
}
async function authenticatedPost(endpoint, payload) {
    const token = sessionStorage.getItem("connectx_token");

    if (!token) {
        throw new Error("Your session has expired. Please sign in again.");
    }

    const response = await fetch(`${API_BASE}${endpoint}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json",
            "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify(payload)
    });

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
        throw new Error(data.message || `Request failed (${response.status}).`);
    }

    return data;
}
async function authenticatedPut(endpoint) {
    const token = sessionStorage.getItem("connectx_token");

    if (!token) {
        throw new Error("Your session has expired. Please sign in again.");
    }

    const response = await fetch(`${API_BASE}${endpoint}`, {
        method: "PUT",
        headers: {
            "Accept": "application/json",
            "Authorization": `Bearer ${token}`
        }
    });

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
        throw new Error(data.message || `Request failed (${response.status}).`);
    }

    return data;
}
async function loadConversation(receiverId) {
    const [sentMessages, receivedMessages] = await Promise.all([
        authenticatedGet("/messages/sent"),
        authenticatedGet("/messages/received")
    ]);

    const sent = sentMessages.filter(message =>
        message.senderId === currentUser.id &&
        message.receiverId === receiverId
    );

    const received = receivedMessages.filter(message =>
        message.senderId === receiverId &&
        message.receiverId === currentUser.id
    );

    const conversation = [...sent, ...received]
        .sort((a, b) =>
            new Date(a.createdAt) - new Date(b.createdAt)
        );

    renderMessages(conversation);
}
function renderMessages(messages) {
    messageList.replaceChildren();

    if (messages.length === 0) {
        const emptyMessage = document.createElement("p");
        emptyMessage.textContent =
            "No messages yet. Start the conversation!";
        messageList.appendChild(emptyMessage);
        return;
    }

    // Find the first unread message received from the other user
    const firstUnreadIndex = messages.findIndex(message =>
        message.senderId === selectedReceiverId &&
        message.receiverId === currentUser.id &&
        message.read === false
    );

    messages.forEach((message, index) => {

        // Insert unread divider before the first unread message
        if (index === firstUnreadIndex) {
            const divider = document.createElement("div");
            divider.className = "unread-divider";

            const dividerText = document.createElement("span");
            dividerText.textContent = "NEW MESSAGES";

            divider.appendChild(dividerText);
            messageList.appendChild(divider);
        }

        const messageElement = document.createElement("div");

        const isSent = message.senderId === currentUser.id;

        messageElement.className = isSent
            ? "message sent"
            : "message received";

        const sender = document.createElement("strong");
        sender.textContent = isSent
            ? "You"
            : usernameMap.get(message.senderId) || "Unknown user";

        const content = document.createElement("p");
        content.textContent = message.content;

        const timestamp = document.createElement("small");
        timestamp.textContent = new Date(message.createdAt)
            .toLocaleString();

        messageElement.append(sender, content, timestamp);
        messageList.appendChild(messageElement);
    });

    // Scroll to the first unread message, or bottom if everything is read
    requestAnimationFrame(() => {
        if (firstUnreadIndex !== -1) {
            const unreadMessage = messageList.children[
                firstUnreadIndex + 1
            ];

            unreadMessage?.scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
        } else {
            messageList.scrollTo({
                top: messageList.scrollHeight,
                behavior: "smooth"
            });
        }
    });
}
messageContent.addEventListener("keydown", (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
        event.preventDefault();
        messageForm.requestSubmit();
    }
});
messageForm.addEventListener("submit", (event) => {
event.preventDefault();
const content = messageContent.value.trim();

if (!content || !selectedReceiverId) {
    return;
}

if (!stompClient || !stompClient.connected) {
    conversationStatus.textContent = "Not connected. Please reconnect.";
    return;
}

sendButton.disabled = true;

try {
    stompClient.send(
        "/app/messages",
        {},
        JSON.stringify({
            receiverId: selectedReceiverId,
            content: content
        })
    );

    messageContent.value = "";
    conversationStatus.textContent = "Message sent.";
} catch (error) {
    conversationStatus.textContent = "Failed to send message.";
    console.error("Message sending failed:", error);
} finally {
    sendButton.disabled = false;
}

});


registerForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearMessage();

    const username = document.getElementById("register-username")
        .value.trim();
    const email = document.getElementById("register-email")
        .value.trim();
    const password = document.getElementById("register-password")
        .value;

    if (username.length < 3 || username.length > 30) {
        showMessage("Username must be between 3 and 30 characters.");
        return;
    }

    if (password.length < 8 || password.length > 72) {
        showMessage("Password must be between 8 and 72 characters.");
        return;
    }

    setLoading(registerForm, true, "Create account");

    try {
        const result = await apiRequest("/auth/register", {
            username,
            email,
            password
        });

        showMessage(
            result.message || "Registration successful! You can now sign in.",
            "success"
        );

        registerForm.reset();

        // Switch to login after displaying the successful registration message.
        currentMode = "register";
        switchMode();

        document.getElementById("login-email").value = email;
        document.getElementById("login-password").focus();
    } catch (error) {
        showMessage(error.message);
    } finally {
        setLoading(registerForm, false, "Create account");
    }
});
console.log("Login event listener registered");
loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    console.log("Login form submitted");
    clearMessage();

    const email = document.getElementById("login-email")
        .value.trim();
    const password = document.getElementById("login-password")
        .value;

    if (password.length > 72) {
        showMessage("Password must not exceed 72 characters.");
        return;
    }

    setLoading(loginForm, true, "Sign in");

    try {
        const result = await apiRequest("/auth/login", {
            email,
            password
        });

        if (!result.token) {
            throw new Error("Login response did not contain an authentication token.");
        }

        // Temporary authentication checkpoint.
        // The messaging dashboard will be implemented next.
        sessionStorage.setItem("connectx_token", result.token);
        sessionStorage.setItem("connectx_user", JSON.stringify({
            id: result.id,
            username: result.username,
            email: result.email
        }));

        showDashboard({
    id: result.id,
    username: result.username,
    email: result.email
});

console.info("ConnectX login successful.");
    } catch (error) {
        showMessage(error.message);
    } finally {
        setLoading(loginForm, false, "Sign in");
    }
});
function showDashboard(user) {
    currentUser = user;

    authContainer.hidden = true;
    dashboard.hidden = false;
    currentUsername.textContent = user.username;

    loadUsers();
    loadConversations();
    console.log("User loading triggered");
    try {
        connectWebSocket();
    } catch (error) {
        console.error("WebSocket initialization failed:", error);
    }
}
// Restore user session after page refresh
const savedToken = sessionStorage.getItem("connectx_token");
const savedUser = sessionStorage.getItem("connectx_user");

if (savedToken && savedUser) {
    try {
        const user = JSON.parse(savedUser);

        if (user.id && user.username && user.email) {
            showDashboard(user);
        } else {
            throw new Error("Invalid saved user");
        }
    } catch (error) {
        sessionStorage.removeItem("connectx_token");
        sessionStorage.removeItem("connectx_user");

        dashboard.hidden = true;
        authContainer.hidden = false;
    }
}
