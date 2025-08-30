package com.mik.icc.icceditor;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class TextTagData implements TagData {
    private String text;
    private Charset charset;

    public TextTagData(String text, Charset charset) {
        this.text = text;
        this.charset = charset;
    }

    public String getText() {
        return text;
    }

    public Charset getCharset() {
        return charset;
    }

    @Override
    public byte[] toBytes() {
        // ICC textType tags are typically null-terminated and can have padding.
        // For simplicity, we'll just convert the string to bytes using the specified charset.
        // More robust handling might be needed for actual ICC profile compliance.
        return text.getBytes(charset);
    }

    @Override
    public String toString() {
        return text;
    }
}
