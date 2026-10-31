# FEMA relief simulation — decision logic and caveats

OpenFEMA source:
`https://www.fema.gov/api/open/v2/DisasterDeclarationsSummaries`

When `FEMA_ENABLED=true`, a scheduler attempts to fetch recent disaster declarations daily at 6 AM New York time, store unique disaster/county rows, and run eligibility checks. By default disabled, since internet isn't required for local startup.

For a manual admin-triggered run: `POST /api/admin/fema/sync`, then `POST /api/admin/fema/evaluate`. `GET /api/admin/fema/disasters` lists cached county disaster data.

**Location matching:** FEMA declarations frequently specify county designations. A ZIP code can span counties. The customer profile stores a ZIP code, but this application only considers a match after an administrator independently verifies and assigns a county FIPS by `POST /api/admin/fema/verify-county/{userId}` with JSON `{"countyFips":"36061"}`. This is manual, not an automatic Census geocoder. For a production integration, geocode independently verified addresses and investigate ambiguous matches, county boundaries, amendments, assistance types and FEMA data quality.

**Demo rule:** for each active loan with a future due date, if a verified customer county matches a FEMA disaster declaration within the last 90 days and this loan has no prior deferral, move the next due date 30 calendar days, store the original and revised dates, prevent duplicates, notify the customer and create an audit record. No real loan servicing or settlement systems are modified.

**Compliance caution:** Federal disaster declarations do not automatically create a legal right to a 30-day payment deferral. In a real bank, legal/compliance and product servicing teams must approve every eligibility and contract-adjustment policy, communicate interest and fee implications, and support appeals/exceptions.
