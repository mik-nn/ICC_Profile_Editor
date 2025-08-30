package com.mik.icc.icceditor;

public enum TagType {
    TEXT_TYPE("text"),
    XYZ_TYPE("XYZ "),
    CURVE_TYPE("curv"),
    MLUC_TYPE("mluc"),
    UNKNOWN("unknown");

    private final String signature;

    TagType(String signature) {
        this.signature = signature;
    }

    public String getSignature() {
        return signature;
    }

    public static TagType fromSignature(String signature) {
        for (TagType type : TagType.values()) {
            if (type.getSignature().equals(signature)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
