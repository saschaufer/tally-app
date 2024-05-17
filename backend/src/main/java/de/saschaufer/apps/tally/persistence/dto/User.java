package de.saschaufer.apps.tally.persistence.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User implements UserDetails {

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Role {
        public static final String NONE = "none";
        public static final String USER = "user";
        public static final String ADMIN = "admin";
        public static final String INVITATION = "invitation";
    }

    @Id
    private Long id;
    private String username;
    private String password;
    private String roles;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Arrays.stream(this.roles.split(","))
                .map(SimpleGrantedAuthority::new)
                .toList();
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
