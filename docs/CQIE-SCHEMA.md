# CQIE Timetable Schema

Observed on 2026-08-31 through an authenticated same-origin WebView request to the frozen endpoint.
The raw response remained in WebView memory. Only key/type projections and allowlisted scheduling
fields were inspected; no account, token, Cookie, course, teacher, room, or identifier value was
written to disk or Git.

## Envelope

- `code`: nullable
- `data`: array; 31 rows in the observed response
- `msg`: nullable
- `status`: string

## Timetable Row

Rows expose course metadata, `classTimetableInstrVOList`, room metadata, and these scheduling fields:

- `teachingWeek`: a week bitmap where position 1 is week 1 (`1010101` means weeks 1, 3, 5, 7)
- `teachingWeekFormat`: display form such as `1-8`, `10-15`, or `1,3,5,7`
- `period`: a period bitmap where position 1 is period 1 (`0011` means periods 3-4)
- `periodFormat`: display form such as `1-2`, `3-4`, or `7-8`
- `serialPeriod`: scheduled period count when present
- `weekDay`: string weekday number, Monday = `1`
- `weekDayFormat`: Chinese weekday label
- `wholeWeekOccupy`: whole-week marker
- `notArrangeTimeAndRoom`: explicit no-time/no-room marker
- `notArrangeRoom`: explicit no-room marker

The observed response contained continuous ranges, odd/even discrete sets, and three rows with
`notArrangeTimeAndRoom=true`. It contained no `wholeWeekOccupy=true` row, so the sanitized fixture
adds one synthetic whole-week boundary row. All fixture names and identifiers are invented.

## Authentication

The public frontend bundle reads the JSON-encoded access token from `cqu_edu_ACCESS_TOKEN`, decodes
it in memory, and sends `Authorization: Bearer <value>` to the same-origin API. Native code must never
receive, log, persist, or expose that value.
