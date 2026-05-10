package com.jushi.demo.main.ui.imports;

public class SourceItem {
    private final String id;
    private final String name;
    private boolean selected;

    public SourceItem(String id, String name, boolean selected) {
        this.id = id;
        this.name = name;
        this.selected = selected;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
