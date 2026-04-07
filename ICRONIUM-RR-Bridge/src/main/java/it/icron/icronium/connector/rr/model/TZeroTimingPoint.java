package it.icron.icronium.connector.rr.model;

import java.util.ArrayList;
import java.util.List;

public class TZeroTimingPoint {

    private String name;
    private String description;
    private List<String> sources = new ArrayList<>();

    public TZeroTimingPoint() {
    }

    public TZeroTimingPoint(String name, String description, List<String> sources) {
        this.name = name;
        this.description = description;
        this.sources = sources == null ? new ArrayList<>() : sources;
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

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources == null ? new ArrayList<>() : sources;
    }
}
