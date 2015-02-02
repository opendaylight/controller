/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import org.opendaylight.yangtools.concepts.ObjectRegistration;

/**
 * A registration of a {@link DOMRpcImplementation}. Used to track and revoke a registration
 * with a {@link DOMRpcProviderService}.
 */
public interface DOMRpcImplementationRegistration extends ObjectRegistration<DOMRpcImplementation> {
    @Override
    void close();
}
