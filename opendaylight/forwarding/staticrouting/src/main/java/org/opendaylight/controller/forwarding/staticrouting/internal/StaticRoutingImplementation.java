/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwarding.staticrouting.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.forwarding.staticrouting.IForwardingStaticRouting;
import org.opendaylight.controller.forwarding.staticrouting.IStaticRoutingAware;
import org.opendaylight.controller.forwarding.staticrouting.StaticRoute;
import org.opendaylight.controller.forwarding.staticrouting.StaticRouteConfig;
import org.opendaylight.controller.hosttracker.HostIdFactory;
import org.opendaylight.controller.hosttracker.IHostId;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.IObjectReader;
import org.opendaylight.controller.sal.utils.ObjectReader;
import org.opendaylight.controller.sal.utils.ObjectWriter;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static Routing feature provides the bridge between SDN and Non-SDN networks.
 */
public class StaticRoutingImplementation implements IfNewHostNotify, IForwardingStaticRouting, IObjectReader,
        IConfigurationContainerAware {
    private static Logger log = LoggerFactory.getLogger(StaticRoutingImplementation.class);
    private static String ROOT = GlobalConstants.STARTUPHOME.toString();
    ConcurrentMap<String, StaticRoute> staticRoutes;
    ConcurrentMap<String, StaticRouteConfig> staticRouteConfigs;
    private IfIptoHost hostTracker;
    private Timer gatewayProbeTimer;
    private String staticRoutesFileName = null;
    private IClusterContainerServices clusterContainerService = null;
    private Set<IStaticRoutingAware> staticRoutingAware = Collections
            .synchronizedSet(new HashSet<IStaticRoutingAware>());
    private ExecutorService executor;

    void setStaticRoutingAware(IStaticRoutingAware s) {
        if (this.staticRoutingAware != null) {
            this.staticRoutingAware.add(s);
        }
    }

    void unsetStaticRoutingAware(IStaticRoutingAware s) {
        if (this.staticRoutingAware != null) {
            this.staticRoutingAware.remove(s);
        }
    }

    public void setHostTracker(IfIptoHost hostTracker) {
        log.debug("Setting HostTracker");
        this.hostTracker = hostTracker;
    }

    public void unsetHostTracker(IfIptoHost hostTracker) {
        if (this.hostTracker == hostTracker) {
            this.hostTracker = null;
        }
    }

    @Override
    public ConcurrentMap<String, StaticRouteConfig> getStaticRouteConfigs() {
        return staticRouteConfigs;
    }

    @Override
    public Object readObject(ObjectInputStream ois) throws FileNotFoundException, IOException, ClassNotFoundException {
        // Perform the class deserialization locally, from inside the package
        // where the class is defined
        return ois.readObject();
    }

    @SuppressWarnings("unchecked")
    private void loadConfiguration() {
        ObjectReader objReader = new ObjectReader();
        ConcurrentMap<String, StaticRouteConfig> confList = (ConcurrentMap<String, StaticRouteConfig>) objReader.read(
                this, staticRoutesFileName);

        if (confList == null) {
            return;
        }

        for (StaticRouteConfig conf : confList.values()) {
            addStaticRoute(conf);
        }
    }

    private Status saveConfig() {
        return saveConfigInternal();
    }

    public Status saveConfigInternal() {
        Status status;
        ObjectWriter objWriter = new ObjectWriter();

        status = objWriter.write(new ConcurrentHashMap<String, StaticRouteConfig>(staticRouteConfigs),
                staticRoutesFileName);

        if (status.isSuccess()) {
            return status;
        } else {
            return new Status(StatusCode.INTERNALERROR, "Save failed");
        }
    }

    @SuppressWarnings("deprecation")
    private void allocateCaches() {
        if (this.clusterContainerService == null) {
            log.info("un-initialized clusterContainerService, can't create cache");
            return;
        }

        try {
            clusterContainerService.createCache("forwarding.staticrouting.routes",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
            clusterContainerService.createCache("forwarding.staticrouting.configs",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
        } catch (CacheExistException cee) {
            log.error("\nCache already exists - destroy and recreate if needed");
        } catch (CacheConfigException cce) {
            log.error("\nCache configuration invalid - check cache mode");
        }
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private void retrieveCaches() {
        if (this.clusterContainerService == null) {
            log.info("un-initialized clusterContainerService, can't retrieve cache");
            return;
        }

        staticRoutes = (ConcurrentMap<String, StaticRoute>) clusterContainerService
                .getCache("forwarding.staticrouting.routes");
        if (staticRoutes == null) {
            log.error("\nFailed to get rulesDB handle");
        }

        staticRouteConfigs = (ConcurrentMap<String, StaticRouteConfig>) clusterContainerService
                .getCache("forwarding.staticrouting.configs");
        if (staticRouteConfigs == null) {
            log.error("\nFailed to get rulesDB handle");
        }
    }

    private void notifyStaticRouteUpdate(StaticRoute s, boolean update) {
        if (this.staticRoutingAware != null) {
            log.info("Invoking StaticRoutingAware listeners");
            synchronized (this.staticRoutingAware) {
                for (IStaticRoutingAware ra : this.staticRoutingAware) {
                    try {
                        ra.staticRouteUpdate(s, update);
                    } catch (Exception e) {
                        log.error("", e);
                    }
                }
            }
        }
    }

    private class NotifyStaticRouteWorker implements Callable<Object> {

        private String name;
        private StaticRoute staticRoute;
        private boolean added;

        public NotifyStaticRouteWorker(String name, StaticRoute s, boolean update) {
            this.name = name;
            this.staticRoute = s;
            this.added = update;
        }

        @Override
        public Object call() throws Exception {
            if (!added || (staticRoute.getType() == StaticRoute.NextHopType.SWITCHPORT)) {
                notifyStaticRouteUpdate(staticRoute, added);
            } else {
                InetAddress nh = staticRoute.getNextHopAddress();
                // HostTracker hosts db key scheme implementation
                IHostId id = HostIdFactory.create(nh, null);
                HostNodeConnector host = hostTracker.hostQuery(id);
                if (host == null) {
                    log.debug("Next hop {}  is not present, try to discover it", nh.getHostAddress());
                    Future<HostNodeConnector> future = hostTracker.discoverHost(id);
                    if (future != null) {
                        try {
                            host = future.get();
                        } catch (InterruptedException ioe) {
                            log.trace("Thread interrupted {}", ioe);
                        } catch (Exception e) {
                            log.error("", e);
                        }
                    }
                }
                if (host != null) {
                    log.debug("Next hop {} is found", nh.getHostAddress());
                    staticRoute.setHost(host);
                    // static route object has changed
                    // put the changed object back in the cache
                    // for it to sync
                    staticRoutes.put(name, staticRoute);
                    notifyStaticRouteUpdate(staticRoute, added);
                } else {
                    log.debug("Next hop {}  is still not present, try again later", nh.getHostAddress());
                }
            }
            return null;
        }
    }

    private void checkAndUpdateListeners(String name, StaticRoute staticRoute, boolean added) {
        NotifyStaticRouteWorker worker = new NotifyStaticRouteWorker(name, staticRoute, added);
        try {
            executor.submit(worker);
        } catch (Exception e) {
            log.error("got Exception ", e);
        }
    }

    private void notifyHostUpdate(HostNodeConnector host, boolean added) {
        if (host == null) {
            return;
        }
        for (Map.Entry<String, StaticRoute> s : staticRoutes.entrySet()) {
            StaticRoute route = s.getValue();
            if (route.getType() == StaticRoute.NextHopType.SWITCHPORT) {
                continue;
            }
            if (route.getNextHopAddress().equals(host.getNetworkAddress())) {
                if (added) {
                    route.setHost(host);
                } else {
                    route.setHost(null);
                }
                // static route object has changed
                // put the changed object back in the cache
                // for it to sync
                staticRoutes.put(s.getKey(), route);
                notifyStaticRouteUpdate(route, added);
            }
        }
    }

    @Override
    public void notifyHTClient(HostNodeConnector host) {
        notifyHostUpdate(host, true);
    }

    @Override
    public void notifyHTClientHostRemoved(HostNodeConnector host) {
        notifyHostUpdate(host, false);
    }

    public boolean isIPv4AddressValid(String cidr) {
        if (cidr == null) {
            return false;
        }

        String values[] = cidr.split("/");
        Pattern ipv4Pattern = Pattern
                .compile("(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])");
        Matcher mm = ipv4Pattern.matcher(values[0]);
        if (!mm.matches()) {
            log.debug("IPv4 source address {} is not valid", cidr);
            return false;
        }
        if (values.length >= 2) {
            int prefix = Integer.valueOf(values[1]);
            if ((prefix < 0) || (prefix > 32)) {
                log.debug("prefix {} is not valid", prefix);
                return false;
            }
        }
        return true;
    }

    public static short getUnsignedByte(ByteBuffer bb, int position) {
        return ((short) (bb.get(position) & (short) 0xff));
    }

    public static int compareByteBuffers(ByteBuffer buf1, ByteBuffer buf2) {
        for (int i = 0; i < buf1.array().length; i++) {
            if (getUnsignedByte(buf1, i) > getUnsignedByte(buf2, i)) {
                return 1;
            } else if (getUnsignedByte(buf1, i) < getUnsignedByte(buf2, i)) {
                return -1;
            }
        }

        return 0;
    }

    @Override
    public StaticRoute getBestMatchStaticRoute(InetAddress ipAddress) {
        ByteBuffer bblongestPrefix = null;
        try {
            bblongestPrefix = ByteBuffer.wrap(InetAddress.getByName("0.0.0.0").getAddress());
        } catch (Exception e) {
            return null;
        }

        if (staticRoutes == null) {
            return null;
        }

        StaticRoute longestPrefixRoute = null;
        for (StaticRoute s : staticRoutes.values()) {
            InetAddress prefix = s.longestPrefixMatch(ipAddress);
            if ((prefix != null) && (prefix instanceof Inet4Address)) {
                ByteBuffer bbtmp = ByteBuffer.wrap(prefix.getAddress());
                if (compareByteBuffers(bbtmp, bblongestPrefix) > 0) {
                    bblongestPrefix = bbtmp;
                    longestPrefixRoute = s;
                }
            }
        }
        return longestPrefixRoute;
    }

    @Override
    public Status addStaticRoute(StaticRouteConfig config) {
        Status status = config.isValid();
        if (!status.isSuccess()) {
            return status;
        }
        if (staticRouteConfigs.get(config.getName()) != null) {
            return new Status(StatusCode.CONFLICT, "A valid Static Route configuration with this name "
                    + "already exists. Please use a different name");
        }

        // Update database
        StaticRoute sRoute = new StaticRoute(config);

        for (Map.Entry<String, StaticRoute> entry : staticRoutes.entrySet()) {
            if (entry.getValue().compareTo(sRoute) == 0) {
                return new Status(StatusCode.CONFLICT, "This conflicts with an existing Static Route "
                        + "Configuration. Please check the configuration " + "and try again");
            }
        }
        staticRoutes.put(config.getName(), sRoute);

        // Update config databse
        staticRouteConfigs.put(config.getName(), config);

        // Notify
        checkAndUpdateListeners(config.getName(), sRoute, true);
        return status;
    }

    @Override
    public Status removeStaticRoute(String name) {
        staticRouteConfigs.remove(name);
        StaticRoute sRoute = staticRoutes.remove(name);
        if (sRoute != null) {
            checkAndUpdateListeners(name, sRoute, false);
            return new Status(StatusCode.SUCCESS, null);
        }
        return new Status(StatusCode.NOTFOUND, "Static Route with name " + name + " is not found");
    }

    void setClusterContainerService(IClusterContainerServices s) {
        log.debug("Cluster Service set");
        this.clusterContainerService = s;
    }

    void unsetClusterContainerService(IClusterContainerServices s) {
        if (this.clusterContainerService == s) {
            log.debug("Cluster Service removed!");
            this.clusterContainerService = null;
        }
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init(Component c) {
        String containerName = null;
        Dictionary props = c.getServiceProperties();
        if (props != null) {
            containerName = (String) props.get("containerName");
        } else {
            // In the Global instance case the containerName is empty
            containerName = "";
        }

        staticRoutesFileName = ROOT + "staticRouting_" + containerName + ".conf";

        log.debug("forwarding.staticrouting starting on container {}", containerName);
        allocateCaches();
        retrieveCaches();
        this.executor = Executors.newFixedThreadPool(1);
        if (staticRouteConfigs.isEmpty()) {
            loadConfiguration();
        }

        /*
         * Slow probe to identify any gateway that might have silently appeared
         * after the Static Routing Configuration.
         */
        gatewayProbeTimer = new Timer();
        gatewayProbeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                for (Map.Entry<String, StaticRoute> s : staticRoutes.entrySet()) {
                    StaticRoute route = s.getValue();
                    if ((route.getType() == StaticRoute.NextHopType.IPADDRESS) && route.getHost() == null) {
                        checkAndUpdateListeners(s.getKey(), route, true);
                    }
                }
            }
        }, 60 * 1000, 60 * 1000);

    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
        log.debug("Destroy all the Static Routing Rules given we are " + "shutting down");

        gatewayProbeTimer.cancel();

        // Clear the listener so to be ready in next life
        this.staticRoutingAware.clear();
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    void start() {
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    void stop() {
        executor.shutdown();
    }

    @Override
    public Status saveConfiguration() {
        return this.saveConfig();
    }

}
