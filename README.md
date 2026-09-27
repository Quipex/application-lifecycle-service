# Application Lifecycle Service

A Java REST service for managing Applications (requests) from creation through
verification, acceptance and publication, or deletion/rejection. It persists
requests and records who changed their state or body and when. The API supports
paginated searches and exposes interactive documentation.

## Build and run

Requires Java 21 and Maven 3.9. From the repository root:

```bash
mvn verify
mvn install
mvn -pl bootstrap spring-boot:run
```

`mvn verify` runs the tests and writes the aggregate JaCoCo report to
`bootstrap/target/site/jacoco-aggregate/index.html`. `mvn install` makes the
other modules available to the standalone bootstrap run; after the first
install, only the last command is needed to restart the service. H2 creates
`./data/application.mv.db` on first run and retains data across restarts.
The server listens on `127.0.0.1:8080`; it has no authentication and should
not be exposed to a network without external access controls.

Try it (replace `{id}` with the UUID returned by the first request):

```bash
curl -X POST http://localhost:8080/applications \
  -H 'Content-Type: application/json' -H 'X-Actor-Id: alice' \
  -d '{"name":"Loan request","body":"Please approve the loan"}'

curl -X POST http://localhost:8080/applications/{id}/verify
curl -X POST http://localhost:8080/applications/{id}/accept
curl -X POST http://localhost:8080/applications/{id}/publish
curl 'http://localhost:8080/applications?name=loan&state=PUBLISHED&page=0&size=10'
curl http://localhost:8080/applications/{id}/history
```

Swagger UI: `http://localhost:8080/swagger-ui.html` (OpenAPI JSON at
`http://localhost:8080/v3/api-docs`).

## Stack

- **Java 21, Maven and Spring Boot 3.5:** a supported Java baseline, a
  reproducible multi-module build and a small application composition root.
- **Spring Web MVC:** REST endpoints; **Jakarta Bean Validation:** reject
  malformed input at the boundary; **Spring transaction management:** save a
  change and its audit record atomically.
- **Spring Data JPA / Hibernate:** database access and optimistic locking
  behind ports. The domain has no JPA annotations; Hibernate also owns the
  persistence entity's creation and update timestamps.
- **H2 (file mode):** persistent storage without an external database setup.
  **Flyway:** versioned SQL schema, validated against the JPA mappings at boot.
- **Lombok:** limited to getters, builders, toString and JPA constructors; no
  generated setters on domain objects. **springdoc-openapi:** browse and try
  the API through Swagger UI.
- **JUnit 5, Mockito, Spring test slices and MockMvc:** unit, persistence and
  HTTP integration tests. **JaCoCo:** aggregate coverage reporting.

## Design

### Modules and boundaries

- `domain`: immutable Application aggregate, value objects, typed events and
  state machine; only compile-time Lombok (`provided`).
- `application`: command use cases, query service and their ports; depends on
  `domain` plus `spring-tx` for `@Transactional`.
- `infrastructure`: JPA entities, explicit mappers, repository and audit
  implementations, H2 and Flyway; depends on `domain` and `application`.
- `web`: controllers, validation and the RFC 7807 error handler; depends on
  `domain` and `application`, not on `infrastructure`.
- `bootstrap`: application entry point, configuration, bean wiring and
  end-to-end tests; depends on all four modules.

This is ports-and-adapters architecture with a small CQRS-style separation:
commands load and save the aggregate; queries read DTO projections from the
same database without loading the body for listings. A second read store and
full CQRS would add synchronization without a read-scale need. Interfaces are
named for their roles (`ApplicationRepository`, `AuditLog`); implementations
for their mechanisms (`ApplicationJpaRepository`, `JpaAuditLog`). The domain
owns ports needed for its invariants (publication ID generation); the
application layer owns orchestration ports (repositories, queries, audit).

### Lifecycle and persistence

A hand-written state machine restricts transitions to:

```text
create -> CREATED
CREATED  -> VERIFIED (verify) | DELETED (delete)
VERIFIED -> ACCEPTED (accept) | REJECTED (reject)
ACCEPTED -> PUBLISHED (publish) | REJECTED (reject)
```

Creation constructs the initial state; body edits in CREATED or VERIFIED do
not change it. Delete and reject require a `Reason` value object. Publishing
allocates a unique numeric ID from an H2 sequence; a database UNIQUE
constraint backs up that guarantee. Spring Statemachine would obscure this
small, fixed transition set with machinery for more complex workflows.

The aggregate is immutable: an operation returns a new Application and a
typed event (`TransitionResult`); its constructor validates invariants even
when a builder is used. Domain-specific IDs, Name, Body and Reason are
self-validating value objects, not raw primitives. Domain/application APIs use
`Optional` for absent values rather than nullable signatures. A separate
`ApplicationJpaEntity` and hand-written mappers keep persistence concerns out
of the model; there are too few mappings to warrant MapStruct. Hibernate
sets creation/update timestamps on the entity. The repository returns the
saved copy with refreshed timestamps. `@Version` detects competing updates
and returns HTTP 409 rather than silently overwriting them.

### Audit and errors

Every transition and body edit writes a typed event to `audit_record` with
actor, action, states, optional reason and time. The use case saves the
aggregate and appends the audit row through a port in the same transaction,
so neither can commit alone. Direct append is simpler than an event bus with
one consumer; a transactional outbox would be appropriate if external
consumers were added. Hibernate Envers was not chosen because this log needs
explicit business events, actor and reason, not entity revision diffs.

A global handler returns RFC 7807 Problem Details: 400 for invalid input
(including pagination, filters and actor header), 404 for unknown IDs, and
409 for illegal transitions or version conflicts.

## Assumptions and limits

- The task does not require a single-item GET or a history endpoint; both are
  offered to inspect persisted requests and their audit trail.
- No users or authentication are specified. `X-Actor-Id` (1-64 characters)
  defaults to `system`; supplied values are self-asserted, not verified IDs.
- Searches use offset pagination (appropriate for this small dataset and
  total-count pages), not cursors. Page size defaults to 10, is capped at 100;
  offset is capped at 100000. Listings sort by creation time descending, then
  ID descending; history sorts by occurrence time ascending, then row ID.
  Clients cannot specify an alternative sort.
- Name and state filters are case-insensitive; state values are normalized to
  the enum. Names are immutable after creation; only the body can be edited.
  Limits: name 1-100, body 1-10000, reason 1-512 characters; the name filter
  is capped at 100 characters.
- DELETED is a retained state, not physical removal. Default listings hide
  these rows; filtering by `state=DELETED` shows them, and GET by ID returns
  the deleted record with HTTP 200.
- Application IDs are UUIDs. Publication IDs are sequence-backed numbers
  assigned only at publication. Sequence gaps after rollback are acceptable:
  uniqueness, not continuity, is required.
- Body edits are audited as EDIT events. The history API is paginated (default
  10, maximum 100 entries); `occurred_at` is stamped by the audit adapter.
- POST /applications is not idempotent: retrying it may create another
  request. Client-supplied idempotency keys, users/roles, event workers and
  external event delivery are outside the task's scope.

## API

Commands return the Application DTO; creation returns HTTP 201 with Location.

- `POST /applications` - create with `{ "name", "body" }`.
- `PATCH /applications/{id}` - edit with `{ "body" }` in CREATED/VERIFIED.
- `POST /applications/{id}/verify` - CREATED to VERIFIED.
- `POST /applications/{id}/delete` - CREATED to DELETED; requires `{ "reason" }`.
- `POST /applications/{id}/accept` - VERIFIED to ACCEPTED.
- `POST /applications/{id}/reject` - VERIFIED/ACCEPTED to REJECTED;
  requires `{ "reason" }`.
- `POST /applications/{id}/publish` - ACCEPTED to PUBLISHED, assigns
  `publicationId`.
- `GET /applications?name=&state=&page=&size=` - filtered, paginated listing.
- `GET /applications/{id}` - one Application, including DELETED.
- `GET /applications/{id}/history?page=&size=` - paginated audit history;
  unknown IDs return 404.

## Tests

`mvn verify` runs domain and use-case unit tests, JPA slice tests for mapping,
queries and audit, web error-handler tests, and a MockMvc integration test with
Flyway and H2. These cover valid/invalid transitions, filtering and pagination,
soft deletion, validation errors, optimistic locking and transactional audit.
The coverage report is at `bootstrap/target/site/jacoco-aggregate/index.html`.
