package com.mik.icc.icceditor;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

    public String getFilePath() {
        return filePath;
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

    public void writeHeader(ICCHeader header) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            raf.seek(0);
            raf.writeInt((int) header.size);
            writeString(raf, header.cmmType, 4);
            writeVersion(raf, header.version);
            writeString(raf, header.deviceClass, 4);
            writeString(raf, header.colorSpace, 4);
            writeString(raf, header.pcs, 4);
            writeDateTime(raf, header.creationDateTime);
            writeString(raf, header.signature, 4);
            writeString(raf, header.primaryPlatform, 4);
            raf.writeInt((int) header.flags);
            writeString(raf, header.manufacturer, 4);
            writeString(raf, header.model, 4);
            raf.writeLong(header.attributes);
            raf.writeInt(header.renderingIntent);
            raf.skipBytes(12); // Skip illuminant
            writeString(raf, header.creator, 4);
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

    private void writeString(RandomAccessFile raf, String value, int length) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        raf.write(bytes);
        if (bytes.length < length) {
            raf.write(new byte[length - bytes.length]); // Pad with zeros
        }
    }

    private void writeVersion(RandomAccessFile raf, String version) throws IOException {
        String[] parts = version.split("\\.");
        raf.writeByte(Integer.parseInt(parts[0]));
        raf.writeByte((Integer.parseInt(parts[1]) << 4) | Integer.parseInt(parts[2]));
        raf.writeShort(0); // Reserved
    }

    private void writeDateTime(RandomAccessFile raf, String dateTime) throws IOException {
        // Assuming dateTime is in "YYYY-MM-DD HH:MM:SS" format
        String[] dateTimeParts = dateTime.split(" ");
        String[] dateParts = dateTimeParts[0].split("-");
        String[] timeParts = dateTimeParts[1].split(":");

        raf.writeShort(Integer.parseInt(dateParts[0])); // Year
        raf.writeShort(Integer.parseInt(dateParts[1])); // Month
        raf.writeShort(Integer.parseInt(dateParts[2])); // Day
        raf.writeShort(Integer.parseInt(timeParts[0])); // Hours
        raf.writeShort(Integer.parseInt(timeParts[1])); // Minutes
        raf.writeShort(Integer.parseInt(timeParts[2])); // Seconds
    }
}
