/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

/**
 * Implementation of PeerAddressResolver that does nothing.
 *
 * @author Thomas Pantelis
 */
public final class NoopPeerAddressResolver implements PeerAddressResolver {
    public static final NoopPeerAddressResolver INSTANCE = new NoopPeerAddressResolver();

    private NoopPeerAddressResolver() {
    }

    @Override
    public String resolve(String peerId) {
        return null;
    }
}
