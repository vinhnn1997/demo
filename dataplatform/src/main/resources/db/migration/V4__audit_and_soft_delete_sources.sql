IF COL_LENGTH(N'dbo.data_source', N'created_by') IS NULL
    ALTER TABLE dbo.data_source ADD created_by varchar(255) NULL;

IF COL_LENGTH(N'dbo.data_source', N'updated_by') IS NULL
    ALTER TABLE dbo.data_source ADD updated_by varchar(255) NULL;

IF COL_LENGTH(N'dbo.data_source', N'deleted_at') IS NULL
    ALTER TABLE dbo.data_source ADD deleted_at datetime2(7) NULL;

IF COL_LENGTH(N'dbo.data_source', N'deleted_by') IS NULL
    ALTER TABLE dbo.data_source ADD deleted_by varchar(255) NULL;

EXEC sys.sp_executesql N'UPDATE dbo.data_source SET created_by = ''system'' WHERE created_by IS NULL;';
EXEC sys.sp_executesql N'UPDATE dbo.data_source SET updated_by = ''system'' WHERE updated_by IS NULL;';

EXEC sys.sp_executesql N'ALTER TABLE dbo.data_source ALTER COLUMN created_by varchar(255) NOT NULL;';
EXEC sys.sp_executesql N'ALTER TABLE dbo.data_source ALTER COLUMN updated_by varchar(255) NOT NULL;';