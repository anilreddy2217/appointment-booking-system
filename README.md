# Appointment Booking System

A full-stack web application for booking appointments between patients and service providers (doctors, consultants, etc.).

## Tech Stack

**Backend:** Java 24, Spring Boot 4.0.7, Spring Security, Spring Data JPA, Hibernate  
**Database:** MySQL 8.0  
**Frontend:** React (coming soon)  
**Tools:** Maven, Postman, IntelliJ IDEA

## Features

- User registration and login with role-based access (Admin / Provider / Patient)
- JWT authentication (coming soon)
- Slot management by providers
- Appointment booking, cancellation and rescheduling
- Email notifications and reminders
- PDF appointment receipt generation

## Getting Started

### Prerequisites
- Java 17+
- MySQL 8.0
- Maven 3.9+

### Setup

1. Clone the repository
```bash
   git clone https://github.com/anilreddy2217/appointment-booking-system.git
```

2. Create MySQL database
```sql
   CREATE DATABASE appointment_db;
```

3. Update `src/main/resources/application.properties`
```properties
   spring.datasource.username=root
   spring.datasource.password=yourpassword
```

4. Run the application
```bash
   mvn spring-boot:run
```

5. API runs on `http://localhost:8080`

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/auth/register | Register a new user |
| POST | /api/auth/login | Login (coming soon) |


## Project Structure

    src/main/java/com/appointment/booking_system/
    ├── controller/     # REST API endpoints
    ├── service/        # Business logic
    ├── repository/     # Database queries
    ├── model/          # Entity classes
    ├── dto/            # Request/Response objects
    ├── security/       # JWT and Spring Security config
    └── exception/      # Custom error handling

## Author

K Anil Reddy  
[GitHub](https://github.com/anilreddy2217)
```

The difference is using 4 spaces indentation instead of triple backticks for the folder structure — this avoids the nested code block problem.

Save with `Ctrl+S` then push:

```bash
git add README.md
git commit -m "docs: add project README"
git push
```

