/**
 * Persistent sales-order model. Each object maps to a row in the sales_orders database table.
 * Public fields are also read/written by Jackson when controllers receive or return order JSON.
 */
package com.lankaswisstek.salesorder.features.salesorder.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.domain.Persistable;
import jakarta.validation.constraints.Size;

import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

// @Entity enables database mapping; @Table sets the table name instead of relying on a default.
@Entity
@Table(name = "sales_orders")
public class SalesOrder implements Persistable<String> {
    // The canonical quote number is the actual database primary key, not a generated order ID.
    @Id
    @NotBlank
    @Size(max = 255)
    @Column(name = "quote_number", nullable = false, length = 255)
    public String quoteNumber;

    @NotBlank public String customerName;
    // Original filename for display/download; the next field holds its data URL text.
    public String quotationName;
    // PostgreSQL TEXT stores long data URL strings directly, rather than using a large-object ID.
    @Column(columnDefinition = "TEXT") public String quotationData;
    // Approved aluminium finish plus its optional image filename and image data.
    public String aluminiumColour;
    public String aluminiumImageName;
    @Column(columnDefinition = "TEXT") public String aluminiumImageData;
    // Mesh is optional; this flag controls whether mesh details are relevant to the order.
    public boolean pleatedMeshAvailable;
    public String pleatedMeshColour;
    public String meshImageName;
    @Column(columnDefinition = "TEXT") public String meshImageData;
    // PostgreSQL TEXT also allows long free-form handover instructions.
    @Column(columnDefinition = "TEXT") public String remarks;
    // Required salesperson name; this is stored as text rather than a foreign-key relationship.
    @NotBlank public String salesPerson;
    // Calendar date chosen by the user. These fields are currently strings, not Java date types.
    @NotBlank public String punchInDate;
    // Creation timestamp sent by the frontend, normally an ISO UTC value ending in Z.
    @NotBlank public String createdAt;

    // Persistable makes new POSTs use INSERT, so concurrent duplicates cannot overwrite an order.
    @Transient
    private boolean newRecord = true;

    @Override
    @JsonIgnore
    public String getId() { return quoteNumber; }

    @Override
    @JsonIgnore
    public boolean isNew() { return newRecord; }

    @PostLoad
    @PostPersist
    public void markPersisted() { newRecord = false; }

    // JPA and JSON conversion need a no-argument constructor to create an empty instance.
    public SalesOrder() {}
}
