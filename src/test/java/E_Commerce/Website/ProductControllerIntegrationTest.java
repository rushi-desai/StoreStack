package E_Commerce.Website;

import E_Commerce.Website.dto.ProductDTO;
import E_Commerce.Website.entity.Category;
import E_Commerce.Website.entity.Product;
import E_Commerce.Website.entity.Role;
import E_Commerce.Website.entity.User;
import E_Commerce.Website.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void adminCanCreateReadUpdateAndDeleteProduct() throws Exception {
        User admin = createUser(Role.ADMIN);
        Category category = createCategory();
        ProductDTO createRequest = new ProductDTO(null, category.getId(), "Laptop", "Gaming laptop", new BigDecimal("1200.00"), "https://example.com/laptop.png", 10);

        MvcResult createResult = mockMvc.perform(post("/api/products")
                        .header("Authorization", bearerTokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.categoryId").value(category.getId()))
                .andReturn();

        JsonNode createdProduct = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long productId = createdProduct.get("id").asLong();

        mockMvc.perform(get("/api/products/{id}", productId)
                        .header("Authorization", bearerTokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.categoryId").value(category.getId()));

        mockMvc.perform(put("/api/products/{id}", productId)
                        .header("Authorization", bearerTokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new ProductDTO(null, category.getId(), "Laptop Pro", "Updated laptop", new BigDecimal("1500.00"), "https://example.com/pro.png", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop Pro"))
                .andExpect(jsonPath("$.price").value(1500.0));

        mockMvc.perform(delete("/api/products/{id}", productId)
                        .header("Authorization", bearerTokenFor(admin)))
                .andExpect(status().isNoContent());

        assertThat(productRepository.findById(productId)).isEmpty();
    }

    @Test
    void customerCannotCreateProduct_returnsForbidden() throws Exception {
        User customer = createUser(Role.USER);
        Category category = createCategory();

        mockMvc.perform(post("/api/products")
                        .header("Authorization", bearerTokenFor(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new ProductDTO(null, category.getId(), "Laptop", "Gaming laptop", new BigDecimal("1200.00"), null, 10))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }

    @Test
    void publicProductList_supportsSearchCategoryPriceSortingAndPagination() throws Exception {
        Category category = createCategory();
        ProductDTO headset = new ProductDTO(
                null, category.getId(), "Wireless Headset", "Bluetooth headset",
                new BigDecimal("79.99"), null, 8
        );
        ProductDTO charger = new ProductDTO(
                null, category.getId(), "Wireless Charger", "Fast charger",
                new BigDecimal("29.99"), null, 5
        );
        ProductDTO keyboard = new ProductDTO(
                null, category.getId(), "Wired Keyboard", "Mechanical keyboard",
                new BigDecimal("99.99"), null, 4
        );
        User admin = createUser(Role.ADMIN);
        for (ProductDTO product : new ProductDTO[]{headset, charger, keyboard}) {
            mockMvc.perform(post("/api/products")
                            .header("Authorization", bearerTokenFor(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(product)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/products")
                        .param("name", "wireless")
                        .param("categoryId", category.getId().toString())
                        .param("minPrice", "20")
                        .param("maxPrice", "80")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sortBy", "price")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Wireless Headset"));
    }

    @Test
    void publicProductList_rejectsInvalidPageBoundsAndSortFields() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/products").param("size", "101"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/products").param("sortBy", "password"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void staleProductUpdate_isRejectedByOptimisticLocking() {
        Product product = createProduct(createCategory());
        Product firstRead = productRepository.findById(product.getId()).orElseThrow();
        Product staleRead = productRepository.findById(product.getId()).orElseThrow();
        firstRead.setStock(0);
        productRepository.saveAndFlush(firstRead);
        staleRead.setStock(0);

        assertThatThrownBy(() -> productRepository.saveAndFlush(staleRead))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isZero();
    }
}
