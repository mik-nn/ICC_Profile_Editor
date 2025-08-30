package com.mik.icc.icceditor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class CurveTagData implements TagData {
    private final double[] curvePoints;

    public CurveTagData(double[] curvePoints) {
        this.curvePoints = curvePoints;
    }

    public double[] getCurvePoints() {
        return curvePoints;
    }

    @Override
    public byte[] toBytes() {
        // Curve data format: 4 bytes for count, then 2 bytes per point (unsigned short)
        ByteBuffer buffer = ByteBuffer.allocate(4 + curvePoints.length * 2).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(curvePoints.length);
        for (double point : curvePoints) {
            buffer.putShort((short) Math.round(point * 65535.0)); // Convert to unsigned short (0-65535)
        }
        return buffer.array();
    }

    @Override
    public String toString() {
        return "Curve Points: " + Arrays.toString(curvePoints);
    }

    // Helper to convert unsigned short to float (0.0-1.0)
    public static double iccUnsignedShortToFloat(short value) {
        return (value & 0xFFFF) / 65535.0;
    }
}
