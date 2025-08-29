package com.mik.icc.icceditor;

public class Tag {
    private final String signature;
    private final long offset;
    private final long size;

    public Tag(String signature, long offset, long size) {
        this.signature = signature;
        this.offset = offset;
        this.size = size;
    }

    public String getSignature() {
        return signature;
    }

    public long getOffset() {
        return offset;
    }

    public long getSize() {
        return size;
    }
}
