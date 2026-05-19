package com.inboxai.security;

import com.inboxai.domain.User;
import com.inboxai.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InboxUserDetailsServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final InboxUserDetailsService service = new InboxUserDetailsService(userRepository);

    @Test
    void loadsUserByEmail() {
        User user = new User("a@example.com", "$2a$10$hash");
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("a@example.com");

        assertThat(details.getUsername()).isEqualTo("a@example.com");
        assertThat(details.getPassword()).isEqualTo("$2a$10$hash");
        assertThat(details.getAuthorities()).isEmpty();
    }

    @Test
    void throwsForMissingUser() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
