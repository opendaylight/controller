/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.broker.listeners;

import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.restconf.broker.event.RemoteDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.DataChangedNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.SalRemoteListener;

public class RemoteDataChangeNotificationListener implements SalRemoteListener {


    private final DataChangeListener dataChangeListener;

    public RemoteDataChangeNotificationListener(DataChangeListener dataChangeListener){
        this.dataChangeListener = dataChangeListener;
    }
    @Override
    public void onDataChangedNotification(DataChangedNotification notification) {
        this.dataChangeListener.onDataChanged(new RemoteDataChangeEvent(notification));
    }
}
