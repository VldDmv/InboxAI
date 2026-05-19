package com.inboxai.web;

import com.inboxai.domain.User;
import com.inboxai.repository.UserRepository;
import com.inboxai.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(RegistrationController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @Test
    void getRendersForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void postWithValidFormCreatesUserWithBcryptedPassword() throws Exception {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        mockMvc.perform(post("/register").with(csrf())
                        .param("email", "new@example.com")
                        .param("password", "longenough"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getPasswordHash())
                .startsWith("$2") // BCrypt prefix
                .isNotEqualTo("longenough");
    }

    @Test
    void postWithDuplicateEmailShowsError() throws Exception {
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        mockMvc.perform(post("/register").with(csrf())
                        .param("email", "dup@example.com")
                        .param("password", "longenough"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("already exists")));

        verify(userRepository, never()).save(any());
    }

    @Test
    void postWithShortPasswordShowsValidationError() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("email", "ok@example.com")
                        .param("password", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));

        verify(userRepository, never()).save(any());
    }
}
