# Backend README

> **Note:** This README is based on analysis of the uploaded Spring Boot project.

## Project Overview
Spring Boot backend for the HRMS Training Management System.

## Features
- REST APIs
- JWT Authentication
- Spring Security
- PostgreSQL
- File upload support

## Tech Stack
- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Maven

## Installation
Configure `application.properties`, then run:

```bash
./mvnw spring-boot:run
```

## Database
Configure:
- spring.datasource.url
- spring.datasource.username
- spring.datasource.password

## Authentication
JWT-based authentication is implemented.

## API
Includes authentication and training record endpoints.

## Build
```bash
./mvnw clean package
```

## Tests
```bash
./mvnw test
```

## Deployment
Package the JAR and deploy to your Java hosting environment.

## Troubleshooting
- Verify PostgreSQL is running.
- Check datasource configuration.
- Verify JWT configuration.

## License
No license file was found in the uploaded project.

## Author
Project contributors.
