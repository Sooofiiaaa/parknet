package com.parknet.service;

import com.parknet.dto.RegisterRequest;
import com.parknet.model.Role;
import com.parknet.model.UserAccount;
import com.parknet.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserAccountServiceTest {

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = registerRequest("Ива Николова", " IVA@example.com ", "secret123", "secret123");

        assertThatThrownBy(() -> userAccountService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("вече е регистриран");
    }

    @Test
    void registerEncodesPassword() {
        String rawPassword = "secret123";
        RegisterRequest request = registerRequest(
                "Мария Георгиева",
                " maria.tests@example.com ",
                rawPassword,
                rawPassword
        );

        UserAccount createdUser = userAccountService.register(request);
        UserAccount persistedUser = userAccountRepository.findByEmail("maria.tests@example.com").orElseThrow();

        assertThat(createdUser.getRole()).isEqualTo(Role.USER);
        assertThat(persistedUser.getPasswordHash()).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, persistedUser.getPasswordHash())).isTrue();
    }

    @Test
    void registerRejectsPasswordMismatch() {
        RegisterRequest request = registerRequest(
                "Петър Димитров",
                " petar.tests@example.com ",
                "secret123",
                "different123"
        );

        assertThatThrownBy(() -> userAccountService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не съвпадат");
    }

    private RegisterRequest registerRequest(String fullName, String email, String password, String confirmPassword) {
        RegisterRequest request = new RegisterRequest();
        request.setFullName(fullName);
        request.setEmail(email);
        request.setPassword(password);
        request.setConfirmPassword(confirmPassword);
        return request;
    }
}
