package E_Commerce.Website;

import E_Commerce.Website.dto.LoginRequestDTO;
import E_Commerce.Website.dto.RegisterDTO;
import E_Commerce.Website.dto.UpdateProfileDTO;
import E_Commerce.Website.entity.Role;
import E_Commerce.Website.entity.User;
import E_Commerce.Website.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registerCustomer_withValidPayload_returnsSuccessMessage() throws Exception {
        RegisterDTO request = new RegisterDTO("Customer User", "Password123", "customer@example.com", "+1234567890", "Customer Address");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));

        assertThat(userRepository.findByEmail("customer@example.com")).isPresent();
        assertThat(userRepository.findByEmail("customer@example.com").orElseThrow().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void loginCustomer_withValidCredentials_returnsSuccessMessage() throws Exception {
        createUser(Role.USER);
        String email = userRepository.findAll().get(0).getEmail();

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new LoginRequestDTO(email, "Password123"))))
                .andExpect(status().isOk())
                .andExpect(content().string("User logged in successfully"));
    }

    @Test
    void loginUser_withWrongPassword_returnsUnauthorized() throws Exception {
        createUser(Role.USER);
        String email = userRepository.findAll().get(0).getEmail();

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new LoginRequestDTO(email, "WrongPassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void loginUser_withUnknownEmail_returnsSameGenericUnauthorizedMessage() throws Exception {
        String unknownMessage = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new LoginRequestDTO("unknown@example.com", "Password123"))))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        createUser(Role.USER);
        String email = userRepository.findAll().get(0).getEmail();
        String wrongPasswordMessage = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new LoginRequestDTO(email, "WrongPassword"))))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readTree(unknownMessage).get("message").asText())
                .isEqualTo(objectMapper.readTree(wrongPasswordMessage).get("message").asText());
    }

    @Test
    void userCanReadAndUpdateOwnProfile_butCannotListUsers() throws Exception {
        User user = createUser(Role.USER);
        String authorization = bearerTokenFor(user);

        mockMvc.perform(get("/api/users/me").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateProfileDTO("Updated User", "+1987654321", "Updated Address"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated User"))
                .andExpect(jsonPath("$.address").value("Updated Address"));

        mockMvc.perform(get("/api/users").header("Authorization", authorization))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListUsersWithoutExposingPasswordHashes() throws Exception {
        User admin = createUser(Role.ADMIN);
        createUser(Role.USER);

        mockMvc.perform(get("/api/users").header("Authorization", bearerTokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }
}
