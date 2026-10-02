IF OBJECT_ID(N'dbo.data_source', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.data_source (
        id uniqueidentifier NOT NULL PRIMARY KEY,
        created_at datetime2(7) NOT NULL,
        updated_at datetime2(7) NOT NULL,
        version bigint NOT NULL,
        tenant_id varchar(100) NOT NULL,
        name varchar(120) NOT NULL,
        type varchar(32) NOT NULL,
        description varchar(500) NULL,
        status varchar(16) NOT NULL,
        CONSTRAINT uk_source_tenant_name UNIQUE (tenant_id, name)
    );
END;

IF OBJECT_ID(N'dbo.data_source_connection', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.data_source_connection (
        source_id uniqueidentifier NOT NULL PRIMARY KEY,
        host varchar(255) NOT NULL,
        port int NOT NULL,
        database_name varchar(128) NOT NULL,
        schema_name varchar(128) NULL,
        username varchar(256) NOT NULL,
        encrypt bit NOT NULL,
        trust_server_certificate bit NOT NULL,
        secret_ref varchar(80) NOT NULL,
        CONSTRAINT fk_source_connection_source
            FOREIGN KEY (source_id) REFERENCES dbo.data_source(id)
    );
END;

IF COL_LENGTH(N'dbo.data_source', N'host') IS NOT NULL
BEGIN
    INSERT INTO dbo.data_source_connection (
        source_id, host, port, database_name, schema_name, username,
        encrypt, trust_server_certificate, secret_ref
    )
    SELECT
        source.id, source.host, source.port, source.database_name, source.schema_name,
        source.username, source.encrypt, source.trust_server_certificate, source.secret_ref
    FROM dbo.data_source AS source
    WHERE NOT EXISTS (
        SELECT 1
        FROM dbo.data_source_connection AS connection_profile
        WHERE connection_profile.source_id = source.id
    );

    ALTER TABLE dbo.data_source DROP COLUMN
        host, port, database_name, schema_name, username,
        encrypt, trust_server_certificate, secret_ref;
END;

IF OBJECT_ID(N'dbo.data_source_credentials', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.data_source_credentials (
        secret_ref varchar(80) NOT NULL PRIMARY KEY,
        tenant_id varchar(100) NOT NULL,
        encrypted_password varchar(512) NOT NULL
    );
END;
