CREATE TABLE workflow_definition (
    id              UUID PRIMARY KEY,
    workflow_key    VARCHAR(120) NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    graph_json      JSONB        NOT NULL DEFAULT '{}',
    created_by      VARCHAR(120) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE workflow_version (
    id                      UUID PRIMARY KEY,
    workflow_definition_id  UUID NOT NULL REFERENCES workflow_definition(id),
    version_number          INT  NOT NULL,
    definition_json         JSONB NOT NULL,
    published_by            VARCHAR(120) NOT NULL,
    published_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    active                  BOOLEAN NOT NULL DEFAULT true,
    UNIQUE (workflow_definition_id, version_number)
);

CREATE INDEX idx_workflow_version_definition ON workflow_version (workflow_definition_id);
CREATE UNIQUE INDEX uq_workflow_version_active
    ON workflow_version (workflow_definition_id)
    WHERE active;
