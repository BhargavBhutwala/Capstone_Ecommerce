package com.ebookstore.catalog.dto;

/**
 * Summary DTO for a category. Matches the OpenAPI {@code CategorySummary} schema.
 */
public class CategorySummary {

    private final Long id;
    private final String name;
    private final String description;
    private final boolean active;

    public CategorySummary(Long id, String name, String description, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.active = active;
    }

    public Long getId()          { return id; }
    public String getName()      { return name; }
    public String getDescription(){ return description; }
    public boolean isActive()    { return active; }
}
