package com.mik.icc.icceditor;

public class ICCHeader {
    private long size;
    private String cmmType;
    private String version;
    private String deviceClass;
    private String colorSpace;
    private String pcs;
    private String creationDateTime;
    private String signature;
    private String primaryPlatform;
    private long flags;
    private String manufacturer;
    private String model;
    private long attributes;
    private int renderingIntent;
    private String creator;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getCmmType() {
        return cmmType;
    }

    public void setCmmType(String cmmType) {
        this.cmmType = cmmType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDeviceClass() {
        return deviceClass;
    }

    public void setDeviceClass(String deviceClass) {
        this.deviceClass = deviceClass;
    }

    public String getColorSpace() {
        return colorSpace;
    }

    public void setColorSpace(String colorSpace) {
        this.colorSpace = colorSpace;
    }

    public String getPcs() {
        return pcs;
    }

    public void setPcs(String pcs) {
        this.pcs = pcs;
    }

    public String getCreationDateTime() {
        return creationDateTime;
    }

    public void setCreationDateTime(String creationDateTime) {
        this.creationDateTime = creationDateTime;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getPrimaryPlatform() {
        return primaryPlatform;
    }

    public void setPrimaryPlatform(String primaryPlatform) {
        this.primaryPlatform = primaryPlatform;
    }

    public long getFlags() {
        return flags;
    }

    public void setFlags(long flags) {
        this.flags = flags;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public long getAttributes() {
        return attributes;
    }

    public void setAttributes(long attributes) {
        this.attributes = attributes;
    }

    public int getRenderingIntent() {
        return renderingIntent;
    }

    public void setRenderingIntent(int renderingIntent) {
        this.renderingIntent = renderingIntent;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

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
                ", renderingIntent='" + getRenderingIntentString() + "'\n" +
                ", creator='" + creator + "'\n" +
                '}';
    }

    public String getRenderingIntentString() {
        return switch (renderingIntent) {
            case 0 -> "Perceptual";
            case 1 -> "Relative Colorimetric";
            case 2 -> "Saturation";
            case 3 -> "Absolute Colorimetric";
            default -> "Unknown";
        };
    }
}