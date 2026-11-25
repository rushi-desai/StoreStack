package E_Commerce.Website.services.impl;

import E_Commerce.Website.dto.ProductDTO;
import E_Commerce.Website.entity.Category;
import E_Commerce.Website.entity.Product;
import E_Commerce.Website.exception.ResourceNotFoundException;
import E_Commerce.Website.repository.CategoryRepository;
import E_Commerce.Website.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void createProduct_whenValid_setsCategoryAndReturnsDto() {
        Category category = new Category();
        category.setId(3L);
        Product savedProduct = new Product(1L, "Laptop", "Gaming laptop", new BigDecimal("1200.00"), "img", 10, null, category);
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(productRepository.save(org.mockito.ArgumentMatchers.any(Product.class))).thenReturn(savedProduct);

        ProductDTO response = productService.createProduct(new ProductDTO(null, 3L, "Laptop", "Gaming laptop", new BigDecimal("1200.00"), "img", 10));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo(category);
        assertThat(response.getCategoryId()).isEqualTo(3L);
    }

    @Test
    void getProductById_whenProductMissing_throwsResourceNotFoundException() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");
    }

    @Test
    void deleteProduct_whenProductMissing_throwsResourceNotFoundException() {
        when(productRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");
    }
}
