/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh.osgi;

import com.google.common.base.Optional;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.opendaylight.controller.netconf.ssh.NetconfSSHServer;
import org.opendaylight.controller.netconf.ssh.authentication.AuthProvider;
import org.opendaylight.controller.netconf.ssh.authentication.PEMGenerator;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.usermanager.IUserManager;
import org.opendaylight.controller.usermanager.UserConfig;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.google.common.base.Preconditions.checkNotNull;

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
    private static final String EXCEPTION_MESSAGE = "Netconf ssh bridge is not available.";
    private IUserManager iUserManager;
    private BundleContext context = null;
    private Optional<String> defaultPassword;
    private Optional<String> defaultUser;

    private ServiceTrackerCustomizer<IUserManager, IUserManager> customizer = new ServiceTrackerCustomizer<IUserManager, IUserManager>(){
        @Override
        public IUserManager addingService(ServiceReference<IUserManager> reference) {
            logger.trace("Service {} added, let there be SSH bridge.", reference);
            iUserManager =  context.getService(reference);
            try {
                onUserManagerFound(iUserManager);
            } catch (Exception e) {
                logger.trace("Can't start SSH server due to {}",e);
            }
            return iUserManager;
        }
        @Override
        public void modifiedService(ServiceReference<IUserManager> reference, IUserManager service) {
            logger.trace("Replacing modified service {} in netconf SSH.", reference);
            server.addUserManagerService(service);
        }
        @Override
        public void removedService(ServiceReference<IUserManager> reference, IUserManager service) {
            logger.trace("Removing service {} from netconf SSH. " +
                    "SSH won't authenticate users until IUserManager service will be started.", reference);
            removeUserManagerService();
        }
    };


    @Override
    public void start(BundleContext context)  {
        this.context = context;
        listenForManagerService();
    }

    @Override
    public void stop(BundleContext context) throws IOException {
        if (this.defaultUser.isPresent()){
            this.iUserManager.removeLocalUser(this.defaultUser.get());
        }
        if (server != null){
            server.stop();
            logger.trace("Netconf SSH bridge is down ...");
        }
    }
    private void startSSHServer() throws IllegalStateException, IOException {
        checkNotNull(this.iUserManager, "No user manager service available.");
        logger.trace("Starting netconf SSH  bridge.");
        Optional<InetSocketAddress> sshSocketAddressOptional = NetconfConfigUtil.extractSSHNetconfAddress(context, EXCEPTION_MESSAGE);
        InetSocketAddress tcpSocketAddress = NetconfConfigUtil.extractTCPNetconfAddress(context,
                EXCEPTION_MESSAGE, true);

        if (sshSocketAddressOptional.isPresent()){
            String path =  FilenameUtils.separatorsToSystem(NetconfConfigUtil.getPrivateKeyPath(context));
            if (path.equals("")){
                throw new IllegalStateException("Missing netconf.ssh.pk.path key in configuration file.");
            }

            File privateKeyFile = new File(path);
            String privateKeyPEMString = null;
            if (privateKeyFile.exists() == false) {
                try {
                    privateKeyPEMString = PEMGenerator.generateTo(privateKeyFile);
                } catch (Exception e) {
                    logger.error("Exception occurred while generating PEM string {}",e);
                }
            } else {
                // read from file
                try (FileInputStream fis = new FileInputStream(path)) {
                    privateKeyPEMString = IOUtils.toString(fis);
                } catch (IOException e) {
                    logger.error("Error reading RSA key from file '{}'", path);
                    throw new IllegalStateException("Error reading RSA key from file " + path);
                }
            }
            AuthProvider authProvider = null;
            try {
                this.defaultPassword = NetconfConfigUtil.getSSHDefaultPassword(context);
                this.defaultUser = NetconfConfigUtil.getSSHDefaultUser(context);
                // Since there is no user data store yet (ldap, ...) this adds default user/password to UserManager
                // if these parameters are set in netconf configuration file.
                if (defaultUser.isPresent() &&
                        defaultPassword.isPresent()){
                    logger.trace(String.format("Default username and password for netconf ssh bridge found. Adding user %s to user manager.",defaultUser.get()));
                    List<String> roles = new ArrayList<String>(1);
                    roles.add(UserLevel.SYSTEMADMIN.toString());
                    iUserManager.addLocalUser(new UserConfig(defaultUser.get(), defaultPassword.get(), roles));
                }
                authProvider = new AuthProvider(iUserManager, privateKeyPEMString);
            } catch (Exception e) {
                logger.error("Error instantiating AuthProvider {}",e);
            }
            this.server = NetconfSSHServer.start(sshSocketAddressOptional.get().getPort(),tcpSocketAddress,authProvider);

            Thread serverThread = new  Thread(server,"netconf SSH server thread");
            serverThread.setDaemon(true);
            serverThread.start();
            logger.trace("Netconf SSH  bridge up and running.");
        } else {
            logger.trace("No valid connection configuration for SSH bridge found.");
            throw new IllegalStateException("No valid connection configuration for SSH bridge found.");
        }
    }
    private void onUserManagerFound(IUserManager userManager) throws IOException {
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
        ServiceTracker<IUserManager, IUserManager> listenerTracker = new ServiceTracker<>(context, IUserManager.class,customizer);
        listenerTracker.open();
    }
}
