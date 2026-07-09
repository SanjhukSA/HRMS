
## Project Overview
Spring Boot backend for the HRMS Training Management System.
A full-stack Human Resource Management System module built for tracking employee training records and certifications,
developed as part of an NIC internship project for the Government of Tripura.

Employees can self-register, submit training records, and upload certificates; administrators review and provision access, 
and approve or invalidate submitted certificates — all backed by role-based JWT authentication.


## Tech Stack
- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Maven

## Installation
- PostgreSQL setup
- Bootstrapping the first admin account
- Configure `application.properties`, then run:

```bash
./mvnw spring-boot:run
```

## Database
Configure:
- spring.datasource.url
- spring.datasource.username
- spring.datasource.password
- jwtsecretkey

## Authentication
JWT-based authentication is implemented.

## API Overview

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Self-register (creates a `PENDING` account) |
| POST | `/api/auth/login` | Public | Authenticate, returns JWT |
| GET | `/api/auth/pending` | Admin | List accounts awaiting approval |
| POST | `/api/auth/approve` | Admin | Approve a pending account with a role |
| POST | `/api/auth/reject/{employeeId}` | Admin | Reject a pending account |
| POST | `/api/auth/provision` | Admin | Directly create/update a known employee's login |
| POST | `/api/certificates/save` | User/Admin | Submit a training record |
| PATCH | `/api/certificates/{id}/complete` | Owner | Upload a certificate for an In Progress record |
| POST | `/api/certificates/{id}/approve` | Admin | Approve/invalidate a completed record |
| GET | `/api/certificates/employee/{id}` | Self/Admin | View an employee's records |
| GET | `/api/certificates/all` | Admin | View all records across employees |
| GET | `/api/certificates/download/{id}` | Owner/Admin | Download a certificate file |
| DELETE | `/api/certificates/{id}` | Owner | Delete own record |




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


## Features
 #Authentication & Access Control:

Self-service employee registration with admin-gated approval (accounts stay PENDING until reviewed)
Role-based access control (USER / ADMIN) enforced at both the URL and method level
JWT-based stateless authentication
Admin can directly provision known-staff accounts, or review self-registrations individually (approve with a role, or reject)
`
#Training Records:

Employees submit training records scoped to their own identity — enforced server-side, not just hidden in the UI
Two-stage submission: mark a record In Progress (certificate optional) or Completed (certificate mandatory)
In-progress records can be completed later by uploading the certificate, which automatically transitions the record to Completed and queues it for admin review
Certificate files (PDF/JPEG/PNG) are validated by content type and stored server-side; download is authenticated and scoped to the owner or an admin

#Admin Review Workflow:

Admins review completed submissions and mark them Approved or Invalid
Marking a record Invalid requires accompanying remarks, visible to the employee
Admins retain full visibility across all employees' records, including those still in progress
Record deletion is owner-only — admins review via approval decisions rather than deleting

## Authors
Project Contributor