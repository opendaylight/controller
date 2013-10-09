/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.routing;

import java.util.Map;
import java.util.Set;

public interface RouteChange<C,P> {

    Map<C,Set<P>> getRemovals();
    Map<C,Set<P>> getAnnouncements();
}
