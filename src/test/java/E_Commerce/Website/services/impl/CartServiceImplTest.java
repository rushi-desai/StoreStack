package E_Commerce.Website.services.impl;

import E_Commerce.Website.dto.CartDTO;
import E_Commerce.Website.dto.CartResponseDTO;
import E_Commerce.Website.dto.QuantityUpdateDTO;
import E_Commerce.Website.entity.Cart;
import E_Commerce.Website.entity.CartItem;
import E_Commerce.Website.entity.Product;
import E_Commerce.Website.entity.User;
import E_Commerce.Website.exception.ConflictException;
import E_Commerce.Website.exception.ResourceNotFoundException;
import E_Commerce.Website.repository.CartItemRepository;
import E_Commerce.Website.repository.CartRepository;
import E_Commerce.Website.repository.ProductRepository;
import E_Commerce.Website.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    @Test
    void addToCart_whenCartDoesNotExist_createsCartAndCartItem() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        Product product = new Product();
        product.setId(5L);
        product.setName("Laptop");
        product.setPrice(new BigDecimal("100.00"));
        product.setStock(5);
        Cart cart = new Cart();
        cart.setId(9L);
        cart.setUser(user);
        cart.setCartItems(new ArrayList<>());
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(9L, 5L)).thenReturn(Optional.empty());

        String response = cartService.addToCart("user@example.com", new CartResponseDTO(5L, 2));

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(response).isEqualTo("Product added to cart successfully");
        assertThat(captor.getValue().getQuantity()).isEqualTo(2);
        assertThat(captor.getValue().getPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void getCartByUserEmail_whenCartExists_mapsCartToDto() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        Product product = new Product();
        product.setId(5L);
        product.setName("Laptop");
        CartItem item = new CartItem(7L, null, product, 2, new BigDecimal("100.00"));
        Cart cart = new Cart();
        cart.setId(9L);
        cart.setUser(user);
        cart.setCartItems(new ArrayList<>());
        cart.getCartItems().add(item);
        item.setCart(cart);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        CartDTO response = cartService.getCartByUserEmail("user@example.com");

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getCartItems()).hasSize(1);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    void removeFromCart_whenCartItemMissing_throwsResourceNotFoundException() {
        when(cartItemRepository.findByIdAndCart_User_EmailIgnoreCase(99L, "user@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeFromCart("user@example.com", 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cart item not found");
    }

    @Test
    void updateQuantity_whenRequestedQuantityExceedsStock_throwsConflict() {
        Product product = new Product();
        product.setName("Laptop");
        product.setStock(3);
        CartItem item = new CartItem();
        item.setProduct(product);
        when(cartItemRepository.findByIdAndCart_User_EmailIgnoreCase(10L, "user@example.com"))
                .thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.updateQuantity(
                "user@example.com", 10L, new QuantityUpdateDTO(4)
        )).isInstanceOf(ConflictException.class)
                .hasMessage("Insufficient stock for product: Laptop");
    }
}
