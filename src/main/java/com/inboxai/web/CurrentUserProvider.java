package com.inboxai.web;

import com.inboxai.domain.User;
import com.inboxai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single-user placeholder for the current request's user. Slice 7 replaces
 * this with a SecurityContext-aware implementation.
 */
@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;
    private final String defaultEmail;

    public CurrentUserProvider(UserRepository userRepository,
                               @Value("${inboxai.default-user-email:default@inboxai.local}") String defaultEmail) {
        this.userRepository = userRepository;
        this.defaultEmail = defaultEmail;
    }

    @Transactional
    public User get() {
        return userRepository.findByEmail(defaultEmail)
                .orElseGet(() -> userRepository.save(new User(defaultEmail, "")));
    }
}
