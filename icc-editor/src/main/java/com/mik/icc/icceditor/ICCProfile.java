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
                    if (data.length >= 8) {
                        String text = new String(data, 8, data.length - 8, StandardCharsets.UTF_8).trim();
                        return new TextTagData(text, StandardCharsets.UTF_8);
                    }
                    break;
                case XYZ_TYPE:
                    if (data.length >= 20) {
                        ByteBuffer xyzBuffer = ByteBuffer.wrap(data, 8, 12).order(ByteOrder.BIG_ENDIAN);
                        double x = XYZTagData.iccS15Fixed16ToFloat(xyzBuffer.getInt());
                        double y = XYZTagData.iccS15Fixed16ToFloat(xyzBuffer.getInt());
                        double z = XYZTagData.iccS15Fixed16ToFloat(xyzBuffer.getInt());
                        return new XYZTagData(x, y, z);
                    }
                    break;
                case CURVE_TYPE:
                    if (data.length >= 12) {
                        ByteBuffer curveBuffer = ByteBuffer.wrap(data, 8, data.length - 8).order(ByteOrder.BIG_ENDIAN);
                        int count = curveBuffer.getInt();
                        double[] curvePoints = new double[count];
                        for (int i = 0; i < count; i++) {
                            curvePoints[i] = CurveTagData.iccUnsignedShortToFloat(curveBuffer.getShort());
                        }
                        return new CurveTagData(curvePoints);
                    }
                    break;
                case MLUC_TYPE:
                    if (data.length >= 16) {
                        ByteBuffer mlucBuffer = ByteBuffer.wrap(data, 8, data.length - 8).order(ByteOrder.BIG_ENDIAN);
                        int numRecords = mlucBuffer.getInt();
                        int recordSize = mlucBuffer.getInt();

                        MultiLocalizedUnicodeTagData mlucData = new MultiLocalizedUnicodeTagData();
                        long baseOffset = tag.getOffset() + 8; 
                        for (int i = 0; i < numRecords; i++) {
                            int entryStart = 16 + (i * recordSize);
                            mlucBuffer.position(entryStart - 8);
                            byte[] languageCodeBytes = new byte[2];
                            mlucBuffer.get(languageCodeBytes);
                            byte[] countryCodeBytes = new byte[2];
                            mlucBuffer.get(countryCodeBytes);
                            String languageCode = new String(languageCodeBytes, StandardCharsets.US_ASCII);
                            String countryCode = new String(countryCodeBytes, StandardCharsets.US_ASCII);
                            int offset = mlucBuffer.getInt();
                            int length = mlucBuffer.getInt();

                            byte[] stringBytes = new byte[length];
                            raf.seek(baseOffset + offset);
                            raf.readFully(stringBytes);
                            String text = new String(stringBytes, StandardCharsets.UTF_16BE);
                            mlucData.addLocalizedString(languageCode, countryCode, text);
                        }
                        return mlucData;
                    }
                    break;
                case UNKNOWN:
                default:
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
            raf.writeInt((int) header.getSize());
            writeString(raf, header.getCmmType(), 4);
            writeVersion(raf, header.getVersion());
            writeString(raf, header.getDeviceClass(), 4);
            writeString(raf, header.getColorSpace(), 4);
            writeString(raf, header.getPcs(), 4);
            writeDateTime(raf, header.getCreationDateTime());
            writeString(raf, header.getSignature(), 4);
            writeString(raf, header.getPrimaryPlatform(), 4);
            raf.writeInt((int) header.getFlags());
            writeString(raf, header.getManufacturer(), 4);
            writeString(raf, header.getModel(), 4);
            raf.writeLong(header.getAttributes());
            raf.writeInt(header.getRenderingIntent());
            raf.skipBytes(12); // Skip illuminant
            writeString(raf, header.getCreator(), 4);
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

        header.setSize(raf.readInt());
        header.setCmmType(readString(raf, 4));
        header.setVersion(readVersion(raf));
        header.setDeviceClass(readString(raf, 4));
        header.setColorSpace(readString(raf, 4));
        header.setPcs(readString(raf, 4));
        header.setCreationDateTime(readDateTime(raf));
        header.setSignature(readString(raf, 4));
        header.setPrimaryPlatform(readString(raf, 4));
        header.setFlags(raf.readInt());
        header.setManufacturer(readString(raf, 4));
        header.setModel(readString(raf, 4));
        header.setAttributes(raf.readLong());
        header.setRenderingIntent(raf.readInt());
        raf.skipBytes(12); // Skip illuminant
        header.setCreator(readString(raf, 4));

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
            raf.write(new byte[length - bytes.length]); 
        }
    }

    private void writeVersion(RandomAccessFile raf, String version) throws IOException {
        String[] parts = version.split("\\.");
        byte major = Byte.parseByte(parts[0]);
        byte minor = Byte.parseByte(parts[1]);
        byte bugfix = Byte.parseByte(parts[2]);
        raf.writeByte(major);
        raf.writeByte((minor << 4) | bugfix);
        raf.writeShort(0); 
    }

    private void writeDateTime(RandomAccessFile raf, String dateTime) throws IOException {
        String[] dateTimeParts = dateTime.split(" ");
        String[] dateParts = dateTimeParts[0].split("-");
        String[] timeParts = dateTimeParts[1].split(":");

        raf.writeShort(Integer.parseInt(dateParts[0])); 
        raf.writeShort(Integer.parseInt(dateParts[1])); 
        raf.writeShort(Integer.parseInt(dateParts[2])); 
        raf.writeShort(Integer.parseInt(timeParts[0])); 
        raf.writeShort(Integer.parseInt(timeParts[1])); 
        raf.writeShort(Integer.parseInt(timeParts[2])); 
    }
}
