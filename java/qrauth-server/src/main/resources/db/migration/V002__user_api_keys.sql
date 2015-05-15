
    create table DBUserApiKey (
        id  bigserial not null,
        attempts INTEGER DEFAULT 0 not null,
        created TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP not null,
        lastAttempt TIMESTAMP WITHOUT TIME ZONE,
        lastSuccess TIMESTAMP WITHOUT TIME ZONE,
        successes INTEGER DEFAULT 0 not null,
        deadline TIMESTAMP WITHOUT TIME ZONE,
        deathMessage varchar(255),
        description varchar(255),
        hashedApiKey varchar(255) not null unique,
        name varchar(255),
        unhashedApiKey varchar(255),
        user_id int8 not null,
        primary key (id)
    );

ALTER TABLE Tenant
	ADD COLUMN apiAccess4All BOOLEAN DEFAULT 'f' NOT NULL;

ALTER TABLE TenantUser
	ADD COLUMN apiAccess BOOLEAN DEFAULT 'f' NOT NULL;

    alter table DBUserApiKey 
        add constraint FK24E9B0AEE4D1151E 
        foreign key (user_id) 
        references DBUser;

