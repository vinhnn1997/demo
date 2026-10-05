package vn.gov.tax.dataplatform.source.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "data_source_credentials")
@Getter
@Setter
@NoArgsConstructor
public class SourceCredential {
    @Id
    @Column(name = "secret_ref", nullable = false, length = 80)
    private String secretRef;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "encrypted_password", nullable = false, length = 512)
    private String encryptedPassword;

    public SourceCredential(String secretRef, String tenantId, String encryptedPassword) {
        this.secretRef = secretRef;
        this.tenantId = tenantId;
        this.encryptedPassword = encryptedPassword;
    }
}
