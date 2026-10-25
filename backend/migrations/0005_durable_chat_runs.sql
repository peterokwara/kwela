CREATE TABLE IF NOT EXISTS chat_runs (
    id TEXT PRIMARY KEY NOT NULL,
    owner_id TEXT NOT NULL,
    idempotency_key TEXT NOT NULL,
    workflow_instance_id TEXT NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 1,
    status TEXT NOT NULL CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETE', 'FAILED')),
    input_key TEXT NOT NULL,
    reply_json TEXT,
    reply_expires_at INTEGER,
    acknowledged_at INTEGER,
    retry_key TEXT,
    error_message TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    input_expires_at INTEGER,
    UNIQUE(owner_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS chat_runs_owner_created_idx ON chat_runs(owner_id, created_at DESC);
CREATE INDEX IF NOT EXISTS chat_runs_input_expiry_idx ON chat_runs(input_expires_at) WHERE input_expires_at IS NOT NULL;
