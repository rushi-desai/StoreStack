package E_Commerce.Website.controllers;

import E_Commerce.Website.dto.LoginRequestDTO;
import E_Commerce.Website.dto.RegisterDTO;
import E_Commerce.Website.dto.UpdateProfileDTO;
import E_Commerce.Website.dto.UserDTO;
import E_Commerce.Website.services.AuthService;
import E_Commerce.Website.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
@Tag(name = "Users", description = "User profile and administration endpoints")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @Operation(summary = "Register a user account")
    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterDTO registerDTO) {
        return authService.register(registerDTO);
    }

    @Operation(summary = "Validate user login credentials")
    @PostMapping("/login")
    public String login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        return authService.login(loginRequestDTO);
    }

    @Operation(summary = "Get the authenticated user's profile")
    @GetMapping("/me")
    public UserDTO getProfile(Authentication authentication) {
        return userService.getProfile(authentication.getName());
    }

    @Operation(summary = "Update the authenticated user's profile")
    @PutMapping("/me")
    public UserDTO updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileDTO updateProfileDTO
    ) {
        return userService.updateProfile(authentication.getName(), updateProfileDTO);
    }

    @Operation(summary = "List users (admin only)")
    @GetMapping
    public List<UserDTO> getAllUsers() {
        return userService.getAllUsers();
    }
}
