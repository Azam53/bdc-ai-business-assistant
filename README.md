# BDC AI Business Assistant

A free, local portfolio demo of natural-language analytics over simulated SAP Business Data Cloud data. Includes a responsive enterprise dashboard, conversational analytics, saved conversations, source management, CSV export, and an interaction audit.

**No SAP license or OpenAI key is needed. No real SAP connection is represented as active.** All monetary calculations use Java `BigDecimal`; the LLM never receives SQL access.

## Quick start with Docker

Install Docker Engine/Desktop with Compose, then run from this directory:

```sh
docker compose up --build
```

Open **http://localhost:3000**. Subsequent runs can use `docker compose up`. The initial build downloads dependencies. MySQL starts first, then Spring Boot and Nginx. If the page briefly reports a gateway error during backend startup, refresh after startup completes.

Optional: copy `.env.example` to `.env` before starting. Defaults already enable the free demo. Docker publishes only the frontend on loopback; the backend and database stay on the Compose network. `docker compose down` stops the stack and preserves its database volume.

## Run without Docker

Requirements: JDK 21+, Maven 3.9+, Node.js 22+ and npm. The Maven compiler targets Java 21. Local verification on this machine used JDK 25; the Docker build explicitly uses JDK 21.

Terminal 1, from the repository root:

```sh
mvn -f backend/pom.xml clean package
cd backend
java -jar target/assistant-1.0.0.jar
```

Terminal 2:

```sh
cd frontend
npm ci
npm run dev
```

Open **http://localhost:5173**. Vite proxies `/api` to `127.0.0.1:8080`. The backend binds to loopback by default. With no database environment variables, a persistent H2 database is created under `backend/data/`. This additional local fallback lets the demo run without installing MySQL. Conversations and audit entries persist across restarts. The business dataset is deterministic and regenerated in memory.

If Maven is not installed on the current workstation, a temporary Maven distribution used during development is available at `.tools/apache-maven-3.9.9/bin/mvn.cmd`; it is deliberately ignored by Git.

## MySQL setup without Docker

Create a database and a dedicated user using your MySQL administrator account:

```sql
CREATE DATABASE bdc CHARACTER SET utf8mb4;
CREATE USER 'bdc'@'localhost' IDENTIFIED BY 'choose-your-local-password';
GRANT ALL PRIVILEGES ON bdc.* TO 'bdc'@'localhost';
```

Set `DB_URL=jdbc:mysql://localhost:3306/bdc`, `DB_USER=bdc`, and `DB_PASSWORD` before starting Java. For example in PowerShell:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/bdc'
$env:DB_USER='bdc'
$env:DB_PASSWORD='your-local-password'
java -jar target/assistant-1.0.0.jar
```

JPA creates/updates conversation and interaction tables. For production, replace `ddl-auto: update` with reviewed migrations.

## Architecture

```text
React dashboard / chat
        | REST JSON
Controller → ChatService → deterministic intent router
        |                     |
        |              approved analytics functions
        |                     |
        |              BusinessDataProvider
        |              ├─ MockBusinessDataProvider (default)
        |              └─ SapBusinessDataCloudProvider (extension boundary)
        |                     |
        |              Java BigDecimal calculations
        |                     |
        |              AIProvider (calculated results only)
        |              ├─ MockAIProvider (default)
        |              ├─ OpenAIProvider (optional)
        |              └─ SapGenerativeAIHubProvider (extension boundary)
        |
        └─ JPA repositories → MySQL / local H2
           conversations, context and audit history
```

Backend packages under `backend/src/main/java/com/bdc`:

| Package | Responsibility |
| --- | --- |
| controller | REST endpoints, input errors, CSV export |
| service | Intent routing, period/country context, explanation and audit orchestration |
| analytics | Approved functions and exact decimal financial calculations |
| provider | Normalized enterprise dataset contract and replaceable adapters |
| ai | Explanation interface, deterministic mock, optional OpenAI, SAP Hub boundary |
| dto | Dataset records, filters, metrics, rows, charts, request/response contracts |
| entity / repository | JPA persistence for conversations and interaction audits |
| config / security | Provider selection and local demo security policy |

Stack: Java 21 target, Spring Boot 3.5, Maven, Spring Web, Spring Data JPA, Spring Security, MySQL 8.4; React 19, Vite 6, Bootstrap 5, Chart.js 4, Lucide icons. The UI uses original styling, with no proprietary SAP assets.

## How the mock BDC works

`MockBusinessDataProvider` uses seed 42 to generate **100 customers, 50 products and 7,116 sales orders/items/invoices** across **September 2024–August 2026**. Each demo order has one item and one paid invoice. Customers and products have normalized IDs. Items include country, region, business unit, quantity, revenue and cost; profit and margin are derived.

Countries: India, Germany, USA, UAE and UK. Regions: Asia Pacific, Europe, North America and Middle East. Business units: Cloud & Software, Industrial Systems, Business Services.

The generator includes India/UAE growth, declining demand for seven products, seasonality, an August downturn, reduced ordering by selected customers, and different cost ratios by product and country. This is synthetic data, not an extract of a live SAP tenant. Revenue is defined as order-item net sales in USD, not invoice-date recognized accounting revenue. No currency conversion or tax accounting is implied.

## Period and metric semantics

- Relative questions anchor to **August 31, 2026**, the final available demo month, rather than the system clock. “Last month” means that latest completed demo month.
- Default queries and dashboard: September 2025–August 2026. “Last 6 months”: March–August 2026.
- “This quarter” covers July–August 2026 (available quarter-to-date data). “Last quarter” covers April–June 2026. “Q2” selects April–June 2026.
- Comparisons use the immediately preceding equal number of calendar months. A two-month quarter-to-date range is compared with the preceding two months, not an entire quarter. For custom partial-month dashboard dates, prior comparison begins the same day shifted by that month count and ends immediately before the current start; select full months for directly comparable monthly reports.
- Dates are inclusive; missing months are returned as zeros. Zero comparison baselines return an unavailable percentage, not an invented growth value.
- Total orders count distinct order IDs. Active customers count customers with orders under the selected filters. Average order value = revenue / orders. Gross profit = revenue − cost. Margin = gross profit / revenue × 100; zero revenue produces 0%.
- Customer-drop analysis compares **order count**, with a strict decline greater than 20%. Other change percentages refer to revenue. Products with no current sales are retained as 100% declines.
- Explanations report observed contributions and do not claim unmeasured business causes.

## Demo walkthrough

1. Open Dashboard and inspect six KPI cards and seven charts/tables. Try country, region, date, customer, product and business unit filters.
2. Select New conversation and ask **“Show revenue for the last 12 months.”**
3. Ask **“Compare India and Germany sales for the last 6 months.”**, followed by **“Why is India growing faster?”** in the same conversation. The comparison countries and period are retained, with calculated business-unit contributions.
4. Start a new conversation and ask **“Show the top 10 customers.”**
5. Ask **“Which products are declining?”**
6. Ask **“Why did revenue change last quarter?”**
7. Try **“Find customers whose orders dropped more than 20%.”**, **“Show monthly revenue as a chart.”**, **“Show revenue by region.”**, or **“Give me an executive summary of Q2 performance.”**
8. Open Audit log to inspect recorded question, user, detected intent, approved function, timestamp, source and status.
9. Open Data Sources and click Connect SAP Business Data Cloud to see the honest configuration explanation.

The mock router supports these scenarios and common keywords; it is not an unrestricted natural-language model. Unsupported requests receive guidance. It never constructs or executes SQL. Follow-up context is explicitly implemented for period/country comparisons; arbitrary conversational references are not a general semantic reasoning system.

## REST API

| Endpoint | Purpose |
| --- | --- |
| `GET /api/health` | Backend health |
| `GET /api/settings` | Effective nonsecret provider settings |
| `GET /api/catalog` | Filter choices and available data extent |
| `GET /api/dashboard` | KPIs, chart rows and calculated insights |
| `POST /api/chat` | Question and optional conversationId |
| `GET /api/conversations` | Saved conversations |
| `GET /api/conversations/{id}` | Ordered interactions and stored results |
| `GET /api/audit` | Latest 200 audit entries |
| `GET /api/export` | Default 12-month country revenue CSV, compatible with Excel |

Dashboard accepts `from`, `to`, `country`, `region`, `customer` (ID), `product` (ID) and `businessUnit`. CSV export currently uses the default reporting period, independently of dashboard filters; CSV/Excel import is not implemented.

Example request:

```json
{"question":"Compare India and Germany sales for the last 6 months.","conversationId":null}
```

The response contains `answer`, `metrics`, `table`, `chart`, `insights`, `dataSource`, `intent`, `function`, `period`, and `conversationId`. Reuse the returned ID for follow-up questions.

Approved functions: `getRevenueByPeriod`, `getRevenueByCountry`, `getRevenueByRegion`, `getTopCustomers`, `getTopProducts`, `getDecliningProducts`, `getCustomerOrderTrend`, `getProfitMargin`, `comparePeriods`, and `compareCountries`.

## Environment configuration

`.env` is consumed automatically by Docker Compose. For direct Java execution, set variables in your shell; Java does not load `.env` automatically. Never commit real credentials.

| Variable | Default / purpose |
| --- | --- |
| `DEMO_MODE` | `true`; forces both mock providers regardless of other selections |
| `AI_PROVIDER` | `mock`; optional `openai` when demo mode is false |
| `BUSINESS_DATA_PROVIDER` | `mock`; `sap` selects the unimplemented adapter |
| `OPENAI_API_KEY` | Optional server-only key |
| `OPENAI_MODEL` | `gpt-4.1-mini`; configurable model identifier |
| `SAP_BDC_BASE_URL` | Reserved for your verified tenant integration URL |
| `SAP_BDC_CLIENT_ID` / `SAP_BDC_CLIENT_SECRET` | Reserved integration credentials |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Direct JDBC configuration; defaults to local H2 |
| `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD` | Compose database settings |
| `SERVER_ADDRESS` | `127.0.0.1` locally; `0.0.0.0` within Docker |

## Optional AI provider

To use OpenAI with synthetic business data, set `DEMO_MODE=false`, `AI_PROVIDER=openai`, `BUSINESS_DATA_PROVIDER=mock` and `OPENAI_API_KEY` on the server. This optional external service may incur charges; it is not needed for the demo.

`OpenAIProvider` sends only calculated insight statements and uses a deliberately restrictive extraction contract: it may select a provided explanation verbatim. Responses not exactly matching an approved statement, timeouts or request failures fall back to the deterministic answer. It cannot introduce financial values, invent prose around unsupported facts, select arbitrary database queries, or access database credentials. Requests have bounded connection/read timeouts. A missing key outside demo mode causes the analysis to fail and be audited. The paid provider was not invoked during local verification.

To add another provider, implement `AIProvider.explain(Result)` and register it in `config/Providers`. SAP Generative AI Hub has an explicit unimplemented boundary; no tenant endpoint or model deployment is fabricated. If implementing richer intent detection, map output to a validated enum of approved functions and validate every filter before executing analytics.

## Future real SAP BDC / BTP integration

1. Discover supported integration contracts in your actual tenant: authorized BDC data products, BTP services, Datasphere/OData or other supported interfaces.
2. Implement `SapBusinessDataCloudProvider.load()` to return the same normalized `DataSet` records. Map real IDs, dimensions, dates, order-item monetary values and currency normalization deliberately.
3. Read credentials only from environment/secret management; implement the authentication flow supported by that service. Add pagination, token refresh, request timeouts, retries, schema validation and access checks.
4. Register/enable `BUSINESS_DATA_PROVIDER=sap` with `DEMO_MODE=false` only after completing the adapter. Setting credentials alone does not make the placeholder functional.
5. Keep the frontend and Java analytics contracts unchanged. For large datasets, evolve the provider toward server-filtered snapshots or approved aggregation requests rather than loading an entire tenant into memory.

There are **no fabricated SAP API endpoints**. The SAP adapter fails explicitly with a configuration message until implemented.

## Security and scope

This is a loopback-only, single-user demo. Spring Security applies response headers; endpoints intentionally permit the local demo user and CSRF is disabled for the same-origin JSON demo API. The displayed identity is `demo-user`; there is no production authentication or admin authorization. Audit data is visible to the local user. Before public or enterprise deployment, add OIDC/BTP identity, tenant ownership, role-based audit permissions, appropriate CSRF policy, rate limits, data retention, managed secrets and schema migrations. Do not expose this demo as a production multi-user service.

Mock enterprise facts live in the provider, while JPA stores conversations/context/audit. No SAP SDK or paid service is required. Fonts have system fallbacks; the application continues to function if the optional Google Fonts stylesheet is unavailable.

## Tests and verification

```sh
mvn -f backend/pom.xml test
cd frontend
npm ci
npm run build
```

`AnalyticsEngineTest`: exact fixture revenue, country comparison, period comparison, customer ranking, declining products, profit margin, inclusive date/country filters, empty results, zero baselines and deterministic mock-data integrity.

`ApiTest`: all five required questions through Spring MVC, six-month country follow-up context and persistence, invalid input and SQL-shaped/unsupported requests.

Local verification: **11 tests passed**, production Vite build passed, and all five questions were submitted through the running React UI and returned charts/tables. Docker files are provided but the Compose/MySQL stack was not executed here because Docker is unavailable. JDK 21 is specified in the Docker build; local tests ran on JDK 25 targeting Java 21 bytecode.

Observed default results (seed 42):

| Query | Verified result |
| --- | --- |
| Last 12 months revenue | $68,666,445.91; 3,516 orders; 35.71% margin |
| India vs Germany (default 12 months) | India $16,688,884.80; Germany $12,200,987.02 |
| Top customers | 10 ranked customer rows |
| Declining products (available current quarter) | 34 products declining versus prior two months |
| Last completed quarter | Revenue change −13.33% versus Q1 2026 |

## Screenshots

Screenshot placeholders for your portfolio presentation:

- Dashboard: KPI cards, monthly revenue and AI insights.
- AI Assistant: India/Germany comparison with explanation, chart and table.
- Data Sources: explicit mock status and future integration boundary.
- Audit: recorded interaction trace.

## Troubleshooting

- Port busy: stop the existing instance or set `SERVER_PORT` for Spring and update the Vite proxy accordingly.
- Gateway error: confirm the backend has finished startup and `/api/health` responds.
- MySQL login failure after changing `.env`: existing MySQL volumes retain their original users/passwords; update credentials deliberately rather than deleting data.
- Blank results: reset dashboard filters and use dates within September 2024–August 2026.
- Unsupported question: use the example prompts; this free mock intentionally uses controlled keyword routing.

## Web search in chat

Select **Web search** beside the chat composer, then enter a standalone public topic such as “What is business intelligence?” or “Explain supply chain management”. Select **Business data** to return to calculated enterprise analytics.

- Default: `WEB_SEARCH_PROVIDER=wikipedia`. Retrieves live Wikipedia search excerpts and source links without an API key. This is background-reference search, not general web/news coverage.
- Optional broader search: set `WEB_SEARCH_PROVIDER=tavily` and `TAVILY_API_KEY` on the server, then restart. Docker Compose passes both variables through. Tavily usage is subject to your account limits and plan; this optional provider has not been called with a live key during verification.
- Search results are excerpts, not an AI-generated synthesis. Each response labels its provider and retrieval time. The snippets are rendered as text, and source links accept only HTTP(S) URLs.
- Only the explicit typed search query is sent externally. No conversation history, internal metrics, customer records or SAP credentials are added. Avoid putting confidential information into a public search query.
- Web results do not change any business calculations or establish why simulated revenue changed. Switching modes preserves the existing business comparison context.
- `POST /api/web-chat` accepts the same question/conversationId shape as business chat, with a 500-character limit. Responses include `sources` (title, URL, snippet) and `sourceNote`; metrics and business tables are empty. Interactions, including empty results and provider failures, are audited.
- Connection and read timeouts are bounded. Missing keys, unavailable providers and zero-result searches produce explicit messages; no fabricated search results are supplied.

Provider documentation: [MediaWiki Search API](https://www.mediawiki.org/wiki/API:Search) and [Tavily Search API](https://docs.tavily.com/documentation/api-reference/endpoint/search).

## Deploy on Render

The root `Dockerfile` builds React and packages its output inside Spring Boot. `render.yaml` deploys the combined application as one **Free Docker web service**, with HTTPS terminated by Render. No separate frontend service or MySQL instance is needed for this initial demo deployment. The existing Docker Compose setup remains available for local MySQL usage.

1. Push the project to a GitHub repository.
2. In Render, choose **New → Blueprint**, authorize/select that repository, and use `render.yaml` from the repository root.
3. Review the service: name `bdc-ai-business-assistant`, runtime Docker, plan Free, Dockerfile `./Dockerfile`, health check `/api/health`. Create the Blueprint.
4. Wait for the build and health check to succeed, then open the service's assigned `https://…onrender.com` URL.
5. Open the service URL. The public demo opens directly without a login.

If creating a Web Service manually instead of using a Blueprint, choose Docker with the repository root as the build context, the root Dockerfile, and Free compute. Set `SPRING_PROFILES_ACTIVE=render`. The remaining demo providers default to mock/Wikipedia. The Render profile reads `PORT` automatically and binds to `0.0.0.0`.

The Render profile opens the demo publicly without authentication, like local development. Conversations and audit history are shared and accessible to anyone with the URL; use simulated data only. CSRF protection remains enabled for browser requests, and session cookies are HTTPS-only.

**Free storage limitations:** conversations and audit entries use in-memory H2 on Render and reset on restarts, redeploys and service sleep. Mock business data is regenerated deterministically. Render Free services sleep after 15 minutes without traffic, and waking can take about a minute. A persistent paid disk or external MySQL database can be configured later via `DB_URL`, `DB_USER` and `DB_PASSWORD`; those resources may cost money. No paid infrastructure is provisioned by this Blueprint.

For an external MySQL database, use a TLS-enabled JDBC URL provided by your database administrator. The application already includes the MySQL driver; a Render Postgres URL is not interchangeable and requires a separate driver/schema migration.

Optional web search still defaults to Wikipedia. Set `WEB_SEARCH_PROVIDER=tavily` and `TAVILY_API_KEY` in Render only if you want to use your own search account. Demo mode keeps the AI provider mock and requires no OpenAI key.

Reference: [Docker on Render](https://render.com/docs/docker), [Blueprint specification](https://render.com/docs/blueprint-spec), [Free-service limitations](https://render.com/docs/free).
