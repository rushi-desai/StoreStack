package E_Commerce.Website.services.impl;

import E_Commerce.Website.entity.*;
import E_Commerce.Website.dto.OrderDTO;
import E_Commerce.Website.dto.OrderItemDTO;
import E_Commerce.Website.exception.ConflictException;
import E_Commerce.Website.exception.ResourceNotFoundException;
import E_Commerce.Website.repository.*;
import E_Commerce.Website.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    @Override
    public String placeOrder(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(()
                -> new ResourceNotFoundException("Cart not found for user ID: " + userId));
        List<CartItem> cartItems = cart.getCartItems();
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty. No items to order.");
        }
        // Validate every line before changing inventory.
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (cartItem.getQuantity() == null || cartItem.getQuantity() < 1) {
                throw new ConflictException("Cart contains an invalid quantity");
            }
            if (product.getStock() == null || product.getStock() < cartItem.getQuantity()) {
                throw new ConflictException("Insufficient stock for product: " + product.getName());
            }
        }

        Order order = new Order();
        order.setUser(cart.getUser());
        order.setStatus(OrderStatus.PENDING);
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setTotalAmount(BigDecimal.ZERO);

        Order savedOrder = orderRepository.save(order);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            orderItemRepository.save(orderItem);
        }

        savedOrder.setTotalAmount(totalAmount);
        orderRepository.save(savedOrder);

        cartItemRepository.deleteAll(cartItems);
        cart.getCartItems().clear();
        cartRepository.save(cart);
        log.info("Placed order {} for user {} with total {}", savedOrder.getId(), userId, totalAmount);

        return "Order placed successfully ";
    }

    @Override
    public String placeOrderByEmail(String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));
        return placeOrder(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByUserId(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(this::toOrderDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByUserEmail(String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return getOrdersByUserId(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId, String userEmail) {
        Order order = orderRepository.findByIdAndUser_EmailIgnoreCase(orderId, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toOrderDto(order);
    }

    @Override
    @Transactional
    public OrderDTO cancelOrder(Long orderId, String userEmail) {
        Order order = orderRepository.findByIdAndUser_EmailIgnoreCase(orderId, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        transitionOrder(order, OrderStatus.CANCELLED);
        return toOrderDto(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream().map(this::toOrderDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        transitionOrder(order, status);
        return toOrderDto(orderRepository.save(order));
    }

    private void transitionOrder(Order order, OrderStatus nextStatus) {
        if (order.getStatus() == null || nextStatus == null) {
            throw new ConflictException("Order status is not valid");
        }
        boolean validTransition = switch (order.getStatus()) {
            case PENDING -> nextStatus == OrderStatus.CONFIRMED || nextStatus == OrderStatus.CANCELLED;
            case CONFIRMED -> nextStatus == OrderStatus.SHIPPED || nextStatus == OrderStatus.CANCELLED;
            case SHIPPED -> nextStatus == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
        if (!validTransition) {
            throw new ConflictException(
                    "Order cannot transition from " + order.getStatus() + " to " + nextStatus
            );
        }

        if (nextStatus == OrderStatus.CANCELLED) {
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    Product product = item.getProduct();
                    if (product.getStock() == null || item.getQuantity() == null || item.getQuantity() < 1) {
                        throw new ConflictException("Product stock is not available for restocking");
                    }
                    long restockedQuantity = (long) product.getStock() + item.getQuantity();
                    if (restockedQuantity > Integer.MAX_VALUE) {
                        throw new ConflictException("Product stock limit would be exceeded");
                    }
                    product.setStock((int) restockedQuantity);
                    productRepository.save(product);
                }
            }
        }
        order.setStatus(nextStatus);
    }

    private OrderDTO toOrderDto(Order order) {
        List<OrderItemDTO> orderItems = order.getOrderItems() == null
                ? new ArrayList<>()
                : order.getOrderItems().stream()
                .map(this::toOrderItemDto)
                .collect(Collectors.toList());

        return new OrderDTO(
                order.getId(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getUser().getId(),
                orderItems
        );
    }

    private OrderItemDTO toOrderItemDto(OrderItem orderItem) {
        return new OrderItemDTO(
                orderItem.getId(),
                orderItem.getProduct().getId(),
                orderItem.getQuantity(),
                orderItem.getPrice()
        );
    }
}
