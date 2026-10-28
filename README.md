# Order Service

Spring Boot 3 / Java 21 service that owns order creation and customer order reads.

Implemented foundation:

- OAuth2 resource-server enforcement (`orders.write` and `orders.read`).
- User- and tenant-scoped orders.
- Idempotent order creation with request-fingerprint conflict detection.
- Authoritative catalog snapshot validation via `GET /products/{id}`.
- MySQL/Flyway persistence and optimistic locking.
- Explicit order state transitions.
- Atomic `order.created.v1` transactional outbox record.

The outbox publisher and payment-event consumer are intentionally separate follow-up work. The service currently uses the single-tenant fallback `ORDER_DEFAULT_TENANT=default` when the issuer does not provide `tenant_id`.

## Run

Set the values shown in `.env.example`, start MySQL, then run:

```powershell
.\mvnw.cmd spring-boot:run
```

Run tests with:

```powershell
.\mvnw.cmd test
```

The MySQL integration tests use Testcontainers and are skipped automatically when Docker is unavailable.
