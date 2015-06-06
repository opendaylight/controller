/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.ssh;

import io.netty.channel.local.LocalAddress;
import java.net.InetSocketAddress;
import org.apache.sshd.common.KeyPairProvider;
import org.apache.sshd.server.PasswordAuthenticator;

public final class SshProxyServerConfigurationBuilder {
    private InetSocketAddress bindingAddress;
    private LocalAddress localAddress;
    private PasswordAuthenticator authenticator;
    private KeyPairProvider keyPairProvider;
    private int idleTimeout;

    public SshProxyServerConfigurationBuilder setBindingAddress(final InetSocketAddress bindingAddress) {
        this.bindingAddress = bindingAddress;
        return this;
    }

    public SshProxyServerConfigurationBuilder setLocalAddress(final LocalAddress localAddress) {
        this.localAddress = localAddress;
        return this;
    }

    public SshProxyServerConfigurationBuilder setAuthenticator(final PasswordAuthenticator authenticator) {
        this.authenticator = authenticator;
        return this;
    }

    public SshProxyServerConfigurationBuilder setKeyPairProvider(final KeyPairProvider keyPairProvider) {
        this.keyPairProvider = keyPairProvider;
        return this;
    }

    public SshProxyServerConfigurationBuilder setIdleTimeout(final int idleTimeout) {
        this.idleTimeout = idleTimeout;
        return this;
    }

    public SshProxyServerConfiguration createSshProxyServerConfiguration() {
        return new SshProxyServerConfiguration(bindingAddress, localAddress, authenticator, keyPairProvider, idleTimeout);
    }

    public static SshProxyServerConfigurationBuilder create() {
        return new SshProxyServerConfigurationBuilder();
    }
}