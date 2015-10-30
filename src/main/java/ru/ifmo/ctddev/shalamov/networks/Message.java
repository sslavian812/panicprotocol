package ru.ifmo.ctddev.shalamov.networks;

/**
 * Created by viacheslav on 23.10.2015.
 */
public class Message {
    public byte leadingByte;
    public byte[] mac;
    public byte[] version;

    protected Message() {
    }

    public int getIntVersion() {
        return ByteUitls.bytesToInt(version);
    }

    public Message(byte[] mac, byte[] version) {
        this.leadingByte = Constants.UDP_LEADING_BYTE;
        this.mac = mac;
        this.version = version;
    }

    public static Message fromArray(byte[] array) {
        if (array[0] != Constants.UDP_LEADING_BYTE)
            return null;
        byte[] mac = new byte[6];
        System.arraycopy(array, 1, mac, 0, 6);
        byte[] version = new byte[4];
        System.arraycopy(array, 7, version, 0, 4);
        return new Message(mac, version);
    }


    public byte[] asByteArray() {
        byte[] bytes = new byte[1 + mac.length + version.length];
        bytes[0] = Constants.UDP_LEADING_BYTE;
        System.arraycopy(mac, 0, bytes, 1, mac.length);
        System.arraycopy(version, 0, bytes, mac.length + 1, version.length);
        return bytes;
    }
}

