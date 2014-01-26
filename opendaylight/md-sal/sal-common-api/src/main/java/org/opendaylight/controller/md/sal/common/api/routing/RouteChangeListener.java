/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.routing;

import java.util.EventListener;

public interface RouteChangeListener<C,P> extends EventListener {

    void onRouteChange(RouteChange<C, P> change);
}
