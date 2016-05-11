/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import com.google.common.annotations.Beta;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;

@Beta
@FunctionalInterface
public interface ClientActorBehaviorFactory<T extends FrontendType> {
    ClientActorBehavior<T> createBehavior(ClientActorContext<T> context);
}
