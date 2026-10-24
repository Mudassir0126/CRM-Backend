# Lanka Swisstek CRM backend

Java 21 / Spring Boot REST backend, organised by feature using Spring MVC with a service and repository layer.

## Folder structure

Paths below are under `src/main/java/com/lankaswisstek/salesorder/`:

```text
SalesOrderBackendApplication.java     Application entry point; scans all feature packages
features/
  salesorder/
    controller/SalesOrderController.java
    service/SalesOrderService.java
    model/SalesOrder.java
    repository/SalesOrderRepository.java
  settings/
    controller/SettingsController.java
    service/SettingsService.java
    model/WorkspaceSettings.java
    repository/WorkspaceSettingsRepository.java
  production/
    controller/package-info.java
    service/package-info.java
    model/package-info.java
    repository/package-info.java
  accounts/
    controller/package-info.java
    service/package-info.java
    model/package-info.java
    repository/package-info.java
shared/
  config/WebConfig.java               Shared frontend CORS configuration
```

Production and Accounts have documented package folders ready for future implementation. Their placeholder packages do not register endpoints, entities, or database tables.

## How MVC fits this app

The React frontend is the **View**. It sends HTTP requests to a **Controller**, which validates the incoming JSON and delegates to a **Service**. The service handles business rules and uses a **Repository** to read or write the **Model** entities in PostgreSQL. Spring MVC serializes the result back to JSON for React.

```text
React View -> REST Controller -> Service -> Repository -> PostgreSQL
                                                ^
                                           Model entity
```

- Controller: URLs, HTTP methods/statuses, JSON input, and validation.
- Service: duplicate/missing-order checks, settings defaults/cleaning, and transaction boundaries.
- Repository: Spring Data JPA database operations.
- Model: the persistent fields, validation constraints, and table mapping.

Add each future feature's classes in its own package. For example, Production's controller, service, entity, and repository belong under `features/production/`. Define its database tables when the actual data requirements are agreed.

## Run locally

Requires Java 21+, Maven, and the configured local PostgreSQL database `lanka_sales` with user `lanka_app`. Connection settings remain in `src/main/resources/application.properties`.

After moving packages, rebuild once to remove compiled classes with the old package names:

```powershell
mvn clean test
mvn spring-boot:run
```

The API uses port 8080. The existing `sales_orders` and `workspace_settings` table names and entity fields are retained. The quote-number migration changes the sales-order primary key while retaining existing records.

## REST endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/orders` | List orders, newest first |
| GET | `/api/orders/{quoteNumber}` | Get an order; 404 if missing |
| POST | `/api/orders` | Create an order; 201, or 409 for a duplicate quote number |
| PUT | `/api/orders/{quoteNumber}` | Replace an existing order; quote number is immutable |
| DELETE | `/api/orders/{quoteNumber}` | Delete an order; 204, or 404 if missing |
| GET | `/api/settings/sales-people` | Read salespeople; store defaults on first access |
| PUT | `/api/settings/sales-people` | Trim, deduplicate, and save the list |
| GET | `/api/settings/workspace` | Read the companyName object |
| PUT | `/api/settings/workspace` | Save companyName; 400 for a blank name |

The existing frontend JSON shapes, attachments, and API paths remain compatible. The frontend uses its feature API files and shared HTTP transport to call these routes. CORS continues to allow the local Vite origins on port 5173.

## Verification

`src/test/java/com/lankaswisstek/salesorder/BackendMvcTest.java` loads the real controllers and services with mocked repositories. The tests cover routes, validation, create/update/delete status codes, attachments, duplicate/missing orders, settings defaults/cleaning, table-name annotations, and local CORS. They do not connect to PostgreSQL or change saved records.

```powershell
mvn test
```

Database connectivity and full persistence behavior require the PostgreSQL server; the MVC contract tests verify the refactor without opening it.

## Quote-number primary key and migration

quoteNumber is the JPA @Id and PostgreSQL sales_orders primary key. The API no
longer returns or requires a generated id. Quote numbers are trimmed and stored
in uppercase, making case/outer-space variants duplicates. The field is immutable
after creation; update attempts that change it return HTTP 400. Creation uses
INSERT semantics and flushes immediately, so concurrent duplicates return HTTP 409
instead of overwriting an existing order. Creation time is retained on updates.

The frontend uses these query routes, which also support quote numbers containing
slashes and other URL-sensitive characters:

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | /api/orders?quoteNumber=QTN-001 | Read one order |
| PUT | /api/orders?quoteNumber=QTN-001 | Update that order |
| DELETE | /api/orders?quoteNumber=QTN-001 | Delete that order |

The path routes remain available for simple quote numbers.

Flyway applies src/main/resources/db/migration/V1__sales_order_quote_primary_key.sql
before Hibernate starts. Existing databases are baselined at version 0. The migration
checks for blank/duplicate normalized quote numbers before changing anything, and
refuses to proceed if either is found. It changes the primary key from id to
quote_number and retains the old id column/values as nullable legacy references.
All other fields and attachments are preserved. Fresh databases get the same
primary-key and canonical-format constraints.

The migration runs transactionally. It does not delete duplicate records or drop
referencing foreign keys automatically; resolve a reported conflict before retrying.
The schema-history table records completion so the migration runs only once.

## Work on another laptop

Install Java 21, Maven 3.9+, Git, and PostgreSQL. Clone both repositories:

```powershell
git clone https://github.com/Mudassir0126/CRM-Backend.git
git clone https://github.com/Mudassir0126/CRM-Frontend.git
cd CRM-Backend
Copy-Item application-local.properties.example application-local.properties
```

In pgAdmin, create login role lanka_app with your chosen password and database
lanka_sales owned by that role. Edit application-local.properties and replace the
placeholder with this local PostgreSQL password. The actual local settings file is
ignored by Git. You may instead set the DB_PASSWORD environment variable.

Run these commands from the backend folder:

```powershell
mvn test
mvn spring-boot:run
```

The API runs on http://localhost:8080. Flyway and Hibernate prepare the tables on
a fresh database. For existing customer records and attachments, transfer a database
backup privately and restore it before starting the backend. Database files and
backups are excluded from this repository.

Before working, run git pull --ff-only. When finished, commit your changes and run
git push. GitHub synchronizes committed source code, not local passwords or records.
