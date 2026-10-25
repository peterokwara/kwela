CREATE TABLE IF NOT EXISTS login_rate_limits (
	limiter_key TEXT PRIMARY KEY NOT NULL,
	window_start INTEGER NOT NULL,
	request_count INTEGER NOT NULL
);
