package com.ebookstore.catalog.service;

import com.ebookstore.catalog.CatalogMapper;
import com.ebookstore.catalog.dto.ProductResponse;
import com.ebookstore.catalog.dto.ProductSummary;
import com.ebookstore.catalog.entity.Brand;
import com.ebookstore.catalog.entity.Category;
import com.ebookstore.catalog.entity.Product;
import com.ebookstore.catalog.repository.ProductRepository;
import com.ebookstore.common.dto.PagedResponse;
import com.ebookstore.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProductService}.
 * No Spring context – ProductRepository is mocked.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    // =========================================================================
    // searchProducts – filter delegation
    // =========================================================================

    @Test
    void searchProducts_delegatesToRepositoryWithSpec() {
        Product p = buildProduct(1L, "Java Basics", true, 5);
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(p)));

        PagedResponse<ProductSummary> result =
                productService.searchProducts(null, null, null, null, null, true, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Java Basics");
        assertThat(result.getPage().getTotalElements()).isEqualTo(1);
    }

    @Test
    void searchProducts_withQFilter_callsRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        PagedResponse<ProductSummary> result =
                productService.searchProducts("java", null, null, null, null, null, pageable);

        verify(productRepository).findAll(any(Specification.class), eq(pageable));
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void searchProducts_withCategoryFilter_callsRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        productService.searchProducts(null, 2L, null, null, null, null, pageable);

        verify(productRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchProducts_withBrandFilter_callsRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        productService.searchProducts(null, null, 3L, null, null, null, pageable);

        verify(productRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchProducts_withPriceFilters_callsRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        productService.searchProducts(null, null, null,
                BigDecimal.valueOf(10), BigDecimal.valueOf(100), null, pageable);

        verify(productRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchProducts_availableOnly_filterApplied() {
        Product available = buildProduct(1L, "In Stock", true, 3);
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(available)));

        PagedResponse<ProductSummary> result =
                productService.searchProducts(null, null, null, null, null, true, pageable);

        assertThat(result.getContent()).allMatch(ProductSummary::isAvailable);
    }

    // =========================================================================
    // getProduct
    // =========================================================================

    @Test
    void getProduct_found_returnsProductResponse() {
        Product p = buildProduct(7L, "Spring Boot", true, 10);
        when(productRepository.findById(7L)).thenReturn(Optional.of(p));

        ProductResponse response = productService.getProduct(7L);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getTitle()).isEqualTo("Spring Boot");
        assertThat(response.isAvailable()).isTrue();
    }

    @Test
    void getProduct_notFound_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getProduct_inactive_availableIsFalse() {
        Product p = buildProduct(5L, "Old Edition", false, 0);
        when(productRepository.findById(5L)).thenReturn(Optional.of(p));

        ProductResponse response = productService.getProduct(5L);

        assertThat(response.isAvailable()).isFalse();
    }

    // =========================================================================
    // getRelatedProducts
    // =========================================================================

    @Test
    void getRelatedProducts_returnsSameCategoryFirst() {
        Product source = buildProduct(1L, "Java 1", true, 5);
        Product related = buildProduct(2L, "Java 2", true, 3);
        when(productRepository.findById(1L)).thenReturn(Optional.of(source));
        when(productRepository.findByCategoryIdAndActiveTrueAndIdNot(
                source.getCategory().getId(), 1L))
                .thenReturn(List.of(related));
        when(productRepository.findByBrandIdAndActiveTrueAndIdNot(
                source.getBrand().getId(), 1L))
                .thenReturn(List.of());

        List<ProductSummary> result = productService.getRelatedProducts(1L, 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(2L);
    }

    @Test
    void getRelatedProducts_supplementsFromBrand_whenCategoryInsufficient() {
        Product source = buildProduct(1L, "Java 1", true, 5);
        Product brandBook = buildProduct(3L, "Brand Book", true, 2);
        when(productRepository.findById(1L)).thenReturn(Optional.of(source));
        when(productRepository.findByCategoryIdAndActiveTrueAndIdNot(
                source.getCategory().getId(), 1L))
                .thenReturn(List.of()); // no category results
        when(productRepository.findByBrandIdAndActiveTrueAndIdNot(
                source.getBrand().getId(), 1L))
                .thenReturn(List.of(brandBook));

        List<ProductSummary> result = productService.getRelatedProducts(1L, 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(3L);
    }

    @Test
    void getRelatedProducts_excludesSelf() {
        Product source = buildProduct(1L, "Java 1", true, 5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(source));
        when(productRepository.findByCategoryIdAndActiveTrueAndIdNot(
                source.getCategory().getId(), 1L))
                .thenReturn(List.of()); // repo already excludes self via query
        when(productRepository.findByBrandIdAndActiveTrueAndIdNot(
                source.getBrand().getId(), 1L))
                .thenReturn(List.of());

        List<ProductSummary> result = productService.getRelatedProducts(1L, 5);

        assertThat(result).isEmpty();
        assertThat(result.stream().map(ProductSummary::getId)).doesNotContain(1L);
    }

    @Test
    void getRelatedProducts_respectsSizeLimit() {
        Product source = buildProduct(1L, "Book", true, 5);
        List<Product> many = List.of(
                buildProduct(2L, "B2", true, 1),
                buildProduct(3L, "B3", true, 1),
                buildProduct(4L, "B4", true, 1),
                buildProduct(5L, "B5", true, 1),
                buildProduct(6L, "B6", true, 1),
                buildProduct(7L, "B7", true, 1));
        when(productRepository.findById(1L)).thenReturn(Optional.of(source));
        // Category provides 6 results — brand query is never reached when size=3
        when(productRepository.findByCategoryIdAndActiveTrueAndIdNot(
                source.getCategory().getId(), 1L)).thenReturn(many);

        List<ProductSummary> result = productService.getRelatedProducts(1L, 3);

        assertThat(result).hasSize(3);
    }

    @Test
    void getRelatedProducts_excludesOutOfStock() {
        Product source = buildProduct(1L, "Book", true, 5);
        Product outOfStock = buildProduct(2L, "OOS", true, 0); // stock = 0
        when(productRepository.findById(1L)).thenReturn(Optional.of(source));
        when(productRepository.findByCategoryIdAndActiveTrueAndIdNot(
                source.getCategory().getId(), 1L))
                .thenReturn(List.of(outOfStock));
        when(productRepository.findByBrandIdAndActiveTrueAndIdNot(
                source.getBrand().getId(), 1L)).thenReturn(List.of());

        List<ProductSummary> result = productService.getRelatedProducts(1L, 5);

        assertThat(result).isEmpty();
    }

    @Test
    void getRelatedProducts_sourceNotFound_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getRelatedProducts(99L, 5))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Product buildProduct(Long id, String title, boolean active, int stock) {
        Category cat = new Category();
        cat.setId(10L);
        cat.setName("Test Category");
        cat.setActive(true);

        Brand brand = new Brand();
        brand.setId(20L);
        brand.setName("Test Brand");
        brand.setActive(true);

        Product p = new Product();
        p.setId(id);
        p.setTitle(title);
        p.setPrice(BigDecimal.valueOf(99.99));
        p.setActive(active);
        p.setStockQuantity(stock);
        p.setCategory(cat);
        p.setBrand(brand);
        return p;
    }
}
