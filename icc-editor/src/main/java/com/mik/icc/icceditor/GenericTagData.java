package com.mik.icc.icceditor;

public class GenericTagData implements TagData {
    private final byte[] data;

    public GenericTagData(byte[] data) {
        this.data = data;
    }

    @Override
    public byte[] toBytes() {
        return data;
    }

    @Override
    public String toString() {
        // For display purposes, convert bytes to hex string
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
