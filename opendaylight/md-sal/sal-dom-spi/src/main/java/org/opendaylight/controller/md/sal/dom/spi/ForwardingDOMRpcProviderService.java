/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.spi;

import com.google.common.collect.ForwardingObject;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;

/**
 * Utility class which implements {@link DOMRpcProviderService} by forwarding
 * requests to a backing instance.
 */
public abstract class ForwardingDOMRpcProviderService extends ForwardingObject implements DOMRpcProviderService {
    @Override
    protected abstract @Nonnull DOMRpcProviderService delegate();

    @Override
    public DOMRpcImplementationRegistration registerRpcImplementation(final DOMRpcImplementation implementation, final DOMRpcIdentifier... types) {
        return delegate().registerRpcImplementation(implementation, types);
    }

    @Override
    public DOMRpcImplementationRegistration registerRpcImplementation(final DOMRpcImplementation implementation, final Collection<DOMRpcIdentifier> types) {
        return delegate().registerRpcImplementation(implementation, types);
    }
}
