package org.opendaylight.controller.netconf.ssh;


import ch.ethz.ssh2.AuthenticationResult;
import ch.ethz.ssh2.PtySettings;
import ch.ethz.ssh2.ServerAuthenticationCallback;
import ch.ethz.ssh2.ServerConnection;
import ch.ethz.ssh2.ServerConnectionCallback;
import ch.ethz.ssh2.ServerSession;
import ch.ethz.ssh2.ServerSessionCallback;
import ch.ethz.ssh2.SimpleServerSessionCallback;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.apache.commons.io.IOUtils;
import org.opendaylight.controller.netconf.ssh.authentication.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SocketThread implements Runnable, ServerAuthenticationCallback, ServerConnectionCallback
{

    private Socket socket;
    private static final String USER = "netconf";
    private static final String PASSWORD = "netconf";
    private static final InetSocketAddress clientAddress = new InetSocketAddress("127.0.0.1", 12023);
    private static final Logger logger =  LoggerFactory.getLogger(SocketThread.class);


    private static ServerConnection conn = null;

    public static void start(Socket socket) throws IOException{
        new Thread(new SocketThread(socket)).start();
    }
    private SocketThread(Socket socket) throws IOException {

        this.socket = socket;

        conn = new ServerConnection(socket);
        RSAKey keyStore = new RSAKey();
        conn.setRsaHostKey(keyStore.getPrivateKey());
        conn.setAuthenticationCallback(this);
        conn.setServerConnectionCallback(this);
        conn.connect();
    }

    @Override
    public void run() {
        //noop
    }
    public ServerSessionCallback acceptSession(final ServerSession session)
    {
        SimpleServerSessionCallback cb = new SimpleServerSessionCallback()
        {
            @Override
            public Runnable requestSubsystem(final ServerSession ss, final String subsystem) throws IOException
            {
                return new Runnable(){
                    public void run()
                    {
                        if (subsystem.equals("netconf")){
                            logger.info("netconf subsystem received");
                            try {
                                String hostName = clientAddress.getHostName();
                                int portNumber = clientAddress.getPort();
                                final Socket echoSocket = new Socket(hostName, portNumber);
                                logger.info("echo socket created");
                                new Thread(){
                                    @Override
                                    public void run() {
                                        try {
                                            IOUtils.copy(echoSocket.getInputStream(), ss.getStdin());
                                        } catch (IOException e) {
                                            logger.error("client -> server stream copy error ",e);
                                        }
                                    }
                                }.start();

                                new Thread(){
                                    @Override
                                    public void run() {
                                        try {
                                            IOUtils.copy(ss.getStdout(), echoSocket.getOutputStream());
                                        } catch (IOException e) {
                                            logger.error("server -> client stream copy error ",e);
                                        }
                                    }
                                }.start();
                            } catch (Throwable t){
                                logger.error(t.getMessage(),t);
                            }
                        }// else? what if not subsystem is sent?
                    }
                };
            }
            @Override
            public Runnable requestPtyReq(final ServerSession ss, final PtySettings pty) throws IOException
            {
                return new Runnable()
                {
                    public void run()
                    {
                        //noop
                    }
                };
            }

            @Override
            public Runnable requestShell(final ServerSession ss) throws IOException
            {
                return new Runnable()
                {
                    public void run()
                    {
                        //noop
                    }
                };
            }
        };

        return cb;
    }

    public String initAuthentication(ServerConnection sc)
    {
        return "";
    }

    public String[] getRemainingAuthMethods(ServerConnection sc)
    {
        return new String[] { ServerAuthenticationCallback.METHOD_PASSWORD,
                ServerAuthenticationCallback.METHOD_PUBLICKEY };// why announing publickey?
    }

    public AuthenticationResult authenticateWithNone(ServerConnection sc, String username)
    {
        return AuthenticationResult.FAILURE;
    }

    public AuthenticationResult authenticateWithPassword(ServerConnection sc, String username, String password)
    {
        if (USER.equals(username) && PASSWORD.equals(password))
            return AuthenticationResult.SUCCESS;

        return AuthenticationResult.FAILURE;
    }

    public AuthenticationResult authenticateWithPublicKey(ServerConnection sc, String username, String algorithm,
            byte[] publickey, byte[] signature)
    {
        return AuthenticationResult.FAILURE;
    }

}
