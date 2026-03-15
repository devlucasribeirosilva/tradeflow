CREATE TABLE accounts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   VARCHAR(100) NOT NULL,
    owner_id    UUID NOT NULL,
    owner_type  VARCHAR(50) NOT NULL,
    balance     NUMERIC(19,2) NOT NULL DEFAULT 0,
    currency    VARCHAR(3) NOT NULL DEFAULT 'BRL',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    CONSTRAINT uq_account_owner UNIQUE (owner_id, owner_type)
);

CREATE TABLE balance_transactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID NOT NULL REFERENCES accounts(id),
    order_id        UUID NOT NULL,
    type            VARCHAR(20) NOT NULL,
    amount          NUMERIC(19,2) NOT NULL,
    balance_before  NUMERIC(19,2) NOT NULL,
    balance_after   NUMERIC(19,2) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_accounts_owner     ON accounts(owner_id);
CREATE INDEX idx_accounts_tenant    ON accounts(tenant_id);
CREATE INDEX idx_transactions_acc   ON balance_transactions(account_id);
CREATE INDEX idx_transactions_order ON balance_transactions(order_id);