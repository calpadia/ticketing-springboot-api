package com.itsm.ticketing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Represents a B2B client/company in the ITSM system.
 * Each client can have multiple tickets and quota allocations.
 */
@Entity
@Table(name = "clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Company name is required")
    @Column(name = "company_name", nullable = false)
    private String companyName;

    @NotBlank(message = "Contact person name is required")
    @Column(name = "contact_person_name", nullable = false)
    private String contactPersonName;

    @NotBlank(message = "Contact person email is required")
    @Column(name = "contact_person_email", nullable = false)
    private String contactPersonEmail;

    @NotBlank(message = "Contact person phone is required")
    @Column(name = "contact_person_phone", nullable = false)
    private String contactPersonPhone;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
