package org.opendaylight.controller.netconf.ssh;

import java.io.IOException;
import java.net.Socket;

import ch.ethz.ssh2.AuthenticationResult;
import ch.ethz.ssh2.PtySettings;
import ch.ethz.ssh2.ServerAuthenticationCallback;
import ch.ethz.ssh2.ServerConnection;
import ch.ethz.ssh2.ServerConnectionCallback;
import ch.ethz.ssh2.ServerSession;
import ch.ethz.ssh2.ServerSessionCallback;
import ch.ethz.ssh2.SimpleServerSessionCallback;


public class SocketThread implements Runnable, ServerAuthenticationCallback, ServerConnectionCallback
{

    private Socket socket;
    private static ServerConnection conn = null;

    public static void start(Socket socket) throws IOException{
        new Thread(new SocketThread(socket)).start();
    }
    private SocketThread(Socket socket) throws IOException {

        this.socket = socket;

        conn = new ServerConnection(socket);
        conn.setRsaHostKey(AuthProvider.getHostkey());
        conn.setAuthenticationCallback(this);
        conn.setServerConnectionCallback(this);
        conn.connect();
    }

    @Override
    public void run() {
        // do sth with socket
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
                            //TODO open connection to netconf server
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
                            while (true)
                            {
                                int c = ss.getStdout().read();
                                if (c < 0)
                                {
                                    System.err.println("SESSION EOF");
                                    return;
                                }
                                //TODO forward data to netconf server
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
        if (AuthProvider.getUser().equals(username) && AuthProvider.getPassword().equals(password))
            return AuthenticationResult.SUCCESS;

        return AuthenticationResult.FAILURE;
    }

    public AuthenticationResult authenticateWithPublicKey(ServerConnection sc, String username, String algorithm,
            byte[] publickey, byte[] signature)
    {
        return AuthenticationResult.FAILURE;
    }

}
