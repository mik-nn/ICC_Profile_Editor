package com.mik.icc.icceditor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;

class DataStructuresTest {

    @Test
    void tagTypeFromSignatureReturnsCorrectType() {
        assertEquals(TagType.TEXT_TYPE, TagType.fromSignature("text"));
        assertEquals(TagType.XYZ_TYPE, TagType.fromSignature("XYZ "));
        assertEquals(TagType.CURVE_TYPE, TagType.fromSignature("curv"));
        assertEquals(TagType.UNKNOWN, TagType.fromSignature("abcd"));
    }

    @Test
    void textTagDataConvertsToBytesCorrectly() {
        String testString = "Hello World";
        TextTagData textTagData = new TextTagData(testString, StandardCharsets.UTF_8);
        assertArrayEquals(testString.getBytes(StandardCharsets.UTF_8), textTagData.toBytes());
        assertEquals(testString, textTagData.getText());
        assertEquals(StandardCharsets.UTF_8, textTagData.getCharset());
    }

    @Test
    void genericTagDataConvertsToBytesCorrectly() {
        byte[] testBytes = {0x01, 0x02, 0x0A, (byte) 0xFF};
        GenericTagData genericTagData = new GenericTagData(testBytes);
        assertArrayEquals(testBytes, genericTagData.toBytes());
        assertEquals("01 02 0A FF", genericTagData.toString());
    }

    @Test
    void tagGettersReturnCorrectValues() {
        Tag tag = new Tag("test", 100, 200);
        assertEquals("test", tag.getSignature());
        assertEquals(100, tag.getOffset());
        assertEquals(200, tag.getSize());
    }
}
