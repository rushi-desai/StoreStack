package E_Commerce.Website.controllers;

import E_Commerce.Website.dto.OrderDTO;
import E_Commerce.Website.services.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Customer order APIs")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Place an order for authenticated user")
    @PostMapping("/place")
    public String placeOrder(Authentication authentication) {
        String userEmail = authentication.getName();
        return orderService.placeOrderByEmail(userEmail);
    }

    @Operation(summary = "Get all orders for authenticated user")
    @GetMapping
    public List<OrderDTO> getOrders(Authentication authentication) {
        String userEmail = authentication.getName();
        return orderService.getOrdersByUserEmail(userEmail);
    }

    @Operation(summary = "Get an order owned by the authenticated user")
    @GetMapping("/{orderId}")
    public OrderDTO getOrder(@PathVariable Long orderId, Authentication authentication) {
        return orderService.getOrderById(orderId, authentication.getName());
    }

    @Operation(summary = "Cancel an order that has not shipped")
    @PostMapping("/{orderId}/cancel")
    public OrderDTO cancelOrder(@PathVariable Long orderId, Authentication authentication) {
        return orderService.cancelOrder(orderId, authentication.getName());
    }
}
