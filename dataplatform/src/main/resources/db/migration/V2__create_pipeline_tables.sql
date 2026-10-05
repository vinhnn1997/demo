IF OBJECT_ID(N'dbo.data_pipeline', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.data_pipeline (
        id uniqueidentifier NOT NULL PRIMARY KEY,
        created_at datetime2(7) NOT NULL,
        updated_at datetime2(7) NOT NULL,
        version bigint NOT NULL,
        tenant_id varchar(100) NOT NULL,
        name varchar(160) NOT NULL,
        source_id uniqueidentifier NOT NULL,
        definition_json nvarchar(max) NOT NULL,
        CONSTRAINT uk_pipeline_tenant_name UNIQUE (tenant_id, name),
        CONSTRAINT fk_pipeline_source FOREIGN KEY (source_id) REFERENCES dbo.data_source(id)
    );
END;