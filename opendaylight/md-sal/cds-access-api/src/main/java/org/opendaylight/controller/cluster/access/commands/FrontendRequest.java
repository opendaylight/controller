/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.yangtools.concepts.Identifier;

@Beta
public abstract class FrontendRequest<T extends Identifier> extends Request<T> {
    private static final long serialVersionUID = 1L;

    FrontendRequest(final T identifier, final ActorRef frontendRef) {
        // Hidden to force use of concrete messages
        super(identifier, frontendRef);
    }

    public abstract FrontendIdentifier getFrontendIdentifier();
}
