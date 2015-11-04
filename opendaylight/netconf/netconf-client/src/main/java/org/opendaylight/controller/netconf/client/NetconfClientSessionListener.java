/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.client;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfSessionListener;
import org.opendaylight.controller.netconf.api.NetconfTerminationReason;

public interface NetconfClientSessionListener extends NetconfSessionListener<NetconfClientSession> {

    @Override void onMessage(NetconfClientSession session, NetconfMessage message);

    @Override void onSessionDown(NetconfClientSession session, Exception e);

    @Override void onSessionTerminated(NetconfClientSession session, NetconfTerminationReason reason);

    @Override void onSessionUp(NetconfClientSession session);
}
