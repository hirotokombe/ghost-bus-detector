# Ottawa Ghost-Bus Detector: Project Brief and Mentor Instructions

> **How to use this file:** Upload it to a Claude Project (or paste it at the start of a chat). Then ask questions like "I'm on Milestone 3 and my importer is slow, here's my code." The instructions in Part 1 tell the AI to teach you, not do the work for you.

---

## Part 1: Instructions for the AI mentor (read this first)

You are my mentor for this project. Act like a senior backend engineer who is patient with a new grad. My goal is to **learn by building it myself**, so the rules below apply to every answer.

### Teaching rules

1. **Don't write my project code for me.** No full classes, files, or features. I write the code.
2. **Give me the level of help I ask for.** I want to learn by building things myself, but many of these technologies are new to me. Be practical and specific rather than giving vague hints.
   - **Default — explain the approach:** When I ask how to build something, give me the overall idea, the steps in order, and the relevant Java/Spring/SQL concepts or APIs. Tell me *what to do next* without writing the entire feature.
   - **When I need more guidance:** Show pseudocode, a method skeleton, or a small, focused code snippet with comments. Explain what each part does and what I still need to implement myself.
   - **When I explicitly ask for the full code:** Give me the complete implementation I requested (method, class, file, or feature), with a clear explanation of how it works and why it's structured that way. **This overrides the no-full-code default in Rule 1 for that request.** Don't withhold the solution or make me ask repeatedly.
   - **Match my request:** If I ask for just a hint, give a hint. If I ask for a concrete example, show one. If I ask for the answer, provide it. Don't automatically dump full implementations when I haven't requested them.
3. **Config files are the one exception.** If I ask, you can show a full `docker-compose.yml`, `application.yml`, or Maven dependency block, but explain what each line does.
4. **Review my code like a real code review.** When I paste code, point out bugs, missed edge cases, naming, structure, and missing tests. Tell me *what* is wrong and *why*, and let me fix it.
5. **Explain the "why".** When you suggest a design choice, give the trade-off in a sentence or two, the way I'd need to explain it in an interview.
6. **Keep me on my current milestone.** If I jump ahead or over-engineer, say so. If I didn't say which milestone I'm on, ask.
7. **Check my understanding.** When I finish a milestone, ask me 2–3 interview-style questions about what I built (e.g. "Why did you poll every 30 seconds instead of every 5?").
8. **Be honest about uncertainty.** Library versions and OC Transpo's API change. If you're not sure, say so and point me to the official docs (links in Part 9).
9. **Plain language.** Explain jargon the first time you use it.

---

## Part 2: About me

- Software Engineering student at Carleton (co-op), graduating June 2026, job hunting in Ottawa/Toronto/remote.
- **Comfortable with:** Java, Spring Boot, MVC, JPA, Maven, JUnit, Mockito, JaCoCo, GitHub Actions CI, Azure deployment (from my SYSC 4806 Mini-SurveyMonkey project).
- **New to me:** GTFS / GTFS-Realtime, Protocol Buffers, scheduled background jobs, bulk data loading, PostGIS, Docker Compose, state machines, Leaflet maps.
- I have Node.js installed. I use IntelliJ IDEA.
- Time: about 20 hours/week. Target: about 3 weeks (50–60 hours).
- **After this project** I'll build a RAPTOR transit trip planner (project 3.1) on the same GTFS data, so the GTFS loader code should be clean and reusable.

---

## Part 3: The project in plain words

OC Transpo publishes two kinds of data:

- **The schedule (GTFS static):** a zip of text files listing every route, stop, and trip and when each trip is supposed to run.
- **The live feed (GTFS-Realtime):** updated about every 30 seconds, showing where buses actually are and their predicted arrival times.

A **ghost bus** is a trip that's on the schedule (and often shown in apps) but never actually shows up. Riders in Ottawa complain about these a lot.

**My app will:**

1. Load the schedule and work out which trips should be running today.
2. Poll the live feed every 30 seconds and record which trips are actually being tracked.
3. Compare the two and give every trip a status: tracked, cancelled (announced), missing, ghost (never showed up), or vanished (disappeared mid-route).
4. Show it all on a live map and through a REST API, plus simple stats like "ghost trips per route today."

### Trip statuses (my definitions, tweak as I learn)

| Status | Meaning |
|---|---|
| `SCHEDULED` | On today's schedule, start time hasn't arrived yet |
| `DUE` | Start time passed, still inside the grace period (e.g. 10 min) |
| `TRACKED` | Seen in the live feed (vehicle position or trip update with a vehicle) |
| `CANCELLED` | Live feed explicitly marks the trip as cancelled. This is **announced**, so it's not a ghost |
| `MISSING` | Grace period passed and still not seen. It might still show up |
| `GHOST` | Scheduled end time passed and the trip was never seen |
| `VANISHED` | Was tracked, then disappeared well before the end of the route |
| `COMPLETED` | Was tracked through to (or near) the end of the route |

### Architecture

```
OC Transpo GTFS zip (updated weekly) ──► Static Loader ──┐
                                                         ▼
OC Transpo GTFS-RT (every 30 s) ──► Realtime Poller ──► PostgreSQL + PostGIS
                                                         ▲      │
                                  Trip State Engine ─────┘      │
                                  (runs every 30 s)             ▼
                                                            REST API
                                                                │
                                                     Leaflet map in browser
```

---

## Part 4: Tech stack and why

| Piece | Choice | Why |
|---|---|---|
| Language | Java 25 (current LTS; Java 21 also fine) | I already know Java; most Ottawa backend jobs use it |
| Framework | Spring Boot (latest stable from Spring Initializr) | I know it; built-in scheduling, REST, and DB support |
| Build | Maven (using the `mvnw` wrapper) | Same as my SYSC 4806 project |
| Database | PostgreSQL with PostGIS, running in Docker | Real-world DB; PostGIS adds location queries |
| DB migrations | Flyway | Database schema is versioned in Git, like real teams do |
| Live feed parsing | `gtfs-realtime-bindings` (MobilityData Java library) | Decodes the Protocol Buffers feed into Java objects |
| Bulk loading | `JdbcTemplate` batch inserts or PostgreSQL `COPY` | JPA `saveAll` is too slow for millions of rows |
| Tests | JUnit 5, Mockito, Testcontainers | Testcontainers runs a real PostgreSQL in tests |
| Front end | Plain HTML + JavaScript + Leaflet, served by Spring | No build step needed; keeps focus on the backend |
| Containers | Docker + Docker Compose | One command starts everything |
| CI | GitHub Actions | Already done this before |

---

## Part 5: Which IDE to use

**Stick with IntelliJ IDEA.** For a Java/Spring backend it's the best tool, and you already know it.

- **Get the free JetBrains student license** with your Carleton email. It unlocks IntelliJ IDEA Ultimate, which includes Spring tools, a database browser, a built-in HTTP client, Docker integration, and all of WebStorm's JavaScript/HTML features.
- **You don't need WebStorm.** IntelliJ Ultimate already includes everything WebStorm does.
- **VS Code isn't better for this project.** It's lighter and great for front-end work, but its Java/Spring refactoring, debugging, and navigation are weaker than IntelliJ's. If you can't get Ultimate, use free IntelliJ for the Java code and VS Code just for the small `static/` front-end folder.

**Useful IntelliJ features for this project:**

- **Database tool window:** browse PostgreSQL tables and run SQL without another app.
- **HTTP Client (`.http` files):** test your REST API and the OC Transpo feed right from the editor.
- **Services tool window:** start and stop Docker Compose containers.
- **Debugger:** step through the state machine with real feed data.

---

## Part 6: Setup checklist

### Install

- [ ] **IntelliJ IDEA**, then activate the JetBrains student license (Carleton email).
- [ ] **JDK 25 (Eclipse Temurin)**. Easiest: in IntelliJ go to *File → Project Structure → SDKs → + → Download JDK*.
- [ ] **Git**. Check with `git --version`.
- [ ] **Docker Desktop**. On Windows, turn on WSL 2 when the installer asks. Check with `docker --version` and `docker compose version`.
- [ ] **Node.js**: already installed. Optional for this project; only needed if I later move the front end to Vite.
- [ ] **(Optional) DBeaver Community**: only if I'm not using IntelliJ Ultimate's database tool.
- [ ] **(Optional) Bruno or Postman**: only if I'm not using IntelliJ's HTTP Client.

**Don't install:** PostgreSQL locally (it runs in Docker), Maven globally (use `mvnw`), or `protoc` (the bindings library handles Protocol Buffers).

### Accounts and keys

- [ ] **OC Transpo GTFS-RT API key.** Go to the Developers page on octranspo.com, follow the link to their developer portal, sign up, and subscribe to the GTFS-Realtime product. The key goes in an HTTP header named `Ocp-Apim-Subscription-Key`.
- [ ] **GTFS static schedule zip.** Linked from the same Developers page. It's also mirrored on the Mobility Database (OC Transpo feed `mdb-2154`), which updates about weekly.
- [ ] **GitHub repo** (public, so recruiters can see it).
- [ ] **(Later) Azure for Students** or a small VPS for deployment.

### Rules for the API key

- [ ] **Never commit the key.** Put it in an environment variable (e.g. `OCTRANSPO_API_KEY`) and add `.env` to `.gitignore` *before* the first commit.
- [ ] **Poll politely.** OC Transpo says bus GPS reaches their servers about every 30 seconds, so poll every 30 seconds, not faster. Use one key and one poller.
- [ ] **Read the developer terms.** An older OC Transpo developer page said API keys may not be used for "data harvesting." The current terms don't define limits, but I'm storing history, so I should email `octranspo-dev@ottawa.ca` to confirm that a personal project storing trip status history is OK.
- [ ] **Don't use OC Transpo logos** or imply the app is official (the terms forbid it). Add a line like "Not affiliated with OC Transpo or the City of Ottawa" to the README and the map page.

---

## Part 7: Milestones

Each milestone has a goal, what I'll learn, tasks, and a "done when" test. **Commit at the end of every milestone.**

### Milestone 0: Setup (about 3 h)

**Goal:** Every tool works and the repo exists.

- [ ] Finish the Part 6 checklist.
- [ ] Create the GitHub repo with a README stub, `.gitignore` (Java + IntelliJ + `.env`), and a license.
- [ ] Generate the project at start.spring.io: Maven, Java 25, dependencies **Spring Web, Spring Data JPA, JDBC API, PostgreSQL Driver, Flyway Migration, Validation, Spring Boot Actuator, Docker Compose Support, Testcontainers**.
- [ ] Write a `docker-compose.yml` with a `postgis/postgis` container.

**Done when:** the app starts, connects to PostgreSQL in Docker, and `GET /actuator/health` returns `UP`.

### Milestone 1: Understand the data before coding (about 5 h)

**Goal:** Know what's in the feeds. Don't skip this; it's where most bugs come from.

- [ ] Download the GTFS zip and open `routes.txt`, `stops.txt`, `trips.txt`, `stop_times.txt`, `calendar.txt`, `calendar_dates.txt` in IntelliJ's CSV viewer or a spreadsheet.
- [ ] Read the GTFS Schedule reference for those files (gtfs.org).
- [ ] Pick one real trip and trace it: route → trip → all its stop times → which days it runs.
- [ ] Call the vehicle positions and trip updates feeds with an IntelliJ `.http` file. The response is binary (Protocol Buffers), so it will look like garbage. That's expected.
- [ ] Write a throwaway Java `main` that downloads one feed and prints each entity using `gtfs-realtime-bindings`.
- [ ] Write a `NOTES.md` answering these questions:
  - How do `trip_id`s in the live feed match `trips.txt`?
  - Does OC Transpo use `calendar.txt`, `calendar_dates.txt`, or both?
  - What does a cancelled trip look like in the feed?
  - Do all vehicle positions have a `trip_id`?
  - How many rows are in `stop_times.txt`?

**Done when:** I can explain both feeds to someone else, and my notes answer every question above.

**Watch out for:** GTFS times can go past `24:00:00` (e.g. `25:30:00` means 1:30 a.m. the next morning, but it belongs to the previous day's service). O-Train lines may appear differently from buses; decide whether to include them or start with buses only.

### Milestone 2: Database schema (about 4 h)

**Goal:** Design tables and create them with Flyway.

- [ ] **Design the schema myself first** on paper or in a `.md` file, then ask the mentor to review it.
  - Schedule tables: routes, stops, trips, stop_times, calendar, calendar_dates, plus a `feed_version` table so I know which schedule is loaded.
  - Live tables: vehicle observations (what the feed said and when) and trip status (one row per trip per service day, with state, first seen, last seen).
- [ ] Write it as Flyway migrations (`V1__schedule.sql`, `V2__live.sql`).
- [ ] Add indexes for the queries I'll run most (think about what the state engine looks up every 30 seconds).

**Done when:** the app starts and Flyway creates all tables. I can explain every index.

### Milestone 3: Load the static schedule (about 8 h)

**Goal:** A reusable loader that imports the GTFS zip into PostgreSQL. **I'll reuse this for RAPTOR, so keep it in its own package with no ghost-bus logic in it.**

- [ ] Download the zip (from a URL in config) and read the CSV files without fully unzipping to disk.
- [ ] Parse each file into Java `record`s.
- [ ] Insert in bulk. Try JPA `saveAll` first, time it on `stop_times`, then switch to `JdbcTemplate.batchUpdate` or PostgreSQL `COPY` and compare.
- [ ] Wrap the import in a transaction so a failed load doesn't leave half a schedule.
- [ ] Re-import on a schedule (e.g. weekly), since `trip_id`s can change when OC Transpo publishes a new schedule.

**Done when:** a fresh import finishes in a reasonable time, row counts match the files, and I can write down how long JPA took vs batch/COPY. (That comparison is a good interview story.)

### Milestone 4: Today's planned service (about 6 h)

**Goal:** Given a date, list every trip that should run, with real start and end times.

- [ ] Work out which `service_id`s are active on a date using `calendar.txt` (day-of-week + date range) and `calendar_dates.txt` (exception type 1 = added, 2 = removed).
- [ ] Turn GTFS times like `25:30:00` into real timestamps in `America/Toronto` time.
- [ ] Include yesterday's late-night trips that are still running after midnight.
- [ ] Create a `trip_status` row for each planned trip, starting as `SCHEDULED`.

**Done when:** unit tests pass for a normal weekday, a holiday (`calendar_dates` exception), a trip after midnight, and a daylight-saving-time change date.

### Milestone 5: Realtime poller (about 6 h)

**Goal:** Every 30 seconds, fetch both live feeds and save what was seen.

- [ ] Use `@Scheduled` with a fixed delay (and know the difference between fixed delay and fixed rate).
- [ ] Use Spring's `RestClient` (or Java's `HttpClient`) with timeouts and the API key header.
- [ ] Parse the vehicle positions and trip updates, and save observations.
- [ ] Handle failures: timeout, HTTP 401/429/500, empty feed. Log them and don't crash.
- [ ] **Detect a stale feed:** if the feed's header timestamp hasn't changed in a few minutes, record that the feed is down.

**Done when:** it runs for an hour without crashing, and I can query how many distinct trips were seen.

**Watch out for:** if the *whole* feed is down, every trip will look missing. The state engine must not mark trips as ghosts during a feed outage.

### Milestone 6: Trip state engine (about 8 h)

**Goal:** Update each trip's status from the schedule and the observations. This is the core of the project.

- [ ] Draw the state diagram first (states from Part 3). Show it to the mentor before coding.
- [ ] Implement transitions as one small, pure, testable piece of logic: input = current state, schedule times, latest observation, current time, feed health; output = new state.
- [ ] Make thresholds configurable (grace period, how long before "vanished").
- [ ] Use cancelled trips from the trip updates feed (`CANCELED` schedule relationship) to mark `CANCELLED`, not `GHOST`.
- [ ] Skip ghost decisions while the feed is stale.
- [ ] (Optional, uses PostGIS) For `VANISHED` vs `COMPLETED`, check how close the last position was to the trip's final stop.

**Done when:** unit tests cover every transition, including feed outage and cancellation. I can explain why the logic is separated from the database code.

### Milestone 7: REST API (about 4 h)

**Goal:** Expose the data.

- [ ] `GET /api/trips?date=&route=&status=` lists trips with their status.
- [ ] `GET /api/vehicles/live` returns the latest positions (GeoJSON is easiest for Leaflet).
- [ ] `GET /api/stats/today` returns ghost/cancelled/tracked counts per route.
- [ ] `GET /api/feed/health` shows the last successful poll and whether the feed is stale.
- [ ] Use DTOs (Java records), not JPA entities, in responses. Validate query parameters.

**Done when:** each endpoint works from a `.http` file, and there's at least one controller test per endpoint.

### Milestone 8: Live map (about 5 h)

**Goal:** A page that makes the project easy to demo.

- [ ] One `index.html` in `src/main/resources/static/` with Leaflet and OpenStreetMap tiles. Follow OpenStreetMap's tile usage policy and show their attribution.
- [ ] Show live buses as markers and refresh every 30 seconds with `fetch`.
- [ ] Add a side panel listing today's ghost and missing trips, filterable by route.
- [ ] Add a small table or chart of ghost counts per route.
- [ ] Add the "not affiliated with OC Transpo" note.

**Done when:** I can open the page and explain what's on screen in 30 seconds, like in a demo.

### Milestone 9: Tests and CI (about 4 h, spread across milestones)

- [ ] Unit tests: calendar logic (M4), state machine (M6).
- [ ] Integration test with Testcontainers: load a **tiny fake GTFS zip** I make myself (3 stops, 2 trips) and check the results.
- [ ] Save one real feed response as a test fixture so tests don't need the network or the API key.
- [ ] GitHub Actions: build and test on every push. JaCoCo coverage report.

**Done when:** CI is green and tests never call the real OC Transpo API.

### Milestone 10: Deploy and write it up (about 5 h)

- [ ] Write a `Dockerfile` for the app; run app + DB with Docker Compose.
- [ ] Deploy to something that stays on 24/7 (the poller must keep running). Free tiers that put apps to sleep won't work. Azure for Students or a small VPS are good options.
- [ ] Pass the API key as an environment variable or secret on the host, never in the image.
- [ ] Write the README: what it is, a screenshot or GIF of the map, the architecture diagram, the trip-state diagram, how to run it locally, design decisions and trade-offs, and known limitations.
- [ ] Let it run for a few days, then add real findings to the README (e.g. which routes had the most ghost trips). **Only use numbers I actually measured.**

**Done when:** there's a live link, a clear README, and I can talk through the project for 5 minutes.

---

## Part 8: After it's done

**Stretch goals (only after Milestone 10):**

- Daily summary table and a "worst routes this week" page.
- Compare ghost rates by time of day or day of week.
- Alerts (e.g. email me if one route has many ghosts in an hour).
- Metrics with Micrometer + Prometheus + Grafana.

**Resume bullet template** (fill in real details; don't invent numbers):

> Built a Java/Spring Boot service that polls OC Transpo's GTFS-Realtime feed every 30 s, compares it with the published schedule using a per-trip state machine, and flags ghost and cancelled trips on a live Leaflet map; PostgreSQL/PostGIS, Flyway, Testcontainers, Docker, GitHub Actions, deployed on [host].

Add a second bullet later with a real measured result, such as how much batch loading sped up the import, or what the data showed about ghost trips.

**Bridge to RAPTOR (project 3.1):** the RAPTOR planner reuses the Milestone 3 loader and the Milestone 4 calendar logic. Keep those clean and well tested.

---

## Part 9: Glossary and official resources

### Glossary

- **GTFS (static / Schedule):** a standard zip of CSV text files describing a transit system's routes, stops, and timetable.
- **GTFS-Realtime (GTFS-RT):** a standard live feed in Protocol Buffers format. It has three parts: vehicle positions, trip updates, and service alerts.
- **Protocol Buffers (protobuf):** Google's compact binary data format. You need a library to decode it.
- **Trip:** one run of a vehicle along a route at a specific time (e.g. the 95 leaving Barrhaven at 7:42).
- **Service ID / service day:** which days a trip runs. A service day can run past midnight.
- **Grace period:** how long after the scheduled start I wait before calling a trip missing.
- **Stale feed:** the live feed stopped updating, so missing data doesn't mean missing buses.
- **PostGIS:** a PostgreSQL extension for location data (points, distances, "near" queries).
- **Flyway:** a tool that applies numbered SQL migration files to keep the DB schema in sync with the code.
- **Testcontainers:** a library that starts real Docker containers (like PostgreSQL) for tests.

### Official resources (check these before trusting AI answers)

- GTFS Schedule reference: https://gtfs.org/documentation/schedule/reference/
- GTFS-Realtime reference: https://gtfs.org/documentation/realtime/reference/
- OC Transpo Developers page: https://www.octranspo.com/en/plan-your-trip/travel-tools/developers
- OC Transpo developer terms: https://www.octranspo.com/en/plan-your-trip/travel-tools/developers/dev-terms/
- OC Transpo GTFS on Mobility Database: https://mobilitydatabase.org/feeds/gtfs/mdb-2154
- MobilityData GTFS-RT bindings (Java): https://github.com/MobilityData/gtfs-realtime-bindings
- Spring Initializr: https://start.spring.io
- Spring Boot docs: https://docs.spring.io/spring-boot/
- Flyway docs: https://documentation.red-gate.com/flyway
- Testcontainers (Java): https://java.testcontainers.org
- Leaflet: https://leafletjs.com
- OpenStreetMap tile usage policy: https://operations.osmfoundation.org/policies/tiles/

---

## Part 10: My progress log (I update this as I go)

| Date | Milestone | What I did | What I learned | Stuck on |
|---|---|---|---|---|
| | M0 | | | |
