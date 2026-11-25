package E_Commerce.Website.support;

import E_Commerce.Website.entity.Cart;
import E_Commerce.Website.entity.CartItem;
import E_Commerce.Website.entity.Category;
import E_Commerce.Website.entity.Product;
import E_Commerce.Website.entity.Role;
import E_Commerce.Website.entity.User;
import E_Commerce.Website.repository.CartItemRepository;
import E_Commerce.Website.repository.CartRepository;
import E_Commerce.Website.repository.CategoryRepository;
import E_Commerce.Website.repository.OrderItemRepository;
import E_Commerce.Website.repository.OrderRepository;
import E_Commerce.Website.repository.ProductRepository;
import E_Commerce.Website.repository.RefreshTokenRepository;
import E_Commerce.Website.repository.UserRepository;
import E_Commerce.Website.security.JwtTokenProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected CategoryRepository categoryRepository;

    @Autowired
    protected ProductRepository productRepository;

    @Autowired
    protected CartRepository cartRepository;

    @Autowired
    protected CartItemRepository cartItemRepository;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected OrderItemRepository orderItemRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAllInBatch();
        orderItemRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        cartItemRepository.deleteAllInBatch();
        cartRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    protected User createUser(Role role) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setName(role.name().toLowerCase() + "-user");
        user.setEmail(role.name().toLowerCase() + suffix + "@example.com");
        user.setPassword(passwordEncoder.encode("Password123"));
        user.setPhoneNumber("+1234567890");
        user.setAddress("Test Address");
        user.setRole(role);
        return userRepository.save(user);
    }

    protected Category createCategory() {
        Category category = new Category();
        category.setName("Category-" + UUID.randomUUID().toString().substring(0, 8));
        category.setDescription("Test category");
        category.setProducts(new ArrayList<>());
        return categoryRepository.save(category);
    }

    protected Product createProduct(Category category) {
        Product product = new Product();
        product.setName("Product-" + UUID.randomUUID().toString().substring(0, 8));
        product.setDescription("Test product");
        product.setPrice(new BigDecimal("199.99"));
        product.setImageUrl("https://example.com/image.png");
        product.setStock(20);
        product.setCategory(category);
        return productRepository.save(product);
    }

    protected Cart createCart(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setCartItems(new ArrayList<>());
        return cartRepository.save(cart);
    }

    protected CartItem addCartItem(Cart cart, Product product, int quantity) {
        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(quantity);
        cartItem.setPrice(product.getPrice());
        CartItem savedItem = cartItemRepository.save(cartItem);
        cart.getCartItems().add(savedItem);
        cartRepository.save(cart);
        return savedItem;
    }

    protected String bearerTokenFor(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole().name());
    }

    protected String toJson(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }
}
