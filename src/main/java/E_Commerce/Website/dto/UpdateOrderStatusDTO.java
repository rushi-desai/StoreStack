package E_Commerce.Website.dto;

import E_Commerce.Website.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateOrderStatusDTO {

    @NotNull(message = "Order status is required")
    private OrderStatus status;

    public UpdateOrderStatusDTO() {
    }

    public UpdateOrderStatusDTO(OrderStatus status) {
        this.status = status;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
