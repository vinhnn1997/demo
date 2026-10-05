IF OBJECT_ID(N'dbo.audit_logs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.audit_logs (
        id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
        service_name nvarchar(120) NOT NULL,
        action nvarchar(120) NOT NULL,
        request_uri nvarchar(500) NULL,
        username nvarchar(150) NULL,
        http_status int NULL,
        duration_ms bigint NULL,
        error_message nvarchar(1000) NULL,
        created_at datetime2(7) NOT NULL
    );

END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.audit_logs')
      AND name = N'idx_audit_logs_created_at'
)
    CREATE INDEX idx_audit_logs_created_at ON dbo.audit_logs(created_at);

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.audit_logs')
      AND name = N'idx_audit_logs_username'
)
    CREATE INDEX idx_audit_logs_username ON dbo.audit_logs(username);