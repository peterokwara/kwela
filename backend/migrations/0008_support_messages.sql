-- Messages sent from the website's Support page. Read it by hand with wrangler; no route returns these rows.
CREATE TABLE IF NOT EXISTS support_messages (
	id TEXT PRIMARY KEY NOT NULL,
	email TEXT NOT NULL,
	message TEXT NOT NULL,
	created_at INTEGER NOT NULL
);
