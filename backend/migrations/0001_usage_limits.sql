CREATE TABLE IF NOT EXISTS usage_events (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	owner_id TEXT NOT NULL,
	created_at INTEGER NOT NULL,
	cost_micros INTEGER NOT NULL,
	kind TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS usage_events_owner_idx
	ON usage_events (owner_id, created_at);
