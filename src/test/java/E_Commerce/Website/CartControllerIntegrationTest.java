package E_Commerce.Website;

import E_Commerce.Website.dto.CartResponseDTO;
import E_Commerce.Website.dto.QuantityUpdateDTO;
import E_Commerce.Website.entity.Category;
import E_Commerce.Website.entity.Product;
import E_Commerce.Website.entity.Role;
import E_Commerce.Website.entity.User;
import E_Commerce.Website.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void customerCanAddViewAndRemoveCartItems() throws Exception {
        User customer = createUser(Role.USER);
        Category category = createCategory();
        Product product = createProduct(category);

        mockMvc.perform(post("/api/carts/add")
                        .header("Authorization", bearerTokenFor(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CartResponseDTO(product.getId(), 2))))
                .andExpect(status().isOk())
                .andExpect(content().string("Product added to cart successfully"));

        MvcResult cartResult = mockMvc.perform(get("/api/carts")
                        .header("Authorization", bearerTokenFor(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(customer.getId()))
                .andExpect(jsonPath("$.cartItems[0].productId").value(product.getId()))
                .andExpect(jsonPath("$.cartItems[0].quantity").value(2))
                .andExpect(jsonPath("$.totalAmount").value(399.98))
                .andReturn();

        JsonNode cart = objectMapper.readTree(cartResult.getResponse().getContentAsString());
        long cartItemId = cart.get("cartItems").get(0).get("id").asLong();

        mockMvc.perform(delete("/api/carts/remove/{cartItemId}", cartItemId)
                        .header("Authorization", bearerTokenFor(customer)))
                .andExpect(status().isOk())
                .andExpect(content().string("Product removed from cart successfully"));

        mockMvc.perform(get("/api/carts")
                        .header("Authorization", bearerTokenFor(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItems").isEmpty());
    }

    @Test
    void adminCannotAccessCustomerCart_returnsForbidden() throws Exception {
        User admin = createUser(Role.ADMIN);
        User customer = createUser(Role.USER);

        mockMvc.perform(get("/api/carts")
                        .header("Authorization", bearerTokenFor(admin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }

    @Test
    void addToCart_withInvalidQuantity_returnsValidationError() throws Exception {
        User customer = createUser(Role.USER);
        Category category = createCategory();
        Product product = createProduct(category);

        mockMvc.perform(post("/api/carts/add")
                        .header("Authorization", bearerTokenFor(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CartResponseDTO(product.getId(), 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.quantity").value("Quantity must be at least 1"));
    }

    @Test
    void cartOwnerCanUpdateAndClearItems_butAnotherUserCannotModifyThem() throws Exception {
        User owner = createUser(Role.USER);
        User otherUser = createUser(Role.USER);
        Product product = createProduct(createCategory());
        mockMvc.perform(post("/api/carts/add")
                        .header("Authorization", bearerTokenFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CartResponseDTO(product.getId(), 2))))
                .andExpect(status().isOk());
        JsonNode cart = objectMapper.readTree(mockMvc.perform(get("/api/carts")
                        .header("Authorization", bearerTokenFor(owner)))
                .andReturn().getResponse().getContentAsString());
        long itemId = cart.get("cartItems").get(0).get("id").asLong();

        mockMvc.perform(put("/api/carts/items/{cartItemId}", itemId)
                        .header("Authorization", bearerTokenFor(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new QuantityUpdateDTO(3))))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/carts/items/{cartItemId}", itemId)
                        .header("Authorization", bearerTokenFor(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new QuantityUpdateDTO(3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItems[0].quantity").value(3))
                .andExpect(jsonPath("$.totalAmount").value(599.97));

        mockMvc.perform(delete("/api/carts").header("Authorization", bearerTokenFor(owner)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/carts").header("Authorization", bearerTokenFor(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItems").isEmpty())
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    void addToCart_whenQuantityExceedsStock_returnsConflict() throws Exception {
        User user = createUser(Role.USER);
        Product product = createProduct(createCategory());
        product.setStock(1);
        productRepository.save(product);

        mockMvc.perform(post("/api/carts/add")
                        .header("Authorization", bearerTokenFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CartResponseDTO(product.getId(), 2))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Insufficient stock for product: " + product.getName()));
    }
}
