package org.cloudcompare.backend.catalog;

public final class Service {
    private final String id;
    private final String name;
    private final String category;
    private final String description;

    public Service(String _id, String _name, String _category, String _description) {
        this.id = _id;
        this.name = _name;
        this.category = _category;
        this.description = _description;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
}