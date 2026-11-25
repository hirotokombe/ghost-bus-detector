# Ghost-Bus Detector — GTFS Data Notes

## Goal
Understand OC Transpo's published transit schedule (GTFS Static) before comparing scheduled trips with live transit data.

## What is GTFS?
**GTFS** stands for **General Transit Feed Specification**. GTFS Static describes what transit services **should** operate. GTFS-Realtime describes live updates such as vehicle positions, trip updates, and cancellations.

## Important GTFS Files

| File | Purpose |
| --- | --- |
| `agency.txt` | Information about the transit agency. |
| `routes.txt` | Transit routes (for example, Route 11). |
| `stops.txt` | Stops, station platforms, and stations, with their IDs and coordinates. |
| `trips.txt` | Individual scheduled journeys on a route. |
| `stop_times.txt` | Scheduled arrival/departure times and stop order for each trip. |
| `calendar.txt` | Regular operating days and date ranges for each service ID. |
| `calendar_dates.txt` | Exceptions that add or remove service on specific dates. |
| `shapes.txt` | Coordinate points outlining route paths. |
| `feed_info.txt` | Information about the published GTFS dataset. |

## How the IDs Connect

- **`route_id` — Which route?** A route describes the overall transit service.
- **`trip_id` — Which journey?** A trip is one scheduled run on that route, not necessarily one unique physical vehicle.
- **`service_id` — Which days?** A service ID links a trip to a calendar of operating dates.
- **`stop_id` — Which stop?** A stop ID links each row in `stop_times.txt` to a location in `stops.txt`.

**Relationships:** `routes.txt` → `trips.txt` → `stop_times.txt` → `stops.txt`. A trip also links to `calendar.txt` and `calendar_dates.txt` through `service_id`, and to `shapes.txt` through `shape_id`.

**`route_id` is not the number on the bus.** The number riders see is `route_short_name`. For most bus routes the two happen to match (route 11 has `route_id = 11`), but not for the O-Train: Line 1 has `route_id = 1-373` and `route_short_name = 1`. Code must join on `route_id` and display `route_short_name`.

## Real Trip Traced: Route 11

The following details came from the OC Transpo GTFS files we inspected:

| Detail | Value |
| --- | --- |
| `route_id` | `11` |
| `route_short_name` | `11` |
| `route_long_name` | Waller <> Bayshore |
| Trip ID | `10000010` |
| Service ID | `1` |
| `direction_id` | `1` |
| `block_id` | `11-07` |
| Destination (`trip_headsign`) | Lincoln Fields |
| First stop | WALLER / LAURIER E (`stop_id`: `10047`) |
| Scheduled departure | `22:48:00` (10:48 PM) |
| Last stop | LINCOLN FIELDS 4B (`stop_id`: `10160`) |
| Scheduled arrival | `23:35:00` (11:35 PM) |
| Last `stop_sequence` | `55` |
| Scheduled trip duration | 47 minutes |

### When Does This Trip Run?

`calendar.txt` shows that `service_id = 1` runs **on Sundays only**, during **November 20–December 19, 2026**. `calendar_dates.txt` contains **no exceptions** for service ID `1`.

That makes the scheduled Sundays within the date range **November 22, November 29, December 6, and December 13, 2026**.

### Calendar Exceptions

- `exception_type = 1`: **Add** service on that date, even if the normal calendar does not include it.
- `exception_type = 2`: **Remove** service on that date, even if the normal calendar includes it.

The application needs both calendar files; otherwise, it might wrongly expect a trip that was removed from the schedule and flag it as a ghost bus. A calendar exception alone doesn't tell us whether an individual bus actually appeared.

**Real example from this feed:**

```csv
service_id,date,exception_type
5,20261122,2
```

- `service_id = 5` normally includes Sundays.
- November 22, 2026 is a Sunday, but `exception_type = 2` removes that service on that specific date.
- Therefore, trips assigned to `service_id = 5` **should not be expected** on November 22, and they **should not be classified as ghosts** for failing to appear.
- In fact, service `5` is removed on **every** Sunday in its range, so it never runs in this schedule (see Calendar under Dataset Observations).

### Overnight Trips and GTFS Service Days

GTFS can represent departures after midnight with hour values **greater than 23**:

| GTFS time | Real-world clock time |
| --- | --- |
| `23:30:00` | 11:30 PM, same day |
| `24:15:00` | 12:15 AM, following day |
| `25:30:00` | 1:30 AM, following day |

**Important distinction:** A trip scheduled for **Sunday** at `25:30:00` takes place at **1:30 AM Monday**, but it still belongs to **Sunday's service day**. When deciding whether this trip should run, evaluate the **Sunday** calendar and its exceptions, not just the Monday calendar.

This matters because a schedule-processing system that only checks today's calendar could miss valid overnight trips or mark them incorrectly.

### Daylight Saving Time

The GTFS spec measures times from **"noon minus 12 hours"** on the service day, not from midnight. On normal days these are the same. On the two days a year when clocks change, they differ by one hour:

- To turn a GTFS time into a real timestamp, start at **noon on the service day in `America/Toronto` time**, subtract 12 hours, then add the GTFS seconds.
- Starting from midnight instead would put every trip on a clock-change day one hour off.

The database stores GTFS times as **seconds since the start of the service day** (`25:30:00` → `91800`), because PostgreSQL's `TIME` type can't hold values past `24:00:00`.

### Shapes: How Scheduled Paths Are Drawn

A trip's `shape_id` links it to a sequence of geographical points in `shapes.txt`.

For our Route 11 trip `10000010`:

- `shape_id`: `shp-11-54`
- First shape record: `shp-11-54,45.424434,-75.687027,1,0`
- The first coordinate matches the coordinates of the first stop, **WALLER / LAURIER E**.

| Field | Meaning |
| --- | --- |
| `shape_id` | Identifies a scheduled path |
| `shape_pt_lat`, `shape_pt_lon` | Latitude and longitude of a point |
| `shape_pt_sequence` | Order in which to connect points when drawing the path |
| `shape_dist_traveled` | Distance along the path, when provided |

**`stop_sequence` vs. `shape_pt_sequence`:** `stop_sequence` orders the **scheduled stops**, while `shape_pt_sequence` orders **map points between and around stops**. A trip could have 30 stops but 500 shape points because roads curve and turn. Shapes describe the **planned path**, not actual GPS tracking.

Knowing the last observed vehicle position relative to the destination might later help distinguish a trip that likely **completed** from one that **vanished early**. But location and timing alone are not proof that the vehicle reached its destination.

## Dataset Observations

### Row counts (this snapshot)

| File | Data rows (excluding header) |
| --- | --- |
| `agency.txt` | 1 |
| `routes.txt` | 178 |
| `stops.txt` | 5,842 |
| `trips.txt` | 73,559 |
| `calendar.txt` | 18 |
| `calendar_dates.txt` | 52 |
| `stop_times.txt` | 2,901,867 |
| `shapes.txt` | 874,720 |

These are the expected row counts for the database tables after an import. `stop_times.txt` is too large for one Excel worksheet.

### File format

- Files are UTF-8. French accents (e.g. `DU PORTAGE / DE L' HÔTEL-DE-VILLE`) read correctly only when UTF-8 is specified explicitly.
- `agency.txt`, `routes.txt`, and `stops.txt` have **no byte-order mark (BOM)**. The reader still strips one if present, in case a future file has it.
- Lines end with Windows-style `\r\n`.
- Empty values appear as nothing between commas (`,,`) and are stored as `NULL`.

### Agency

- One agency: `agency_id = 1`, OC Transpo.
- `agency_timezone` is `Canada/Eastern`, an older alias for `America/Toronto`. Java accepts both.
- `agency_id` is filled in on every route, so no special handling is needed for a missing agency ID.

### Routes

| Kind | `route_type` | Example `route_id` | `route_short_name` |
| --- | --- | --- | --- |
| O-Train (light rail) | `0` | `1-373`, `2-354`, `4-354` | `1`, `2`, `4` |
| Bus | `3` | `11`, `105`, `E1`, `R1` | `11`, `105`, `E1`, `R1` |

- There are **three** rail lines (1, 2 and 4). Their `route_id`s have a suffix; bus `route_id`s don't.
- `route_short_name` is not always a number: `E1` (Shuttle-Express Downtown <> Blair) and `R1` (Tunney's Pasture <> Blair) are bus routes with letters. Store it as text.
- `route_sort_order` is empty for the three O-Train lines and filled in for every bus route.
- `route_desc`, `route_url`, and `network_id` are empty on every route.
- `continuous_pickup` and `continuous_drop_off` are `1` on every route.

### Stops

GTFS marks the kind of location with `location_type` (0 = stop/platform, 1 = station, 2 = entrance/exit, …) and links a platform to its station with `parent_station`. OC Transpo uses only **two levels**:

| Kind | `location_type` | `parent_station` | Example `stop_id` | Example name |
| --- | --- | --- | --- | --- |
| Station | `1` | empty | `3009_stn` | RIDEAU |
| Bus platform at a station | `0` | `3009_stn` | `9833` | RIDEAU A |
| O-Train platform at a station | `0` | `3009_stn` | `CD990` | RIDEAU O-TRAIN WEST / OUEST |
| Drop-off-only stop at a station | `0` | `3012_stn` | `10377` | WESTBORO OFF ONLY |
| Ordinary street stop | `0` (or empty) | empty | `10047` | WALLER / LAURIER E |

- **61 stations** (`location_type = 1`), all with IDs ending in `_stn`. They include bus-only Transitway stations (Teron, Eagleson, Moodie, …), not just O-Train stations.
- **245 child stops** point to a station through `parent_station`. All of them are `location_type = 0`. No entrances, generic nodes or boarding areas (types 2–4) appear in the file.
- **`stop_code` is shared** by a station and all its platforms (e.g. `3009_stn`, `9833` and `CD990` all have `stop_code = 3009`). It's the number riders see; `stop_id` is the unique key.
- **O-Train platform IDs** are two letters plus digits (`CD990`, `NA998`, `RF995`). Their names contain `O-TRAIN`, often with a direction (`WEST / OUEST`, `NORTH / NORD`).
- `platform_code` holds the platform letter or number (`A`, `2A`, `1`) when there is one, and is empty for drop-off-only stops and some O-Train platforms.
- **Children appear before their parent station in the file** (e.g. `9833`–`CD995` are listed around `3009_stn`, some before it). This is why the `parent_station` foreign key is `DEFERRABLE INITIALLY DEFERRED`: a row-by-row check would fail on the first platform.
- `location_type` is sometimes written as `0` and sometimes left **empty** on ordinary stops. Both mean 0; the loader turns empty into 0, because the database column is `NOT NULL`.
- `tts_stop_name`, `stop_desc`, `zone_id`, `stop_url`, `stop_timezone`, `wheelchair_boarding` and `level_id` are empty on every station and platform checked.
- All stops have coordinates in the Ottawa area (around `45.x, -75.x`).

**Why it matters for ghost detection:** a trip's `stop_times` (and the live feed) refer to a specific **platform** `stop_id`, not the station. Comparing a bus's position with "the final stop" means using the platform's coordinates, and grouping by station means following `parent_station`.

### Trips

- **`block_id`** groups the trips that one vehicle runs one after another (e.g. `11-07` for trip `10000010`). If an earlier trip in a block runs very late, the next trip in that block may start late too. This could help explain some "missing" trips later.
- `direction_id` is filled in (`0` or `1`), so trips on a route can be split by direction.
- `trip_short_name` is empty on the trip checked.

### Calendar

- OC Transpo uses **both** `calendar.txt` and `calendar_dates.txt`.

**`calendar.txt` (18 services):**

| `service_id` | Days | Date range |
| --- | --- | --- |
| `1`, `5` | Sunday only | Nov 20 – Dec 19 |
| `7`, `11` | Monday only | Nov 20 – Dec 19 |
| `10` | Tuesday only | Nov 20 – Dec 19 |
| `4` | Wednesday only | Nov 20 – Dec 19 |
| `2`, `9` | Thursday only | Nov 20 – Dec 19 |
| `3` | Friday only | Nov 20 – Dec 19 |
| `6`, `8` | Saturday only | Nov 20 – Dec 19 |
| `SEPT26-TCFDSun-Sunday-01`, `SEPT26-TRILSUN-Sunday-02` | Sunday | Nov 22 – Dec 13 |
| `SEPT26-TCFDMth-Weekday-01` | Mon–Fri | Nov 23 – Dec 17 |
| `SEPT26-TRILLTP-Weekday-01` | Mon–Fri | Nov 23 – Dec 18 |
| `SEPT26-TCFDFri-Weekday-02` | Mon–Fri | Nov 27 – Dec 18 |
| `SEPT26-TCFDSat-Saturday-01`, `SEPT26-TRILSAT-Saturday-02` | Saturday | Nov 28 – Dec 19 |

- Each **numbered** service runs on exactly one day of the week. There is no single "weekday" bus service.
- The **named** services likely belong to the O-Train (`TCFD`/`CFD` = Confederation Line 1?, `TRIL` = Trillium Line 2?). **To confirm** by checking which `route_id`s in `trips.txt` use them.

**`calendar_dates.txt` (52 exceptions) changes the picture a lot:**

- **Four `service_id`s exist only in `calendar_dates.txt`**, with no row in `calendar.txt`: `SEPT26-CFDFri-Weekday-03`, `SEPT26-CFDSA-Saturday-03`, `SEPT26-CFDSU-Sunday-03`, `SEPT26-CFDMTh-Weekday-03`. They run only on the dates where they are ADDED. This is why the schema has a separate `services` table built from both files.
- **Some services never run at all once exceptions are applied:**
  - `5` (Sunday) is REMOVED on every Sunday in its range (Nov 22, 29, Dec 6, 13).
  - `2` (Thursday) is REMOVED on every Thursday in its range (Nov 26, Dec 3, 10, 17).
  - Reading `calendar.txt` alone would expect all their trips and flag every one as a ghost.
- **Some services run on one day only:** `8` (Saturday) runs only Nov 21 (where it replaces `6`); `11` (Monday) runs only Nov 23 (where it replaces `7`).
- **Confirmed: names hide day patterns that `calendar.txt` doesn't show.** `SEPT26-TCFDMth-Weekday-01` says Mon–Fri, but every Friday is REMOVED, so it's really Mon–Thu. `SEPT26-TCFDFri-Weekday-02` says Mon–Fri, but Mon–Thu are REMOVED, so it's really Friday only.
- **Replacement schedules:** for Nov 30 – Dec 5, the regular `TCFD` services are REMOVED and the `CFD...-03` services are ADDED instead (reason unknown, possibly planned work on Line 1).
- **Some ADDED rows are redundant:** e.g. `8` is ADDED on Nov 21, a Saturday it already runs on. Harmless, but the planner must not count such a trip twice.

**Rule for the daily planner:** a service runs on date D if (it's in `calendar.txt`, D is in its date range, its weekday flag is true, **and** D isn't REMOVED) **or** D is ADDED for it in `calendar_dates.txt`.

### Stop times

- One `trip_id` appears in multiple `stop_times.txt` rows—one for each scheduled stop on its journey.
- Trip `10000010` has 55 rows, `stop_sequence` 1–55, departing `10047` at 82080 s (22:48:00) and arriving at `10160` at 84900 s (23:35:00), matching the trace.
- Every one of its 55 stops has a scheduled time, and `arrival_time` equals `departure_time` at each stop. (Other trips may still leave times empty at non-timepoint stops.)
- **Times are filled in at non-timepoint stops too.** The first stop of trip `10000010` has `timepoint = 1` and the next stops `timepoint = 0`, yet all have times. Only timepoints are times the bus is held to; the rest are OC Transpo's estimates.
- **`shape_dist_traveled` is filled in** and looks like metres along the shape (`0.0`, `516.49`, `1153.94` for the first three stops). It could help later to measure how far along its route a vehicle was when it disappeared.
- The first stop has `drop_off_type = 1` (no drop-off), as expected at the start of a trip.
- `stop_times` uses **platform-level `stop_id`s**: the trip stops at `9449` (MACKENZIE KING A), `9868` (PARLIAMENT B), `9945` (TUNNEY'S PASTURE D) and ends at `10160` (LINCOLN FIELDS 4B), all children of `_stn` stations.

### Shapes

- 874,720 points in total.
- The first point of `shp-11-54` (`45.424434, -75.687027`) is exactly the location of the trip's first stop, `10047`.
- **`shape_dist_traveled` looks shifted by one point.** Points 1→2 and 2→3 are each about 20 m apart, but the values are `0.0`, `0.01`, `20.1` instead of roughly `0`, `20`, `40`. Don't rely on it for precise distances without checking more shapes; computing distance from the coordinates is safer.

### Performance baseline

| Step | Rows | Time | Notes |
| --- | --- | --- | --- |
| Read `stop_times.txt`, check `trip_id` only (no database) | 2,901,867 | ~1.1 s | Streaming CSV reader; other columns parsed for one trip only |
| Read and parse every row into `GtfsStopTime` (no database) | 2,901,867 | ~2.2 s | All 10 columns parsed per row (two times, ints, a double) |
| Read and parse every row into `GtfsShape` (no database) | 874,720 | ~0.5 s | 5 columns per row |

The second row is the real floor for any import: whatever the database step adds comes on top of about 2.2 s. Parsing every column roughly doubled the time compared with reading alone.

## Open Decisions

- **Buses only, or buses and O-Train?** Rail lines are `route_type = 0` and buses `3`, so either choice is easy to filter. Undecided until the realtime feed shows whether O-Train vehicles appear in it.

## Realtime Concepts Learned (Without Live API Access)

### Feed Health and Stale Data

If **all buses disappear** from the feed, it may indicate a **feed outage**, not that every bus suddenly vanished. Our app should monitor whether the *data itself* is still updating.

| Value | What it tells us |
| --- | --- |
| `lastSuccessfulPoll` | Last time our app successfully fetched a response |
| `lastFeedUpdate` | Last time the upstream feed says its content was updated |
| `feedStatus` | A derived status, such as healthy, stale, or unavailable |

Example: The app receives a successful HTTP response every 30 seconds, but the feed timestamp stays at `10:00:30` until `10:05:00`. The **requests are succeeding**, but the **data is stale**.

**Design idea:** Display a feed-health warning and **pause new `GHOST`/`VANISHED` decisions** when the feed is stale or unavailable; otherwise, missing realtime observations could create false positives. The exact timeout threshold will be decided and tested later.

### Interpreting Last-Seen GPS Positions

Hypothetical example for a trip scheduled to end at **11:35 PM**:

- Last seen **near the final stop at 11:34 PM** → possibly `COMPLETED`.
- Last seen **far away at 11:10 PM**, with no later updates → possibly `VANISHED`.

These are **potential classifications**, not guarantees. A vehicle can disappear due to tracking/network problems, and we need feed health and sensible thresholds before assigning a status.

## Realtime Questions — Pending API Access

- [ ] Do GTFS-Realtime `trip_id` values match the IDs in `trips.txt` directly?
- [ ] How is a cancelled trip represented in OC Transpo's trip-updates feed?
- [ ] Do all vehicle-position records include a `trip_id`?
- [ ] Does the feed always include `start_date` (needed to match overnight trips to the right service day)?
- [ ] Do O-Train vehicles appear in the vehicle-positions feed?
- [ ] What does an actual vehicle-positions or trip-updates response look like when decoded?

**Blocked for now:** OC Transpo developer registration returns a server error. An email has been sent to `octranspo-dev@ottawa.ca`. Don't guess at the realtime feed's behavior before testing it.

## Next Steps

- [x] Identify the roles of the main GTFS Static files.
- [x] Trace one real trip from route to stop times and operating days.
- [x] Check the calendar for exceptions, including a real service-removal example (`service_id = 5`).
- [x] Understand GTFS overnight times and service days.
- [x] Trace `shape_id = shp-11-54` and distinguish shape points from stops.
- [x] Understand the difference between successful polling and fresh data.
- [x] Design the database schema (see `docs/DATABASE.md`).
- [x] Record row counts and data quirks for `agency.txt`, `routes.txt`, and `stops.txt`.
- [x] Confirm the station / platform hierarchy in `stops.txt` (`location_type`, `parent_station`).
- [x] Record row count and quirks for `trips.txt`.
- [x] Record patterns and quirks for `calendar.txt` and `calendar_dates.txt`.
- [ ] Confirm which routes use the named (`TCFD`, `CFD`, `TRIL`) services.
- [x] Record row count, quirks and a parse-time baseline for `stop_times.txt`.
- [x] Record row count, quirks and a parse-time baseline for `shapes.txt`.
- [ ] Read the GTFS-Realtime reference and test the live feed once API access is available.

## Reference

GTFS Schedule specification: https://gtfs.org/documentation/schedule/reference/

GTFS-Realtime reference: https://gtfs.org/documentation/realtime/reference/