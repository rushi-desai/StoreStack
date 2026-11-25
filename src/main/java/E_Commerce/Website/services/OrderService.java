package E_Commerce.Website.services;

import E_Commerce.Website.dto.OrderDTO;
import E_Commerce.Website.entity.OrderStatus;

import java.util.List;

public interface OrderService {
    String placeOrder(Long userId);
    String placeOrderByEmail(String userEmail);

    List<OrderDTO> getOrdersByUserId(Long userId);
    List<OrderDTO> getOrdersByUserEmail(String userEmail);
    OrderDTO getOrderById(Long orderId, String userEmail);
    OrderDTO cancelOrder(Long orderId, String userEmail);
    List<OrderDTO> getAllOrders();
    OrderDTO updateOrderStatus(Long orderId, OrderStatus status);
}