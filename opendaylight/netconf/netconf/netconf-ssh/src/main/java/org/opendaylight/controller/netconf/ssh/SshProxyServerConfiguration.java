/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.ssh;

import com.google.common.base.Preconditions;
import io.netty.channel.local.LocalAddress;
import java.net.InetSocketAddress;
import org.apache.sshd.common.KeyPairProvider;
import org.apache.sshd.server.PasswordAuthenticator;

public final class SshProxyServerConfiguration {
    private final InetSocketAddress bindingAddress;
    private final LocalAddress localAddress;
    private final PasswordAuthenticator authenticator;
    private final KeyPairProvider keyPairProvider;
    private final int idleTimeout;

    SshProxyServerConfiguration(final InetSocketAddress bindingAddress, final LocalAddress localAddress, final PasswordAuthenticator authenticator, final KeyPairProvider keyPairProvider, final int idleTimeout) {
        this.bindingAddress = Preconditions.checkNotNull(bindingAddress);
        this.localAddress = Preconditions.checkNotNull(localAddress);
        this.authenticator = Preconditions.checkNotNull(authenticator);
        this.keyPairProvider = Preconditions.checkNotNull(keyPairProvider);
        // Idle timeout cannot be disabled in the sshd by using =< 0 value
        Preconditions.checkArgument(idleTimeout > 0, "Idle timeout has to be > 0");
        this.idleTimeout = idleTimeout;
    }

    public InetSocketAddress getBindingAddress() {
        return bindingAddress;
    }

    public LocalAddress getLocalAddress() {
        return localAddress;
    }

    public PasswordAuthenticator getAuthenticator() {
        return authenticator;
    }

    public KeyPairProvider getKeyPairProvider() {
        return keyPairProvider;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }


}
