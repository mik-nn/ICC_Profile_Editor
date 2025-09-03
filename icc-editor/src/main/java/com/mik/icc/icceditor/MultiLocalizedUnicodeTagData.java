package com.mik.icc.icceditor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class MultiLocalizedUnicodeTagData implements TagData {
    private final Map<String, String> localizedStrings;

    public MultiLocalizedUnicodeTagData() {
        this.localizedStrings = new LinkedHashMap<>();
    }

    public void addLocalizedString(String languageCode, String countryCode, String text) {
        localizedStrings.put(languageCode + "-" + countryCode, text);
    }

    public Map<String, String> getLocalizedStrings() {
        return localizedStrings;
    }

    @Override
    public byte[] toBytes() {
        int numRecords = localizedStrings.size();
        int recordSize = 12; // 2 bytes for lang, 2 for country, 4 for offset, 4 for length
        int headerSize = 16; // 4 for type, 4 for reserved, 4 for numRecords, 4 for recordSize
        int recordsTableSize = numRecords * recordSize;
        int stringsSize = 0;
        for (String text : localizedStrings.values()) {
            stringsSize += text.getBytes(StandardCharsets.UTF_16BE).length;
        }

        int totalSize = headerSize + recordsTableSize + stringsSize;
        ByteBuffer buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.BIG_ENDIAN);

        // Header
        buffer.put("mluc".getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(0); // Reserved
        buffer.putInt(numRecords);
        buffer.putInt(recordSize);

        // Records table
        int stringOffset = headerSize + recordsTableSize;
        for (Map.Entry<String, String> entry : localizedStrings.entrySet()) {
            String[] codes = entry.getKey().split("-");
            String languageCode = codes[0];
            String countryCode = codes[1];
            byte[] textBytes = entry.getValue().getBytes(StandardCharsets.UTF_16BE);

            buffer.put(languageCode.getBytes(StandardCharsets.US_ASCII));
            buffer.put(countryCode.getBytes(StandardCharsets.US_ASCII));
            buffer.putInt(stringOffset);
            buffer.putInt(textBytes.length);

            stringOffset += textBytes.length;
        }

        // String data
        for (String text : localizedStrings.values()) {
            buffer.put(text.getBytes(StandardCharsets.UTF_16BE));
        }

        return buffer.array();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        localizedStrings.forEach((key, value) -> sb.append(key).append(": ").append(value).append("\n"));
        return sb.toString();
    }
}
