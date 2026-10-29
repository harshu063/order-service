CREATE TABLE payment_outcome_inbox
(
    event_id     CHAR(36)     NOT NULL,
    tenant_id    VARCHAR(100) NOT NULL,
    order_id     CHAR(36)     NOT NULL,
    payment_id   CHAR(36)     NOT NULL,
    event_type   VARCHAR(150) NOT NULL,
    processed_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_payment_outcome_inbox PRIMARY KEY (event_id),
    CONSTRAINT fk_payment_outcome_inbox_order FOREIGN KEY (order_id) REFERENCES orders (id)
);
