CREATE TABLE workflow_instance (
    id                   UUID PRIMARY KEY,
    workflow_version_id  UUID NOT NULL REFERENCES workflow_version(id),
    business_key         VARCHAR(200),
    status               VARCHAR(20) NOT NULL,
    started_by           VARCHAR(120) NOT NULL,
    started_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at         TIMESTAMPTZ,
    current_node_id      VARCHAR(120),
    lock_version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_workflow_instance_status ON workflow_instance (status);
CREATE INDEX idx_workflow_instance_version ON workflow_instance (workflow_version_id);

CREATE TABLE node_instance (
    id                     UUID PRIMARY KEY,
    workflow_instance_id   UUID NOT NULL REFERENCES workflow_instance(id),
    node_id                VARCHAR(120) NOT NULL,
    node_type              VARCHAR(30) NOT NULL,
    status                 VARCHAR(20) NOT NULL,
    attempt                INT NOT NULL DEFAULT 1,
    input_json             JSONB,
    output_json            JSONB,
    error_json             JSONB,
    started_at             TIMESTAMPTZ,
    completed_at           TIMESTAMPTZ
);

CREATE INDEX idx_node_instance_instance ON node_instance (workflow_instance_id);

CREATE TABLE workflow_variable (
    workflow_instance_id  UUID PRIMARY KEY REFERENCES workflow_instance(id),
    variables_json        JSONB NOT NULL DEFAULT '{}',
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
