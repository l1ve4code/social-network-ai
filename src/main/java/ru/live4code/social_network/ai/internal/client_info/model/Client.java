package ru.live4code.social_network.ai.internal.client_info.model;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.live4code.social_network.ai.generated.model.ClientInfo;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class Client implements UserDetails {

    private final long id;

    @NonNull
    private final String email;

    @Nullable
    private final String name;

    @Nullable
    private final String surname;

    @NonNull
    private final String password;

    private final boolean enabled;

    @NonNull
    private final Role role;

    @NonNull
    private final LocalDateTime createdAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
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
        return enabled;
    }

    public ClientInfo toClientInfo() {
        return new ClientInfo(name, surname, email);
    }

}
