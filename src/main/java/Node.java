/**
 * Created by viacheslav on 23.10.2015.
 */
public class Node implements Runnable {
    @Override
    public void run() {

        ConfigurationManager configurationManager = ConfigurationManager.getInstance();

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                configurationManager.receiveReconfigure();
            }
        });

        TokenManager tokenManager = new TokenManager(configurationManager);

        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                tokenManager.receveToken();
            }
        });

        Thread t3 = new Thread(new Runnable() {
            @Override
            public void run() {
                tokenManager.sendToken();
            }
        });

        t1.start();

        configurationManager.initReconfigure(1);

        try {
            Thread.sleep(Constants.TICK);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        t2.start();
        t3.start();
    }
}
