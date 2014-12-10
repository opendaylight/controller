/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.sal.connect.api;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Factory for DOMDataBrokers(transaction producers) for remote devices.
 * This factory enables provisioning of custom Read, Write and ReadWrite transactions.
 */
public interface DataBrokerFactory<PREF> {

    DOMDataBroker createBroker(final RemoteDeviceId id, final RpcImplementation deviceRpc, final SchemaContext schemaContext, final PREF netconfSessionPreferences);
}
