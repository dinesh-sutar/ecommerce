# E-Commerce Backend

A production-oriented **E-Commerce REST API** built using **Java 17 and Spring Boot 3**.

The application provides JWT-based authentication with refresh tokens, product management, shopping cart functionality, coupons, product-level discounts, and Cash on Delivery (COD) order checkout.

The project focuses on clean backend architecture, server-side pricing, validation, exception handling, security, and automated testing.

---

## Features

### Authentication

- User registration
- User login
- JWT access token generation
- Refresh token generation and validation
- Refresh token revocation on logout
- Password encryption using BCrypt
- Stateless authentication using Spring Security

### Product Management

- Create products
- Get all products
- Search products
- Filter products by category
- Pagination support
- Get product by ID
- SKU uniqueness validation

### Cart Management

- Add products to cart
- Get logged-in user's cart
- Update product quantity
- Remove products from cart
- Clear entire cart
- Apply coupon
- Remove coupon

### Coupon Management

- Percentage-based coupons
- Flat amount coupons
- Minimum cart value validation
- Maximum discount cap
- Expiry validation
- Active/inactive coupon validation

### Product Discounts

- Product-level discounts
- Percentage discounts
- Flat discounts
- Discount validity period
- Minimum cart value support
- Maximum discount cap
- Multiple discounts can be associated with a product
- Best eligible discount is automatically selected
- Multiple discounts do not stack on the same product

### Order Management

- Cash on Delivery (COD) checkout
- Server-side price calculation
- Product stock validation
- Stock reduction during checkout
- Order creation
- Order item snapshots
- Get logged-in user's orders
- Get order by ID

---

# Tech Stack

| Technology | Usage |
|---|---|
| Java 17 | Programming Language |
| Spring Boot 3.5 | Backend Framework |
| Spring Security | Authentication and Security |
| JWT | Stateless Authentication |
| Spring Data JPA | Database Access |
| Hibernate | ORM |
| PostgreSQL | Relational Database |
| Lombok | Boilerplate Reduction |
| Jakarta Validation | Request Validation |
| JUnit 5 | Unit Testing |
| Mockito | Mocking Framework |
| JaCoCo | Test Coverage |
| springdoc-openapi | Swagger / OpenAPI Documentation |
| Maven | Build Tool |

---

# Project Architecture

The project follows a layered architecture.

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

The request flow is generally:

```text
Client Request
      ↓
Controller
      ↓
Service
      ↓
Repository
      ↓
PostgreSQL Database
```

---

# Project Structure

```text
com.example.ecommerce
│
├── auth
│   ├── controller
│   ├── dto
│   └── service
│
├── cart
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   └── service
│
├── coupon
│   ├── dto
│   ├── entity
│   ├── enums
│   ├── repository
│   └── service
│
├── discount
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── enums
│   ├── repository
│   └── service
│
├── order
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── enums
│   ├── repository
│   └── service
│
├── product
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   └── service
│
├── user
│   ├── dto
│   ├── entity
│   └── repository
│
├── security
│
├── exception
│
└── EcommerceApplication.java
```

---

# Prerequisites

Before running the application, make sure the following are installed:

- Java 17 or higher
- Maven
- PostgreSQL 14 or higher

> The project targets Java 17. It can also run on newer compatible JDK versions.

---

# Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE ecommerce_db;
```

---

# Application Configuration

Configure the application in:

```text
src/main/resources/application.properties
```

Example configuration:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce_db
spring.datasource.username=postgres
spring.datasource.password=root

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

jwt.secret=your-very-secure-secret-key-with-at-least-32-characters
jwt.access-token-expiration=900000
jwt.refresh-token-expiration=604800000
```

Update the database credentials and JWT secret according to your local environment.

---

# Token Expiration

| Token | Expiration |
|---|---|
| Access Token | 15 Minutes |
| Refresh Token | 7 Days |

The expiration values are configured in milliseconds.

```properties
jwt.access-token-expiration=900000
jwt.refresh-token-expiration=604800000
```

---

# Running the Application

Using Maven:

```bash
mvn spring-boot:run
```

Or using Maven Wrapper:

### Windows

```bash
mvnw.cmd spring-boot:run
```

### Linux / macOS

```bash
./mvnw spring-boot:run
```

The application starts at:

```text
http://localhost:8080
```

---

# API Documentation

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON is available at:

```text
http://localhost:8080/v3/api-docs
```

Swagger can be used to explore and test the available API endpoints.

For protected endpoints, use the access token obtained from login or registration.

```text
Authorization: Bearer <access_token>
```

---

# API Overview

## Authentication

| Method | Endpoint | Description | Authentication |
|---|---|---|---|
| POST | `/api/auth/register` | Register a new user | Public |
| POST | `/api/auth/login` | Login user | Public |
| POST | `/api/auth/refresh` | Generate a new access token | Refresh Token |
| POST | `/api/auth/logout` | Revoke refresh token | Protected |

---

## Products

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/products` | Get products |
| GET | `/api/products/{id}` | Get product by ID |
| POST | `/api/products` | Create a product |

### Product Search and Pagination

Example:

```http
GET /api/products?search=mouse&category=Electronics&page=0&size=10
```

### Create Product

```json
{
  "name": "Wireless Mouse",
  "description": "Ergonomic wireless mouse",
  "sku": "WM-001",
  "price": 799.00,
  "stock": 50,
  "category": "Electronics",
  "active": true
}
```

---

## Cart

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/cart` | Get logged-in user's cart |
| POST | `/api/cart/items` | Add product to cart |
| PUT | `/api/cart/items/{productId}` | Update cart item quantity |
| DELETE | `/api/cart/items/{productId}` | Remove product from cart |
| DELETE | `/api/cart` | Clear entire cart |

### Add Product to Cart

```json
{
  "productId": 1,
  "quantity": 2
}
```

### Update Cart Item

```json
{
  "quantity": 5
}
```

---

## Coupons

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/cart/coupon` | Apply coupon |
| DELETE | `/api/cart/coupon` | Remove applied coupon |

### Apply Coupon

```json
{
  "code": "SAVE10"
}
```

---

## Discounts

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/discounts` | Create a product discount |

Discounts are associated with products and evaluated during cart and checkout calculations.

If multiple discounts are eligible for the same product, the system selects the discount providing the highest benefit.

Multiple discounts do not stack on the same product.

---

## Orders

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/orders` | Checkout and create an order |
| GET | `/api/orders` | Get logged-in user's orders |
| GET | `/api/orders/{orderId}` | Get order by ID |

All order pricing is calculated on the server.

The client does not provide the final product prices or total amount.

---

# Authentication Flow

The application uses JWT-based authentication with access and refresh tokens.

```text
                 Register / Login
                        │
                        ▼
                Generate Tokens
                        │
            ┌───────────┴───────────┐
            ▼                       ▼
       Access Token             Refresh Token
       15 Minutes                  7 Days
            │                       │
            ▼                       ▼
      Protected APIs           Stored in Database
                                    │
                                    ▼
                             Refresh Access Token
                                    │
                                    ▼
                                  Logout
                                    │
                                    ▼
                              Token Revoked
```

---

# JWT Authentication

## Access Token

The access token:

- Is generated after registration or login
- Has a default expiration of 15 minutes
- Contains the user's email as the JWT subject
- Is used to access protected APIs
- Is validated by the JWT authentication filter

Example header:

```text
Authorization: Bearer <access_token>
```

---

## Refresh Token

The refresh token:

- Has a default expiration of 7 days
- Is stored in the database
- Can be revoked during logout
- Must exist in the database
- Must not be revoked
- Must not be expired
- Must pass JWT signature validation

The refresh flow validates both:

```text
JWT Signature
      +
Database Token Record
      +
Expiry
      +
Revocation Status
```

---

# Pricing Flow

All pricing is calculated on the server.

The checkout calculation follows this flow:

```text
Product Original Price
        │
        ▼
Product-Level Discount
        │
        ▼
Subtotal After Discounts
        │
        ▼
Coupon Validation
        │
        ▼
Coupon Discount
        │
        ▼
Final Order Amount
```

---

# Coupon and Discount Design

The application treats coupons and discounts as two separate promotional mechanisms.

## Product-Level Discount

A product discount applies to an individual product.

```text
Product
   │
   ▼
Discount Evaluation
   │
   ▼
Best Eligible Discount
```

If multiple discounts are available for a product:

```text
Discount 1
Discount 2
Discount 3
     │
     ▼
Compare Discount Amount
     │
     ▼
Apply Best Discount Only
```

Discounts do not stack with each other.

---

## Cart-Level Coupon

A coupon applies to the overall cart.

```text
Cart
  │
  ▼
Discounted Subtotal
  │
  ▼
Coupon Validation
  │
  ▼
Coupon Discount
```

Only one coupon can be applied to a cart.

---

## Discount and Coupon Together

Product-level discounts and cart-level coupons operate at different scopes.

Therefore, both can be applied to the same order.

```text
Product Price
      │
      ▼
Product Discount
      │
      ▼
Discounted Product Total
      │
      ▼
Cart Subtotal
      │
      ▼
Coupon
      │
      ▼
Final Payable Amount
```

Example:

```text
Original Cart Total:        ₹2,000
Product Discounts:          ₹200
--------------------------------
Discounted Subtotal:        ₹1,800

Coupon Discount:            ₹180
--------------------------------
Final Order Amount:         ₹1,620
```

---

# Database Design

The application uses PostgreSQL with JPA and Hibernate.

| Table | Responsibility |
|---|---|
| `users` | User information and credentials |
| `refresh_tokens` | Refresh token storage and revocation |
| `products` | Product catalogue and inventory |
| `carts` | User shopping carts |
| `cart_items` | Products stored in carts |
| `coupons` | Cart-level promotional coupons |
| `discounts` | Product discount rules |
| `discount_items` | Mapping between discounts and products |
| `orders` | Completed customer orders |
| `order_items` | Products associated with completed orders |

---

# Order Snapshot Design

Order items store a snapshot of important product information at the time of checkout.

This includes values such as:

- Product name
- SKU
- Unit price

This prevents historical orders from changing when the product is modified later.

Example:

```text
Product Price at Checkout: ₹799
```

Later:

```text
Product Price Updated To: ₹999
```

The previously created order still stores:

```text
₹799
```

This preserves the historical accuracy of the order.

---

# Stock Management

During checkout, the application:

1. Validates that products exist.
2. Validates available stock.
3. Calculates pricing on the server.
4. Creates the order.
5. Creates order item snapshots.
6. Reduces product stock.
7. Saves the transaction.

The checkout operation is designed to run within a transaction to prevent partial order creation when an error occurs.

---

# Validation

Request validation is implemented using Jakarta Validation.

Typical validation includes:

- Required fields
- Valid email format
- Password validation
- Positive product quantity
- Positive price
- Valid request payloads

Invalid requests are rejected before reaching the main business logic where applicable.

---

# Exception Handling

The application uses custom exceptions for business-related errors.

Examples include:

- Duplicate email
- Invalid credentials
- Invalid refresh token
- Expired refresh token
- Revoked refresh token
- Product not found
- Cart-related errors
- Coupon validation errors
- Insufficient stock

These exceptions can be handled centrally to provide consistent API error responses.

---

# Testing

The project includes automated tests using:

- JUnit 5
- Mockito
- Spring Boot Test
- MockMvc
- JaCoCo

The test suite covers the main service and controller layers.

Tested areas include:

- Authentication Service
- Authentication Controller
- Cart Service
- Cart Controller
- Coupon Service
- Discount Service
- Order Service
- Product Service
- Product and other controller functionality

Run all tests:

```bash
mvn clean test
```

Based on the latest completed test run:

```text
Tests run: 146
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

---

# Test Coverage

JaCoCo is configured to generate a coverage report automatically.

Run:

```bash
mvn clean test
```

After the tests complete, open:

```text
target/site/jacoco/index.html
```

The report provides coverage information including:

- Instruction coverage
- Branch coverage
- Line coverage
- Method coverage
- Class coverage

The current testing effort focuses primarily on:

- Service-layer business logic
- Success scenarios
- Validation scenarios
- Exception scenarios
- Authentication flows
- Controller request handling

---

# Security Features

The application includes the following security features:

- BCrypt password hashing
- JWT-based authentication
- Stateless Spring Security sessions
- JWT validation
- Refresh token database storage
- Refresh token revocation
- Request validation
- Protected API endpoints
- Server-side pricing
- Server-side stock validation

---

# Design Decisions

## 1. Server-Side Pricing

Product prices and order totals are never trusted from the client.

The server calculates:

```text
Product Price
+
Product Discount
+
Coupon Discount
=
Final Order Amount
```

This prevents users from modifying request values to manipulate the final price.

---

## 2. Best Discount Selection

A product can have multiple associated discounts.

The system evaluates all eligible discounts and selects the discount that provides the highest benefit.

```text
Eligible Discounts
       │
       ▼
Calculate Each Discount
       │
       ▼
Compare Discount Amounts
       │
       ▼
Apply Best Discount
```

Only one discount is applied to a product line.

---

## 3. Coupon Applied After Product Discounts

Coupons are applied after product-level discounts.

```text
Original Product Prices
        │
        ▼
Product-Level Discounts
        │
        ▼
Discounted Cart Subtotal
        │
        ▼
Coupon
        │
        ▼
Final Amount
```

This keeps the pricing calculation predictable.

---

## 4. Refresh Token Database Storage

Refresh tokens are stored in the database.

This makes it possible to revoke tokens before their natural JWT expiration.

Without server-side storage, a JWT generally remains valid until expiration.

The refresh token table allows:

```text
Token Exists?
     │
     ▼
Is Revoked?
     │
     ▼
Is Expired?
     │
     ▼
Is JWT Valid?
     │
     ▼
Generate New Access Token
```

---

## 5. Order Data Snapshot

Order items intentionally store product details instead of relying only on the current product data.

This ensures that historical orders remain accurate even when products are updated.

---

# Assumptions and Trade-Offs

## Single Discount Per Product

Multiple discounts can be associated with a product.

However:

```text
Discount 1
+
Discount 2
=
Not Applied Together
```

The system selects only the best eligible discount.

This prevents unpredictable promotional calculations.

---

## Coupon and Product Discounts Can Both Apply

Coupons and product discounts operate at different levels.

```text
Product Discount
        +
Cart Coupon
        =
Allowed
```

---

## No Role-Based Access Control

The current implementation does not include separate roles such as:

```text
ADMIN
USER
```

Therefore, product and discount creation endpoints are not separately restricted by an administrator role.

A production implementation should protect administrative operations using role-based authorization.

---

## Refresh Tokens Are Not Rotated

The refresh token remains the same when generating a new access token.

It remains valid until:

- The token expires
- The user logs out
- The token is revoked

A future improvement could implement refresh token rotation:

```text
Old Refresh Token
        │
        ▼
Invalidate
        │
        ▼
Generate New Refresh Token
```

---

## Access and Refresh Token Type

The current JWT implementation differentiates access and refresh tokens primarily through expiration duration.

A stricter implementation could include an explicit JWT claim:

```json
{
  "type": "access"
}
```

or:

```json
{
  "type": "refresh"
}
```

The authentication filter could then explicitly reject refresh tokens when they are presented as access tokens.

---

## Database Schema Management

The project currently uses:

```properties
spring.jpa.hibernate.ddl-auto=update
```

This is suitable for development and assignment purposes.

For production environments, schema changes should be managed using migration tools such as:

- Flyway
- Liquibase

---

## Concurrency and Inventory

Stock reduction occurs inside the checkout transaction.

This helps prevent partial writes if checkout fails.

For high-concurrency production environments, inventory handling could be improved with:

- Optimistic locking using `@Version`
- Pessimistic locking
- Database row-level locking

---

# Not Implemented

The following features are currently outside the scope of this project:

- Frontend application
- Docker
- Docker Compose
- CI/CD pipeline
- Redis caching
- Role-based access control
- Payment gateway integration
- Multiple payment methods
- Refresh token rotation
- Email verification
- Password reset
- Testcontainers integration tests

The current order flow supports:

```text
Cash on Delivery (COD)
```

---

# Future Improvements

Possible improvements include:

- Admin and user roles
- Product update API
- Product delete API
- Coupon CRUD APIs
- Discount management APIs
- Payment gateway integration
- Online payments
- Refresh token rotation
- JWT token type claims
- Email verification
- Password reset functionality
- Redis caching
- Docker and Docker Compose
- CI/CD pipeline
- Testcontainers integration tests
- Optimistic locking for inventory
- Rate limiting
- Audit logging
- Monitoring and metrics

---

# Build

To build the project:

```bash
mvn clean package
```

The generated JAR file will be available in:

```text
target/
```

Run the JAR:

```bash
java -jar target/ecommerce-0.0.1-SNAPSHOT.jar
```

---

# Author

**Dinesh Kumar Sutar**

Backend / Full Stack Developer

---

## License

This project was developed for learning, demonstration, and assignment purposes.