package com.inboxai.web;

import com.inboxai.domain.User;
import com.inboxai.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/register")
public class RegistrationController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String form(Model model) {
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new RegistrationForm());
        }
        return "register";
    }

    @PostMapping
    public String register(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                           BindingResult binding) {
        if (binding.hasErrors()) {
            return "register";
        }
        if (userRepository.existsByEmail(form.getEmail())) {
            binding.rejectValue("email", "email.exists", "An account with this email already exists");
            return "register";
        }
        userRepository.save(new User(form.getEmail(), passwordEncoder.encode(form.getPassword())));
        return "redirect:/login?registered";
    }
}
