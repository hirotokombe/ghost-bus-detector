# Ghost-Bus Detector — Milestone 1 Notes

## Goal
Understand OC Transpo's published transit schedule (GTFS Static) before comparing scheduled trips with live transit data.

## What is GTFS?
**GTFS** stands for **General Transit Feed Specification**. GTFS Static describes what transit services **should** operate. GTFS-Realtime describes live updates such as vehicle positions, trip updates, and cancellations.

## Important GTFS Files

| File | Purpose |
| --- | --- |
| `agency.txt` | Information about the transit agency. |
| `routes.txt` | Transit routes (for example, Route 11). |
| `stops.txt` | Stops, their names, IDs, and coordinates. |
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

## Real Trip Traced: Route 11

The following details came from the OC Transpo GTFS files we inspected:

| Detail | Value |
| --- | --- |
| Route | 11 |
| Trip ID | `10000010` |
| Service ID | `1` |
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

### Overnight Trips and GTFS Service Days

GTFS can represent departures after midnight with hour values **greater than 23**:

| GTFS time | Real-world clock time |
| --- | --- |
| `23:30:00` | 11:30 PM, same day |
| `24:15:00` | 12:15 AM, following day |
| `25:30:00` | 1:30 AM, following day |

**Important distinction:** A trip scheduled for **Sunday** at `25:30:00` takes place at **1:30 AM Monday**, but it still belongs to **Sunday's service day**. When deciding whether this trip should run, evaluate the **Sunday** calendar and its exceptions, not just the Monday calendar.

This matters because a schedule-processing system that only checks today's calendar could miss valid overnight trips or mark them incorrectly.

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

- OC Transpo uses **both** `calendar.txt` and `calendar_dates.txt`.
- This downloaded snapshot has **18 service rows** in `calendar.txt` and **52 exception rows** in `calendar_dates.txt`.
- `stop_times.txt` has **2,901,867 data rows**, excluding the header. This is too many rows for one Excel worksheet.
- One `trip_id` appears in multiple `stop_times.txt` rows—one for each scheduled stop on its journey.

## Realtime Concepts Learned (Without Live API Access)

### Feed Health and Stale Data

If **all buses disappear** from the feed, it may indicate a **feed outage**, not that every bus suddenly vanished. Our app should monitor whether the *data itself* is still updating.

| Value | What it tells us |
| --- | --- |
| `lastSuccessfulPoll` | Last time our app successfully fetched a response |
| `lastFeedUpdate` | Last time the upstream feed says its content was updated |
| `feedStatus` | A derived status, such as healthy, stale, or unavailable |

Example: The app receives a successful HTTP response every 30 seconds, but the feed timestamp stays at `10:00:30` until `10:05:00`. The **requests are succeeding**, but the **data is stale**.

**Design idea for later milestones:** Display a feed-health warning and **pause new `GHOST`/`VANISHED` decisions** when the feed is stale or unavailable; otherwise, missing realtime observations could create false positives. The exact timeout threshold will be decided and tested later.

### Interpreting Last-Seen GPS Positions

Hypothetical example for a trip scheduled to end at **11:35 PM**:

- Last seen **near the final stop at 11:34 PM** → possibly `COMPLETED`.
- Last seen **far away at 11:10 PM**, with no later updates → possibly `VANISHED`.

These are **potential classifications**, not guarantees. A vehicle can disappear due to tracking/network problems, and we need feed health and sensible thresholds before assigning a status.

## Realtime Questions — Pending API Access

- [ ] Do GTFS-Realtime `trip_id` values match the IDs in `trips.txt` directly?
- [ ] How is a cancelled trip represented in OC Transpo's trip-updates feed?
- [ ] Do all vehicle-position records include a `trip_id`?
- [ ] What does an actual vehicle-positions or trip-updates response look like when decoded?

**Blocked for now:** OC Transpo developer registration is returning a server error. Don't guess at the realtime feed's behavior before testing it.

## Next Steps

- [x] Identify the roles of the main GTFS Static files.
- [x] Trace one real trip from route to stop times and operating days.
- [x] Check the calendar for exceptions, including a real service-removal example (`service_id = 5`).
- [x] Understand GTFS overnight times and service days.
- [x] Trace `shape_id = shp-11-54` and distinguish shape points from stops.
- [x] Understand the difference between successful polling and fresh data.
- [ ] Read the GTFS-Realtime reference and test the live feed once API access is available.
- [ ] Continue to Milestone 2 (design the database schema) when ready.

## Reference

GTFS Schedule specification: https://gtfs.org/documentation/schedule/reference/

GTFS-Realtime reference: https://gtfs.org/documentation/realtime/reference/
