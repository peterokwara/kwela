-- People who asked to test Kwela from the website. Read it by hand with wrangler; no route returns these rows.
-- The email and phone number are not verified, so one row per pair is kept and nothing is overwritten:
-- a stranger cannot change the number stored for someone else's email.
CREATE TABLE IF NOT EXISTS tester_signups (
	email TEXT NOT NULL,
	phone TEXT NOT NULL,
	consent_at INTEGER NOT NULL,
	created_at INTEGER NOT NULL,
	PRIMARY KEY (email, phone)
);
