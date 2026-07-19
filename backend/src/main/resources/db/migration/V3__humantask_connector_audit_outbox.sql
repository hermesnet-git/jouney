CREATE TABLE human_task (
    id                    UUID PRIMARY KEY,
    workflow_instance_id  UUID NOT NULL REFERENCES workflow_instance(id),
    node_instance_id      UUID NOT NULL REFERENCES node_instance(id),
    name                  VARCHAR(200) NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    assignee              VARCHAR(120),
    candidate_group       VARCHAR(120),
    form_schema_json      JSONB NOT NULL,
    form_data_json        JSONB,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    claimed_at            TIMESTAMPTZ,
    completed_at          TIMESTAMPTZ
);

CREATE INDEX idx_human_task_assignee ON human_task (assignee);
CREATE INDEX idx_human_task_group ON human_task (candidate_group);
CREATE INDEX idx_human_task_status ON human_task (status);

CREATE TABLE connector_definition (
    id                 VARCHAR(120) PRIMARY KEY,
    type               VARCHAR(20) NOT NULL,
    base_config_json   JSONB NOT NULL DEFAULT '{}',
    credential_ref     VARCHAR(200) NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_entry (
    id            UUID PRIMARY KEY,
    actor         VARCHAR(120) NOT NULL,
    action        VARCHAR(60) NOT NULL,
    entity_type   VARCHAR(60) NOT NULL,
    entity_id     VARCHAR(120) NOT NULL,
    occurred_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    detail_json   JSONB
);

CREATE INDEX idx_audit_entity ON audit_entry (entity_type, entity_id);
CREATE INDEX idx_audit_actor ON audit_entry (actor);
CREATE INDEX idx_audit_occurred_at ON audit_entry (occurred_at);

CREATE TABLE outbox_event (
    id               UUID PRIMARY KEY,
    aggregate_type   VARCHAR(60) NOT NULL,
    aggregate_id     UUID NOT NULL,
    payload_json     JSONB NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at     TIMESTAMPTZ
);

CREATE INDEX idx_outbox_status ON outbox_event (status);
