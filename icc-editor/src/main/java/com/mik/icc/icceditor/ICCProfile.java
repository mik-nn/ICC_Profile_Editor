package com.mik.icc.icceditor;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ICCProfile {

    private final String filePath;
    private final ICCHeader header;
    private final List<Tag> tags;

    public ICCProfile(String filePath) throws IOException {
        this.filePath = filePath;
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            this.header = parseHeader(raf);
            this.tags = parseTagTable(raf);
        }
    }

    public ICCHeader getHeader() {
        return header;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public TagData readTagData(Tag tag) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek(tag.getOffset());
            byte[] data = new byte[(int) tag.getSize()];
            raf.readFully(data);

            TagType type = TagType.fromSignature(tag.getSignature());

            switch (type) {
                case TEXT_TYPE:
                    // Assuming textType tags are null-terminated and potentially padded
                    // The first 4 bytes are the tag type signature, then 4 bytes for reserved
                    // The actual text starts from offset 8
                    if (data.length >= 8) {
                        String text = new String(data, 8, data.length - 8, StandardCharsets.UTF_8);
                        return new TextTagData(text.trim(), StandardCharsets.UTF_8);
                    }
                    break;
                case XYZ_TYPE:
                    if (data.length >= 12) {
                        ByteBuffer xyzBuffer = ByteBuffer.wrap(data, 0, 12).order(ByteOrder.BIG_ENDIAN);
                        double x = XYZTagData.iccS15Fixed16ToFloat(xyzBuffer.getInt());
                        double y = XYZTagData.iccS15Fixed16ToFloat(xyzBuffer.getInt());
                        double z = XYZTagData.iccS15Fixed16ToFloat(xyzBuffer.getInt());
                        return new XYZTagData(x, y, z);
                    }
                    break;
                case CURVE_TYPE:
                    if (data.length >= 8) { // 4 bytes for count, 4 bytes for reserved
                        ByteBuffer curveBuffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
                        curveBuffer.position(4); // Skip type signature and reserved
                        int count = curveBuffer.getInt();
                        double[] curvePoints = new double[count];
                        for (int i = 0; i < count; i++) {
                            curvePoints[i] = CurveTagData.iccUnsignedShortToFloat(curveBuffer.getShort());
                        }
                        return new CurveTagData(curvePoints);
                    }
                    break;
                case MLUC_TYPE:
                    if (data.length >= 16) { // type signature (4) + reserved (4) + numRecords (4) + recordSize (4)
                        ByteBuffer mlucBuffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
                        mlucBuffer.position(8); // Skip type signature and reserved
                        int numRecords = mlucBuffer.getInt();
                        int recordSize = mlucBuffer.getInt();

                        MultiLocalizedUnicodeTagData mlucData = new MultiLocalizedUnicodeTagData();
                        for (int i = 0; i < numRecords; i++) {
                            int entryStart = 16 + (i * recordSize);
                            mlucBuffer.position(entryStart);
                            byte[] languageCodeBytes = new byte[2];
                            mlucBuffer.get(languageCodeBytes);
                            byte[] countryCodeBytes = new byte[2];
                            mlucBuffer.get(countryCodeBytes);
                            String languageCode = new String(languageCodeBytes, StandardCharsets.US_ASCII);
                            String countryCode = new String(countryCodeBytes, StandardCharsets.US_ASCII);
                            int offset = mlucBuffer.getInt();
                            int length = mlucBuffer.getInt();

                            // Read string data
                            byte[] stringBytes = new byte[length];
                            mlucBuffer.position(offset);
                            mlucBuffer.get(stringBytes);
                            String text = new String(stringBytes, StandardCharsets.UTF_16BE);
                            mlucData.addLocalizedString(languageCode, countryCode, text);
                        }
                        return mlucData;
                    }
                    break;
                case UNKNOWN:
                default:
                    // For now, return a generic TagData for unknown types
                    return new GenericTagData(data);
            }
            return new GenericTagData(data);
        }
    }

    public void writeTagData(Tag tag, TagData tagData) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            raf.seek(tag.getOffset());
            raf.write(tagData.toBytes());
        }
    }

    public Tag getTagBySignature(String signature) {
        for (Tag tag : tags) {
            if (tag.getSignature().equals(signature)) {
                return tag;
            }
        }
        return null;
    }

    private ICCHeader parseHeader(RandomAccessFile raf) throws IOException {
        raf.seek(0);
        ICCHeader header = new ICCHeader();

        header.size = raf.readInt();
        header.cmmType = readString(raf, 4);
        header.version = readVersion(raf);
        header.deviceClass = readString(raf, 4);
        header.colorSpace = readString(raf, 4);
        header.pcs = readString(raf, 4);
        header.creationDateTime = readDateTime(raf);
        header.signature = readString(raf, 4);
        header.primaryPlatform = readString(raf, 4);
        header.flags = raf.readInt();
        header.manufacturer = readString(raf, 4);
        header.model = readString(raf, 4);
        header.attributes = raf.readLong();
        header.renderingIntent = raf.readInt();
        raf.skipBytes(12); // Skip illuminant
        header.creator = readString(raf, 4);

        return header;
    }

    private List<Tag> parseTagTable(RandomAccessFile raf) throws IOException {
        raf.seek(128);
        int tagCount = raf.readInt();
        List<Tag> tags = new ArrayList<>();
        for (int i = 0; i < tagCount; i++) {
            String signature = readString(raf, 4);
            long offset = raf.readInt();
            long size = raf.readInt();
            tags.add(new Tag(signature, offset, size));
        }
        return tags;
    }

    private String readString(RandomAccessFile raf, int length) throws IOException {
        byte[] bytes = new byte[length];
        raf.read(bytes);
        return new String(bytes, StandardCharsets.US_ASCII).trim();
    }

    private String readVersion(RandomAccessFile raf) throws IOException {
        byte[] versionBytes = new byte[4];
        raf.read(versionBytes);
        return String.format("%d.%d.%d", versionBytes[0], (versionBytes[1] >> 4) & 0x0F, versionBytes[1] & 0x0F);
    }

    private String readDateTime(RandomAccessFile raf) throws IOException {
        int year = raf.readUnsignedShort();
        int month = raf.readUnsignedShort();
        int day = raf.readUnsignedShort();
        int hours = raf.readUnsignedShort();
        int minutes = raf.readUnsignedShort();
        int seconds = raf.readUnsignedShort();
        return String.format("%04d-%02d-%02d %02d:%02d:%02d", year, month, day, hours, minutes, seconds);
    }
}
