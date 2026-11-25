package E_Commerce.Website;

import E_Commerce.Website.dto.CategoryDTO;
import E_Commerce.Website.entity.Category;
import E_Commerce.Website.entity.Role;
import E_Commerce.Website.entity.User;
import E_Commerce.Website.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void adminCanCreateReadUpdateAndDeleteCategory() throws Exception {
        User admin = createUser(Role.ADMIN);
        CategoryDTO createRequest = new CategoryDTO(null, "Electronics", "Electronic items");

        MvcResult createResult = mockMvc.perform(post("/api/categories")
                        .header("Authorization", bearerTokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andReturn();

        JsonNode createdCategory = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long categoryId = createdCategory.get("id").asLong();

        mockMvc.perform(get("/api/categories/{id}", categoryId)
                        .header("Authorization", bearerTokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId))
                .andExpect(jsonPath("$.name").value("Electronics"));

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .header("Authorization", bearerTokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CategoryDTO(null, "Updated Electronics", "Updated description"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Electronics"));

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", bearerTokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Updated Electronics"));

        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .header("Authorization", bearerTokenFor(admin)))
                .andExpect(status().isOk());

        assertThat(categoryRepository.findById(categoryId)).isEmpty();
    }

    @Test
    void customerCannotCreateCategory_returnsForbidden() throws Exception {
        User customer = createUser(Role.USER);

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", bearerTokenFor(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CategoryDTO(null, "Books", "Books category"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }

    @Test
    void categoriesArePublic_andDeletingCategoryWithProductsConflicts() throws Exception {
        Category category = createCategory();
        createProduct(category);
        User admin = createUser(Role.ADMIN);

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());

        mockMvc.perform(delete("/api/categories/{id}", category.getId())
                        .header("Authorization", bearerTokenFor(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Cannot delete a category that still contains products"));
    }
}
