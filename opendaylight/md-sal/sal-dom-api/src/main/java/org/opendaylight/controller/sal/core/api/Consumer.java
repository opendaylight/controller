/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api;

import java.util.Collection;

import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;

/**
 *
 * Defines the component of controller and supplies additional metadata. A
 * component of the controller or application supplies a concrete implementation
 * of this interface.
 *
 * A user-implemented component (application) which facilitates the SAL and SAL
 * services to access infrastructure services or providers' functionality.
 *
 *
 */
public interface Consumer {

    /**
     * Callback signaling initialization of the consumer session to the SAL.
     *
     * The consumer MUST use the session for all communication with SAL or
     * retrieving SAL infrastructure services.
     *
     * This method is invoked by {@link Broker#registerConsumer(Consumer)}
     *
     * @param session
     *            Unique session between consumer and SAL.
     */
    void onSessionInitiated(ConsumerSession session);

    /**
     * @deprecated - no longer used or needed
     * *
     * Suggested implementation until removed:
     * @code {
     * public Collection<ConsumerFunctionality> getConsumerFunctionality() {
     *    return Collections.emptySet();
     * }
     * }
     */
    @Deprecated
    Collection<ConsumerFunctionality> getConsumerFunctionality();

    /**
     * @deprecated - no longer used or needed
     */
    @Deprecated
    interface ConsumerFunctionality {

    }
}
