/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.spi.remote;

import java.util.EventListener;

import org.opendaylight.controller.md.sal.common.api.routing.RouteChange;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface RouteChangeListener extends EventListener {

    void onRouteChange(RouteChange<Class<? extends BaseIdentity>, InstanceIdentifier<?>> change);

}
