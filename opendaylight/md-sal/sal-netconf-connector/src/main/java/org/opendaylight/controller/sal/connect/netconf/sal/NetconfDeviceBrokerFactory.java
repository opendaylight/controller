/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.sal.connect.netconf.sal;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.connect.api.DataBrokerFactory;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionCapabilities;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class NetconfDeviceBrokerFactory implements DataBrokerFactory<NetconfSessionCapabilities> {

    @Override
    public DOMDataBroker createBroker(final RemoteDeviceId id, final RpcImplementation deviceRpc, final SchemaContext schemaContext, final NetconfSessionCapabilities netconfSessionPreferences) {
        return new NetconfDeviceDataBroker(id, deviceRpc, schemaContext, netconfSessionPreferences);
    }
}
