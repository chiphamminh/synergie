## 1. Questions for the owner (and how the answers would change the design)

**Q1: Is a paired booking (2 students, 1 tutor, 1 slot during exam season, half price) an official booking type the system should support, or is it just something Mai does manually outside the system?**
- If yes → I need a field such as `booking_type: single | paired`. The system should treat this as a valid exception, not as a normal conflict.
- If no → I keep the rule as strict: 1 tutor = 1 seat at a time. Paired bookings are outside the tool’s scope, but then historical data such as L009/L010 will always look like a conflict to the system. In that case, we need a clear override flow.

**Q2: When the owner says the maximum is 6 bookings per tutor per day, does that mean a hard block, or just a warning that can still be overridden?**
- If hard block → validation happens when creating a booking, and the system rejects it.
- If warning + override → the system must record the override: who did it, when, and why. This matches the real operational need and helps the owner review exceptions later.

**Q3: How is “family has been notified” confirmed for the 16:00 cut-off rule? Is there a clear step in the process, or is it all remembered manually by Mai?**
- If there is a formal step → I can add a field such as `notified_at` and use it as the reference point.
- If there is no formal step, as the current process suggests → I need to make an assumption: any booking created before the cut-off time for that day is treated as “already notified,” and anything after cut-off is “not yet notified.” This assumption is noted below.

**Q4: After Mai leaves in 8 weeks, how many people will operate the tool at the same time?**
- If only one person → no major concurrency issue is needed.
- If multiple people → the system needs a clear source of truth and conflict handling when two people edit the same booking at the same time.

---

## 2. Where the brief contradicts itself

**Contradiction 1 — Double-booking: “This can never happen” vs “It happens every week”**
The owner says clearly: if the system allows a student or tutor to be double-booked, the system is broken.
But the receptionist describes a regular weekly practice during exam season: 2 students, 1 tutor, 1 slot, half price.
This is not a random bug. It is a deliberate business exception.
The problem is that, in the raw data, it looks exactly like the type of conflict the owner says must never happen.
My reading: these are two different kinds of conflicts, even if they look similar in the data. The system must distinguish them using a separate flag or field.

**Contradiction 2 — The 6-booking rule: enforced on paper, broken in practice**
The rule says a tutor can have at most 6 bookings per day.
But the brief also says: “Mai breaks this rule when she is desperate, and the owner wants it enforced.”
This means the rule is strict in theory, but not always followed in real operations.
My reading: this is a sign that the system should use soft enforcement: warning + override + audit log, rather than only a hard block. Otherwise, the process becomes unusable in real emergencies.

---

## 3. Assumptions I had to make

- **“Today” for testing the 16:00 cut-off and the 4-hour late cancellation rule is 2026-03-06.**
  I chose this date because it has many bookings and enough variation to test load, cancellation, and scheduling rules.
- **“Notified” is inferred from the booking time relative to the cut-off time, because the data does not contain a notification field.**
  In other words, bookings created before the cut-off are treated as “already notified.”
- **I distinguish “bad double-booking” from “intentional paired booking” by looking at the `note` field**, especially values like “exam pair - half price.”
  There is no dedicated field for this in the data, so I treat the note as the best available signal.
- **Room and tutor are treated as independent constraints**:
  - 1 room can hold 1 lesson at a time
  - 1 tutor can teach 1 lesson at a time
  This is because the data shows the same tutor teaching in different rooms at different times, and there is no fixed room-tutor mapping.

## 4. Features I could build

- **Conflict detection** — check tutor, room, and time before a booking is saved or changed.
- **Daily view API** — a quick summary of today's bookings for the owner.
- **Cancellation and no-show flow** — apply the 4-hour rule and charge correctly.
- **Change log for cut-off** — show what changed after 16:00 instead of silently overwriting the old schedule.
- **Tutor load warning** — warn (not block) when a tutor passes 6 bookings a day, and log any override.
- **Tutor notification** — send one clear message when a booking changes, instead of many separate messages.

## 5. The feature I choose: Conflict detection

**Why this one:**
This is the most painful problem in the brief. The owner's main story is a student who was double-booked and nobody knew until the family showed up. This is also the task title: "Scheduling & conflict detection." It is the feature that protects every other part of the system — if bookings can still conflict, nothing else (notifications, daily view, cancellation flow) can be trusted. It also fits my backend skills best: strong data model and API design, less about UI or messaging.

**What I leave broken by choosing this:**
- No tutor notification. Tutors still get told by message outside the system.
- No daily dashboard view for the owner. The owner still checks the raw data.
- No cancellation fee logic. Charging is still manual.
- No change log for edits after cut-off. I may only add a minimal version if time allows, not a full history view.

I accept these gaps because one feature that works well is better than many half-built ones, and conflict detection is the feature every other feature depends on.

## 6. Data model

**Tutor**
- tutor_id, name, subject, phone (from `tutors.csv`)

**Booking**
- lesson_id, date, start_time, duration_min, student_name, tutor_id (FK), room_id, status (booked / cancelled / no_show), booking_type (single / paired), cancelled_at, note, created_at, updated_at

**BookingChange** (new table, not in the source data)
- id, booking_id (FK), changed_at, change_type (created / rescheduled / cancelled / overridden), old_value, new_value, after_cutoff (boolean)

I add `BookingChange` because of the cut-off rule: a change after 16:00 must show up as a change, not silently overwrite the old booking. Without this table, an update would just replace the row and lose the fact that a change happened.

**How I represent a booking cancelled or moved after the tutor was told:**
The `Booking` row is updated as normal (new date/time, or status = cancelled), but every update also creates one `BookingChange` row. If the update happens after that day's 16:00 cut-off, `after_cutoff` is set true. This way, the current state is always in `Booking`, and the full history of what changed and when is in `BookingChange` — nothing is silently lost.

## 7. Rules enforced in DB vs in code

**In DB (constraints):**
- `lesson_id` unique
- `tutor_id` foreign key must exist
- `status` and `booking_type` limited to fixed values (enum/check constraint)
- required fields (date, start_time, duration_min, tutor_id, room_id) cannot be null

**In code (service layer):**
- Tutor/room time-overlap check — MySQL cannot enforce "no overlapping time range" as a constraint, so this must run as a query + check in the service before saving.
- Closed on Monday — a simple day-of-week check.
- Max 6 bookings/day per tutor — soft rule: warn by default, only save if `override=true` is passed, and every override is written to `BookingChange`.
- Paired booking exception — if `booking_type = paired`, two bookings with the same tutor/room/time are allowed instead of rejected.
- Cut-off tracking — explained above, always in code, since it depends on the current time vs the change time.

I keep overlap and business rules in code because MySQL cannot express "no time overlap" as a constraint, and because several rules (6/day, paired) need to be soft rules with logging, not hard blocks.

## 8. API shape

- `POST /api/bookings` — create a booking. Runs all checks above. Returns 201, or 409 with the conflicting booking id.
- `PUT /api/bookings/{id}` — reschedule or edit a booking. Creates a `BookingChange` row. Runs the same checks again for the new time.
- `POST /api/bookings/{id}/cancel` — cancel a booking. Applies the 4-hour rule to decide if it is free or charged.
- `GET /api/bookings?date=YYYY-MM-DD` — list bookings for one day. Used for testing, not a UI.
- `GET /api/bookings/{id}/changes` — show the change history of one booking.

**Endpoint I rejected: `DELETE /api/bookings/{id}` (hard delete).**
I will not add a real delete endpoint. The whole point of `BookingChange` is to keep a record of what happened to a booking. A hard delete would remove that history and break the cut-off rule, which says changes must be visible, not erased. Cancelling is done through `POST /api/bookings/{id}/cancel`, which keeps the row and marks it cancelled instead.