package E_Commerce.Website.services.impl;

import E_Commerce.Website.dto.UpdateProfileDTO;
import E_Commerce.Website.dto.UserDTO;
import E_Commerce.Website.entity.Role;
import E_Commerce.Website.entity.User;
import E_Commerce.Website.exception.ResourceNotFoundException;
import E_Commerce.Website.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void updateProfile_updatesOnlyProfileFields() {
        User user = new User();
        user.setId(5L);
        user.setName("Old Name");
        user.setEmail("customer@example.com");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserDTO response = userService.updateProfile(
                "customer@example.com",
                new UpdateProfileDTO("New Name", "+1987654321", "New Address")
        );

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getEmail()).isEqualTo("customer@example.com");
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        assertThat(user.getAddress()).isEqualTo("New Address");
        verify(userRepository).save(user);
    }

    @Test
    void getProfile_whenUserDoesNotExist_throwsResourceNotFoundException() {
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile("customer@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }
}
