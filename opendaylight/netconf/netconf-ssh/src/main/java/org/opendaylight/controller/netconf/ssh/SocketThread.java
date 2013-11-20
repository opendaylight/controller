package org.opendaylight.controller.netconf.ssh;


import ch.ethz.ssh2.AuthenticationResult;
import ch.ethz.ssh2.PtySettings;
import ch.ethz.ssh2.ServerAuthenticationCallback;
import ch.ethz.ssh2.ServerConnection;
import ch.ethz.ssh2.ServerConnectionCallback;
import ch.ethz.ssh2.ServerSession;
import ch.ethz.ssh2.ServerSessionCallback;
import ch.ethz.ssh2.SimpleServerSessionCallback;
import com.google.common.base.Optional;
import io.netty.channel.nio.NioEventLoopGroup;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import javax.net.ssl.SSLContext;
import org.opendaylight.controller.netconf.client.NetconfClient;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.NetconfClientSession;
import org.opendaylight.controller.netconf.ssh.authentication.RSAKey;
import org.opendaylight.controller.netconf.ssh.handler.SSHChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SocketThread implements Runnable, ServerAuthenticationCallback, ServerConnectionCallback
{

    private Socket socket;
    private static final String USER = "netconf";
    private static final String PASSWORD = "netconf";
    private NetconfClient netconfClient;
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
            public Runnable requestSubsystem(ServerSession ss, final String subsystem) throws IOException
            {
                return new Runnable(){
                    public void run()
                    {
                        if (subsystem.equals("netconf")){
                            logger.info("netconf subsystem received");
                            try {
                                NetconfClientDispatcher clientDispatcher = null;
                                NioEventLoopGroup nioGrup = new NioEventLoopGroup(1);
                                clientDispatcher = new NetconfClientDispatcher(Optional.<SSLContext>absent(), nioGrup, nioGrup);
                                logger.info("dispatcher created");
                                netconfClient = new NetconfClient("ssh_" + clientAddress.toString(),clientAddress,5000,clientDispatcher);
                                logger.info("netconf client created");
                            } catch (Throwable t){
                                logger.error(t.getMessage(),t);
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
                        System.out.println("Client requested " + pty.term + " pty");
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
                        try
                        {
                            try (NetconfClientSession session = netconfClient.getClientSession())
                            {
                                session.getChannel().pipeline().addLast(new SSHChannelInboundHandler(ss));
                                byte[] bytes = new byte[1024];
                                while (true)
                                {
                                    int size = ss.getStdout().read(bytes);
                                    if (size < 0)
                                    {
                                        System.err.println("SESSION EOF");
                                        return;
                                    }
                                    session.getChannel().write(ByteBuffer.wrap(bytes,0,size));
                                }
                            }

                        }
                        catch (IOException e)
                        {
                            System.err.println("SESSION DOWN");
                            e.printStackTrace();
                        }
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
                ServerAuthenticationCallback.METHOD_PUBLICKEY };
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
