# Turf Booking System

A Java / Spring Boot REST API for booking sports courts. It prevents double bookings and keeps a waiting list that automatically assigns a slot when someone cancels.

## Tech stack
Java 17, Spring Boot 3, Spring Data JPA (Hibernate), MySQL, Bean Validation, JUnit 5 + Mockito, Swagger UI (springdoc), Maven.

## Features
- Manage courts and customers
- Book a court for a time range (price calculated from duration)
- Double-booking prevention (overlap detection)
- Cancel (soft cancel, history kept) and reschedule bookings
- Waiting list that auto-assigns freed slots, first come first served
- Clean JSON error responses with proper HTTP status codes (400, 404, 409)

## DSA used
| Where | Structure / algorithm | Why |
|---|---|---|
| `SchedulingService.hasConflict` | `TreeMap` (sorted by start time), `floorEntry` / `ceilingEntry` | Only the two neighbouring bookings need checking, so each check is O(log n) instead of scanning all bookings |
| `WaitlistService.processWaitlist` | `PriorityQueue` (min-heap) | The longest-waiting customer is always served first; `poll()` is O(log n) |

Overlap rule: two ranges overlap when `newStart < existingEnd` AND `newEnd > existingStart`. Back-to-back bookings (10:00-11:00 and 11:00-12:00) are allowed.

Concurrency: the court row is locked (`PESSIMISTIC_WRITE`) inside a transaction while booking, cancelling or rescheduling, so two simultaneous requests cannot both pass the conflict check.

## Run locally
1. Install JDK 17, Maven (or use VS Code's Java tools) and MySQL.
2. Set your database credentials as environment variables (do not commit your password):
   - Windows (PowerShell): `$env:DB_USERNAME="root"; $env:DB_PASSWORD="your_password"`
   - Mac/Linux: `export DB_USERNAME=root DB_PASSWORD=your_password`
3. The database `turf_booking` is created automatically if it does not exist. Tables are created on first run.
4. Run: `mvn spring-boot:run` (or run `TurfBookingApplication` from VS Code).
5. API runs on http://localhost:8080 and Swagger UI is at http://localhost:8080/swagger-ui.html
6. Run tests: `mvn test` (the tests do not need a database).

## API
Date format: `yyyy-MM-dd`. Time format: `HH:mm`. Bookings must be between 06:00 and 23:00 and at least 30 minutes long.

| Method | URL | Description |
|---|---|---|
| POST | `/courts` | Create a court |
| GET | `/courts`, `/courts/{id}` | List / get courts |
| PUT | `/courts/{id}` | Update a court |
| POST | `/customers` | Create a customer |
| GET | `/customers`, `/customers/{id}` | List / get customers |
| PUT | `/customers/{id}` | Update a customer |
| POST | `/bookings` | Create a booking (409 if the slot is taken) |
| GET | `/bookings/{id}` | Get a booking |
| GET | `/bookings?customerId=1` or `?courtId=1&date=2026-10-20` | Search bookings |
| PUT | `/bookings/{id}/reschedule` | Move a booking |
| DELETE | `/bookings/{id}` | Cancel a booking and trigger the waiting list |
| POST | `/waitlist` | Join the waiting list for a taken slot |
| GET | `/waitlist?courtId=1&date=2026-10-20` | View the waiting list |

### Sample requests
Create a court (`POST /courts`):
```json
{ "name": "Court A", "sportType": "Football", "pricePerHour": 1200 }
```
Create a customer (`POST /customers`):
```json
{ "name": "Rahul Patil", "phone": "9876543210", "email": "rahul@example.com" }
```
Book a slot (`POST /bookings`), use a future date:
```json
{ "courtId": 1, "customerId": 1, "date": "2026-12-20", "startTime": "18:00", "endTime": "19:30" }
```
Reschedule (`PUT /bookings/1/reschedule`):
```json
{ "date": "2026-12-21", "startTime": "19:00", "endTime": "20:00" }
```

### Try the full story in Postman
1. Create a court and two customers.
2. Customer 1 books Court A, 18:00-19:00 -> 201 Created.
3. Customer 2 books the same slot -> 409 Conflict.
4. Customer 2 joins the waiting list (`POST /waitlist`, same body) -> 201.
5. Cancel booking 1 (`DELETE /bookings/1`).
6. `GET /bookings?courtId=1&date=...` shows a new BOOKED booking for customer 2, and the waiting entry is now ASSIGNED.

## Project structure
```
controller/  REST endpoints
service/     business logic (SchedulingService = overlap + price, WaitlistService = PriorityQueue)
repository/  Spring Data JPA repositories
model/       JPA entities and enums
dto/         request / response objects
exception/   custom exceptions + global handler
```

## Possible improvements
Peak-hour pricing, user login (Spring Security), pagination, Docker, a small frontend.
