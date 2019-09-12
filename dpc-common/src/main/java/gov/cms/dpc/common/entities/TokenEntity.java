package gov.cms.dpc.common.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.cms.dpc.common.converters.OffsetDateTimeToStringConverter;
import gov.cms.dpc.common.converters.StringToOffsetDateTimeConverter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity(name = "organization_tokens")
public class TokenEntity implements Serializable {
    public static final long serialVersionUID = 42L;

    public enum TokenType {
        MACAROON,
        OAUTH
    }

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @ManyToOne
    @JsonIgnore
    private OrganizationEntity organization;

    @Column(name = "type")
    private TokenType tokenType;

    @Column
    private String label;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
    @JsonDeserialize(converter = StringToOffsetDateTimeConverter.class)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
    @JsonDeserialize(converter = StringToOffsetDateTimeConverter.class)
    private OffsetDateTime expiresAt;


    public TokenEntity() {
        // Hibernate required
    }

    public TokenEntity(String tokenID, OrganizationEntity organization, TokenType type) {
        this.id = tokenID;
        this.organization = organization;
        this.tokenType = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public OrganizationEntity getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationEntity organization) {
        this.organization = organization;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TokenEntity)) return false;
        TokenEntity that = (TokenEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(organization, that.organization) &&
                tokenType == that.tokenType &&
                Objects.equals(label, that.label) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(expiresAt, that.expiresAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, organization, tokenType, label, createdAt, expiresAt);
    }

    @Override
    public String toString() {
        return "TokenEntity{" +
                "id='" + id + '\'' +
                ", organization=" + organization +
                ", tokenType=" + tokenType +
                ", label='" + label + '\'' +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
