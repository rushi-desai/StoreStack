package E_Commerce.Website.services.impl;

import E_Commerce.Website.dto.LoginRequestDTO;
import E_Commerce.Website.dto.RegisterDTO;
import E_Commerce.Website.entity.User;
import E_Commerce.Website.exception.ConflictException;
import E_Commerce.Website.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_whenEmailAlreadyExists_throwsConflictException() {
        RegisterDTO request = new RegisterDTO("Admin", "Password123", "admin@example.com", "+1234567890", "Address");
        when(userRepository.existsByEmailIgnoreCase(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email already exists");
    }

    @Test
    void register_whenValid_savesUserWithEncodedPassword() {
        RegisterDTO request = new RegisterDTO("Admin", "Password123", "admin@example.com", "+1234567890", "Address");
        when(userRepository.existsByEmailIgnoreCase(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded-password");

        String response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(response).isEqualTo("User registered successfully");
        assertThat(captor.getValue().getEmail()).isEqualTo("admin@example.com");
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(captor.getValue().getRole().name()).isEqualTo("USER");
    }

    @Test
    void login_whenPasswordIsInvalid_throwsGenericAuthenticationFailure() {
        User user = new User();
        user.setEmail("admin@example.com");
        user.setPassword("encoded-password");
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("admin@example.com", "WrongPassword")))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void register_normalizesEmailBeforeSaving() {
        RegisterDTO request = new RegisterDTO("User", "Password123", " User@Example.com ", "+1234567890", "Address");
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded-password");

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("user@example.com");
    }
}
