package com.parknet.service;

import com.parknet.dto.RegisterRequest;
import com.parknet.model.Role;
import com.parknet.model.UserAccount;
import com.parknet.repository.UserAccountRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Паролите не съвпадат.");
        }
        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Имейлът вече е регистриран.");
        }

        UserAccount userAccount = new UserAccount(
                request.getFullName().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.getPassword()),
                normalizedEmail,
                Role.USER
        );
        return userAccountRepository.save(userAccount);
    }

    @Transactional(readOnly = true)
    public UserAccount findByEmail(String email) {
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Потребителят не е намерен."));
    }

    @Transactional(readOnly = true)
    public UserAccount currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication)) {
            throw new IllegalStateException("Необходим е вход в системата.");
        }
        return findByEmail(authentication.getName());
    }

    public boolean isCurrentUserAuthenticated() {
        return isAuthenticated(SecurityContextHolder.getContext().getAuthentication());
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getName() != null;
    }
}
