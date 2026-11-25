package E_Commerce.Website.services.impl;

import E_Commerce.Website.dto.UpdateProfileDTO;
import E_Commerce.Website.dto.UserDTO;
import E_Commerce.Website.entity.User;
import E_Commerce.Website.exception.ResourceNotFoundException;
import E_Commerce.Website.repository.UserRepository;
import E_Commerce.Website.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserDTO getProfile(String email) {
        return toUserDto(findByEmail(email));
    }

    @Override
    @Transactional
    public UserDTO updateProfile(String email, UpdateProfileDTO updateProfileDTO) {
        User user = findByEmail(email);
        user.setName(updateProfileDTO.getName());
        user.setPhoneNumber(updateProfileDTO.getPhoneNumber());
        user.setAddress(updateProfileDTO.getAddress());
        return toUserDto(userRepository.save(user));
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserDto)
                .toList();
    }

    private User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserDTO toUserDto(User user) {
        return new UserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getPhoneNumber(),
                user.getAddress()
        );
    }
}
