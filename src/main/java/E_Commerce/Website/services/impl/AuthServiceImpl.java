package E_Commerce.Website.services.impl;

import E_Commerce.Website.dto.LoginRequestDTO;
import E_Commerce.Website.dto.RegisterDTO;
import E_Commerce.Website.entity.Role;
import E_Commerce.Website.entity.User;
import E_Commerce.Website.exception.ConflictException;
import E_Commerce.Website.repository.UserRepository;
import E_Commerce.Website.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public String register(RegisterDTO registerDTO) {
        String email = registerDTO.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.warn("Registration rejected for duplicate email {}", email);
            throw new ConflictException("Email already exists");
        }

        User user = new User();
        user.setName(registerDTO.getName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setPhoneNumber(registerDTO.getPhone());
        user.setAddress(registerDTO.getAddress());
        user.setRole(Role.USER);
        userRepository.save(user);
        log.info("Customer account registered for {}", user.getEmail());
        return "User registered successfully";
    }

    @Override
    public String login(LoginRequestDTO loginRequestDTO) {
        String email = loginRequestDTO.getEmail().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
            log.warn("Login failed for {}", email);
            throw new BadCredentialsException("Invalid email or password");
        }

        log.info("Login succeeded for {}", user.getEmail());
        return "User logged in successfully";
    }
}
