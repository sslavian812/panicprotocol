package ru.ifmo.ctddev.shalamov.networks;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by viacheslav on 30.10.2015.
 */
public class TCPMessage extends Message {
    public byte[] nDigits;
    public byte[] piBytes;

    private TCPMessage() {
    }

    public static TCPMessage readTCPMessage(InputStream in) throws IOException {
        int len = 1 + 6 + 4;
        byte[] bytes = new byte[len];
        in.read(bytes, 0, len);
        if (bytes[0] != Constants.UDP_LEADING_BYTE)
            return null;
        byte[] mac = new byte[6];
        System.arraycopy(bytes, 1, mac, 0, 6);
        byte[] version = new byte[4];
        System.arraycopy(bytes, 7, version, 0, 4);
        byte[] nDigits = new byte[4];
        System.arraycopy(bytes, 1 + 6 + 4, nDigits, 0, 4);
        return new TCPMessage(mac, ByteUitls.bytesToInt(version), ByteUitls.bytesToInt(nDigits));
    }

    public TCPMessage(byte[] mac, int version, int nDigits) {
        super(mac, ByteUitls.intToBytes(version));
        this.nDigits = ByteUitls.intToBytes(nDigits);
        piBytes = PiHolder.getInstance().getHeadDigits(nDigits);
    }

    public byte[] asByteArray() {
        int len = 1 + mac.length + version.length + nDigits.length + piBytes.length;
        byte[] bytes = new byte[len];
        bytes[0] = Constants.UDP_LEADING_BYTE;
        System.arraycopy(mac, 0, bytes, 1, mac.length);
        System.arraycopy(version, 0, bytes, mac.length + 1, version.length);
        int curlen = 1 + mac.length + version.length;
        System.arraycopy(nDigits, 0, bytes, curlen, nDigits.length);
        curlen += nDigits.length;
        System.arraycopy(piBytes, 0, bytes, curlen, piBytes.length);
        return bytes;
    }
}

