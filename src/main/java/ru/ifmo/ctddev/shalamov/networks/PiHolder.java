package ru.ifmo.ctddev.shalamov.networks;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static ru.ifmo.ctddev.shalamov.networks.Constants.piFile;

/**
 * This singleon class stores Pi's digits as array of bytes and provides digits bu position.
 * Created by viacheslav on 23.10.2015.
 */
public class PiHolder {

    private List<Byte> digits;

    // lazy holder way:
    private static class LazyHolder {
        public static final PiHolder INSTANCE = new PiHolder();
    }

    /**
     * Provides instance of singleton class.
     *
     * @return
     */
    public static PiHolder getInstance() {
        return LazyHolder.INSTANCE;
    }

    /**
     * Provides digit of Pi as a byte by it's position. 0 -> 3, 1->1, 2->4, 3->1 and so on.
     *
     * @param k digit's position
     * @return k-th digit
     */
    public byte getDigit(int k) {
        if (k < digits.size())
            return digits.get(k);
        else
            return getDigit(k % digits.size());
    }

    public byte[] getHeadDigits(int k) {
        return toPrimitives((Byte[]) digits.subList(0, k).toArray());
    }

    byte[] toPrimitives(Byte[] oBytes) {
        byte[] bytes = new byte[oBytes.length];
        for (int i = 0; i < oBytes.length; i++) {
            bytes[i] = oBytes[i];
        }
        return bytes;
    }


    /**
     * private constructor. Reads File storing Pi digits.
     */
    private PiHolder() {
        digits = new ArrayList<>(10 * 1000 * 1000 + 60);
        BufferedReader br = null;
        String line = "";
        try {
            br = new BufferedReader(new FileReader(piFile));
            while ((line = br.readLine()) != null && digits.size() < 10000000) {

                String s = line.split(":")[0].replaceAll(" ", "");
                for (int i = 0; i < s.length(); i++) {
                    digits.add((byte) (s.charAt(i) - '0'));
                }
            }

        } catch (FileNotFoundException e) {
            System.out.println("file: " + piFile + " not found!");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
