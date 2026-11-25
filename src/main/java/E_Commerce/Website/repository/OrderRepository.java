package E_Commerce.Website.repository;

import E_Commerce.Website.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"user", "orderItems", "orderItems.product"})
    List<Order> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "orderItems", "orderItems.product"})
    Optional<Order> findByIdAndUser_EmailIgnoreCase(Long id, String email);

    @EntityGraph(attributePaths = {"user", "orderItems", "orderItems.product"})
    Optional<Order> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"user", "orderItems", "orderItems.product"})
    List<Order> findAll();
}