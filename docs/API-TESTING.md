# API Testing

Base URL: `http://localhost:8080`

Import `postman/Postroom.postman_collection.json` into Postman. The collection captures the access token and created post ID automatically. Keep Postman cookie handling enabled so the HttpOnly `refresh_token` cookie is sent to `/api/auth/refresh` and `/api/auth/logout`.

## Requests

Register:

```http
POST /api/auth/register
Content-Type: application/json

{"email":"alex@example.com","displayName":"Alex Rivera","password":"correct-horse-battery-42"}
```

Login:

```http
POST /api/auth/login
Content-Type: application/json

{"email":"alex@example.com","password":"correct-horse-battery-42"}
```

Create a published post:

```http
POST /api/posts
Authorization: Bearer <accessToken>
Content-Type: application/json

{"content":"A thought worth sharing.","status":"PUBLISHED"}
```

Schedule a post with an ISO-8601 timestamp in the future:

```json
{"content":"Coming soon.","status":"SCHEDULED","scheduledAt":"2027-01-01T16:00:00Z"}
```

Paginate and sort:

```http
GET /api/posts?page=0&size=10&sortBy=createdAt&sortDir=desc&status=PUBLISHED
Authorization: Bearer <accessToken>
```

Update or delete an owned post at `/api/posts/{postId}`. A post owned by another account returns 404. The `size` parameter is capped at 50. Allowed `sortBy` values are `createdAt`, `updatedAt`, `status`, and `scheduledAt`.

Additional checks:

- `GET /api/posts/trending?limit=10` returns the cached recent published feed.
- `POST /api/auth/refresh` rotates the refresh cookie. Replaying the previous cookie must return 401.
- `POST /api/auth/logout` revokes the current refresh token and expires the cookie.
- `GET /api/admin/overview` requires `ROLE_ADMIN`; a normal account receives 403.
- Successful and failed responses use `timestamp`, `status`, `message`, `data`, and `errors`.
- Every response includes `X-Correlation-ID`; a valid caller-provided ID is propagated to logs.

## JMeter

Open `performance/jmeter/posts-read-load.jmx` in Apache JMeter. Supply an access token as a JMeter property:

```powershell
jmeter -n -t performance/jmeter/posts-read-load.jmx -JaccessToken=PASTE_TOKEN_HERE -l results.jtl
```

Use a non-production database and test account for load runs. Increase thread count gradually and monitor the database, JVM, and Actuator health metrics.
