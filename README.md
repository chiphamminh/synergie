# Bright Path Scheduling

This Spring Boot backend detects tutor and room conflicts for Bright Path lessons.
The application seeds the CSV files from `src/main/resources/seed` when the
corresponding tables are empty.

## Run locally

Start MySQL with Docker Compose:

```bash
docker compose up -d
```

The local database is `brightpath` on port `3306`. The development root
password is `brightpath`.

Run the application:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The API is available at `http://localhost:8081`.

To start from a completely clean database (fresh seed reload):

```bash
docker compose down -v
docker compose up -d
./mvnw spring-boot:run
```

## Endpoints

- `POST /api/bookings` creates a booking and checks all scheduling rules.
- `PUT /api/bookings/{id}` reschedules a booking and checks all scheduling rules,
  including the 6-bookings-per-day cap.
- `POST /api/bookings/{id}/cancel` cancels a booking and preserves history.
- `GET /api/bookings?date=2026-03-06` lists bookings for a date.
- `GET /api/bookings/{id}/changes` lists the booking change history.
- `GET /api/bookings/conflicts` scans **existing** data and reports conflicts
  already present in the database — double-booked tutors, double-booked rooms,
  bookings on a closed day, and tutors over the 6-per-day cap. Omit `date` to
  scan every date in the database, or pass `?date=2026-03-10` to check one day.

`POST /api/bookings` and `PUT /api/bookings/{id}` both accept `override: true`
when a tutor already has six bookings on that date; without it the request is
rejected with `409 Conflict`. Paired bookings are allowed only when both
overlapping bookings are explicitly marked `PAIRED` and use the same tutor,
room, and time.

The application uses a fixed local time of `2026-03-06T16:30:00+07:00` by
default so cut-off and cancellation behavior is repeatable. Override it with
the `APP_FIXED_NOW` environment variable when needed.

## Link postman: https://chi12345pham-5419304.postman.co/workspace/Synergie~d59ad583-9f1d-424e-bd05-8b65dad52782/request/48736994-a6031e08-814c-4e3d-a910-c55b1020cf80?action=share&creator=48736994&ctx=documentation

## Test flow

The seed week (`2026-03-03` → `2026-03-10`) has known cases built into the
CSV. Run these in order against a freshly seeded database to see each rule
in action.

**1. Confirm the seed loaded and inspect a day with a known room conflict**

```bash
curl "http://localhost:8081/api/bookings?date=2026-03-10"
```

Look for `L033` and `L034` — same tutor, same time, different rooms.

**2. Run the conflict report over all existing data**

```bash
curl "http://localhost:8081/api/bookings/conflicts"
```

Expected findings from the seed data:
- `ROOM_DOUBLE_BOOKED` — `L033`/`L034` on `2026-03-10`
- `DAILY_CAP_EXCEEDED` — tutor `T1` on `2026-03-06` (7 bookings)
- `CLOSED_DAY_BOOKING` — `L032` on `2026-03-09` (a Monday)
- **No** report involving `L009`/`L010` — same tutor/room/time but both
  `PAIRED`, so it's a valid exam-pair exception, not a conflict.

You can scope this to one day, e.g. `?date=2026-03-06`, to isolate the
tutor-cap case.

**3. Create a booking — happy path**

```bash
curl -X POST http://localhost:8081/api/bookings \
  -H "Content-Type: application/json" \
  -d '{"date":"2026-03-04","startTime":"10:00","durationMin":60,
       "studentName":"Test Student","tutorId":"T2","roomId":"R2"}'
```

Should return `201` with the new booking.

**4. Create a booking that violates the room rule**

Reuse `T2`'s room/time from step 3 but with a different tutor:

```bash
curl -X POST http://localhost:8081/api/bookings \
  -H "Content-Type: application/json" \
  -d '{"date":"2026-03-04","startTime":"10:00","durationMin":60,
       "studentName":"Another Student","tutorId":"T3","roomId":"R2"}'
```

Should return `409 Conflict` (room already in use).

**5. Try to book on a closed day**

```bash
curl -X POST http://localhost:8081/api/bookings \
  -H "Content-Type: application/json" \
  -d '{"date":"2026-03-16","startTime":"10:00","durationMin":60,
       "studentName":"Test Student","tutorId":"T2","roomId":"R3"}'
```

Should return `400 Bad Request` (Monday is closed).

**6. Hit the 6-per-day cap on create, then override it**

Create a 7th booking for `T1` on `2026-03-06` (already has 6 active bookings
after seeding) without `override` — expect `409`. Retry with
`"override": true` — expect `201`, and check `GET /api/bookings/{id}/changes`
on the result to see both a `CREATED` and an `OVERRIDDEN` entry.

**7. Reschedule a booking into an already-full day**

Move any existing booking (`PUT /api/bookings/{id}`) to `2026-03-06` for
tutor `T1` without `override` — expect `409`. Retry with `"override": true`
in the body — expect success, and confirm `RESCHEDULED` + `OVERRIDDEN`
entries show up in that booking's change history.

**8. Cancel a booking outside the 4-hour window (free)**

Pick a booking on `2026-03-08` or later (fixed "now" is
`2026-03-06 16:30`, well outside 4 hours):

```bash
curl -X POST http://localhost:8081/api/bookings/{id}/cancel
```

Response should show `charged=false` in the change log.

**9. Cancel a booking inside the 4-hour window (charged)**

Pick a booking later on `2026-03-06` itself, close to the fixed "now":

```bash
curl -X POST http://localhost:8081/api/bookings/{id}/cancel
```

Response should show `charged=true`.

**10. Re-run the conflict report to confirm cancellations freed capacity**

```bash
curl "http://localhost:8081/api/bookings/conflicts?date=2026-03-06"
```

The `DAILY_CAP_EXCEEDED` entry for `T1` should reflect the cancellation from
step 8/9 if it lowered the active count, while a `no_show` booking (if any)
would still count — cancelling and not-showing behave differently on purpose.

## Structure
com.example.brightpath
├── config
│   └── SeedDataLoader.java
├── controller
│   └── BookingController.java
├── dto
│   ├── BookingRequest.java
│   ├── BookingUpdateRequest.java
│   └── ConflictReport.java
├── entity
│   ├── Booking.java
│   ├── BookingChange.java
│   └── Tutor.java
├── enums
│   ├── BookingStatus.java
│   ├── BookingType.java
│   ├── ChangeType.java
│   └── ConflictType.java
├── exception
│   ├── ApiException.java
│   └── ApiExceptionHandler.java
├── repository
│   ├── BookingChangeRepository.java
│   ├── BookingRepository.java
│   └── TutorRepository.java
└── service
    └── BookingService.java