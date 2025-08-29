package com.mik.icc.icceditor;

public class ICCHeader {
    public long size;
    public String cmmType;
    public String version;
    public String deviceClass;
    public String colorSpace;
    public String pcs;
    public String creationDateTime;
    public String signature;
    public String primaryPlatform;
    public long flags;
    public String manufacturer;
    public String model;
    public long attributes;
    public String renderingIntent;
    public String creator;

    @Override
    public String toString() {
        return "ICCHeader{\n" +
                "  size=" + size + "\n" +
                ", cmmType='" + cmmType + "'\n" +
                ", version='" + version + "'\n" +
                ", deviceClass='" + deviceClass + "'\n" +
                ", colorSpace='" + colorSpace + "'\n" +
                ", pcs='" + pcs + "'\n" +
                ", creationDateTime='" + creationDateTime + "'\n" +
                ", signature='" + signature + "'\n" +
                ", primaryPlatform='" + primaryPlatform + "'\n" +
                ", flags=" + flags + "\n" +
                ", manufacturer='" + manufacturer + "'\n" +
                ", model='" + model + "'\n" +
                ", attributes=" + attributes + "\n" +
                ", renderingIntent='" + renderingIntent + "'\n" +
                ", creator='" + creator + "'\n" +
                '}';
    }
}
