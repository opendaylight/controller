/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.impl.osgi;

import java.util.HashSet;
import java.util.Set;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;

public class NetconfOperationServiceFactoryListenerImpl implements NetconfOperationServiceFactoryListener,
        NetconfOperationProvider {
    private final Set<NetconfOperationServiceFactory> allFactories = new HashSet<>();

    @Override
    public synchronized void onAddNetconfOperationServiceFactory(NetconfOperationServiceFactory service) {
        allFactories.add(service);
    }

    @Override
    public synchronized void onRemoveNetconfOperationServiceFactory(NetconfOperationServiceFactory service) {
        allFactories.remove(service);
    }

    @Override
    public synchronized NetconfOperationServiceSnapshotImpl openSnapshot(String sessionIdForReporting) {
        return new NetconfOperationServiceSnapshotImpl(allFactories, sessionIdForReporting);
    }

}
