/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api;

import java.util.Collection;

import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;

/**
 *
 * Defines the component of controller and supplies additional metadata. A
 * component of the controller or application supplies a concrete implementation
 * of this interface.
 *
 * <p>
 * A user-implemented component (application) which facilitates the SAL and SAL
 * services to access infrastructure services and to provide functionality to
 * {@link Consumer}s and other providers.
 *
 *
 */
public interface Provider {

    /**
     * Callback signaling initialization of the provider session to the SAL.
     *
     * <p>
     * The provider <b>MUST use the session</b> for all communication with SAL
     * or retrieving SAL infrastructure services.
     *
     * <p>
     * This method is invoked by {@link Broker#registerConsumer(Consumer)}
     *
     * @param session
     *            Unique session between provider and SAL.
     */
    void onSessionInitiated(ProviderSession session);

    /**
     * @deprecated - No longer used or needed
     *
     * Suggested implementation until removed:
     * @code {
     * public Collection<ProviderFunctionality> getProviderFunctionality() {
     *  return Collections.emptySet();
     * }
     * }
     */
    @Deprecated
    Collection<ProviderFunctionality> getProviderFunctionality();

    /**
     * @deprecated - no longer used or needed
     */
    @Deprecated
    interface ProviderFunctionality {

    }
}
