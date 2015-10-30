package ru.ifmo.ctddev.shalamov.networks;

import java.io.UnsupportedEncodingException;

/**
 * Created by viacheslav on 18.09.2015.
 */
public class ByteUitls {

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    /**
     * Converts byte array to string, using hexadecimal representation.
     *
     * @param bytes
     * @return
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * converts byte array to string, separating each byte with dots.
     *
     * @param bytes
     * @return
     */
    public static String bytesToDec(byte[] bytes) {
        String s = "" + ((int) bytes[0] + 127);

        for (int j = 1; j < bytes.length; j++) {
            s = s + "." + ((int) bytes[j] + 127);
        }
        return s;
    }

    /**
     * Converts string to utf-8 byte array.
     *
     * @param string
     * @return
     */
    public static byte[] stringToBytes(String string) {
        try {
            return string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.err.println("COULD NEVER HAPPEN!! " + e.getMessage());
            return new byte[]{};
        }
    }


    /**
     * converts int number to 8 byte char array.
     *
     * @param number
     * @return
     */
    public static byte[] intToBytes(int number) {
        return new byte[]{
                (byte) (number >> 24),
                (byte) (number >> 16),
                (byte) (number >> 8),
                (byte) number
        };
    }


    public static int bytesToInt(byte[] bytes) {
        int acc = 0;

        for (int j = 0; j < bytes.length; j++) {
            acc += ((int) bytes[j] + 127);
        }

        return acc;
    }


    public static byte[] getRandom6bytes(long seed) {
        return new byte[]{
                (byte) (seed >> 40),
                (byte) (seed >> 32),
                (byte) (seed >> 24),
                (byte) (seed >> 16),
                (byte) (seed >> 8),
                (byte) seed
        };
    }

    public static byte[] getRandomMac() {
        return getRandom6bytes(System.currentTimeMillis());
    }

    public static String getRandomHostName() {
        try {
            return new String(getRandomMac(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "abacaba";
        }
    }
}
