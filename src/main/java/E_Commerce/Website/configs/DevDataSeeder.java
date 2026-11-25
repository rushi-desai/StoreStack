package E_Commerce.Website.configs;

import E_Commerce.Website.entity.Category;
import E_Commerce.Website.entity.Product;
import E_Commerce.Website.entity.Role;
import E_Commerce.Website.entity.User;
import E_Commerce.Website.repository.CategoryRepository;
import E_Commerce.Website.repository.ProductRepository;
import E_Commerce.Website.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.email}")
    private String adminEmail;

    @Value("${app.seed.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        seedAdmin();
        Category electronics = createCategoryIfMissing("Electronics", "Devices and accessories");
        Category books = createCategoryIfMissing("Books", "Books for learning and leisure");
        createProductIfMissing("Wireless Headphones", "Bluetooth over-ear headphones", "79.99", 25, electronics);
        createProductIfMissing("Java Fundamentals", "A practical introduction to Java", "39.99", 40, books);
    }

    private void seedAdmin() {
        if (userRepository.existsByEmailIgnoreCase(adminEmail)) {
            return;
        }

        User admin = new User();
        admin.setName("StoreStack Admin");
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
    }

    private Category createCategoryIfMissing(String name, String description) {
        return categoryRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Category category = new Category();
            category.setName(name);
            category.setDescription(description);
            return categoryRepository.save(category);
        });
    }

    private void createProductIfMissing(
            String name,
            String description,
            String price,
            int stock,
            Category category
    ) {
        if (productRepository.existsByNameIgnoreCase(name)) {
            return;
        }

        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setCategory(category);
        productRepository.save(product);
    }
}
