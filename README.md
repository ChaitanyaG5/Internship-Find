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
| `ADZUNA_APP_ID`, `ADZUNA_APP_KEY` | Adzuna source |
| `RAPIDAPI_KEY` | JSearch source |
| `CORS_ALLOWED_ORIGINS` | Comma-separated API origins when serving a separate frontend |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | PostgreSQL `prod` profile |

To use PostgreSQL, set the database environment variables and run:

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
mvn -pl backend spring-boot:run
```

## API

`GET /api/internships?keywords=java,spring&hours=24` returns recent matching listings newest first. Keywords may also be repeated (`keywords=java&keywords=spring`); any keyword may match. `hours` is between 1 and 168 and defaults to 24. `source` optionally limits results to one source.

Examples:

```text
http://localhost:8080/api/internships
http://localhost:8080/api/internships?keywords=java,spring&hours=72
http://localhost:8080/api/internships?keywords=design&hours=168&source=remotive
```

The response is an array of objects containing `title`, `company`, `location`, `postedAt` (UTC instant), `url`, and `source`.

## Scheduling and retention

Sources run once after application startup and then on `app.fetch.cron` (default every six hours, Asia/Kolkata). Each HTTP request has a 5-second connect timeout and 15-second read timeout. A source failure is logged without stopping the remaining sources. Apply URLs are unique across sources. The nightly retention job removes listings older than `app.retention.days` (default 7); its schedule is `app.retention.cron`.

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
