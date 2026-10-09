package org.kruskopf.backend.offworlders;

/**
 * A skill or ability entry: a name plus an optional description.
 * <p>
 * Catalog skills/abilities are stored with an empty description (their text
 * lives in the frontend catalog); custom entries carry the description the
 * player typed. Stored as a JSON array via {@link OffworldersEntriesConverter}.
 */
public class OffworldersEntry {
    private String name = "";
    private String description = "";

    public OffworldersEntry() {
    }

    public OffworldersEntry(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "OffworldersEntry{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
