IF OBJECT_ID(N'dbo.data_execution', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.data_execution (
        id uniqueidentifier NOT NULL PRIMARY KEY,
        created_at datetime2(7) NOT NULL,
        updated_at datetime2(7) NOT NULL,
        version bigint NOT NULL,
        tenant_id varchar(100) NOT NULL,
        pipeline_id uniqueidentifier NOT NULL,
        airflow_dag_run_id varchar(160) NOT NULL UNIQUE,
        status varchar(32) NOT NULL,
        started_at datetime2(7) NULL,
        finished_at datetime2(7) NULL,
        status_message varchar(500) NULL,
        tasks_json nvarchar(max) NOT NULL,
        CONSTRAINT fk_execution_pipeline FOREIGN KEY (pipeline_id) REFERENCES dbo.data_pipeline(id)
    );
END;