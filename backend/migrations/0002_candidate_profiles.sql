CREATE TABLE IF NOT EXISTS candidate_profiles (
	owner_id TEXT PRIMARY KEY NOT NULL,
	data TEXT NOT NULL,
	updated_at INTEGER NOT NULL
);
