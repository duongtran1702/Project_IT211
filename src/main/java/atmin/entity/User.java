package atmin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;

@Entity
@Table(name = "users",
        indexes = @Index(name = "idx_username",
                columnList = "username",
                unique = true)
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "update users set is_deleted = true, updated_at = NOW(), is_enabled = false where id = ?")
@SQLRestriction("is_deleted = false")
public class User extends BaseEntity implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String username;
    private String password;
    private String fullName;
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Builder.Default
    @Column(name = "is_enabled")
    private boolean isEnabled = true;

    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Role> roles;

    @Transient
    private Collection<? extends GrantedAuthority> authorities;

    @Override
    @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    @NullMarked
    public String getUsername() {
        return this.username;
    }

    @Override
    @NullMarked
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @NullMarked
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @NullMarked
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @NullMarked
    public boolean isEnabled() {
        return this.isEnabled && !this.isDeleted();
    }
}