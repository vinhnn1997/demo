package vn.gov.tax.dataplatform.source.domain;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "data_source_connection")
@Getter
@Setter
@NoArgsConstructor
public class SourceConnection {
    @Id
    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Column(nullable = false, length = 255)
    private String host;

    @Column(nullable = false)
    private int port;

    @Column(name = "database_name", nullable = false, length = 128)
    private String databaseName;

    @Column(name = "schema_name", length = 128)
    private String schemaName;

    @Column(nullable = false, length = 256)
    private String username;

    @Column(nullable = false)
    private boolean encrypt;

    @Column(name = "trust_server_certificate", nullable = false)
    private boolean trustServerCertificate;

    @Column(name = "secret_ref", nullable = false, length = 80)
    private String secretRef;
}
