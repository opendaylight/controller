/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;

/**
 * Adapter between the legacy controller API-based DOMDataBroker and the mdsal API-based DOMDataBroker.
 *
 * @author Thomas Pantelis
 */
@Deprecated(forRemoval = true)
public class LegacyDOMDataBrokerAdapter extends AbstractLegacyDOMDataBrokerAdapter {
    public LegacyDOMDataBrokerAdapter(final org.opendaylight.mdsal.dom.api.DOMDataBroker delegate) {
        super(delegate);
    }

    @Override
    DOMTransactionChain createDelegateChain(DOMTransactionChainListener listener) {
        return delegate().createTransactionChain(listener);
    }
}
