CREATE TABLE payment
(
    id             UUID           NOT NULL,
    merchant_id    VARCHAR(64)    NOT NULL,
    user_id        UUID           NOT NULL,
    payment_type   VARCHAR(30)    NOT NULL,
    amount         NUMERIC(19, 4) NOT NULL,
    pg_payment_key VARCHAR(255),
    status         VARCHAR(20)    NOT NULL,
    expires_at     TIMESTAMPTZ    NOT NULL,
    created_at     TIMESTAMPTZ    NOT NULL,
    created_by     VARCHAR(100)   NOT NULL,
    updated_at     TIMESTAMPTZ    NOT NULL,
    updated_by     VARCHAR(100)   NOT NULL,
    CONSTRAINT pk_payment PRIMARY KEY (id),
    CONSTRAINT uq_payment_merchant_id UNIQUE (merchant_id),
    CONSTRAINT uq_payment_pg_payment_key UNIQUE (pg_payment_key)
);

CREATE TABLE payment_history
(
    id           UUID         NOT NULL,
    payment_id   UUID         NOT NULL,
    from_status  VARCHAR(20)  NOT NULL,
    to_status    VARCHAR(20)  NOT NULL,
    pg_response  TEXT,
    requested_at TIMESTAMPTZ  NOT NULL,
    requested_by VARCHAR(100) NOT NULL,
    responded_at TIMESTAMPTZ,
    CONSTRAINT pk_payment_history PRIMARY KEY (id),
    CONSTRAINT fk_payment_history_payment FOREIGN KEY (payment_id) REFERENCES payment (id)
);

CREATE TABLE subscription
(
    id                UUID        NOT NULL,
    user_id           UUID        NOT NULL,
    billing_key       VARCHAR(255),
    status            VARCHAR(30) NOT NULL,
    next_billing_date DATE,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL,
    version           BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_subscription PRIMARY KEY (id)
);

CREATE TABLE subscription_history
(
    id              UUID        NOT NULL,
    subscription_id UUID        NOT NULL,
    from_status     VARCHAR(30) NOT NULL,
    to_status       VARCHAR(30) NOT NULL,
    reason          TEXT,
    changed_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_subscription_history PRIMARY KEY (id),
    CONSTRAINT fk_subscription_history_subscription FOREIGN KEY (subscription_id) REFERENCES subscription (id)
);

CREATE INDEX idx_payment_user_id ON payment (user_id);
CREATE INDEX idx_payment_status ON payment (status);
CREATE INDEX idx_payment_history_payment_id ON payment_history (payment_id);
CREATE INDEX idx_subscription_user_id ON subscription (user_id);
CREATE INDEX idx_subscription_status ON subscription (status);
CREATE INDEX idx_subscription_next_billing ON subscription (next_billing_date) WHERE status = 'ACTIVE';
CREATE INDEX idx_subscription_history_subscription_id ON subscription_history (subscription_id);
