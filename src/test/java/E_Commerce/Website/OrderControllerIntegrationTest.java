package E_Commerce.Website;

import E_Commerce.Website.entity.Cart;
import E_Commerce.Website.entity.Category;
import E_Commerce.Website.entity.OrderStatus;
import E_Commerce.Website.entity.Product;
import E_Commerce.Website.entity.Role;
import E_Commerce.Website.entity.User;
import E_Commerce.Website.dto.UpdateOrderStatusDTO;
import E_Commerce.Website.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void customerCanPlaceOrder_whenCartHasItems() throws Exception {
        User customer = createUser(Role.USER);
        Category category = createCategory();
        Product product = createProduct(category);
        Cart cart = createCart(customer);
        addCartItem(cart, product, 2);
        product.setPrice(new BigDecimal("299.99"));
        productRepository.save(product);

        mockMvc.perform(post("/api/orders/place")
                        .header("Authorization", bearerTokenFor(customer)))
                .andExpect(status().isOk())
                .andExpect(content().string("Order placed successfully "));

        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(cartItemRepository.count()).isZero();
        assertThat(cartRepository.findByUserId(customer.getId()).orElseThrow().getCartItems()).isEmpty();
        E_Commerce.Website.entity.Order order = orderRepository.findAll().get(0);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("599.98");
        assertThat(order.getOrderItems().get(0).getPrice()).isEqualByComparingTo("299.99");
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(18);
        Product repricedProduct = productRepository.findById(product.getId()).orElseThrow();
        repricedProduct.setPrice(new BigDecimal("499.99"));
        productRepository.save(repricedProduct);
        assertThat(orderRepository.findById(order.getId()).orElseThrow()
                .getOrderItems().get(0).getPrice()).isEqualByComparingTo("299.99");
    }

    @Test
    void customerCannotPlaceOrder_whenCartIsEmpty() throws Exception {
        User customer = createUser(Role.USER);
        createCart(customer);

        mockMvc.perform(post("/api/orders/place")
                        .header("Authorization", bearerTokenFor(customer)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cart is empty. No items to order."));
    }

    @Test
    void orderHistoryAndDetailsAreOwnerScoped_andCancellationRestoresStock() throws Exception {
        User owner = createUser(Role.USER);
        User otherUser = createUser(Role.USER);
        Product product = createProduct(createCategory());
        Cart cart = createCart(owner);
        addCartItem(cart, product, 2);
        mockMvc.perform(post("/api/orders/place").header("Authorization", bearerTokenFor(owner)))
                .andExpect(status().isOk());
        E_Commerce.Website.entity.Order order = orderRepository.findAll().get(0);

        mockMvc.perform(get("/api/orders").header("Authorization", bearerTokenFor(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(order.getId()));
        mockMvc.perform(get("/api/orders/{orderId}", order.getId())
                        .header("Authorization", bearerTokenFor(otherUser)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/orders/{orderId}/cancel", order.getId())
                        .header("Authorization", bearerTokenFor(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(20);

        mockMvc.perform(post("/api/orders/{orderId}/cancel", order.getId())
                        .header("Authorization", bearerTokenFor(owner)))
                .andExpect(status().isConflict());
    }

    @Test
    void checkout_whenStockChangesAfterAddingToCart_returnsConflictAndKeepsCart() throws Exception {
        User user = createUser(Role.USER);
        Product product = createProduct(createCategory());
        Cart cart = createCart(user);
        addCartItem(cart, product, 2);
        product.setStock(1);
        productRepository.save(product);

        mockMvc.perform(post("/api/orders/place").header("Authorization", bearerTokenFor(user)))
                .andExpect(status().isConflict());
        assertThat(orderRepository.count()).isZero();
        assertThat(cartItemRepository.count()).isEqualTo(1);
    }

    @Test
    void adminCanAdvanceOrderStatus_andCannotCancelAfterShipping() throws Exception {
        User user = createUser(Role.USER);
        User admin = createUser(Role.ADMIN);
        Product product = createProduct(createCategory());
        Cart cart = createCart(user);
        addCartItem(cart, product, 1);
        mockMvc.perform(post("/api/orders/place").header("Authorization", bearerTokenFor(user)))
                .andExpect(status().isOk());
        E_Commerce.Website.entity.Order order = orderRepository.findAll().get(0);

        mockMvc.perform(get("/api/admin/orders").header("Authorization", bearerTokenFor(user)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/orders").header("Authorization", bearerTokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(order.getId()));

        mockMvc.perform(put("/api/admin/orders/{orderId}/status", order.getId())
                        .header("Authorization", bearerTokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateOrderStatusDTO(OrderStatus.CONFIRMED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        mockMvc.perform(put("/api/admin/orders/{orderId}/status", order.getId())
                        .header("Authorization", bearerTokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateOrderStatusDTO(OrderStatus.SHIPPED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
        mockMvc.perform(put("/api/admin/orders/{orderId}/status", order.getId())
                        .header("Authorization", bearerTokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateOrderStatusDTO(OrderStatus.CANCELLED))))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/admin/orders/{orderId}/status", order.getId())
                        .header("Authorization", bearerTokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateOrderStatusDTO(OrderStatus.DELIVERED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
        mockMvc.perform(put("/api/admin/orders/{orderId}/status", order.getId())
                        .header("Authorization", bearerTokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new UpdateOrderStatusDTO(OrderStatus.CANCELLED))))
                .andExpect(status().isConflict());
    }
}
