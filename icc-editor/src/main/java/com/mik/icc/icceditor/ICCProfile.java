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

    public byte[] readTagData(Tag tag) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek(tag.getOffset());
            byte[] data = new byte[(int) tag.getSize()];
            raf.readFully(data);
            return data;
        }
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
        raf.skipBytes(4); // Rendering Intent is at offset 64, but attributes is a long (8 bytes), so we are at 64, not 60.
        header.renderingIntent = readString(raf, 4);
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
