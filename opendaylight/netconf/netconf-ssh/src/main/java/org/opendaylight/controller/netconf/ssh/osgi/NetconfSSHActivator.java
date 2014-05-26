/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh.osgi;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.opendaylight.controller.netconf.ssh.NetconfSSHServer;
import org.opendaylight.controller.netconf.ssh.authentication.AuthProvider;
import org.opendaylight.controller.netconf.ssh.authentication.PEMGenerator;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.opendaylight.controller.usermanager.IUserManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activator for netconf SSH bundle which creates SSH bridge between netconf client and netconf server. Activator
 * starts SSH Server in its own thread. This thread is closed when activator calls stop() method. Server opens socket
 * and listens for client connections. Each client connection creation is handled in separate
 * {@link org.opendaylight.controller.netconf.ssh.threads.SocketThread} thread.
 * This thread creates two additional threads {@link org.opendaylight.controller.netconf.ssh.threads.IOThread}
 * forwarding data from/to client.IOThread closes servers session and server connection when it gets -1 on input stream.
 * {@link org.opendaylight.controller.netconf.ssh.threads.IOThread}'s run method waits for -1 on input stream to finish.
 * All threads are daemons.
 **/
public class NetconfSSHActivator implements BundleActivator{

    private NetconfSSHServer server;
    private static final Logger logger =  LoggerFactory.getLogger(NetconfSSHActivator.class);
    private IUserManager iUserManager;
    private BundleContext context = null;

    private ServiceTrackerCustomizer<IUserManager, IUserManager> customizer = new ServiceTrackerCustomizer<IUserManager, IUserManager>(){
        @Override
        public IUserManager addingService(final ServiceReference<IUserManager> reference) {
            logger.trace("Service {} added, let there be SSH bridge.", reference);
            iUserManager =  context.getService(reference);
            try {
                onUserManagerFound(iUserManager);
            } catch (final Exception e) {
                logger.trace("Can't start SSH server due to {}",e);
            }
            return iUserManager;
        }
        @Override
        public void modifiedService(final ServiceReference<IUserManager> reference, final IUserManager service) {
            logger.trace("Replacing modified service {} in netconf SSH.", reference);
            server.addUserManagerService(service);
        }
        @Override
        public void removedService(final ServiceReference<IUserManager> reference, final IUserManager service) {
            logger.trace("Removing service {} from netconf SSH. " +
                    "SSH won't authenticate users until IUserManager service will be started.", reference);
            removeUserManagerService();
        }
    };


    @Override
    public void start(final BundleContext context) {
        this.context = context;
        listenForManagerService();
    }

    @Override
    public void stop(BundleContext context) throws IOException {
        if (server != null){
            server.stop();
            logger.trace("Netconf SSH bridge is down ...");
        }
    }
    private void startSSHServer() throws IOException {
        checkNotNull(this.iUserManager, "No user manager service available.");
        logger.trace("Starting netconf SSH  bridge.");
        final InetSocketAddress sshSocketAddress = NetconfConfigUtil.extractSSHNetconfAddress(context,
                NetconfConfigUtil.DEFAULT_NETCONF_SSH_ADDRESS);
        final InetSocketAddress tcpSocketAddress = NetconfConfigUtil.extractTCPNetconfClientAddress(context,
               NetconfConfigUtil.DEFAULT_NETCONF_TCP_ADDRESS);

        String path =  FilenameUtils.separatorsToSystem(NetconfConfigUtil.getPrivateKeyPath(context));

        if (path.isEmpty()) {
            throw new IllegalStateException("Missing netconf.ssh.pk.path key in configuration file.");
        }

        final File privateKeyFile = new File(path);
        final String privateKeyPEMString;
        if (privateKeyFile.exists() == false) {
            // generate & save to file
            try {
                privateKeyPEMString = PEMGenerator.generateTo(privateKeyFile);
            } catch (Exception e) {
                logger.error("Exception occurred while generating PEM string {}", e);
                throw new IllegalStateException("Error generating RSA key from file " + path);
            }
        } else {
            // read from file
            try (FileInputStream fis = new FileInputStream(path)) {
                privateKeyPEMString = IOUtils.toString(fis);
            } catch (final IOException e) {
                logger.error("Error reading RSA key from file '{}'", path);
                throw new IOException("Error reading RSA key from file " + path, e);
            }
        }
        final AuthProvider authProvider = new AuthProvider(iUserManager, privateKeyPEMString);
        this.server = NetconfSSHServer.start(sshSocketAddress.getPort(), tcpSocketAddress, authProvider);

        final Thread serverThread = new Thread(server, "netconf SSH server thread");
        serverThread.setDaemon(true);
        serverThread.start();
        logger.trace("Netconf SSH  bridge up and running.");
    }

    private void onUserManagerFound(final IUserManager userManager) throws Exception{
        if (server!=null && server.isUp()){
           server.addUserManagerService(userManager);
        } else {
           startSSHServer();
        }
    }
    private void removeUserManagerService(){
        this.server.removeUserManagerService();
    }
    private void listenForManagerService(){
        final ServiceTracker<IUserManager, IUserManager> listenerTracker = new ServiceTracker<>(context, IUserManager.class,customizer);
        listenerTracker.open();
    }
}
