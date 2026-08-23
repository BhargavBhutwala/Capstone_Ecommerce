package com.ebookstore.catalog.service;

import com.ebookstore.catalog.CatalogMapper;
import com.ebookstore.catalog.dto.CategorySummary;
import com.ebookstore.catalog.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only service for category catalog operations.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategorySummary> listActiveCategories() {
        return categoryRepository.findByActiveTrue()
                .stream()
                .map(CatalogMapper::toSummary)
                .toList();
    }
}
