package com.jushi.demo.main.ui.imports;

public class SourceItem {
    private final String packageName;
    private final String name;
    private boolean selected;

    public SourceItem(String packageName, String name, boolean selected) {
        this.packageName = packageName;
        this.name = name;
        this.selected = selected;
    }

    public String getPackageName() {
        return packageName;
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
