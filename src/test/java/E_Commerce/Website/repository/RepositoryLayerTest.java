package E_Commerce.Website.repository;

import E_Commerce.Website.entity.Cart;
import E_Commerce.Website.entity.CartItem;
import E_Commerce.Website.entity.Category;
import E_Commerce.Website.entity.Product;
import E_Commerce.Website.entity.RefreshToken;
import E_Commerce.Website.entity.Role;
import E_Commerce.Website.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RepositoryLayerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void userRepositoryFindByEmailAndExistsByEmail_workAsExpected() {
        User user = new User();
        user.setName("User");
        user.setEmail("user@example.com");
        user.setPassword("encoded-password");
        user.setPhoneNumber("+1234567890");
        user.setAddress("Address");
        user.setRole(Role.USER);
        userRepository.save(user);

        assertThat(userRepository.findByEmail("user@example.com")).isPresent();
        assertThat(userRepository.existsByEmail("user@example.com")).isTrue();
    }

    @Test
    void cartRepositoryFindByUserId_loadsCartItemsAndProducts() {
        User user = new User();
        user.setName("User");
        user.setEmail("cart@example.com");
        user.setPassword("encoded-password");
        user.setPhoneNumber("+1234567890");
        user.setAddress("Address");
        user.setRole(Role.USER);
        userRepository.save(user);

        Category category = new Category();
        category.setName("Electronics");
        category.setDescription("Category");
        category.setProducts(new ArrayList<>());
        categoryRepository.save(category);

        Product product = new Product();
        product.setName("Laptop");
        product.setDescription("Gaming laptop");
        product.setPrice(new BigDecimal("1200.00"));
        product.setImageUrl("img");
        product.setStock(10);
        product.setCategory(category);
        productRepository.save(product);

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setCartItems(new ArrayList<>());
        cartRepository.save(cart);

        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
        cartItem.setPrice(new BigDecimal("1200.00"));
        cartItemRepository.save(cartItem);
        cart.getCartItems().add(cartItem);
        cartRepository.save(cart);

        Cart loadedCart = cartRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(loadedCart.getCartItems()).hasSize(1);
        assertThat(loadedCart.getCartItems().get(0).getProduct().getName()).isEqualTo("Laptop");
    }

    @Test
    void refreshTokenRepositoryFindMethods_workAsExpected() {
        User user = new User();
        user.setName("User");
        user.setEmail("refresh@example.com");
        user.setPassword("encoded-password");
        user.setPhoneNumber("+1234567890");
        user.setAddress("Address");
        user.setRole(Role.USER);
        userRepository.save(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        refreshToken.setExpiryDate(Instant.now().plusSeconds(300));
        refreshToken.setUser(user);
        refreshTokenRepository.save(refreshToken);

        assertThat(refreshTokenRepository.findByToken("refresh-token")).isPresent();
        assertThat(refreshTokenRepository.findByUserId(user.getId())).isPresent();
    }
}
