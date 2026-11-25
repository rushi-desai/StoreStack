package E_Commerce.Website.services;

import E_Commerce.Website.dto.CartDTO;
import E_Commerce.Website.dto.CartResponseDTO;
import E_Commerce.Website.dto.QuantityUpdateDTO;

public interface CartService {
    String addToCart(String userEmail, CartResponseDTO cartResponseDTO);
    String updateQuantity(String userEmail, Long cartItemId, QuantityUpdateDTO quantityUpdateDTO);
    String removeFromCart(String userEmail, Long cartItemId);
    void clearCart(String userEmail);
    CartDTO getCartByUserEmail(String userEmail);
}
