package com.ebookstore.catalog.dto;

import java.math.BigDecimal;

/**
 * Full product detail DTO. Matches the OpenAPI {@code ProductResponse} schema
 * ({@code allOf: [ProductSummary, ...extra fields]}).
 */
public class ProductResponse {

    private final Long id;
    private final String title;
    private final String isbn;
    private final BigDecimal price;
    private final boolean available;
    private final Integer stockQuantity;
    private final String description;
    private final String imageUrl;
    private final CategorySummary category;
    private final BrandSummary brand;
    private final DeliveryEstimate deliveryEstimate;

    public ProductResponse(Long id,
                           String title,
                           String isbn,
                           BigDecimal price,
                           boolean available,
                           Integer stockQuantity,
                           String description,
                           String imageUrl,
                           CategorySummary category,
                           BrandSummary brand,
                           DeliveryEstimate deliveryEstimate) {
        this.id = id;
        this.title = title;
        this.isbn = isbn;
        this.price = price;
        this.available = available;
        this.stockQuantity = stockQuantity;
        this.description = description;
        this.imageUrl = imageUrl;
        this.category = category;
        this.brand = brand;
        this.deliveryEstimate = deliveryEstimate;
    }

    public Long getId()                        { return id; }
    public String getTitle()                   { return title; }
    public String getIsbn()                    { return isbn; }
    public BigDecimal getPrice()               { return price; }
    public boolean isAvailable()               { return available; }
    public Integer getStockQuantity()          { return stockQuantity; }
    public String getDescription()             { return description; }
    public String getImageUrl()                { return imageUrl; }
    public CategorySummary getCategory()       { return category; }
    public BrandSummary getBrand()             { return brand; }
    public DeliveryEstimate getDeliveryEstimate(){ return deliveryEstimate; }
}
