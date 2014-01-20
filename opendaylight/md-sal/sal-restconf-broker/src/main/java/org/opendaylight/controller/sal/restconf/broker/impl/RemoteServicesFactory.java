/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.broker.impl;

import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.yangtools.restconf.client.api.RestconfClientContext;

public class RemoteServicesFactory {

    private final RestconfClientContext restconfClientContext;

    public RemoteServicesFactory(RestconfClientContext restconfClientContext){
        this.restconfClientContext = restconfClientContext;
    }

    public DataBrokerService getDataBrokerService(){
        return new DataBrokerServiceImpl(this.restconfClientContext);
    }

    public NotificationService getNotificationService(){
        return  new NotificationServiceImpl(this.restconfClientContext);
    }

    public RpcConsumerRegistry getRpcConsumerRegistry(){
        return new RpcConsumerRegistryImpl(this.restconfClientContext);
    }

}
