package com.backend.infrastructure.persistence.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "customers")
@PrimaryKeyJoinColumn(name = "user_id")
@DiscriminatorValue("CUSTOMER")
@Getter
@Setter
public class JpaCustomerEntity extends JpaUserEntity {
}
