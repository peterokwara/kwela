CREATE TABLE IF NOT EXISTS accounts (
	id TEXT PRIMARY KEY NOT NULL,
	email TEXT NOT NULL UNIQUE,
	created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS login_codes (
	email TEXT PRIMARY KEY NOT NULL,
	code_hash TEXT NOT NULL,
	expires_at INTEGER NOT NULL,
	attempts INTEGER NOT NULL DEFAULT 0,
	window_start INTEGER NOT NULL,
	request_count INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS sessions (
	token_hash TEXT PRIMARY KEY NOT NULL,
	account_id TEXT NOT NULL,
	created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS sessions_account_idx ON sessions (account_id);
