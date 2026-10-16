# Turf Booking System

## About the Project
This project is built using Java and Spring Boot for booking sports courts. Customers can book a court for a selected date and time. The system prevents double bookings and maintains a waiting list. When a customer cancels a booking, the system automatically assigns the available slot to the next eligible customer on the waiting list.

## Technologies Used
- Java 17
- Spring Boot 3
- Spring Data JPA and Hibernate
- MySQL
- Bean Validation
- JUnit 5 and Mockito
- Swagger UI
- Maven

## Features
- Create, view, and update courts and customers.
- Book courts for a selected date and time.
- Calculate the booking price based on duration.
- Prevent overlapping bookings.
- Cancel bookings while keeping their history.
- Reschedule existing bookings.
- Maintain a waiting list and automatically assign cancelled slots.
- Return appropriate HTTP status codes and error messages.

## DSA Used

**1. TreeMap**
Used in `SchedulingService` to keep bookings sorted by start time. The system checks the bookings immediately before and after the requested time slot to detect conflicts, instead of scanning every booking.

**2. PriorityQueue**
Used in `WaitlistService` to manage the waiting list. It processes customers in first-come, first-served order, so the customer who has waited the longest gets the available slot first.

## Booking Conflict Rule
Two bookings overlap when the new booking starts before an existing booking ends and ends after the existing booking starts.

This allows back-to-back bookings. For example, one customer can book from 10:00 to 11:00 and another from 11:00 to 12:00.

## Concurrency Handling
The system uses database row locking with `PESSIMISTIC_WRITE` inside a transaction when booking, cancelling, or rescheduling. This helps prevent simultaneous requests from booking the same court and time slot.

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/courts` | Create a court |
| GET | `/courts` | Get all courts |
| GET | `/courts/{id}` | Get a court by ID |
| PUT | `/courts/{id}` | Update a court |
| POST | `/customers` | Create a customer |
| GET | `/customers` | Get all customers |
| GET | `/customers/{id}` | Get a customer by ID |
| PUT | `/customers/{id}` | Update a customer |
| POST | `/bookings` | Create a booking |
| GET | `/bookings/{id}` | Get a booking |
| GET | `/bookings?customerId=1` | Search bookings by customer |
| GET | `/bookings?courtId=1&date=2026-12-20` | Search bookings by court and date |
| PUT | `/bookings/{id}/reschedule` | Reschedule a booking |
| DELETE | `/bookings/{id}` | Cancel a booking |
| POST | `/waitlist` | Join the waiting list |
| GET | `/waitlist?courtId=1&date=2026-12-20` | View the waiting list |

A conflicting booking returns `409 Conflict`, while invalid requests return `400 Bad Request` and missing records return `404 Not Found`.

## Sample Requests

**Create a court** — `POST /courts`

```json
{
  "name": "Court A",
  "sportType": "Football",
  "pricePerHour": 1200
}
```

**Create a customer** — `POST /customers`

```json
{
  "name": "Rahul Patil",
  "phone": "9876543210",
  "email": "rahul@example.com"
}
```

**Book a court** — `POST /bookings`

```json
{
  "courtId": 1,
  "customerId": 1,
  "date": "2026-12-20",
  "startTime": "18:00",
  "endTime": "19:30"
}
```

**Reschedule a booking** — `PUT /bookings/1/reschedule`

```json
{
  "date": "2026-12-21",
  "startTime": "19:00",
  "endTime": "20:00"
}
```

## How to Run the Project
1. Install Java 17, Maven, and MySQL.
2. Set `DB_USERNAME` and `DB_PASSWORD` as environment variables. In VS Code you can do this in `.vscode/launch.json` under `env`. Never commit your password.
3. Run `mvn spring-boot:run` from the project directory, or run `TurfBookingApplication` from VS Code.
4. Open `http://localhost:8080/swagger-ui.html` to view and test the APIs.
5. Run `mvn test` to execute the test cases.

## What I Learned
While working on this project, I learned how to build REST APIs using Spring Boot, connect the application to a MySQL database, and manage data using Spring Data JPA.

I also learned how to test APIs using Swagger UI, write unit tests with JUnit and Mockito, and use a TreeMap and a PriorityQueue to solve a real scheduling problem.

## Challenges
One of the main challenges was preventing two customers from booking the same court and time slot. I addressed this using booking overlap checks and database locking.

Another challenge was managing the waiting list so that a cancelled slot could be assigned to the next customer in the correct order.

## Future Improvements
- Add user authentication and authorization.
- Develop a frontend for customers.
- Introduce peak-hour pricing.
- Add pagination for large datasets.
- Containerize the application using Docker.
