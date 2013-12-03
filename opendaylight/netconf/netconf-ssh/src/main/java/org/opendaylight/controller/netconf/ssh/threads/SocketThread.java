package org.opendaylight.controller.netconf.ssh.threads;


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
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.netconf.ssh.authentication.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public class SocketThread implements Runnable, ServerAuthenticationCallback, ServerConnectionCallback
{

    private Socket socket;
    private static final String USER = "netconf";
    private static final String PASSWORD = "netconf";
    private InetSocketAddress clientAddress;
    private static final Logger logger =  LoggerFactory.getLogger(SocketThread.class);
    private ServerConnection conn = null;
    private long sessionId;
    private String currentUser;
    private final String remoteAddressWithPort;


    public static void start(Socket socket, InetSocketAddress clientAddress, long sessionId) throws IOException{
        Thread netconf_ssh_socket_thread = new Thread(new SocketThread(socket,clientAddress,sessionId));
        netconf_ssh_socket_thread.setDaemon(true);
        netconf_ssh_socket_thread.start();
    }
    private SocketThread(Socket socket, InetSocketAddress clientAddress, long sessionId) throws IOException {

        this.socket = socket;
        this.clientAddress = clientAddress;
        this.sessionId = sessionId;
        this.remoteAddressWithPort = socket.getRemoteSocketAddress().toString().replaceFirst("/","");

    }

    @Override
    public void run() {
        conn = new ServerConnection(socket);
        RSAKey keyStore = new RSAKey();
        conn.setRsaHostKey(keyStore.getPrivateKey());
        conn.setAuthenticationCallback(this);
        conn.setServerConnectionCallback(this);
        try {
            conn.connect();
        } catch (IOException e) {
            logger.error("SocketThread error ",e);
        }
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
                            IOThread netconf_ssh_input = null;
                            IOThread  netconf_ssh_output = null;
                            try {
                                String hostName = clientAddress.getHostName();
                                int portNumber = clientAddress.getPort();
                                final Socket echoSocket = new Socket(hostName, portNumber);
                                logger.trace("echo socket created");

                                logger.trace("starting netconf_ssh_input thread");
                                netconf_ssh_input =  new IOThread(echoSocket.getInputStream(),ss.getStdin(),"input_thread_"+sessionId,ss,conn);
                                netconf_ssh_input.setDaemon(false);
                                netconf_ssh_input.start();

                                logger.trace("starting netconf_ssh_output thread");
                                final String customHeader = "["+currentUser+";"+remoteAddressWithPort+";ssh;;;;;;]\n";
                                netconf_ssh_output = new IOThread(ss.getStdout(),echoSocket.getOutputStream(),"output_thread_"+sessionId,ss,conn,customHeader);
                                netconf_ssh_output.setDaemon(false);
                                netconf_ssh_output.start();

                            } catch (Throwable t){
                                logger.error(t.getMessage(),t);

                                try {
                                    if (netconf_ssh_input!=null){
                                        netconf_ssh_input.join();
                                    }
                                } catch (InterruptedException e) {
                                   logger.error("netconf_ssh_input join error ",e);
                                }

                                try {
                                    if (netconf_ssh_output!=null){
                                        netconf_ssh_output.join();
                                    }
                                } catch (InterruptedException e) {
                                    logger.error("netconf_ssh_output join error ",e);
                                }

                            }
                        } else {
                            try {
                                ss.getStdin().write("wrong subsystem requested - closing connection".getBytes());
                                ss.close();
                            } catch (IOException e) {
                                logger.debug("excpetion while sending bad subsystem response",e);
                            }
                        }
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
        logger.trace("Established connection with host {}",remoteAddressWithPort);
        return "Established connection with host "+remoteAddressWithPort+"\r\n";
    }

    public String[] getRemainingAuthMethods(ServerConnection sc)
    {
        return new String[] { ServerAuthenticationCallback.METHOD_PASSWORD };
    }

    public AuthenticationResult authenticateWithNone(ServerConnection sc, String username)
    {
        return AuthenticationResult.FAILURE;
    }

    public AuthenticationResult authenticateWithPassword(ServerConnection sc, String username, String password)
    {
        if (USER.equals(username) && PASSWORD.equals(password)){
            currentUser = username;
            logger.trace("user {}@{} authenticated",currentUser,remoteAddressWithPort);
            return AuthenticationResult.SUCCESS;
        }


        return AuthenticationResult.FAILURE;
    }

    public AuthenticationResult authenticateWithPublicKey(ServerConnection sc, String username, String algorithm,
            byte[] publickey, byte[] signature)
    {
        return AuthenticationResult.FAILURE;
    }

}
