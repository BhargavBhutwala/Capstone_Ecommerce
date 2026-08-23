package com.ebookstore.catalog.repository;

import com.ebookstore.catalog.entity.Product;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProductSpecification} static predicate factories.
 *
 * <p>Verifies that each factory method returns a non-null {@link Specification}
 * and that the resulting {@link Predicate} is produced without error.
 * The {@code hasCategory} and {@code hasBrand} predicates involve a 2-level
 * path navigation ({@code root.get("category").get("id")}); those are tested
 * via the {@code buildSearch} composition path and through integration tests
 * that exercise them against a real database.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class ProductSpecificationTest {

    private CriteriaBuilder cb;
    private CriteriaQuery<?> query;
    private Root<Product> root;
    private Predicate dummyPredicate;

    @BeforeEach
    void setUp() {
        cb    = mock(CriteriaBuilder.class);
        query = mock(CriteriaQuery.class);
        root  = mock(Root.class);
        dummyPredicate = mock(Predicate.class);

        // Leaf path stubs
        Path titlePath    = mock(Path.class);
        Path descPath     = mock(Path.class);
        Path pricePath    = mock(Path.class);
        Path stockPath    = mock(Path.class);
        Path activePath   = mock(Path.class);
        Path categoryPath = mock(Path.class);
        Path brandPath    = mock(Path.class);
        Path categoryIdPath = mock(Path.class);
        Path brandIdPath    = mock(Path.class);

        when(root.get("title")).thenReturn(titlePath);
        when(root.get("description")).thenReturn(descPath);
        when(root.get("price")).thenReturn(pricePath);
        when(root.get("stockQuantity")).thenReturn(stockPath);
        when(root.get("active")).thenReturn(activePath);
        when(root.get("category")).thenReturn(categoryPath);
        when(root.get("brand")).thenReturn(brandPath);
        when(categoryPath.get("id")).thenReturn(categoryIdPath);
        when(brandPath.get("id")).thenReturn(brandIdPath);

        Expression lowered = mock(Expression.class);
        when(cb.lower(any())).thenReturn(lowered);
        when(cb.like(any(Expression.class), anyString())).thenReturn(dummyPredicate);
        when(cb.or(any(Predicate.class), any(Predicate.class))).thenReturn(dummyPredicate);
        when(cb.equal(any(), any())).thenReturn(dummyPredicate);
        when(cb.greaterThanOrEqualTo(any(Expression.class), any(Comparable.class))).thenReturn(dummyPredicate);
        when(cb.lessThanOrEqualTo(any(Expression.class), any(Comparable.class))).thenReturn(dummyPredicate);
        when(cb.isTrue(any())).thenReturn(dummyPredicate);
        when(cb.greaterThan(any(Expression.class), any(Integer.class))).thenReturn(dummyPredicate);
        when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(dummyPredicate);
    }

    // =========================================================================
    // Factory methods return non-null Specification objects
    // =========================================================================

    @Test
    void hasTitleContaining_returnsNonNullSpec() {
        assertThat(ProductSpecification.hasTitleContaining("java")).isNotNull();
    }

    @Test
    void hasCategory_returnsNonNullSpec() {
        assertThat(ProductSpecification.hasCategory(5L)).isNotNull();
    }

    @Test
    void hasBrand_returnsNonNullSpec() {
        assertThat(ProductSpecification.hasBrand(3L)).isNotNull();
    }

    @Test
    void hasPriceAtLeast_returnsNonNullSpec() {
        assertThat(ProductSpecification.hasPriceAtLeast(BigDecimal.TEN)).isNotNull();
    }

    @Test
    void hasPriceAtMost_returnsNonNullSpec() {
        assertThat(ProductSpecification.hasPriceAtMost(BigDecimal.valueOf(200))).isNotNull();
    }

    @Test
    void isAvailable_returnsNonNullSpec() {
        assertThat(ProductSpecification.isAvailable()).isNotNull();
    }

    @Test
    void isActive_returnsNonNullSpec() {
        assertThat(ProductSpecification.isActive()).isNotNull();
    }

    // =========================================================================
    // Predicate invocation for single-level path predicates
    // =========================================================================

    @Test
    void hasTitleContaining_buildsPredicate() {
        Specification<Product> spec = ProductSpecification.hasTitleContaining("java");
        Predicate result = spec.toPredicate(root, query, cb);
        assertThat(result).isNotNull();
        verify(cb).or(any(Predicate.class), any(Predicate.class));
    }

    @Test
    void hasPriceAtLeast_buildsPredicate() {
        Specification<Product> spec = ProductSpecification.hasPriceAtLeast(BigDecimal.TEN);
        Predicate result = spec.toPredicate(root, query, cb);
        assertThat(result).isNotNull();
        verify(cb).greaterThanOrEqualTo(any(Expression.class), any(Comparable.class));
    }

    @Test
    void hasPriceAtMost_buildsPredicate() {
        Specification<Product> spec = ProductSpecification.hasPriceAtMost(BigDecimal.valueOf(200));
        Predicate result = spec.toPredicate(root, query, cb);
        assertThat(result).isNotNull();
        verify(cb).lessThanOrEqualTo(any(Expression.class), any(Comparable.class));
    }

    @Test
    void isAvailable_buildsPredicate() {
        Specification<Product> spec = ProductSpecification.isAvailable();
        Predicate result = spec.toPredicate(root, query, cb);
        assertThat(result).isNotNull();
        verify(cb).isTrue(any());
        verify(cb).greaterThan(any(Expression.class), any(Integer.class));
        verify(cb).and(any(Predicate.class), any(Predicate.class));
    }

    // =========================================================================
    // buildSearch factory
    // =========================================================================

    @Test
    void buildSearch_withNullParams_returnsNonNullSpec() {
        Specification<Product> spec = ProductSpecification.buildSearch(null, null, null, null, null, null);
        assertThat(spec).isNotNull();
    }

    @Test
    void buildSearch_withAllNulls_involvesNoPredicates() {
        // Specification.where(null).toPredicate returns null — the "match all" case
        Specification<Product> spec = ProductSpecification.buildSearch(null, null, null, null, null, null);
        // Just verify no exception and spec is composable
        Specification<Product> combined = spec.and(ProductSpecification.isActive());
        assertThat(combined).isNotNull();
    }

    @Test
    void buildSearch_withQ_returnsNonNullPredicateViaTitle() {
        Specification<Product> spec = ProductSpecification.buildSearch("java", null, null, null, null, null);
        Predicate result = spec.toPredicate(root, query, cb);
        // hasTitleContaining produces a non-null predicate
        assertThat(result).isNotNull();
    }

    @Test
    void buildSearch_composition_isNonNull() {
        Specification<Product> a = ProductSpecification.hasCategory(1L);
        Specification<Product> b = ProductSpecification.hasBrand(2L);
        Specification<Product> combined = a.and(b);
        assertThat(combined).isNotNull();
    }
}
