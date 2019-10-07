package org.aion.avm.embed.tetryon.bn128;

import java.math.BigInteger;
import java.util.Arrays;

public class Util {

    public static final int FP_SIZE=32;

    public static byte[] serializeG1(G1Point p) {
        byte[] data = new byte[FP_SIZE*2];

        byte[] px = p.x.c0.toByteArray();
        System.arraycopy(px, 0, data, FP_SIZE - px.length, px.length);

        byte[] py = p.y.c0.toByteArray();
        System.arraycopy(py, 0, data, FP_SIZE*2 - py.length, py.length);

        return data;
    }

    public static G1Point deserializeG1(byte[] data) {
        byte[] pxData = Arrays.copyOfRange(data, 0, FP_SIZE);
        byte[] pyData = Arrays.copyOfRange(data, FP_SIZE, data.length);
        Fp p1x = new Fp(new BigInteger(pxData));
        Fp p1y = new Fp(new BigInteger(pyData));
        G1Point p1 = new G1Point(p1x, p1y);
        return p1;
    }

    public static byte[] serializeG2(G2Point p) {
        byte[] data = new byte[FP_SIZE*4]; // zero byte array

        byte[] px1 = p.x.a.toByteArray();
        System.arraycopy(px1, 0, data, FP_SIZE*1 - px1.length, px1.length);

        byte[] px2 = p.x.b.toByteArray();
        System.arraycopy(px2, 0, data, FP_SIZE*2 - px2.length, px2.length);

        byte[] py1 = p.y.a.toByteArray();
        System.arraycopy(py1, 0, data, FP_SIZE*3 - py1.length, py1.length);

        byte[] py2 = p.y.b.toByteArray();
        System.arraycopy(py2, 0, data, FP_SIZE*4 - py2.length, py2.length);
        return data;
    }

    public static G2Point deserializeG2(byte[] data) {

        byte[] px1Data = Arrays.copyOfRange(data, 0, FP_SIZE);
        byte[] px2Data = Arrays.copyOfRange(data, 1*FP_SIZE, 2*FP_SIZE);
        byte[] py1Data = Arrays.copyOfRange(data, 2*FP_SIZE, 3*FP_SIZE);
        byte[] py2Data = Arrays.copyOfRange(data, 3*FP_SIZE, data.length);

        Fp2 x = new Fp2(new BigInteger(px1Data), new BigInteger(px2Data));
        Fp2 y = new Fp2(new BigInteger(py1Data), new BigInteger(py2Data));

        G2Point p = new G2Point(x, y);

        return p;
    }

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }



}