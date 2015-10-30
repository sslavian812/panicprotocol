import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;


/**
 * TokenManager class handles all messages related to token.
 * Token consists of following bytes:
 * [0x03, macAddr0, ..., macAddr5,
 * version >>> 24, ..., version >>> 0,
 * nDigits >>> 24, ..., nDigits >>> 0,
 * 3, ...]
 * ^ all calculated Pi's numbers, including "3". (nDigits)
 * <p>
 * Created by viacheslav on 29.10.2015.
 */
public class TokenManager {

    private int nDigits;
    private ConfigurationManager configurationManager;
    private ServerSocket serverSocket;


    private volatile Boolean hasToken;

    public TokenManager(ConfigurationManager manager) {
        this.configurationManager = manager;
        this.nDigits = 0;
        hasToken = false;
        try {
            serverSocket = new ServerSocket(Constants.TCP_PORT);
            serverSocket.setSoTimeout(Constants.TICK);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void gotToken() {
        synchronized (hasToken) {
            hasToken = true;
            hasToken.notify();
        }
    }

    public synchronized void lostToken() {
        synchronized (hasToken) {
            hasToken = false;
            hasToken.notify();
        }
    }

    // todo: russian javadoc

    /**
     * ”зел ждЄт соединение в течение TICK, если не дождалс€, значит,
     * всЄ плохо, и нужно начать реконфигурацию. ≈сли дождалс€,
     * вычитывает токен, провер€ет его и обновл€ет свою версию числа ?,
     * после чего пытаетс€ передавать токен. ≈сли кто-то пытаетс€ соединитьс€
     * с узлом, пока тот находитс€ в состо€нии CONFIG, то, наверное,
     * он не знает, что сейчас идЄт реконфигураци€, поэтому, на вс€кий случай,
     * нужно ему сообщить, начав новую реконфигурацию.
     */
    public void receveToken() {
        while (true) {
            try {
                synchronized (hasToken) {
                    while (hasToken) {
                    System.out.println("RecvToken: waiting while we have token.");
                        hasToken.wait();
                    }
                    System.out.println("RecvToken: NO toke. accepting...");
                }
                Socket socket = serverSocket.accept();
                System.out.println("Just connected to "
                        + socket.getRemoteSocketAddress());


                TCPMessage token = TCPMessage.readTCPMessage(socket.getInputStream());
                socket.close();

                int expectedPrevMac = ByteUitls.bytesToInt(configurationManager.getPrevMac());
                if (configurationManager.getState() == StateEnum.CONFIG
                        || token.getIntVersion() != configurationManager.getVersion()
                        || ByteUitls.bytesToInt(token.mac) != expectedPrevMac)
                    configurationManager.initReconfigure(
                            Math.max(
                                    ByteUitls.bytesToInt(token.version),
                                    configurationManager.getVersion()
                            ) + 1);
                else {
                    nDigits = ByteUitls.bytesToInt(token.nDigits);
//                    update Pi from token
//                    calculate 20 more digits of Pi
                    nDigits += 20;
                    gotToken();
                }
            } catch (SocketTimeoutException s) {
                System.err.println("RecvToken: Socket timed out. reconfiguring with version "
                        + (configurationManager.getVersion() + 1));

                configurationManager.initReconfigure(configurationManager.getVersion() + 1);
            } catch (IOException e) {
                System.err.println(e.getMessage());
                configurationManager.initReconfigure(configurationManager.getVersion() + 1);
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
                configurationManager.initReconfigure(configurationManager.getVersion() + 1);
            }
        }
    }


// todo: russian javadoc

    /**
     *  огда узел получает токен, он пытаетс€ установить соединение со следующим в кольце в течение TICK, если не получилось, паникует, иначе просто передаЄт токен.
     */
    public void sendToken() {
        while (true) {
            try {
                synchronized (hasToken) {
                    System.out.println("SendToken: waiting while we have NO token.");
                    while (!hasToken) {
                        hasToken.wait();
                    }
                    System.out.println("SendToken: we GOT the token. trying to send.");
                }

                System.out.println("Connecting to " + configurationManager.getNextAddress().getHostName() +
                        " on port " + Constants.TCP_PORT);

                Socket client = new Socket(configurationManager.getNextAddress(), Constants.TCP_PORT);

                System.out.println("Just connected to " + client.getRemoteSocketAddress());

                OutputStream outToServer = client.getOutputStream();

                byte[] message = new TCPMessage(configurationManager.getMac(),
                        configurationManager.getVersion(), nDigits).asByteArray();

                outToServer.write(message);
                outToServer.flush();
                client.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
                configurationManager.initReconfigure(configurationManager.getVersion() + 1);
            } catch (SocketTimeoutException s) {
                System.err.println("SendToken: Socket timed out. Reconfiguring with "
                        + (configurationManager.getVersion() + 1));
                configurationManager.initReconfigure(configurationManager.getVersion() + 1);
            } catch (IOException e) {
                e.printStackTrace();
                configurationManager.initReconfigure(configurationManager.getVersion() + 1);
            }
            lostToken();
        }
    }

}
