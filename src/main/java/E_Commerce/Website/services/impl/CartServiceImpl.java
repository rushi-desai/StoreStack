package E_Commerce.Website.services.impl;

import E_Commerce.Website.dto.CartDTO;
import E_Commerce.Website.dto.CartItemDTO;
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
import E_Commerce.Website.services.CartService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public String addToCart(String userEmail, CartResponseDTO cartResponseDTO) {
        if (cartResponseDTO.getQuantity() == null || cartResponseDTO.getQuantity() < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        User user = findUser(userEmail);
        Cart cart = getOrCreateCart(user);
        Product product = productRepository.findById(cartResponseDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(cart);
                    newItem.setProduct(product);
                    newItem.setQuantity(0);
                    return newItem;
                });
        long requestedQuantity = (long) cartItem.getQuantity() + cartResponseDTO.getQuantity();
        ensureStockAvailable(product, requestedQuantity);
        cartItem.setQuantity(Math.toIntExact(requestedQuantity));
        cartItem.setPrice(product.getPrice());
        cartItemRepository.save(cartItem);
        log.info("Added product {} to cart for user {}", product.getId(), user.getId());
        return "Product added to cart successfully";
    }

    @Override
    @Transactional
    public String updateQuantity(String userEmail, Long cartItemId, QuantityUpdateDTO quantityUpdateDTO) {
        if (quantityUpdateDTO.getQuantity() == null || quantityUpdateDTO.getQuantity() < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        CartItem cartItem = findCartItem(userEmail, cartItemId);
        ensureStockAvailable(cartItem.getProduct(), quantityUpdateDTO.getQuantity());
        cartItem.setQuantity(quantityUpdateDTO.getQuantity());
        cartItem.setPrice(cartItem.getProduct().getPrice());
        cartItemRepository.save(cartItem);
        return "Cart item quantity updated";
    }

    @Override
    @Transactional
    public String removeFromCart(String userEmail, Long cartItemId) {
        CartItem cartItem = findCartItem(userEmail, cartItemId);
        cartItemRepository.delete(cartItem);
        log.info("Removed cart item {} for user {}", cartItemId, userEmail);
        return "Product removed from cart successfully";
    }

    @Override
    @Transactional
    public void clearCart(String userEmail) {
        User user = findUser(userEmail);
        cartRepository.findByUserId(user.getId()).ifPresent(cart -> {
            if (cart.getCartItems() != null && !cart.getCartItems().isEmpty()) {
                cartItemRepository.deleteAll(cart.getCartItems());
                cart.getCartItems().clear();
            }
        });
    }

    @Override
    @Transactional
    public CartDTO getCartByUserEmail(String userEmail) {
        return toCartDto(getOrCreateCart(findUser(userEmail)));
    }

    private User findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(user);
            cart.setCartItems(new ArrayList<>());
            return cartRepository.save(cart);
        });
    }

    private CartItem findCartItem(String userEmail, Long cartItemId) {
        return cartItemRepository.findByIdAndCart_User_EmailIgnoreCase(cartItemId, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
    }

    private void ensureStockAvailable(Product product, long quantity) {
        if (product.getStock() == null || quantity > product.getStock()) {
            throw new ConflictException("Insufficient stock for product: " + product.getName());
        }
    }

    private CartDTO toCartDto(Cart cart) {
        List<CartItemDTO> itemDtos = cart.getCartItems() == null
                ? List.of()
                : cart.getCartItems().stream().map(this::toCartItemDto).toList();
        BigDecimal totalAmount = itemDtos.stream()
                .map(CartItemDTO::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartDTO(cart.getId(), cart.getUser().getId(), itemDtos, totalAmount);
    }

    private CartItemDTO toCartItemDto(CartItem cartItem) {
        BigDecimal lineTotal = cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
        return new CartItemDTO(
                cartItem.getId(),
                cartItem.getProduct().getId(),
                cartItem.getProduct().getName(),
                cartItem.getQuantity(),
                cartItem.getPrice(),
                lineTotal
        );
    }
}
