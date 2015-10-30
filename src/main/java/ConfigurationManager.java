import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ConfigurationManager class handles all the service
 * messages intended to establish connection between nodes.
 * This class deals with following service messages:
 * bytes: [0x02, macAddr0, ..., macAddr5,
 * version >>> 24, ..., version >>> 0]
 * <p>
 * Created by viacheslav on 23.10.2015.
 */
public class ConfigurationManager {
    private int version;
    private byte[] mac;
    private StateEnum state; // also used as monitor.
    private byte[] nextMac, prevMac;
    private Map<Integer, InetAddress> addresses;
    private boolean isBadConfig = false;
    private HashMap<NetworkInterface, byte[]> networksToMacs;
    private HashMap<InetAddress, byte[]> broadcastToMacs;
    private DatagramSocket sendSocket;
    private DatagramSocket socket;

    // lazy holder way:
    private static class LazyHolder {
        public static final ConfigurationManager INSTANCE = new ConfigurationManager();
    }
    /**
     * Created ConfigurationManager object, but without specified mac.
     */
    private ConfigurationManager() {
        this.mac = null;
        this.version = 0;
        state = StateEnum.CONFIG;
        addresses = new HashMap<>();
        networksToMacs = new HashMap<>();
        broadcastToMacs = new HashMap<>();
        try {
            sendSocket = new DatagramSocket();
            sendSocket.setBroadcast(true);
//            socket = new DatagramSocket(Constants.BROADCAST_PORT, InetAddress.getByName(Constants.LOCAL_ADDRESS));
//            socket.setBroadcast(true);
        } catch (SocketException e) {
            System.out.println("NO socket!!");
//        } catch (UnknownHostException e) {
//            // never thrown;
        }
    }
    /**
     * Provides instance of singleton class.
     *
     * @return
     */
    public static ConfigurationManager getInstance() {
        if (LazyHolder.INSTANCE.mac == null) {
            LazyHolder.INSTANCE.scanNetwork();
        }
        return LazyHolder.INSTANCE;
    }

    private void scanNetwork() {
        if (this.mac != null)
            return;
        try {
            Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                    .filter(networkInterface -> {
                        try {
                            return networkInterface.isUp() && (!networkInterface.isLoopback());
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .forEach(network -> {
                        try {
                            final byte[] currentMac = network.getHardwareAddress();
                            if (currentMac != null)
                                network.getInterfaceAddresses().stream()
                                        .filter(address -> address.getBroadcast() != null)
                                        .forEach(address -> {
                                            InetAddress broadcastAddress = address.getBroadcast();
                                            networksToMacs.put(network, currentMac);
                                            broadcastToMacs.put(broadcastAddress, currentMac);
                                        });
                        } catch (SocketException e) {
                            System.err.println(e.getMessage());
                        }
                    });
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println(networksToMacs.size() + " up networks found.");
        networksToMacs.entrySet().stream().forEach(e -> {
            System.out.println(e.getKey() + " - " + ByteUitls.bytesToHex(e.getValue()));
            this.mac = e.getValue();
        });

        System.out.println();
        System.out.println("addresses: ");
        broadcastToMacs.entrySet().stream().forEach(e -> {
            System.out.println(e.getKey() + " - " + ByteUitls.bytesToHex(e.getValue()));
            this.mac = e.getValue();
        });
    }


    private void setState(StateEnum st) {
        this.state = st;
        this.state.notifyAll();
    }

    public InetAddress getNextAddress() throws InterruptedException {
        while (state == StateEnum.CONFIG) {
            state.wait();
        }
        return addresses.get(ByteUitls.bytesToInt(nextMac));
    }

    public byte[] getPrevMac() throws InterruptedException {
        while (state == StateEnum.CONFIG) {
            state.wait();
        }
        return prevMac;
    }


    public int getVersion() {
        return version;
    }

    public byte[] getMac() {
        return mac;
    }

    public StateEnum getState() {
        return state;
    }


    // todo russian javadoc

    /**
     * ≈сли узел получает сообщение Reconfigure(macAddr, version)
     * с большей версией, чем актуальна€ дл€ него, то он принимает
     * новую версию и переходит в состо€ние реконфигурации.
     */
    public synchronized void receiveReconfigure() {

        while (true) {
            try {
                socket = new DatagramSocket(Constants.BROADCAST_PORT, InetAddress.getByName(Constants.LOCAL_ADDRESS));
                socket.setBroadcast(true);
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }


            Message message = receiveBroadcastMessage(); // blocking call
            if (message.getIntVersion() > this.version)
                reconfigure(message.version, message.mac);
            else if (message.getIntVersion() < this.version
                    || message.getIntVersion() == this.version
                    && this.state != StateEnum.CONFIG)
                reconfigure(ByteUitls.intToBytes(this.version + 1), this.mac);
        }
    }

    private void sendBroadcastMessage(int version) {
        broadcastToMacs.entrySet().stream().forEach(entry -> {
            InetAddress broadcastAddress = entry.getKey();
            byte[] messageBytes = new Message(entry.getValue(), ByteUitls.intToBytes(version)).asByteArray();
            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length);
            packet.setPort(Constants.BROADCAST_PORT);
            packet.setAddress(broadcastAddress);
            try {
                sendSocket.send(packet);
            } catch (IOException exc) {
                System.out.println("fail to send packet");
            }
        });
    }

    private Message receiveBroadcastMessage() {

        try {
            byte[] recvBuf = new byte[1 + 6 + 4];
            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(packet);
            return Message.fromArray(recvBuf);

        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            return null;
        }
    }

    //todo russian javadoc

    /**
     * ¬ состо€нии реконфигурации узел рассылает сообщени€ со своим MAC-адресом
     * и версией и параллельно принимает такие сообщени€ от других узлов.
     * ≈сли вдруг узлу приходит сообщение Reconfigure с неправильной версией,
     * то он считает эту попытку реконфигурации неудачной и начинает новую
     * (рекурсивный вызов reconfigure).
     *
     * @param newVersion
     * @param initializerMacAddr
     */
    private void reconfigure(byte[] newVersion, byte[] initializerMacAddr) {
        setState(StateEnum.CONFIG);
        this.version = ByteUitls.bytesToInt(newVersion);

        Set<byte[]> neighbours = new HashSet<>(2);
        neighbours.add(this.mac);
        neighbours.add(initializerMacAddr);

        isBadConfig = false;

        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int k = 0; k < Constants.BROADCASTS_COUNT
                        && !Thread.currentThread().isInterrupted(); ++k) {
                    sendBroadcastMessage(version);
                }
            }
        });

        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                // todo: while true/alive/!interrupted here?
                while (!Thread.currentThread().isInterrupted()) {
                    Message message = receiveBroadcastMessage();
                    if (message.getIntVersion() == version)
                        neighbours.add(message.mac);
                    else {
                        isBadConfig = true;
                        return;
                    }
                }
            }
        });

        thread1.start();
        thread2.start();

        try {
            thread1.join(Constants.TICK * 1000);
            thread1.interrupt();
            thread2.join(Constants.TICK * 1000);
            thread2.interrupt();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        isBadConfig |= neighbours.size() <= 1;

        // todo: what the mac addr here?
        if (isBadConfig)
            reconfigure(ByteUitls.intToBytes(version + 1), this.mac);
        else {

            int first = updateNextAndPrev(neighbours);
            if (first == ByteUitls.bytesToInt(this.mac)) {
                try {
                    Thread.sleep(Constants.TICK * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //gotToken();
            }
            setState(StateEnum.WORKING);
        }
    }

    private int updateNextAndPrev(Set<byte[]> neighbours) {
        List<Integer> macs = neighbours.stream()
                .map(b -> ByteUitls.bytesToInt(b))
                .sorted(Comparator.<Integer>naturalOrder())
                .collect(Collectors.toList());
        int p = macs.indexOf(ByteUitls.bytesToInt(this.mac));
        nextMac = ByteUitls.intToBytes(macs.get((p + 1) % macs.size()));
        prevMac = ByteUitls.intToBytes(macs.get((p - 1) % macs.size()));
        return macs.get(0);
    }


    // todo: russian javadoc

    /**
     * Ётот метод позвол€ет узлу начать реконфигурацию
     *
     * @param newVersion
     */
    public synchronized void initReconfigure(int newVersion) {
        while (this.version < newVersion) {
            sendBroadcastMessage(newVersion);
        }
    }
}
