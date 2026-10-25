# Internship Tracker

Spring Boot application that collects recent internship listings from job-provider APIs and stores them in H2 or PostgreSQL. The project separates its API/backend and plain HTML/CSS/JS frontend into `backend/` and `frontend/`.

```text
internship-tracker/
├── backend/       Spring Boot API, sources, schedulers, and tests
├── frontend/      Static HTML, CSS, and JavaScript
└── pom.xml        Maven parent project
```

Maven copies the frontend files into the backend's static resources when building, so the backend serves the website and API from the same origin. The frontend source remains independent in `frontend/`.
The page includes a moonlit falling `dog.png` background animation and a persistent dark/light theme toggle; the animation is decorative, does not intercept input, and is disabled for users who prefer reduced motion.

## Requirements

- Java 17+
- Maven 3.8+

## Run locally

```powershell
mvn -pl backend spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). The development database is stored under `./data/` and is created automatically.

Remotive is enabled by default and requires no credentials. To enable Adzuna, set `ADZUNA_APP_ID` and `ADZUNA_APP_KEY` in the environment and change `sources.adzuna.enabled=true` in `backend/src/main/resources/application.properties`. Do not commit API credentials.

Optional environment variables:

| Variable | Used by |
| --- | --- |
| `OPENROUTER_API_KEY` | OpenRouter query-to-filter extraction |
| `OPENROUTER_MODEL` | OpenRouter model identifier (defaults to `openai/gpt-4o-mini`) |
| `ADZUNA_APP_ID`, `ADZUNA_APP_KEY` | Adzuna scheduled and direct-search source |
| `ADZUNA_COUNTRY`, `SOURCES_ADZUNA_ENABLED` | Adzuna market and enablement (`in` / `false` by default) |
| `SOURCES_REMOTIVE_ENABLED` | Enable the public Remotive source (`true` by default) |
| `RAPIDAPI_KEY` | Existing optional JSearch scheduler integration |
| `CORS_ALLOWED_ORIGINS` | Comma-separated API origins when serving a separate frontend |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | PostgreSQL `prod` profile |

Spring Boot loads `.env` from the current project directory, or its parent directory when you launch from `backend/`. You can also export values into the environment before starting Spring Boot. In PowerShell:

```powershell
$env:OPENROUTER_API_KEY = "your-openrouter-key"
$env:ADZUNA_APP_ID = "your-adzuna-app-id"
$env:ADZUNA_APP_KEY = "your-adzuna-app-key"
$env:SOURCES_ADZUNA_ENABLED = "true"
mvn -pl backend spring-boot:run
```

OpenRouter is used for natural-language filter extraction. Create an OpenRouter API key and set `OPENROUTER_API_KEY` in `.env` or the environment. Remotive searches do not require AI or job-source credentials; the AI key is only needed for natural-language searches. Adzuna is optional. Keep real keys private and do not commit them.

To use PostgreSQL, set the database environment variables and run:

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
mvn -pl backend spring-boot:run
```

## Deploy with Docker

Build the image from the repository root:

```powershell
docker build -t internship-tracker .
```

For deployment, provide `OPENROUTER_API_KEY` and the PostgreSQL connection variables (`SPRING_PROFILES_ACTIVE=prod`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`) through the hosting platform's secret/environment settings. Do not copy `.env` into the image or commit it. The Docker build runs the backend tests before packaging.

The default local profile uses a file-based H2 database. For production, use PostgreSQL or configure persistent storage for the H2 data directory; container filesystems may be temporary.

## API

`GET /api/internships?keywords=java,spring&hours=24` returns recent matching listings newest first. Keywords may also be repeated (`keywords=java&keywords=spring`); any keyword may match. `hours` is between 1 and 168 and defaults to 24. `source` optionally limits results to one source.

Natural-language searches use `POST /api/jobs/search`. The selected language model extracts filters only; job listings always come from configured job sources:

```json
{
  "query": "Remote Java backend internships in India posted in the last 24 hours",
  "provider": "AUTO",
  "postedWithinHours": 24
}
```

`provider` accepts `OPENROUTER` or `AUTO` (which uses OpenRouter). The response contains `searchFilters`, normalized `jobs` with their source IDs and direct application links, and any partial-source `warnings`. Listings without a valid original posting time or HTTP(S) application URL are excluded. The existing GET endpoint remains available.

The direct-search job adapters are Remotive (public, no key) and Adzuna (API credentials required). Remotive is enabled by default. Adzuna direct search is enabled by setting `SOURCES_ADZUNA_ENABLED=true` alongside both Adzuna credentials.

Examples:

```text
http://localhost:8080/api/internships
http://localhost:8080/api/internships?keywords=java,spring&hours=72
http://localhost:8080/api/internships?keywords=design&hours=168&source=remotive
```

The response is an array of objects containing `title`, `company`, `location`, `postedAt` (UTC instant), `applicationDeadline` (UTC instant or `null` when the source does not provide one), `url`, and `source`.

## Scheduling and expiration

Sources run once after application startup and then on `app.fetch.cron` (default every six hours, Asia/Kolkata). Each HTTP request has a 5-second connect timeout and 15-second read timeout. A source failure is logged without stopping the remaining sources. Apply URLs are unique across sources, and `lastCheckedAt` records when an existing listing was last seen by a scheduled fetch without changing its original `postedAt`. The expiration job deletes listings with a known `applicationDeadline` once that instant has passed; it runs hourly by default and can be configured with `app.expiration.cron`. Listings without a deadline are retained because the current sources do not provide closing dates. The application deadline is stored as an optional UTC instant and is included in the API response.

## Sources

Remotive and Adzuna are implemented. Greenhouse, Lever, The Muse, and JSearch are disabled by default; The Muse and JSearch mapping remains marked TODO pending response-schema verification. Greenhouse and Lever accept comma-separated slugs through `sources.greenhouse.companies` and `sources.lever.companies`. Enable a source using its corresponding `sources.<name>.enabled` property. Use providers' official APIs and observe their terms and rate limits.

## How to add a new job source

Create a Spring component implementing `JobSource`, set its `source` field and map provider dates to UTC `Instant`, and sanitize descriptions. Then add its property flag and any credentials; the scheduler and API do not need changes:

```java
@Component
@ConditionalOnProperty(name = "sources.example.enabled", havingValue = "true")
public class ExampleSource implements JobSource {
    @Override
    public String name() {
        return "example";
    }

    @Override
    public List<Internship> fetch() {
        // Call the provider's official API and map each result to an Internship.
        return List.of();
    }
}
```

```properties
sources.example.enabled=false
```

## Tests

```powershell
mvn test
```
