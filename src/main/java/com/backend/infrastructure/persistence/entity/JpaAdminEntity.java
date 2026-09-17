package com.backend.infrastructure.persistence.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbl_admin")
@PrimaryKeyJoinColumn(name = "user_id")
@DiscriminatorValue("ADMIN")
public class JpaAdminEntity extends JpaUserEntity {
}
