/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.persist.impl;

import io.netty.channel.EventLoopGroup;

import javax.annotation.concurrent.Immutable;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Configuration properties for ConfigPusher. Contains delays and timeouts for netconf
 * connection establishment, netconf capabilities stabilization and configuration push.
 */
@Immutable
public final class ConfigPusherConfiguration {

    public static final long DEFAULT_CONNECTION_ATTEMPT_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5);
    public static final int DEFAULT_CONNECTION_ATTEMPT_DELAY_MS = 5000;

    public static final int DEFAULT_NETCONF_SEND_MESSAGE_MAX_ATTEMPTS = 20;
    public static final int DEFAULT_NETCONF_SEND_MESSAGE_DELAY_MS = 1000;

    public static final long DEFAULT_NETCONF_CAPABILITIES_WAIT_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(2);

    public static final int DEFAULT_NETCONF_PUSH_CONFIG_ATTEMPTS = 30;
    public static final long DEFAULT_NETCONF_PUSH_CONFIG_DELAY_MS = TimeUnit.MINUTES.toMillis(1);

    final InetSocketAddress netconfAddress;
    final EventLoopGroup eventLoopGroup;

    /**
     * Total time to wait for capability stabilization
     */
    final long netconfCapabilitiesWaitTimeoutMs;

    /**
     * Delay between message send attempts
     */
    final int netconfSendMessageDelayMs;
    /**
     * Total number attempts to send a message
     */
    final int netconfSendMessageMaxAttempts;

    /**
     * Delay between connection establishment attempts
     */
    final int connectionAttemptDelayMs;
    /**
     * Total number of attempts to perform connection establishment
     */
    final long connectionAttemptTimeoutMs;

    /**
     * Total number of attempts to push configuration to netconf
     */
    final int netconfPushConfigAttempts;
    /**
     * Delay between configuration push attempts
     */
    final long netconfPushConfigDelayMs;

    ConfigPusherConfiguration(InetSocketAddress netconfAddress, long netconfCapabilitiesWaitTimeoutMs,
            int netconfSendMessageDelayMs, int netconfSendMessageMaxAttempts, int connectionAttemptDelayMs,
            long connectionAttemptTimeoutMs, EventLoopGroup eventLoopGroup, int netconfPushConfigAttempts,
            long netconfPushConfigDelayMs) {
        this.netconfAddress = netconfAddress;
        this.netconfCapabilitiesWaitTimeoutMs = netconfCapabilitiesWaitTimeoutMs;
        this.netconfSendMessageDelayMs = netconfSendMessageDelayMs;
        this.netconfSendMessageMaxAttempts = netconfSendMessageMaxAttempts;
        this.connectionAttemptDelayMs = connectionAttemptDelayMs;
        this.connectionAttemptTimeoutMs = connectionAttemptTimeoutMs;
        this.eventLoopGroup = eventLoopGroup;
        this.netconfPushConfigAttempts = netconfPushConfigAttempts;
        this.netconfPushConfigDelayMs = netconfPushConfigDelayMs;
    }
}
