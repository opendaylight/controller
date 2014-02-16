/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh.threads;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.controller.netconf.ssh.authentication.AuthProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.ssh2.AuthenticationResult;
import ch.ethz.ssh2.PtySettings;
import ch.ethz.ssh2.ServerAuthenticationCallback;
import ch.ethz.ssh2.ServerConnection;
import ch.ethz.ssh2.ServerConnectionCallback;
import ch.ethz.ssh2.ServerSession;
import ch.ethz.ssh2.ServerSessionCallback;
import ch.ethz.ssh2.SimpleServerSessionCallback;

@ThreadSafe
public class SocketThread implements Runnable, ServerAuthenticationCallback, ServerConnectionCallback {
    private static final Logger logger =  LoggerFactory.getLogger(SocketThread.class);

    private final Socket socket;
    private final InetSocketAddress clientAddress;
    private ServerConnection conn = null;
    private final long sessionId;
    private String currentUser;
    private final String remoteAddressWithPort;
    private final AuthProvider authProvider;


    public static void start(Socket socket,
                             InetSocketAddress clientAddress,
                             long sessionId,
                             AuthProvider authProvider) throws IOException{
        Thread netconf_ssh_socket_thread = new Thread(new SocketThread(socket,clientAddress,sessionId,authProvider));
        netconf_ssh_socket_thread.setDaemon(true);
        netconf_ssh_socket_thread.start();
    }
    private SocketThread(Socket socket,
                         InetSocketAddress clientAddress,
                         long sessionId,
                         AuthProvider authProvider) throws IOException {

        this.socket = socket;
        this.clientAddress = clientAddress;
        this.sessionId = sessionId;
        this.remoteAddressWithPort = socket.getRemoteSocketAddress().toString().replaceFirst("/","");
        this.authProvider = authProvider;

    }

    @Override
    public void run() {
        conn = new ServerConnection(socket);
        try {
            conn.setPEMHostKey(authProvider.getPEMAsCharArray(),"netconf");
        } catch (Exception e) {
            logger.debug("Server authentication setup failed.");
        }
        conn.setAuthenticationCallback(this);
        conn.setServerConnectionCallback(this);
        try {
            conn.connect();
        } catch (IOException e) {
            logger.error("SocketThread error ",e);
        }
    }
    @Override
    public ServerSessionCallback acceptSession(final ServerSession session)
    {
        SimpleServerSessionCallback cb = new SimpleServerSessionCallback()
        {
            @Override
            public Runnable requestSubsystem(final ServerSession ss, final String subsystem) throws IOException
            {
                return new Runnable(){
                    @Override
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
                                logger.error("SSH bridge couldn't create echo socket",t.getMessage(),t);

                                try {
                                    if (netconf_ssh_input!=null){
                                        netconf_ssh_input.join();
                                    }
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                   logger.error("netconf_ssh_input join error ",e);
                                }

                                try {
                                    if (netconf_ssh_output!=null){
                                        netconf_ssh_output.join();
                                    }
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
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
                    @Override
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
                    @Override
                    public void run()
                    {
                        //noop
                    }
                };
            }
        };

        return cb;
    }

    @Override
    public String initAuthentication(ServerConnection sc)
    {
        logger.trace("Established connection with host {}",remoteAddressWithPort);
        return "Established connection with host "+remoteAddressWithPort+"\r\n";
    }

    @Override
    public String[] getRemainingAuthMethods(ServerConnection sc)
    {
        return new String[] { ServerAuthenticationCallback.METHOD_PASSWORD };
    }

    @Override
    public AuthenticationResult authenticateWithNone(ServerConnection sc, String username)
    {
        return AuthenticationResult.FAILURE;
    }

    @Override
    public AuthenticationResult authenticateWithPassword(ServerConnection sc, String username, String password)
    {

        try {
            if (authProvider.authenticated(username,password)){
                currentUser = username;
                logger.trace("user {}@{} authenticated",currentUser,remoteAddressWithPort);
                return AuthenticationResult.SUCCESS;
            }
        } catch (Exception e){
            logger.warn("Authentication failed due to :" + e.getLocalizedMessage());
        }
        return AuthenticationResult.FAILURE;
    }

    @Override
    public AuthenticationResult authenticateWithPublicKey(ServerConnection sc, String username, String algorithm,
            byte[] publickey, byte[] signature)
    {
        return AuthenticationResult.FAILURE;
    }

}
