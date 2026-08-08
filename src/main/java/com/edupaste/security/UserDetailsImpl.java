package com.edupaste.security;

import com.edupaste.models.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String fullName;

    private String email;

    @JsonIgnore
    private String password;

    private Collection<? extends GrantedAuthority> authorities;
    
    private String role;
    
    private Long schoolId;

    private Boolean mustResetPassword;

    public Long getSchoolId() {
        if (this.schoolId == null && !"SUPER_ADMIN".equalsIgnoreCase(this.role)) {
            return this.id != null ? this.id : 1L;
        }
        return this.schoolId;
    }

    public static UserDetailsImpl build(User user) {
        List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(user.getRole().name()));
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        Long assignedSchoolId = user.getSchoolId();
        if (assignedSchoolId == null && user.getRole() != com.edupaste.models.Role.SUPER_ADMIN) {
            assignedSchoolId = user.getId() != null ? user.getId() : 1L;
        }

        return new UserDetailsImpl(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPassword(),
                authorities,
                user.getRole().name(),
                assignedSchoolId,
                Boolean.TRUE.equals(user.getMustResetPassword()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return id.equals(user.id);
    }
}
