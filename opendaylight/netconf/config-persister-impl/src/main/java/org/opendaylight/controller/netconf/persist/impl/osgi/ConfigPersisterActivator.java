/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.persist.impl.osgi;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.opendaylight.controller.netconf.persist.impl.ConfigPersisterNotificationHandler;
import org.opendaylight.controller.netconf.persist.impl.ConfigPusher;
import org.opendaylight.controller.netconf.persist.impl.ConfigPusherConfiguration;
import org.opendaylight.controller.netconf.persist.impl.ConfigPusherConfigurationBuilder;
import org.opendaylight.controller.netconf.persist.impl.PersisterAggregator;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Pattern;

public class ConfigPersisterActivator implements BundleActivator {

    private static final Logger logger = LoggerFactory.getLogger(ConfigPersisterActivator.class);

    public static final String IGNORED_MISSING_CAPABILITY_REGEX_SUFFIX = "ignoredMissingCapabilityRegex";

    public static final String MAX_WAIT_FOR_CAPABILITIES_MILLIS = "maxWaitForCapabilitiesMillis";

    public static final String NETCONF_CONFIG_PERSISTER = "netconf.config.persister";

    public static final String STORAGE_ADAPTER_CLASS_PROP_SUFFIX = "storageAdapterClass";

    public static final String DEFAULT_IGNORED_REGEX = "^urn:ietf:params:xml:ns:netconf:base:1.0";

    private final MBeanServer platformMBeanServer;

    private final Optional<ConfigPusherConfiguration> initialConfigForPusher;
    private volatile ConfigPersisterNotificationHandler jmxNotificationHandler;
    private Thread initializationThread;
    private ThreadFactory initializationThreadFactory;
    private EventLoopGroup nettyThreadGroup;
    private PersisterAggregator persisterAggregator;

    public ConfigPersisterActivator() {
        this(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable initializationRunnable) {
                return new Thread(initializationRunnable, "ConfigPersister-registrator");
            }
        }, ManagementFactory.getPlatformMBeanServer(), null);
    }

    @VisibleForTesting
    protected ConfigPersisterActivator(ThreadFactory threadFactory, MBeanServer mBeanServer,
            ConfigPusherConfiguration initialConfigForPusher) {
        this.initializationThreadFactory = threadFactory;
        this.platformMBeanServer = mBeanServer;
        this.initialConfigForPusher = Optional.fromNullable(initialConfigForPusher);
    }

    @Override
    public void start(final BundleContext context) throws Exception {
        logger.debug("ConfigPersister starting");

        PropertiesProviderBaseImpl propertiesProvider = new PropertiesProviderBaseImpl(context);

        final Pattern ignoredMissingCapabilityRegex = getIgnoredCapabilitiesProperty(propertiesProvider);

        persisterAggregator = PersisterAggregator.createFromProperties(propertiesProvider);

        final ConfigPusher configPusher = new ConfigPusher(getConfigurationForPusher(context, propertiesProvider));

        // offload initialization to another thread in order to stop blocking activator
        Runnable initializationRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    configPusher.pushConfigs(persisterAggregator.loadLastConfigs());
                    jmxNotificationHandler = new ConfigPersisterNotificationHandler(platformMBeanServer, persisterAggregator,
                            ignoredMissingCapabilityRegex);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Interrupted while waiting for netconf connection");
                    // uncaught exception handler will deal with this failure
                    throw new RuntimeException("Interrupted while waiting for netconf connection", e);
                }
                logger.info("Configuration Persister initialization completed.");
            }
        };

        initializationThread = initializationThreadFactory.newThread(initializationRunnable);
        initializationThread.start();
    }

    private Pattern getIgnoredCapabilitiesProperty(PropertiesProviderBaseImpl propertiesProvider) {
        String regexProperty = propertiesProvider.getProperty(IGNORED_MISSING_CAPABILITY_REGEX_SUFFIX);
        String regex;
        if (regexProperty != null) {
            regex = regexProperty;
        } else {
            regex = DEFAULT_IGNORED_REGEX;
        }
        return Pattern.compile(regex);
    }

    private Optional<Long> getMaxWaitForCapabilitiesProperty(PropertiesProviderBaseImpl propertiesProvider) {
        String timeoutProperty = propertiesProvider.getProperty(MAX_WAIT_FOR_CAPABILITIES_MILLIS);
        return Optional.fromNullable(timeoutProperty == null ? null : Long.valueOf(timeoutProperty));
    }

    private ConfigPusherConfiguration getConfigurationForPusher(BundleContext context,
            PropertiesProviderBaseImpl propertiesProvider) {

        // If configuration was injected via constructor, use it
        if(initialConfigForPusher.isPresent())
            return initialConfigForPusher.get();

        Optional<Long> maxWaitForCapabilitiesMillis = getMaxWaitForCapabilitiesProperty(propertiesProvider);
        final InetSocketAddress address = NetconfConfigUtil.extractTCPNetconfAddress(context,
                "Netconf is not configured, persister is not operational", true);

        nettyThreadGroup = new NioEventLoopGroup();

        ConfigPusherConfigurationBuilder configPusherConfigurationBuilder = ConfigPusherConfigurationBuilder.aConfigPusherConfiguration();

        if(maxWaitForCapabilitiesMillis.isPresent())
            configPusherConfigurationBuilder.withNetconfCapabilitiesWaitTimeoutMs(maxWaitForCapabilitiesMillis.get());

        return configPusherConfigurationBuilder
                .withEventLoopGroup(nettyThreadGroup)
                .withNetconfAddress(address)
                .build();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        initializationThread.interrupt();
        if (jmxNotificationHandler != null) {
            jmxNotificationHandler.close();
        }
        if(nettyThreadGroup!=null)
            nettyThreadGroup.shutdownGracefully();
        persisterAggregator.close();
    }
}
