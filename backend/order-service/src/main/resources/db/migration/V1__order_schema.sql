CREATE TABLE buyers (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    tenant_id   VARCHAR(100) NOT NULL
);

CREATE TABLE suppliers (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    tenant_id   VARCHAR(100) NOT NULL
);

CREATE TABLE orders (
    id                UUID PRIMARY KEY,
    idempotency_key   VARCHAR(255) NOT NULL UNIQUE,
    buyer_id          UUID NOT NULL REFERENCES buyers(id),
    supplier_id       UUID NOT NULL REFERENCES suppliers(id),
    status            VARCHAR(50) NOT NULL,
    total_amount      NUMERIC(19,2),
    total_currency    VARCHAR(3),
    tenant_id         VARCHAR(100) NOT NULL,
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP,
    CONSTRAINT fk_buyer    FOREIGN KEY (buyer_id)    REFERENCES buyers(id),
    CONSTRAINT fk_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

CREATE TABLE order_items (
    id                   UUID PRIMARY KEY,
    order_id             UUID NOT NULL,
    product_name         VARCHAR(255) NOT NULL,
    quantity             INTEGER NOT NULL CHECK (quantity > 0),
    unit_price_amount    NUMERIC(19,2) NOT NULL,
    unit_price_currency  VARCHAR(3) NOT NULL,
    CONSTRAINT fk_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX idx_orders_tenant    ON orders(tenant_id);
CREATE INDEX idx_orders_status    ON orders(status);
CREATE INDEX idx_orders_idem_key  ON orders(idempotency_key);
CREATE INDEX idx_items_order      ON order_items(order_id);