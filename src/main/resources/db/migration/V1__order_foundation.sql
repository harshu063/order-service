CREATE TABLE orders
(
    id                CHAR(36)     NOT NULL,
    tenant_id         VARCHAR(100) NOT NULL,
    user_id           VARCHAR(255) NOT NULL,
    status            VARCHAR(32)  NOT NULL,
    total_minor       BIGINT       NOT NULL,
    currency          CHAR(3)      NOT NULL,
    aggregate_version BIGINT       NOT NULL,
    lock_version      BIGINT       NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT ck_orders_total_minor CHECK (total_minor >= 0),
    CONSTRAINT ck_orders_aggregate_version CHECK (aggregate_version >= 1)
);

CREATE INDEX ix_orders_owner_page
    ON orders (tenant_id, user_id, created_at DESC, id DESC);

CREATE TABLE order_items
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    order_id         CHAR(36)     NOT NULL,
    product_id       BIGINT       NOT NULL,
    product_version  BIGINT       NOT NULL,
    product_title    VARCHAR(200) NOT NULL,
    unit_price_minor BIGINT       NOT NULL,
    quantity         INT          NOT NULL,
    line_total_minor BIGINT       NOT NULL,
    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT uq_order_items_product UNIQUE (order_id, product_id),
    CONSTRAINT ck_order_items_price CHECK (unit_price_minor >= 0),
    CONSTRAINT ck_order_items_quantity CHECK (quantity BETWEEN 1 AND 100),
    CONSTRAINT ck_order_items_line_total CHECK (line_total_minor >= 0)
);

CREATE TABLE idempotency_records
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id       VARCHAR(100) NOT NULL,
    user_id         VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash    CHAR(64)     NOT NULL,
    order_id        CHAR(36)     NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_idempotency_records PRIMARY KEY (id),
    CONSTRAINT fk_idempotency_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT uq_idempotency_owner_key UNIQUE (tenant_id, user_id, idempotency_key)
);

CREATE TABLE outbox_events
(
    event_id          CHAR(36)     NOT NULL,
    aggregate_type    VARCHAR(100) NOT NULL,
    aggregate_id      VARCHAR(200) NOT NULL,
    aggregate_version BIGINT       NOT NULL,
    event_type        VARCHAR(150) NOT NULL,
    schema_version    INT          NOT NULL,
    ordering_key      VARCHAR(300) NOT NULL,
    payload           JSON         NOT NULL,
    occurred_at       DATETIME(6)  NOT NULL,
    published_at      DATETIME(6)  NULL,
    publish_attempts  INT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_outbox_events PRIMARY KEY (event_id),
    CONSTRAINT uq_outbox_aggregate_event UNIQUE
        (aggregate_type, aggregate_id, aggregate_version, event_type),
    CONSTRAINT ck_outbox_schema_version CHECK (schema_version >= 1),
    CONSTRAINT ck_outbox_publish_attempts CHECK (publish_attempts >= 0)
);

CREATE INDEX ix_outbox_unpublished
    ON outbox_events (published_at, occurred_at);
