package com.ebookstore.catalog;

import com.ebookstore.catalog.dto.BrandSummary;
import com.ebookstore.catalog.dto.CategorySummary;
import com.ebookstore.catalog.dto.DeliveryEstimate;
import com.ebookstore.catalog.dto.ProductResponse;
import com.ebookstore.catalog.dto.ProductSummary;
import com.ebookstore.catalog.entity.Brand;
import com.ebookstore.catalog.entity.Category;
import com.ebookstore.catalog.entity.Product;

/**
 * Hand-written mapper: catalog entities → catalog DTOs.
 * Never exposes JPA entities outside the catalog module.
 */
public final class CatalogMapper {

    private CatalogMapper() {}

    public static CategorySummary toSummary(Category c) {
        return new CategorySummary(c.getId(), c.getName(), c.getDescription(), c.isActive());
    }

    public static BrandSummary toSummary(Brand b) {
        return new BrandSummary(b.getId(), b.getName(), b.getDescription(), b.isActive());
    }

    /** {@code available} = active AND stockQuantity > 0 */
    public static ProductSummary toSummary(Product p) {
        boolean available = p.isActive() && p.getStockQuantity() != null && p.getStockQuantity() > 0;
        return new ProductSummary(
                p.getId(), p.getTitle(), p.getIsbn(), p.getPrice(), available, p.getStockQuantity());
    }

    public static ProductResponse toResponse(Product p) {
        boolean available = p.isActive() && p.getStockQuantity() != null && p.getStockQuantity() > 0;
        DeliveryEstimate estimate = (p.getDeliveryDaysMin() != null && p.getDeliveryDaysMax() != null)
                ? new DeliveryEstimate(p.getDeliveryDaysMin(), p.getDeliveryDaysMax())
                : null;
        return new ProductResponse(
                p.getId(), p.getTitle(), p.getIsbn(), p.getPrice(),
                available, p.getStockQuantity(),
                p.getDescription(),
                toSummary(p.getCategory()),
                toSummary(p.getBrand()),
                estimate);
    }
}
