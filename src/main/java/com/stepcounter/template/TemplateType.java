package com.stepcounter.template;

public enum TemplateType {
    JAVA("Java"),
    SALESFORCE("Salesforce"),
    TYPESCRIPT("TypeScript (Node/Yarn)"),
    PYTHON("Python"),
    KOTLIN("Kotlin"),
    SWIFT("Swift");

    private final String label;

    TemplateType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
