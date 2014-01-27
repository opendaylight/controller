/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl.forward;

import java.util.Collection;
import java.util.Collections;

import org.opendaylight.controller.sal.binding.impl.RootDataBrokerImpl;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingDomConnectorDeployer;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingIndependentConnector;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;

public class DomForwardedDataBrokerImpl extends RootDataBrokerImpl implements Provider, DomForwardedBroker {

    private BindingIndependentConnector connector;
    private ProviderSession domProviderContext;

    public void setConnector(BindingIndependentConnector connector) {
        this.connector = connector;
    }

    @Override
    public void onSessionInitiated(ProviderSession session) {
        this.setDomProviderContext(session);
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public BindingIndependentConnector getConnector() {
        return connector;
    }

    @Override
    public ProviderSession getDomProviderContext() {
        return domProviderContext;
    }

    public void setDomProviderContext(ProviderSession domProviderContext) {
        this.domProviderContext = domProviderContext;
    }

    @Override
    public void startForwarding() {
        BindingDomConnectorDeployer.startDataForwarding(getConnector(), this, getDomProviderContext());
    }
}
