package org.mule.tooling.devkit.common;

public enum AuthenticationType {
    NONE("none"), BASIC("Basic"), OAUTH_V1("OAuth V1"), OAUTH_V2("OAuth V2");

    AuthenticationType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    private String label;

    public static AuthenticationType fromLabel(String text) {
        for (AuthenticationType item : values()) {
            if (item.label().equals(text)) {
                return item;
            }
        }
        return null;
    }
}
