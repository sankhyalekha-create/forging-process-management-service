package com.jangid.forging_process_management_service.entities.security;

import com.jangid.forging_process_management_service.entities.Tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "usr")
@EntityListeners(AuditingEntityListener.class)
public class Usr implements UserDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "usr_key_sequence_generator")
  @SequenceGenerator(name = "usr_key_sequence_generator", sequenceName = "usr_sequence", allocationSize = 1)
  private Long id;

  private String username;
  private String password;

  @ManyToOne
  @JoinColumn(name = "tenant_id")
  private Tenant tenant;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Version
  private LocalDateTime updatedAt;

  private LocalDateTime deletedAt;

  private boolean deleted;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "usr_roles", joinColumns = @JoinColumn(name = "usr_id"))
  @Column(name = "role") // Specify the correct column name
  private Set<String> roles; // e.g., ROLE_ADMIN, ROLE_USER

  // Implement UserDetails methods
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return roles.stream().map(SimpleGrantedAuthority::new).toList();
//    return roles.stream()
//        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
//        .toList();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
