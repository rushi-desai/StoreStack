package E_Commerce.Website.services;


import E_Commerce.Website.dto.LoginRequestDTO;
import E_Commerce.Website.dto.UpdateProfileDTO;
import E_Commerce.Website.dto.UserDTO;

import java.util.List;

public interface UserService {
    UserDTO getProfile(String email);
    UserDTO updateProfile(String email, UpdateProfileDTO updateProfileDTO);
    List<UserDTO> getAllUsers();
}