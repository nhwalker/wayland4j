package org.wayland4j.scanner.model;

/**
 * A {@code <description>} element. {@code summary} is the attribute; {@code body}
 * is the trimmed character content (may be empty).
 */
public record Description(String summary, String body) {

    public static final Description EMPTY = new Description("", "");

    public Description {
        summary = summary == null ? "" : summary;
        body = body == null ? "" : body;
    }

    public boolean isEmpty() {
        return summary.isEmpty() && body.isEmpty();
    }
}
