/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.toaster.provider;

import java.util.Collection;
import java.util.Collections;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterService;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToasterProvider extends AbstractBindingAwareProvider {
    private static final Logger log = LoggerFactory.getLogger(ToasterProvider.class);

    private ProviderContext providerContext;
    private final OpendaylightToaster toaster;

    public ToasterProvider() {
        toaster = new OpendaylightToaster();
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        log.info("Provider Session initialized");

        this.providerContext = session;
        toaster.setNotificationProvider(session.getSALService(NotificationProviderService.class));
        providerContext.addRpcImplementation(ToasterService.class, toaster);
    }

    @Override
    public Collection<? extends RpcService> getImplementations() {
        return Collections.emptySet();
    }

    @Override
    public Collection<? extends ProviderFunctionality> getFunctionality() {
        return Collections.emptySet();
    }
}
