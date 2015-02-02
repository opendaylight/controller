/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.spi;

import java.util.Arrays;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;

/**
 * Convenience abstract base class for {@link DOMRpcProviderService} implementations.
 */
public abstract class AbstractDOMRpcProviderService implements DOMRpcProviderService {
    @Override
    public final DOMRpcImplementationRegistration registerRpcImplementation(final DOMRpcImplementation implementation, final DOMRpcIdentifier... types) {
        return registerRpcImplementation(implementation, Arrays.asList(types));
    }
}
