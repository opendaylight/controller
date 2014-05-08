/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.api;

import org.opendaylight.controller.sal.binding.api.BindingAwareService;

/**
 *
 * Marker interface for MD-SAL services which are available for users of MD-SAL.
 *
 * BindingService is marker interface for infrastructure services provided by
 * the SAL. These services may be session-specific, and wrapped by custom
 * delegator patterns in order to introduce additional semantics / checks
 * to the system.
 *
 * This interface extends {@link BindingAwareService}, order to be make
 * new services available via
 * {@link org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext}
 * and via
 * {@link org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext}
 *
 */
public interface BindingService extends BindingAwareService {

}
