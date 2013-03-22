/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api;

import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;

/**
 * 
 * Session-specific instance of the broker functionality.
 * 
 * <p>
 * BrokerService is marker interface for infrastructure services provided by the
 * SAL. These services are session-specific, each {@link Provider} and
 * {@link Consumer} usually has own instance of the service with it's own
 * context.
 * 
 * <p>
 * The consumer's (or provider's) instance of specific service could be obtained
 * by invoking {@link ConsumerSession#getService(Class)} method on session
 * assigned to the consumer.
 * 
 * <p>
 * {@link BrokerService} and {@link Provider} may seem similar, but provider
 * provides YANG model-based functionality and {@link BrokerService} exposes the
 * necessary supporting functionality to implement specific functionality of
 * YANG and to reuse it in the development of {@link Consumer}s and
 * {@link Provider}s.
 * 
 * 
 */
public interface BrokerService {

    void closeSession();
}
