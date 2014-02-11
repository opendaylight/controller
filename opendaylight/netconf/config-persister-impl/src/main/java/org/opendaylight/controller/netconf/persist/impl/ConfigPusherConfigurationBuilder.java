/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.persist.impl;

import io.netty.channel.EventLoopGroup;

import java.net.InetSocketAddress;

public class ConfigPusherConfigurationBuilder {
    InetSocketAddress netconfAddress;
    EventLoopGroup eventLoopGroup;

    long netconfCapabilitiesWaitTimeoutMs = ConfigPusherConfiguration.DEFAULT_NETCONF_CAPABILITIES_WAIT_TIMEOUT_MS;
    int netconfSendMessageDelayMs = ConfigPusherConfiguration.DEFAULT_NETCONF_SEND_MESSAGE_DELAY_MS;
    int netconfSendMessageMaxAttempts = ConfigPusherConfiguration.DEFAULT_NETCONF_SEND_MESSAGE_MAX_ATTEMPTS;
    int connectionAttemptDelayMs = ConfigPusherConfiguration.DEFAULT_CONNECTION_ATTEMPT_DELAY_MS;
    long connectionAttemptTimeoutMs = ConfigPusherConfiguration.DEFAULT_CONNECTION_ATTEMPT_TIMEOUT_MS;
    int netconfPushConfigAttempts = ConfigPusherConfiguration.DEFAULT_NETCONF_PUSH_CONFIG_ATTEMPTS;
    long netconfPushConfigDelayMs = ConfigPusherConfiguration.DEFAULT_NETCONF_PUSH_CONFIG_DELAY_MS;

    private ConfigPusherConfigurationBuilder() {
    }

    public static ConfigPusherConfigurationBuilder aConfigPusherConfiguration() {
        return new ConfigPusherConfigurationBuilder();
    }

    public ConfigPusherConfigurationBuilder withNetconfAddress(InetSocketAddress netconfAddress) {
        this.netconfAddress = netconfAddress;
        return this;
    }

    public ConfigPusherConfigurationBuilder withNetconfCapabilitiesWaitTimeoutMs(long netconfCapabilitiesWaitTimeoutMs) {
        this.netconfCapabilitiesWaitTimeoutMs = netconfCapabilitiesWaitTimeoutMs;
        return this;
    }

    public ConfigPusherConfigurationBuilder withNetconfSendMessageDelayMs(int netconfSendMessageDelayMs) {
        this.netconfSendMessageDelayMs = netconfSendMessageDelayMs;
        return this;
    }

    public ConfigPusherConfigurationBuilder withNetconfSendMessageMaxAttempts(int netconfSendMessageMaxAttempts) {
        this.netconfSendMessageMaxAttempts = netconfSendMessageMaxAttempts;
        return this;
    }

    public ConfigPusherConfigurationBuilder withConnectionAttemptDelayMs(int connectionAttemptDelayMs) {
        this.connectionAttemptDelayMs = connectionAttemptDelayMs;
        return this;
    }

    public ConfigPusherConfigurationBuilder withConnectionAttemptTimeoutMs(long connectionAttemptTimeoutMs) {
        this.connectionAttemptTimeoutMs = connectionAttemptTimeoutMs;
        return this;
    }

    public ConfigPusherConfigurationBuilder withEventLoopGroup(EventLoopGroup eventLoopGroup) {
        this.eventLoopGroup = eventLoopGroup;
        return this;
    }

    public ConfigPusherConfigurationBuilder withNetconfPushConfigAttempts(int netconfPushConfigAttempts) {
        this.netconfPushConfigAttempts = netconfPushConfigAttempts;
        return this;
    }

    public ConfigPusherConfigurationBuilder withNetconfPushConfigDelayMs(long netconfPushConfigDelayMs) {
        this.netconfPushConfigDelayMs = netconfPushConfigDelayMs;
        return this;
    }

    public ConfigPusherConfiguration build() {
        ConfigPusherConfiguration configPusherConfiguration = new ConfigPusherConfiguration(netconfAddress,
                netconfCapabilitiesWaitTimeoutMs, netconfSendMessageDelayMs, netconfSendMessageMaxAttempts,
                connectionAttemptDelayMs, connectionAttemptTimeoutMs, eventLoopGroup, netconfPushConfigAttempts,
                netconfPushConfigDelayMs);
        return configPusherConfiguration;
    }
}
