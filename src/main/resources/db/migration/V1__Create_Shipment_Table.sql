CREATE TABLE address (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    address_line VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL,
    postal_code VARCHAR(255) NOT NULL,
    state VARCHAR(255)
);

CREATE TABLE item (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit VARCHAR(255),
    weight NUMERIC(10,2),
    shipment_id BIGINT NOT NULL,

    CONSTRAINT fk_shipment_item
    FOREIGN KEY (shipment_id)
    REFERENCES shipment(id)
    ON DELETE CASCADE
);

CREATE INDEX idx_item_shipment_id
    ON item(shipment_id);

CREATE TABLE shipment (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    idempotency_key VARCHAR(255) NOT NULL,

    client_id INTEGER NOT NULL,
    external_id VARCHAR(50) NOT NULL,
    tracking_number VARCHAR(50) NOT NULL,
    order_id VARCHAR(50) NOT NULL,

    shipment_status VARCHAR(50),

    carrier VARCHAR(50) NOT NULL,

    shipping_method VARCHAR(50) NOT NULL,

    recipient_name VARCHAR(100) NOT NULL,
    recipient_address_id BIGINT NOT NULL UNIQUE,

    recipient_phone_number VARCHAR(255) NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,

    origin_name VARCHAR(100) NOT NULL,
    origin_address_id BIGINT NOT NULL UNIQUE,

    origin_phone_number VARCHAR(255) NOT NULL,
    origin_email VARCHAR(255) NOT NULL,

    estimated_pickup TIMESTAMP,
    estimated_delivery TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    actual_pickup TIMESTAMP,
    actual_delivery TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_event_timestamp TIMESTAMP,

    CONSTRAINT fk_shipment_recipient_address
        FOREIGN KEY (recipient_address_id)
        REFERENCES address(id),

    CONSTRAINT fk_shipment_origin_address
        FOREIGN KEY (origin_address_id)
        REFERENCES address(id),

    CONSTRAINT uq_external_id UNIQUE (external_id, client_id)
);