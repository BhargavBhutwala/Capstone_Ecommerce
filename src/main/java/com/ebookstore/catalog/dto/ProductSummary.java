package com.ebookstore.catalog.dto;

import java.math.BigDecimal;

/**
 * Summary DTO for a product listing. Matches the OpenAPI {@code ProductSummary} schema.
 *
 * <p>{@code price} is {@code BigDecimal} — the OpenAPI {@code format: double} is a
 * presentation hint only and must not influence the Java type.
 *
 * <p>{@code available} is a derived field: {@code active == true && stockQuantity > 0}.
 */
public class ProductSummary {

    private final Long id;
    private final String title;
    private final String isbn;
    private final BigDecimal price;
    private final boolean available;
    private final Integer stockQuantity;

    public ProductSummary(Long id,
                          String title,
                          String isbn,
                          BigDecimal price,
                          boolean available,
                          Integer stockQuantity) {
        this.id = id;
        this.title = title;
        this.isbn = isbn;
        this.price = price;
        this.available = available;
        this.stockQuantity = stockQuantity;
    }

    public Long getId()             { return id; }
    public String getTitle()        { return title; }
    public String getIsbn()         { return isbn; }
    public BigDecimal getPrice()    { return price; }
    public boolean isAvailable()    { return available; }
    public Integer getStockQuantity(){ return stockQuantity; }
}
