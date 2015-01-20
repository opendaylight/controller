/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.api;

import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public interface RemoteDeviceHandler<PREF> extends AutoCloseable {

    void onDeviceConnected(SchemaContext remoteSchemaContext,
                           PREF netconfSessionPreferences, RpcImplementation deviceRpc);

    void onDeviceDisconnected();

    void onDeviceFailed();

    void onNotification(CompositeNode domNotification);

    void close();
}
