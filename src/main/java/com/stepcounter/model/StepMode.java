package com.stepcounter.model;

public enum StepMode {
    FULL("全量モード（コメント・空行含む）"),
    SUBSTANTIVE("実質モード（コメント・空行を除く）");

    private final String label;

    StepMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
