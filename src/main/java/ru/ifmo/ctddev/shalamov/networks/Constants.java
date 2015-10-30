package ru.ifmo.ctddev.shalamov.networks;

/**
 * Created by viacheslav on 23.10.2015.
 */
public interface Constants {

    int TICK = 5;
    int BROADCAST_PORT = 1234;
    int TCP_PORT = 4321;
    int BROADCASTS_COUNT = 5;

    byte TCP_LEADING_BYTE = 0x03;
    byte UDP_LEADING_BYTE = 0x02;
    public static final String LOCAL_ADDRESS = "0.0.0.0";

    public static final String piFile = "Pi100million.txt";
}
