package io.etherflow.codec;

import java.util.Objects;

public class MediaType {

    public static final MediaType ALL = new MediaType("*", "*");
    public static final MediaType APPLICATION_JSON = new MediaType("application", "json");
    public static final MediaType APPLICATION_OCTET_STREAM = new MediaType("application", "octet-stream");
    public static final MediaType TEXT_PLAIN = new MediaType("text", "plain");
    public static final MediaType TEXT_HTML = new MediaType("text", "html");

    private final String type;
    private final String subtype;

    public MediaType(String type, String subtype) {
        this.type = Objects.requireNonNull(type);
        this.subtype = Objects.requireNonNull(subtype);
    }

    public String type() { return type; }

    public String subtype() { return subtype; }

    public boolean isCompatibleWith(MediaType other) {
        if (other == null) return false;
        return (this.type.equals("*") || other.type.equals("*") || this.type.equals(other.type))
                && (this.subtype.equals("*") || other.subtype.equals("*") || this.subtype.equals(other.subtype));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MediaType that)) return false;
        return type.equals(that.type) && subtype.equals(that.subtype);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, subtype);
    }

    @Override
    public String toString() {
        return type + "/" + subtype;
    }

    public static MediaType parse(String value) {
        String[] parts = value.split("/");
        if (parts.length != 2) throw new IllegalArgumentException("Invalid media type: " + value);
        return new MediaType(parts[0].trim().toLowerCase(), parts[1].trim().toLowerCase());
    }
}
