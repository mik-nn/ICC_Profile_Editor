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
        // This is a simplified implementation. Actual mlucType is more complex
        // with count, record size, and offsets.
        // For now, we'll just concatenate UTF-16BE encoded strings.
        ByteBuffer buffer = ByteBuffer.allocate(1024); // Arbitrary initial size
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Header for mlucType (type signature + reserved + number of records + record size)
        buffer.putInt(TagType.MLUC_TYPE.getSignature().getBytes(StandardCharsets.US_ASCII)[0] << 24 |
                       TagType.MLUC_TYPE.getSignature().getBytes(StandardCharsets.US_ASCII)[1] << 16 |
                       TagType.MLUC_TYPE.getSignature().getBytes(StandardCharsets.US_ASCII)[2] << 8 |
                       TagType.MLUC_TYPE.getSignature().getBytes(StandardCharsets.US_ASCII)[3]); // 'mluc'
        buffer.putInt(0); // Reserved
        buffer.putInt(localizedStrings.size()); // Number of records
        buffer.putInt(12); // Record size (language code + country code + string offset + string length)

        int currentOffset = 16 + (localizedStrings.size() * 12); // Start of string data

        for (Map.Entry<String, String> entry : localizedStrings.entrySet()) {
            String[] codes = entry.getKey().split("-");
            String languageCode = codes[0];
            String countryCode = codes[1];
            byte[] textBytes = entry.getValue().getBytes(StandardCharsets.UTF_16BE);

            buffer.put(languageCode.getBytes(StandardCharsets.US_ASCII));
            buffer.put(countryCode.getBytes(StandardCharsets.US_ASCII));
            buffer.putInt(currentOffset); // Offset to string
            buffer.putInt(textBytes.length); // Length of string

            // Store current position and move to write string data
            int mark = buffer.position();
            buffer.position(currentOffset);
            buffer.put(textBytes);
            currentOffset += textBytes.length;
            buffer.position(mark); // Restore position
        }

        buffer.flip();
        byte[] result = new byte[buffer.limit()];
        buffer.get(result);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        localizedStrings.forEach((key, value) -> sb.append(key).append(": ").append(value).append("\n"));
        return sb.toString();
    }
}
