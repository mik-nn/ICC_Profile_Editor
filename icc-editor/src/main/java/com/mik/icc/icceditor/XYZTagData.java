package com.mik.icc.icceditor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class XYZTagData implements TagData {
    private double x;
    private double y;
    private double z;

    public XYZTagData(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    @Override
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(iccFloatToS15Fixed16(x));
        buffer.putInt(iccFloatToS15Fixed16(y));
        buffer.putInt(iccFloatToS15Fixed16(z));
        return buffer.array();
    }

    @Override
    public String toString() {
        return String.format("X: %.4f, Y: %.4f, Z: %.4f", x, y, z);
    }

    // Helper to convert float to S15Fixed16 (ICC profile format)
    private int iccFloatToS15Fixed16(double value) {
        return (int) Math.round(value * 65536.0);
    }

    // Helper to convert S15Fixed16 to float
    public static double iccS15Fixed16ToFloat(int value) {
        return value / 65536.0;
    }
}
