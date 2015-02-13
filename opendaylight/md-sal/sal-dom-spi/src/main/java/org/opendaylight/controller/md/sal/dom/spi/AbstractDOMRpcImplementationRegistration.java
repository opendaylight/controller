/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.spi;

import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;

/**
 * Abstract base class for {@link DOMRpcImplementationRegistration} implementations.
 */
public abstract class AbstractDOMRpcImplementationRegistration<T extends DOMRpcImplementation> extends AbstractObjectRegistration<T> implements DOMRpcImplementationRegistration<T> {
    protected AbstractDOMRpcImplementationRegistration(final T instance) {
        super(instance);
    }
}
