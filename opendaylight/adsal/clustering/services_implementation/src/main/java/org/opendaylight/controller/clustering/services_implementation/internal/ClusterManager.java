
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.services_implementation.internal;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jgroups.Channel;
import org.jgroups.Event;
import org.jgroups.stack.GossipRouter;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.CacheListenerAddException;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.clustering.services.IGetUpdates;
import org.opendaylight.controller.clustering.services.IListenRoleChange;
import org.opendaylight.controller.clustering.services.ListenRoleChangeAddException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterManager implements IClusterServices {
    protected static final Logger logger = LoggerFactory
            .getLogger(ClusterManager.class);
    private DefaultCacheManager cm;
    GossipRouter gossiper;
    private HashSet<IListenRoleChange> roleChangeListeners;
    private ViewChangedListener cacheManagerListener;

    private static String loopbackAddress = InetAddress.getLoopbackAddress().getHostAddress();
    private static final int gossipRouterPortDefault = 12001;
    // defaultTransactionTimeout is 60 seconds
    private static int DEFAULT_TRANSACTION_TIMEOUT = 60;

    /**
     * Start a JGroups GossipRouter if we are a supernode. The
     * GosispRouter is nothing more than a simple
     * rendevouz-pointer. All the nodes that wants to join the cluster
     * will come to any of the rendevouz point and they introduce the
     * nodes to all the others. Once the meet and greet phase if over,
     * the nodes will open a full-mesh with the remaining n-1 nodes,
     * so even if the GossipRouter goes down nothing is lost.
     * NOTE: This function has the side effect to set some of the
     * JGROUPS configurations, this because in this function already
     * we try to retrieve some of the network capabilities of the
     * device and so it's better not to do that again
     *
     *
     * @return GossipRouter
     */
    private GossipRouter startGossiper() {
        boolean amIGossipRouter = false;
        Integer gossipRouterPort = gossipRouterPortDefault;
        InetAddress gossipRouterAddress = null;
        String supernodes_list = System.getProperty("supernodes",
                loopbackAddress);
        /*
         * Check the environment for the "container" variable, if this is set
         * and is equal to "lxc", then ODL is running inside an lxc
         * container, and address resolution of supernodes will be modified
         * accordingly.
         */
        boolean inContainer = "lxc".equals(System.getenv("container"));
        StringBuffer sanitized_supernodes_list = new StringBuffer();
        List<InetAddress> myAddresses = new ArrayList<InetAddress>();

        if (inContainer) {
            logger.trace("DOCKER: Resolving supernode host names using docker container semantics");
        }

        StringTokenizer supernodes = new StringTokenizer(supernodes_list, ":");
        if (supernodes.hasMoreTokens()) {
            // Populate the list of my addresses
            try {
                Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
                while (e.hasMoreElements()) {
                    NetworkInterface n = e.nextElement();
                    Enumeration<InetAddress> ee = n.getInetAddresses();
                    while (ee.hasMoreElements()) {
                        InetAddress i = ee.nextElement();
                        myAddresses.add(i);
                    }
                }
            } catch (SocketException se) {
                logger.error("Cannot get the list of network interfaces");
                return null;
            }
        }
        while (supernodes.hasMoreTokens()) {
            String curr_supernode = supernodes.nextToken();
            logger.debug("Examining supernode {}", curr_supernode);
            StringTokenizer host_port = new StringTokenizer(curr_supernode,
                    "[]");
            String host;
            String port;
            Integer port_num = gossipRouterPortDefault;
            if (host_port.countTokens() > 2) {
                logger.error("Error parsing supernode {} proceed to the next one",
                        curr_supernode);
                continue;
            }
            host = host_port.nextToken();
            InetAddress hostAddr;
            /*
             * If we are in a container and the hostname begins with a '+', this is
             * an indication that we should resolve this host name in the context
             * of a docker container.
             *
             * Specifically this means:
             * '+self'   : self reference and the host will be mapped to the value of
             *             HOSTNAME in the environment
             * '+<name>' : references another container by its name. The docker established
             *             environment variables will be used to resolve the host to an
             *             IP address.
             */
            if (inContainer && host != null && host.charAt(0) == '+') {
                if ("+self".equals(host)) {
                    host = System.getenv("HOSTNAME");
                } else {
                    String link = System.getenv(host.substring(1).toUpperCase() + "_PORT");
                    if (link != null) {
                        try {
                            host = new URI(link).getHost();
                        } catch (URISyntaxException e) {
                            logger.error("DOCKER: Unable to translate container reference ({}) to host IP Address, will attempt using normal host name",
                                host.substring(1));
                        }
                    }
                }
            }

            try {
                hostAddr = InetAddress.getByName(host);
            } catch (UnknownHostException ue) {
                logger.error("Host {} is not known", host);
                continue;
            }
            if (host_port.hasMoreTokens()) {
                port = host_port.nextToken();
                try {
                    port_num = Integer.valueOf(port);
                } catch (NumberFormatException ne) {
                    logger.error("Supplied supernode gossip port is not recognized, using default gossip port {}",
                                 gossipRouterPortDefault);
                    port_num = gossipRouterPortDefault;
                }
                if ((port_num > 65535) || (port_num < 0)) {
                    logger.error("Supplied supernode gossip port is outside a valid TCP port range");
                    port_num = gossipRouterPortDefault;
                }
            }
            if (!amIGossipRouter) {
                if (host != null) {
                    for (InetAddress myAddr : myAddresses) {
                        if (myAddr.equals(hostAddr)) {
                            amIGossipRouter = true;
                            gossipRouterAddress = hostAddr;
                            gossipRouterPort = port_num;
                            break;
                        }
                    }
                }
            }
            if (!sanitized_supernodes_list.toString().equals("")) {
                sanitized_supernodes_list.append(",");
            }
            sanitized_supernodes_list.append(hostAddr.getHostAddress()).append("[").append(port_num).append("]");
        }

        if (amIGossipRouter) {
            // Set the Jgroups binding interface to the one we got
            // from the supernodes attribute
            if (gossipRouterAddress != null) {
                System.setProperty("jgroups.tcp.address", gossipRouterAddress
                        .getHostAddress());
            }
        } else {
            // Set the Jgroup binding interface to the one we are well
            // known outside or else to the first with non-local
            // scope.
            try {
                String myBind = InetAddress.getLocalHost().getHostAddress();
                if (myBind == null
                        || InetAddress.getLocalHost().isLoopbackAddress()) {
                    for (InetAddress myAddr : myAddresses) {
                        if (myAddr.isLoopbackAddress()
                                || myAddr.isLinkLocalAddress()) {
                            logger.debug("Skipping local address {}",
                                         myAddr.getHostAddress());
                            continue;
                        } else {
                            // First non-local address
                            myBind = myAddr.getHostAddress();
                            logger.debug("First non-local address {}", myBind);
                            break;
                        }
                    }
                }
                String jgroupAddress = System
                        .getProperty("jgroups.tcp.address");
                if (jgroupAddress == null) {
                    if (myBind != null) {
                        logger.debug("Set bind address to be {}", myBind);
                        System.setProperty("jgroups.tcp.address", myBind);
                    } else {
                        logger
                                .debug("Set bind address to be LOCALHOST=127.0.0.1");
                        System.setProperty("jgroups.tcp.address", "127.0.0.1");
                    }
                } else {
                    logger.debug("jgroup.tcp.address already set to be {}",
                            jgroupAddress);
                }
            } catch (UnknownHostException uhe) {
                logger
                        .error("Met UnknownHostException while trying to get binding address for jgroups");
            }
        }

        // The supernodes list constitute also the tcpgossip initial
        // host list
        System.setProperty("jgroups.tcpgossip.initial_hosts",
                sanitized_supernodes_list.toString());
        logger.debug("jgroups.tcp.address set to {}",
                System.getProperty("jgroups.tcp.address"));
        logger.debug("jgroups.tcpgossip.initial_hosts set to {}",
                System.getProperty("jgroups.tcpgossip.initial_hosts"));
        GossipRouter res = null;
        if (amIGossipRouter) {
            logger.info("I'm a GossipRouter will listen on port {}",
                    gossipRouterPort);
            // Start a GossipRouter with JMX support
            res = new GossipRouter(gossipRouterPort, null, true);
        }
        return res;
    }

    private void exitOnSecurityException(Exception ioe) {
        Throwable cause = ioe.getCause();
        while (cause != null) {
            if (cause instanceof java.lang.SecurityException) {
                logger.error("Failed Cluster authentication. Stopping Controller...");
                System.exit(0);
            }
            cause = cause.getCause();
        }
    }

    public void start() {
        this.gossiper = startGossiper();
        if (this.gossiper != null) {
            logger.debug("Trying to start Gossiper");
            try {
                this.gossiper.start();
                logger.info("Started GossipRouter");
            } catch (Exception e) {
                logger.error("GossipRouter didn't start. Exception Stack Trace",
                             e);
            }
        }
        logger.info("Starting the ClusterManager");
        try {
            ParserRegistry parser = new ParserRegistry(this.getClass()
                    .getClassLoader());
            String infinispanConfigFile =
                    System.getProperty("org.infinispan.config.file", "config/infinispan-config.xml");
            logger.debug("Using configuration file:{}", infinispanConfigFile);
            ConfigurationBuilderHolder holder = parser.parseFile(infinispanConfigFile);
            GlobalConfigurationBuilder globalBuilder = holder.getGlobalConfigurationBuilder();
            globalBuilder.serialization()
                    .classResolver(new ClassResolver())
                    .build();
            this.cm = new DefaultCacheManager(holder, false);
            logger.debug("Allocated ClusterManager");
            if (this.cm != null) {
                this.cm.start();
                this.cm.startCache();
                logger.debug("Started the ClusterManager");
            }
        } catch (Exception ioe) {
            logger.error("Cannot configure infinispan .. bailing out ");
            logger.error("Stack Trace that raised th exception");
            logger.error("",ioe);
            this.cm = null;
            exitOnSecurityException(ioe);
            this.stop();
        }
        logger.debug("Cache Manager has value {}", this.cm);
    }

    public void stop() {
        logger.info("Stopping the ClusterManager");
        if (this.cm != null) {
            logger.info("Found a valid ClusterManager, now let it be stopped");
            this.cm.stop();
            this.cm = null;
        }
        if (this.gossiper != null) {
            this.gossiper.stop();
            this.gossiper = null;
        }
    }

    @Override
    public ConcurrentMap<?, ?> createCache(String containerName,
            String cacheName, Set<cacheMode> cMode) throws CacheExistException,
            CacheConfigException {
        EmbeddedCacheManager manager = this.cm;
        Cache<Object,Object> c;
        String realCacheName = "{" + containerName + "}_{" + cacheName + "}";
        if (manager == null) {
            return null;
        }

        if (manager.cacheExists(realCacheName)) {
            throw new CacheExistException();
        }

        // Sanity check to avoid contrasting parameters between transactional
        // and not
        if (cMode.containsAll(EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL,
                IClusterServices.cacheMode.TRANSACTIONAL))) {
            throw new CacheConfigException();
        }

        // Sanity check to avoid contrasting parameters between sync and async
        if (cMode.containsAll(EnumSet.of(IClusterServices.cacheMode.SYNC, IClusterServices.cacheMode.ASYNC))) {
            throw new CacheConfigException();
        }

        Configuration fromTemplateConfig = null;
        /*
         * Fetch transactional/non-transactional templates
         */
        // Check if transactional
        if (cMode.contains(IClusterServices.cacheMode.TRANSACTIONAL)) {
            fromTemplateConfig = manager.getCacheConfiguration("transactional-type");
        } else if (cMode.contains(IClusterServices.cacheMode.NON_TRANSACTIONAL)) {
            fromTemplateConfig = manager.getDefaultCacheConfiguration();
        }

        // If none set the transactional property then just return null
        if (fromTemplateConfig == null) {
            return null;
        }

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.read(fromTemplateConfig);
        /*
         * Now evaluate async/sync
         */
        if (cMode.contains(IClusterServices.cacheMode.ASYNC)) {
            builder.clustering()
                    .cacheMode(fromTemplateConfig.clustering()
                            .cacheMode()
                            .toAsync());
        } else if (cMode.contains(IClusterServices.cacheMode.SYNC)) {
            builder.clustering()
                    .cacheMode(fromTemplateConfig.clustering()
                            .cacheMode()
                            .toSync());
        }

        manager.defineConfiguration(realCacheName, builder.build());
        c = manager.getCache(realCacheName);
        return c;
    }

    @Override
    public ConcurrentMap<?, ?> getCache(String containerName, String cacheName) {
        EmbeddedCacheManager manager = this.cm;
        Cache<Object,Object> c;
        String realCacheName = "{" + containerName + "}_{" + cacheName + "}";
        if (manager == null) {
            return null;
        }

        if (manager.cacheExists(realCacheName)) {
            c = manager.getCache(realCacheName);
            return c;
        }
        return null;
    }

    @Override
    public void destroyCache(String containerName, String cacheName) {
        EmbeddedCacheManager manager = this.cm;
        String realCacheName = "{" + containerName + "}_{" + cacheName + "}";
        if (manager == null) {
            return;
        }
        if (manager.cacheExists(realCacheName)) {
            manager.removeCache(realCacheName);
        }
    }

    @Override
    public boolean existCache(String containerName, String cacheName) {
        EmbeddedCacheManager manager = this.cm;

        if (manager == null) {
            return false;
        }

        String realCacheName = "{" + containerName + "}_{" + cacheName + "}";
        return manager.cacheExists(realCacheName);
    }

    @Override
    public Set<String> getCacheList(String containerName) {
        Set<String> perContainerCaches = new HashSet<String>();
        EmbeddedCacheManager manager = this.cm;
        if (manager == null) {
            return null;
        }
        for (String cacheName : manager.getCacheNames()) {
            if (!manager.isRunning(cacheName)) continue;
            if (cacheName.startsWith("{" + containerName + "}_")) {
                String[] res = cacheName.split("[{}]");
                if (res.length >= 4 && res[1].equals(containerName)
                        && res[2].equals("_")) {
                    perContainerCaches.add(res[3]);
                }
            }
        }

        return (perContainerCaches);
    }

    @Override
    public Properties getCacheProperties(String containerName, String cacheName) {
        EmbeddedCacheManager manager = this.cm;
        if (manager == null) {
            return null;
        }
        String realCacheName = "{" + containerName + "}_{" + cacheName + "}";
        if (!manager.cacheExists(realCacheName)) {
            return null;
        }
        Configuration conf = manager.getCache(realCacheName).getAdvancedCache()
                .getCacheConfiguration();
        Properties p = new Properties();
        p.setProperty(IClusterServices.cacheProps.TRANSACTION_PROP.toString(),
                conf.transaction().toString());
        p.setProperty(IClusterServices.cacheProps.CLUSTERING_PROP.toString(),
                conf.clustering().toString());
        p.setProperty(IClusterServices.cacheProps.LOCKING_PROP.toString(), conf
                .locking().toString());
        return p;
    }

    @Override
    public void addListener(String containerName, String cacheName,
            IGetUpdates<?, ?> u) throws CacheListenerAddException {
        EmbeddedCacheManager manager = this.cm;
        Cache<Object,Object> c;
        String realCacheName = "{" + containerName + "}_{" + cacheName + "}";
        if (manager == null) {
            return;
        }

        if (!manager.cacheExists(realCacheName)) {
            throw new CacheListenerAddException();
        }
        c = manager.getCache(realCacheName);
        CacheListenerContainer cl = new CacheListenerContainer(u,
                containerName, cacheName);
        c.addListener(cl);
    }

    @Override
    public Set<IGetUpdates<?, ?>> getListeners(String containerName,
            String cacheName) {
        EmbeddedCacheManager manager = this.cm;
        Cache<Object,Object> c;
        String realCacheName = "{" + containerName + "}_{" + cacheName + "}";
        if (manager == null) {
            return null;
        }

        if (!manager.cacheExists(realCacheName)) {
            return null;
        }
        c = manager.getCache(realCacheName);

        Set<IGetUpdates<?, ?>> res = new HashSet<IGetUpdates<?, ?>>();
        Set<Object> listeners = c.getListeners();
        for (Object listener : listeners) {
            if (listener instanceof CacheListenerContainer) {
                CacheListenerContainer cl = (CacheListenerContainer) listener;
                res.add(cl.whichListener());
            }
        }

        return res;
    }

    @Override
    public void removeListener(String containerName, String cacheName,
            IGetUpdates<?, ?> u) {
        EmbeddedCacheManager manager = this.cm;
        Cache<Object,Object> c;
        String realCacheName = "{" + containerName + "}_{" + cacheName + "}";
        if (manager == null) {
            return;
        }

        if (!manager.cacheExists(realCacheName)) {
            return;
        }
        c = manager.getCache(realCacheName);

        Set<Object> listeners = c.getListeners();
        for (Object listener : listeners) {
            if (listener instanceof CacheListenerContainer) {
                CacheListenerContainer cl = (CacheListenerContainer) listener;
                if (cl.whichListener() == u) {
                    c.removeListener(listener);
                    return;
                }
            }
        }
    }

    @Override
    public void tbegin() throws NotSupportedException, SystemException {
        // call tbegin with the default timeout
        tbegin(DEFAULT_TRANSACTION_TIMEOUT, TimeUnit.SECONDS);
    }

    @Override
    public void tbegin(long timeout, TimeUnit unit) throws NotSupportedException, SystemException {
        EmbeddedCacheManager manager = this.cm;
        if (manager == null) {
            throw new IllegalStateException();
        }
        TransactionManager tm = manager.getCache("transactional-type")
                .getAdvancedCache().getTransactionManager();
        if (tm == null) {
            throw new IllegalStateException();
        }
        long timeoutSec = unit.toSeconds(timeout);
        if((timeoutSec > Integer.MAX_VALUE) || (timeoutSec <= 0)) {
            // fall back to the default timeout
            tm.setTransactionTimeout(DEFAULT_TRANSACTION_TIMEOUT);
        } else {
            // cast is ok here
            // as here we are sure that timeoutSec < = Integer.MAX_VALUE.
            tm.setTransactionTimeout((int) timeoutSec);
        }
        tm.begin();
    }

    @Override
    public void tcommit() throws RollbackException, HeuristicMixedException,
            HeuristicRollbackException, java.lang.SecurityException,
            java.lang.IllegalStateException, SystemException {
        EmbeddedCacheManager manager = this.cm;
        if (manager == null) {
            throw new IllegalStateException();
        }
        TransactionManager tm = manager.getCache("transactional-type")
                .getAdvancedCache().getTransactionManager();
        if (tm == null) {
            throw new IllegalStateException();
        }
        tm.commit();
    }

    @Override
    public void trollback() throws java.lang.IllegalStateException,
            java.lang.SecurityException, SystemException {
        EmbeddedCacheManager manager = this.cm;
        if (manager == null) {
            throw new IllegalStateException();
        }
        TransactionManager tm = manager.getCache("transactional-type")
                .getAdvancedCache().getTransactionManager();
        if (tm == null) {
            throw new IllegalStateException();
        }
        tm.rollback();
    }

    @Override
    public Transaction tgetTransaction() throws SystemException {
        EmbeddedCacheManager manager = this.cm;
        if (manager == null) {
            throw new IllegalStateException();
        }
        TransactionManager tm = manager.getCache("transactional-type")
                .getAdvancedCache().getTransactionManager();
        if (tm == null) {
            return null;
        }
        return tm.getTransaction();
    }

    @Override
    public boolean amIStandby() {
        EmbeddedCacheManager manager = this.cm;
        if (manager == null) {
            // In case we cannot fetch the information, lets assume we
            // are standby, so to have less responsibility.
            return true;
        }
        return (!manager.isCoordinator());
    }

    private InetAddress addressToInetAddress(Address a) {
        EmbeddedCacheManager manager = this.cm;
        if ((manager == null) || (a == null)) {
            // In case we cannot fetch the information, lets assume we
            // are standby, so to have less responsibility.
            return null;
        }
        Transport t = manager.getTransport();
        if (t instanceof JGroupsTransport) {
            JGroupsTransport jt = (JGroupsTransport) t;
            Channel c = jt.getChannel();
            if (a instanceof JGroupsAddress) {
                JGroupsAddress ja = (JGroupsAddress) a;
                org.jgroups.Address phys = (org.jgroups.Address) c
                        .down(new Event(Event.GET_PHYSICAL_ADDRESS, ja
                                .getJGroupsAddress()));
                if (phys instanceof org.jgroups.stack.IpAddress) {
                    InetAddress bindAddress = ((org.jgroups.stack.IpAddress) phys)
                            .getIpAddress();
                    return bindAddress;
                }
            }
        }
        return null;
    }

    @Override
    public List<InetAddress> getClusteredControllers() {
        EmbeddedCacheManager manager = this.cm;
        if (manager == null) {
            return null;
        }
        List<Address> controllers = manager.getMembers();
        if ((controllers == null) || controllers.size() == 0) {
            return null;
        }

        List<InetAddress> clusteredControllers = new ArrayList<InetAddress>();
        for (Address a : controllers) {
            InetAddress inetAddress = addressToInetAddress(a);
            if (inetAddress != null
                    && !inetAddress.getHostAddress().equals(loopbackAddress)) {
                clusteredControllers.add(inetAddress);
            }
        }
        return clusteredControllers;
    }

    @Override
    public InetAddress getMyAddress() {
        EmbeddedCacheManager manager = this.cm;
        if (manager == null) {
            return null;
        }
        return addressToInetAddress(manager.getAddress());
    }

    @Override
    public InetAddress getActiveAddress() {
        EmbeddedCacheManager manager = this.cm;
        if (manager == null) {
            // In case we cannot fetch the information, lets assume we
            // are standby, so to have less responsibility.
            return null;
        }

        return addressToInetAddress(manager.getCoordinator());
    }

    @Override
    public void listenRoleChange(IListenRoleChange i)
            throws ListenRoleChangeAddException {
        EmbeddedCacheManager manager = this.cm;
        if (manager == null) {
            // In case we cannot fetch the information, lets assume we
            // are standby, so to have less responsibility.
            throw new ListenRoleChangeAddException();
        }

        if (this.roleChangeListeners == null) {
            this.roleChangeListeners = new HashSet<IListenRoleChange>();
            this.cacheManagerListener = new ViewChangedListener(
                    this.roleChangeListeners);
            manager.addListener(this.cacheManagerListener);
        }

        if (this.roleChangeListeners != null) {
            this.roleChangeListeners.add(i);
        }
    }

    @Override
    public void unlistenRoleChange(IListenRoleChange i) {
        EmbeddedCacheManager manager = this.cm;
        if (manager == null) {
            // In case we cannot fetch the information, lets assume we
            // are standby, so to have less responsibility.
            return;
        }

        if (this.roleChangeListeners != null) {
            this.roleChangeListeners.remove(i);
        }

        if ((this.roleChangeListeners != null && this.roleChangeListeners
                .isEmpty())
                && (this.cacheManagerListener != null)) {
            manager.removeListener(this.cacheManagerListener);
            this.cacheManagerListener = null;
            this.roleChangeListeners = null;
        }
    }

    @Listener
    public class ViewChangedListener {
        Set<IListenRoleChange> roleListeners;

        public ViewChangedListener(Set<IListenRoleChange> s) {
            this.roleListeners = s;
        }

        @ViewChanged
        public void viewChanged(ViewChangedEvent e) {
            for (IListenRoleChange i : this.roleListeners) {
                i.newActiveAvailable();
            }
        }
    }
}
