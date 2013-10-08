/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.osgi;

import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;

public interface NetconfOperationServiceFactoryListener {

    void onAddNetconfOperationServiceFactory(NetconfOperationServiceFactory service);

    void onRemoveNetconfOperationServiceFactory(NetconfOperationServiceFactory service);

    NetconfOperationServiceSnapshot getSnapshot(long sessionId);
}
