/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import java.util.Collection;
import java.util.Collections;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider;

class DummyDOMProvider implements Provider {

    @Override
    @Deprecated
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptyList();
    }

    @Override
    public void onSessionInitiated(ProviderSession session) {
        // NOOP
    }
}
