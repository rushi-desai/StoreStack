package E_Commerce.Website.services.impl;

import E_Commerce.Website.dto.CategoryDTO;
import E_Commerce.Website.entity.Category;
import E_Commerce.Website.exception.ConflictException;
import E_Commerce.Website.exception.ResourceNotFoundException;
import E_Commerce.Website.repository.CategoryRepository;
import E_Commerce.Website.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(new ModelMapper(), categoryRepository, productRepository);
    }

    @Test
    void getCategoryById_whenCategoryMissing_throwsResourceNotFoundException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");
    }

    @Test
    void updateCategory_whenCategoryExists_returnsUpdatedDto() {
        Category category = new Category(1L, "Old", "Old description", null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryDTO response = categoryService.updateCategory(1L, new CategoryDTO(null, "New", "New description"));

        assertThat(response.getName()).isEqualTo("New");
        assertThat(response.getDescription()).isEqualTo("New description");
    }

    @Test
    void deleteCategory_whenProductsExist_throwsConflict() {
        Category category = new Category(1L, "Category", "Description", null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Cannot delete a category that still contains products");
        verify(categoryRepository, org.mockito.Mockito.never()).delete(category);
    }
}
