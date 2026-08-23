package com.ebookstore.catalog.service;

import com.ebookstore.catalog.CatalogMapper;
import com.ebookstore.catalog.dto.BrandSummary;
import com.ebookstore.catalog.repository.BrandRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only service for brand catalog operations.
 */
@Service
public class BrandService {

    private final BrandRepository brandRepository;

    public BrandService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Transactional(readOnly = true)
    public List<BrandSummary> listActiveBrands() {
        return brandRepository.findByActiveTrue()
                .stream()
                .map(CatalogMapper::toSummary)
                .toList();
    }
}
