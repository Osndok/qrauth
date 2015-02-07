
--
-- Sketch of qrauth-web database schema
-- qrauth is a multi-tenant authentication system
--

--
-- NB: 'user' is a reserved word in SQL, so we avoid it... using 'person' instead.
-- config might include: "singleSignOff"?
--

create table person
(
	id           SERIAL,
	created      TIMESTAMP WITHOUT TIMEZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	globalLogout TIMESTAMP WITHOUT TIMEZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

	-- NB: username is initially *NULLABLE* !!! and therefore optional, but probably not *changable* (once assigned)
	username VARCHAR(255) UNIQUE,

	level TINYINT NOT NULL,

	lastAttempt    TIMESTAMP WITHOUT TIMEZONE,
	lastSuccess    TIMESTAMP WITHOUT TIMEZONE,
	attempts       INTEGER   NOT NULL DEFAULT 0,
	successes      INTEGER   NOT NULL DEFAULT 0,

	deathMessage   VARCHAR(255),
	deadline       TIMESTAMP WITHOUT TIMEZONE,

	config         VARCHAR(2550) NOT NULL DEFAULT '{}',
);

--
-- This is a running log of account activity. It *must* include each time the system grants access to a tenant site,
-- as well as every time an authentication method is added/removed/updated, except when the update is to support a
-- stateful authentication method (usually incrementing a counter).
--
-- Although this is called 'personlog', we may allow tenants to *read* this log if they are also the subject.
--

create table personlog
(
	id BIGSERIAL,
	time TIMESTAMP WITHOUT TIMEZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	person_id      INTEGER NOT NULL REFERENCES person(id),
	method_id      INTEGER REFERENCES method(id),
	tenant_id      INTEGER REFERENCES tenant(id),
	actionKey      VARCHAR( 25) NOT NULL,
	message        VARCHAR(255) NOT NULL,
	userSeen       BOOLEAN NOT NULL DEFAULT 'f',
	tenantSeen     BOOLEAN NOT NULL DEFAULT 'f',
	important      BOOLEAN NOT NULL DEFAULT 'f',
);


--
-- For some methods (such as SQRL, RSA, and yubikey), a username is not strictly required... therefor
-- we let the user decide if they should further restrict the authentication method to require a username
-- by the 'requireUsername' flag... which defaults to false for creating-an-account safety (as they may not
-- *have* a username yet!).
--
-- For yubikey, the pubkey shall be the static prefix of the key.
--
-- For ssh keys (like RSA) the type, secret, and comment fields shall be the same format as the commonly available
-- id_rsa.pub file (e.g. type = 'ssh-rsa', comment = 'user@host").
--

create table method
(
	id             SERIAL,
	created        TIMESTAMP WITHOUT TIMEZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	person_id      INTEGER   NOT NULL REFERENCES person(id),
	lastAttempt    TIMESTAMP WITHOUT TIMEZONE,
	lastSuccess    TIMESTAMP WITHOUT TIMEZONE,
	attempts       INTEGER   NOT NULL DEFAULT 0,
	successes      INTEGER   NOT NULL DEFAULT 0,

	millisGranted  INTEGER,

	deathMessage   VARCHAR(255),
	deadline       TIMESTAMP WITHOUT TIMEZONE,

	type           VARCHAR(10) NOT NULL,
	secret         VARCHAR(255),
	comment        VARCHAR(255),

	pubkey VARCHAR(2048) UNIQUE,

);


--
-- Since the api-key is effectively a server-to-server password (or shared secret), we absolutely
-- *MUST* have it hashed. However, since it is also a primary lookup key... we cannot have it
-- salted. :( To compromise, we will provide a method of automatically rolling over to new
-- api keys without service interuption.
--

create table tenant
(
	id             SERIAL,
	created        TIMESTAMP WITHOUT TIMEZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	contact        INTEGER REFERENCES person(id),
	hashedApiKeyPrimary   VARCHAR(255) UNIQUE NOT NULL,
	hashedApiKeySecondary VARCHAR(255) UNIQUE NOT NULL,

	lastAttempt    TIMESTAMP WITHOUT TIMEZONE,
	lastSuccess    TIMESTAMP WITHOUT TIMEZONE,
	attempts       INTEGER   NOT NULL DEFAULT 0,
	successes      INTEGER   NOT NULL DEFAULT 0,

	deathMessage   VARCHAR(255),
	deadline       TIMESTAMP WITHOUT TIMEZONE,

	anonRegister   BOOLEAN NOT NULL DEFAULT 'f',

	fieldDescriptions VARCHAR(25000) NOT NULL DEFAULT '{}',
);

--
-- the tenant-person record is totally at the control of the tenant (or the holder of his/her api key)
-- the tenant can read and write every json field, even the 'authAdmin' flag (which is one of three fields we help 
-- them out with), even if the user/person is logged off!
--
-- for the sake of transparency, we may grant users *read-only* access to this record... particularly with the
-- upsurge of privacy-related issues, and that subordinate sites might wholly rely on this mechanism as their
-- user-table.
--
-- if the tenant sets the deadline *OR* deathMessage flag, the user is effectively banned from the tenant's website,
-- and from deleting their tenant person (which would remove the ban).
--
-- authAdmin has the 'auth' prefix to separate it from what the *tenant* might consider to be an administrator (like, one
-- who is able to edit pages and create user accounts) that they might not want to have full control over their authentication
-- system. Also, it is a separate field so that (1) we can key-into it without decoding the json, (2) that a bulk-set will
-- not remove it, and (3) because we want to always keep in mind that it is *special* and highly sensitive.
--
-- There is yet one more level of protection, however... in the same way that the first person to present a tenant API key
-- becomes that tenant's *primary*, a "regular" auth-admin cannot change (or demote) that primary's tenantperson
-- (a last line of defense against a coup from a recently-blessed admin).
--

create table tenantperson
(
	person_id      INTEGER NOT NULL REFERENCES person(id),
	tenant_id      INTEGER NOT NULL REFERENCES tenant(id),

	authAdmin      BOOLEAN NOT NULL DEFAULT 'f',
	deathMessage   VARCHAR(255),
	deadline       TIMESTAMP WITHOUT TIMEZONE,

	config         VARCHAR(2550) NOT NULL DEFAULT '{}',

	PRIMARY KEY (persion_id, tenant_id)
);

--
-- the tenant-session both tracks login attempts and allows us to connect the tenant's session id
-- with "our" session id (which requires cookies). Since we encourage tenant-side hashing of the
-- session token, there is really no reason to store it hashed here too.
--
-- The user_id is nullable, because during the login process we know about the session *before* we
-- ever know about (or authenticate) the user.
--
-- We may allow a user to 'obsolete' any/all active sessions, but we can't really force tenants to
-- abide with that, if they want sessions to linger. Instead, as part of the protocol, we will have
-- a special return value if the session has already reached it's deadline. Also, we will adjust the
-- deadline to be the sooner of the user's auth method (as indicated by the cookie or login action),
-- their global logout (as indicated in their user record), or the tenant-provided session expiry
-- (plus a small margin for transit latency).
--
create table tenantsession
(
	id          BIGSERIAL,
	tenant_id   INTEGER      NOT NULL REFERENCES tenant(id),
	session_id  VARCHAR(255) NOT NULL,
	noticed     TIMESTAMP    WITHOUT TIMEZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	deadline    TIMESTAMP    WITHOUT TIMEZONE NOT NULL,

	person_id   INTEGER   REFERENCES person(id),
	connected   TIMESTAMP WITHOUT TIMEZONE,
);

INDEX idx_tenantside_session ON tenantsession(session_id);

--
-- nuts are alphanumeric blobs used by the sqrl & rsa methods, and are sensitive to replay attacks so we must
-- ensure that they are verifiable and only claimed once.
--
-- we mark a nut as having been used by setting the person field, that we can make a note in that person's log
--
-- it is expected that each nut have an embedded timestamp, so it is probably safe to delete really-old nuts
-- once they would no longer be accepted.
--

create table nuts
(
	id            SERIAL,
	created       TIMESTAMP WITHOUT TIMEZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	deadline      TIMESTAMP WITHOUT TIMEZONE NOT NULL,
	encodedString VARCHAR(255) NOT NULL UNIQUE,
	person_id     INTEGER REFERENCES person(id),
);

-- a place to persist timing infos, mostly to *try* and avoid timing attacks by measuring our own performance
create table stats
(
	id SERIAL,
	name,
	min,
	max,
	count,
	sum
);

