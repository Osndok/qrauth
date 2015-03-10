
    create table DBGroup (
        id  serial not null,
        created TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP not null,
        deadline TIMESTAMP WITHOUT TIME ZONE,
        deathMessage varchar(255),
        name varchar(255) not null,
        nameRedux varchar(255) not null unique,
        shellGroup BOOLEAN DEFAULT 'f' not null,
        owner_id int8,
        primary key (id)
    );

    create table DBUser (
        id  bigserial not null,
        attempts INTEGER DEFAULT 0 not null,
        created TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP not null,
        lastAttempt TIMESTAMP WITHOUT TIME ZONE,
        lastSuccess TIMESTAMP WITHOUT TIME ZONE,
        successes INTEGER DEFAULT 0 not null,
        deadline TIMESTAMP WITHOUT TIME ZONE,
        deathMessage varchar(255),
        displayName varchar(255),
        epoch INTEGER DEFAULT 0 not null,
        globalLogout TIMESTAMP WITHOUT TIME ZONE not null,
        preferencesJson VARCHAR(2000) DEFAULT '{}' not null,
        verifiedEmail varchar(255) unique,
        lastLoginIP_id int8,
        primary key (id)
    );

    create table DBUserAuth (
        id  bigserial not null,
        attempts INTEGER DEFAULT 0 not null,
        created TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP not null,
        lastAttempt TIMESTAMP WITHOUT TIME ZONE,
        lastSuccess TIMESTAMP WITHOUT TIME ZONE,
        successes INTEGER DEFAULT 0 not null,
        authMethod VARCHAR(20) not null,
        comment varchar(255),
        deadline TIMESTAMP WITHOUT TIME ZONE,
        deathMessage varchar(255),
        disclose_csv VARCHAR(30),
        idRecoveryLock varchar(255),
        millisGranted int4,
        pubKey varchar(2048) unique,
        secret varchar(255),
        silentAlarm BOOLEAN DEFAULT 'f' not null,
        lastTenantSession_id int8,
        user_id int8 not null,
        primary key (id)
    );

    create table LogEntry (
        id  bigserial not null,
        actionKey varchar(25) not null,
        deadline TIMESTAMP WITHOUT TIME ZONE,
        important BOOLEAN DEFAULT 'f' not null,
        message varchar(255) not null,
        tenantSeen BOOLEAN DEFAULT 'f' not null,
        time TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP not null,
        userSeen BOOLEAN DEFAULT 'f' not null,
        tenant_id int8,
        tenantIP_id int8,
        tenantSession_id int8,
        user_id int8,
        userAuth_id int8,
        username_id int8,
        primary key (id)
    );

    create table Nut (
        id  bigserial not null,
        command varchar(255),
        created TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP not null,
        deadline TIMESTAMP WITHOUT TIME ZONE,
        deathMessage varchar(255),
        mutex varchar(255),
        semiSecretValue varchar(30) not null,
        stringValue varchar(30) not null unique,
        tenantIP_id int8 not null,
        tenantSession_id int8,
        user_id int8,
        primary key (id)
    );

    create table Tenant (
        id  bigserial not null,
        attempts INTEGER DEFAULT 0 not null,
        created TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP not null,
        lastAttempt TIMESTAMP WITHOUT TIME ZONE,
        lastSuccess TIMESTAMP WITHOUT TIME ZONE,
        successes INTEGER DEFAULT 0 not null,
        config VARCHAR(2000) DEFAULT '{}' not null,
        deadline TIMESTAMP WITHOUT TIME ZONE,
        deathMessage varchar(255),
        fieldDescriptionsJson VARCHAR(25000) DEFAULT '{}' not null,
        hashedApiKeyPrimary varchar(255) not null unique,
        hashedApiKeySecondary varchar(255) not null unique,
        name varchar(255) unique,
        nameRedux varchar(255) unique,
        newUsers BOOLEAN DEFAULT 't' not null,
        permissionsDescriptionsJson VARCHAR(25000) DEFAULT '{}' not null,
        qrauthHostAndPort varchar(255) unique,
        unhashedShellKey varchar(255) unique,
        url varchar(255) unique,
        urlRedux varchar(255) unique,
        primaryContact_id int8,
        tenantIP_id int8,
        primary key (id)
    );

    create table TenantGroup (
        id  serial not null,
        created TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP not null,
        customName varchar(255),
        permissionsCsv VARCHAR(2000) not null,
        DBGroup_id int4 not null,
        tenant_id int8 not null,
        primary key (id)
    );

    create table TenantGroupMember (
        id  serial not null,
        created TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP not null,
        tenantGroup_id int4 not null,
        user_id int8 not null,
        primary key (id)
    );

    create table TenantIP (
        id  bigserial not null,
        attempts INTEGER DEFAULT 0 not null,
        created TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP not null,
        lastAttempt TIMESTAMP WITHOUT TIME ZONE,
        lastSuccess TIMESTAMP WITHOUT TIME ZONE,
        successes INTEGER DEFAULT 0 not null,
        deadline TIMESTAMP WITHOUT TIME ZONE,
        deathMessage varchar(255),
        ipAddress varchar(45) not null,
        tenant_id int8,
        primary key (id)
    );

    create table TenantSession (
        id  bigserial not null,
        connected TIMESTAMP WITHOUT TIME ZONE,
        deadline TIMESTAMP WITHOUT TIME ZONE,
        deathMessage varchar(255),
        noticed TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP not null,
        return_url varchar(255),
        session_id varchar(255) not null,
        tenant_id int8 not null,
        tenantIP_id int8 not null,
        user_id int8,
        userAuth_id int8,
        username_id int8,
        primary key (id)
    );

    create table TenantSync (
        id  bigserial not null,
        authMethod varchar(255) not null,
        clearTime TIMESTAMP WITHOUT TIME ZONE,
        effectiveTime TIMESTAMP WITHOUT TIME ZONE not null,
        requestString varchar(255) not null,
        userAtHost varchar(255) not null,
        userName varchar(255) not null,
        tenant_id int8 not null,
        primary key (id)
    );

    create table TenantUser (
        id  bigserial not null,
        attempts INTEGER DEFAULT 0 not null,
        created TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP not null,
        lastAttempt TIMESTAMP WITHOUT TIME ZONE,
        lastSuccess TIMESTAMP WITHOUT TIME ZONE,
        successes INTEGER DEFAULT 0 not null,
        authAdmin BOOLEAN DEFAULT 'f' not null,
        configJson VARCHAR(2000) DEFAULT '{}' not null,
        deadline TIMESTAMP WITHOUT TIME ZONE,
        deathMessage varchar(255),
        shellAccess BOOLEAN DEFAULT 'f' not null,
        tenant_id int8 not null,
        user_id int8 not null,
        username_id int8,
        primary key (id)
    );

    create table TimingStat (
        id  serial not null,
        count int8 not null,
        created TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP not null,
        max int8 not null,
        min int8 not null,
        name varchar(255) not null unique,
        recent_csv VARCHAR(2000) not null,
        sum int8 not null,
        primary key (id)
    );

    create table Username (
        id  bigserial not null,
        attempts INTEGER DEFAULT 0 not null,
        created TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP not null,
        lastAttempt TIMESTAMP WITHOUT TIME ZONE,
        lastSuccess TIMESTAMP WITHOUT TIME ZONE,
        successes INTEGER DEFAULT 0 not null,
        deadline TIMESTAMP WITHOUT TIME ZONE,
        deathMessage varchar(255),
        displayValue varchar(255) not null unique,
        matchValue varchar(255) not null unique,
        silentAlarm BOOLEAN DEFAULT 'f' not null,
        user_id int8,
        primary key (id)
    );

    alter table DBGroup 
        add constraint FK81E59CA16DFDA097 
        foreign key (owner_id) 
        references Tenant;

    alter table DBUser 
        add constraint FK77D3F969F1EA4817 
        foreign key (lastLoginIP_id) 
        references TenantIP;

    alter table DBUserAuth 
        add constraint FK11166EB1E4D1151E 
        foreign key (user_id) 
        references DBUser;

    alter table DBUserAuth 
        add constraint FK11166EB114009C2A 
        foreign key (lastTenantSession_id) 
        references TenantSession;

    alter table LogEntry 
        add constraint FK7A7043AEBD5D763E 
        foreign key (userAuth_id) 
        references DBUserAuth;

    alter table LogEntry 
        add constraint FK7A7043AEE4D1151E 
        foreign key (user_id) 
        references DBUser;

    alter table LogEntry 
        add constraint FK7A7043AEF35D7A40 
        foreign key (username_id) 
        references Username;

    alter table LogEntry 
        add constraint FK7A7043AE93A9F594 
        foreign key (tenantSession_id) 
        references TenantSession;

    alter table LogEntry 
        add constraint FK7A7043AE9C6CD340 
        foreign key (tenant_id) 
        references Tenant;

    alter table LogEntry 
        add constraint FK7A7043AE35152060 
        foreign key (tenantIP_id) 
        references TenantIP;

    alter table Nut 
        add constraint FK1336DE4D1151E 
        foreign key (user_id) 
        references DBUser;

    alter table Nut 
        add constraint FK1336D93A9F594 
        foreign key (tenantSession_id) 
        references TenantSession;

    alter table Nut 
        add constraint FK1336D35152060 
        foreign key (tenantIP_id) 
        references TenantIP;

    alter table Tenant 
        add constraint FK9519D4CAE679742B 
        foreign key (primaryContact_id) 
        references DBUser;

    alter table Tenant 
        add constraint FK9519D4CA35152060 
        foreign key (tenantIP_id) 
        references TenantIP;

    alter table TenantGroup 
        add constraint FKD2BB9FD5117A4914 
        foreign key (DBGroup_id) 
        references DBGroup;

    alter table TenantGroup 
        add constraint FKD2BB9FD59C6CD340 
        foreign key (tenant_id) 
        references Tenant;

    alter table TenantGroupMember 
        add constraint FK25FF22CFE4D1151E 
        foreign key (user_id) 
        references DBUser;

    alter table TenantGroupMember 
        add constraint FK25FF22CFFB7F1F74 
        foreign key (tenantGroup_id) 
        references TenantGroup;

    create index idx_tenant_ip on TenantIP (ipAddress);

    alter table TenantIP 
        add constraint FKB5F7D3719C6CD340 
        foreign key (tenant_id) 
        references Tenant;

    create index idx_tenantsession_id on TenantSession (session_id);

    alter table TenantSession 
        add constraint FK77262E6CBD5D763E 
        foreign key (userAuth_id) 
        references DBUserAuth;

    alter table TenantSession 
        add constraint FK77262E6CE4D1151E 
        foreign key (user_id) 
        references DBUser;

    alter table TenantSession 
        add constraint FK77262E6CF35D7A40 
        foreign key (username_id) 
        references Username;

    alter table TenantSession 
        add constraint FK77262E6C9C6CD340 
        foreign key (tenant_id) 
        references Tenant;

    alter table TenantSession 
        add constraint FK77262E6C35152060 
        foreign key (tenantIP_id) 
        references TenantIP;

    create index idx_tenantsync_clearTime on TenantSync (clearTime);

    alter table TenantSync 
        add constraint FK1755EE859C6CD340 
        foreign key (tenant_id) 
        references Tenant;

    alter table TenantUser 
        add constraint FK1756BFB5E4D1151E 
        foreign key (user_id) 
        references DBUser;

    alter table TenantUser 
        add constraint FK1756BFB5F35D7A40 
        foreign key (username_id) 
        references Username;

    alter table TenantUser 
        add constraint FK1756BFB59C6CD340 
        foreign key (tenant_id) 
        references Tenant;

    alter table Username 
        add constraint FKF403ECF6E4D1151E 
        foreign key (user_id) 
        references DBUser;
