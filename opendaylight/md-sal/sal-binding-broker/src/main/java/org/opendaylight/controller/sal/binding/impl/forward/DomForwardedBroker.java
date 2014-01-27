/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl.forward;

import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingIndependentConnector;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;

interface DomForwardedBroker {

    public BindingIndependentConnector getConnector();
    
    public void setConnector(BindingIndependentConnector connector);
    
    public void setDomProviderContext(ProviderSession domProviderContext);

    public ProviderSession getDomProviderContext();

    void startForwarding();
}
