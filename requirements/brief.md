# SYNERGIE GLOBAL - ENGINEERING ASSESSMENT
**Client:** Bright Path Learning Centre  
**Exercise:** Take-home exercise 01 - Scheduling & conflict detection  
**Time box:** 2.5 hours (Do not exceed)

---

## 1. THE CLIENT & CONTEXT
* **Centre:** Bright Path is a tutoring centre in Da Nang.
* **Scale:** 12 tutors on payroll, ~200 families enrolled.
* **Operation:** Lessons are 1-on-1, lasting 60 or 90 minutes, held in 6 rooms above a bakery.
* **Current workflow:** Run via one shared spreadsheet and WhatsApp groups.
  * Mai (receptionist) opens the sheet at 07:00 AM daily, works out who teaches whom, and messages each tutor their daily schedule.
  * When a cancellation occurs, she edits the sheet, messages the tutor, and tries to remember to notify the next family about open slots.
  * Mai is the only one who understands the sheet, and she goes on leave in 8 weeks.
* **Goal:** Build a minimal internal tool that takes the daily pain away ("the smallest thing that takes the daily pain away").

---

## 2. STAKEHOLDER FEEDBACK (VOICE OF CUSTOMER)

### The Owner:
* "Twice last term we had a student booked into two places at once. The family turned up and nobody knew. That can never happen again – if the system allows it, the system is broken."
* "And I want to open the laptop and see today. Not scroll. See it."

### The Receptionist (Mai):
* "Cancellations are the worst part. Someone messages at night, I fix the sheet in the morning, and by then the tutor has already left home."
* "In exam season I put two students with one tutor in the same room and the same slot, on purpose. It is half price and the families like it. I do that most weeks."

### The Tutor:
* "I get my day in a message, then a correction, then sometimes a third message. I do not always know which one is real."
* "I have driven in for a lesson that was called off the night before. Twice."

---

## 3. HOW THE CENTRE ACTUALLY OPERATES (BUSINESS RULES)

1. **Opening days:**
   * Open: Tuesday to Sunday (mid-morning to mid-evening).
   * Closed: Monday (rooms cleaned).
2. **Tutor load:**
   * Maximum 6 bookings per tutor per single day.
   * (Mai breaks this rule when desperate, but the owner wants it strictly enforced).
3. **Late cancellation:**
   * Free cancellation up to 4 hours before lesson start.
   * Within 4 hours: charged in full, tutor is still paid.
   * Cancelling frees the room and the slot.
   * A "no show" (student doesn't arrive without cancelling) frees neither room nor slot.
4. **The cut-off (Freeze window):**
   * Tomorrow's schedule is finalized at 16:00 today.
   * Changes after cut-off still happen, but MUST be explicitly visible as changes/updates rather than quietly overwriting what the tutor was already told.
5. **Rooms:**
   * 6 rooms total.
   * A room holds 1 lesson at a time. A tutor can only be in 1 room at a time.
   *(Note: Contradicts Mai's practical behaviour of grouping 2 students in 1 room during exam season).*

---

## 4. ASSIGNMENT DELIVERABLES (4 PHASES)

### Phase 1: Read the situation
Create a file named `DECISIONS.md` at root. In it, document:
* Questions you would ask the owner first, and how each answer would change what you build.
* Inconsistencies / contradictions in this brief, and the interpretation/reading you chose.
* Assumptions you had to invent.

### Phase 2: Choose what to build
In `DECISIONS.md`:
* List the features you see this tool needing (1 line each, ~6 features).
* **Pick exactly ONE feature** and argue for it: Why is this one worth more to Bright Path than the others in the available time?
* State clearly what you leave broken/unaddressed by choosing only this one.

### Phase 3: Design and build that one
In `DECISIONS.md`:
* The data model needed for this feature.
* How you represent a booking cancelled or moved after the tutor was already notified.
* Which rules are enforced in the database vs. in code, and why.
* API shape (endpoints), plus **one endpoint you considered and rejected** (and why).
* **Code implementation:**
  * Build only that one feature in your stack of choice.
  * Load the CSV export as seed data.
  * Provide run commands in `README.md` with observations from the seed data.

### Phase 4: Reflect
Close `DECISIONS.md` honestly with:
* What you would build next with another week.
* What you know is weak/flawed in your current solution.
* Where your AI assistant helped.
* **One suggestion from your AI assistant that you threw away and why you were right to do so.**

---

## 5. CONSTRAINTS & SUBMISSION CRITERIA

* **Timebox:** 2.5 hours strictly.
* **Pin dates:** Synthetic seed data covers one specific week. Pin "today" to a date within that week rather than using the system clock.
* **Git hygiene:** Push to a GitHub repository. Atomic commits: one commit per change with clear messages explaining what and why (DO NOT squash).
* **AI disclosure:** Transparent use of AI is required. You will be asked to explain any line of code and modify it live during technical interview.