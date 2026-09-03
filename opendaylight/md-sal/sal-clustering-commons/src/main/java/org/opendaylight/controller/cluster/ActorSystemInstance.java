/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster;

import org.apache.pekko.actor.ActorSystem;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A service that encapsulates a single {@link ActorSystem}.
 *
 * @author Thomas Pantelis
 */
@NonNullByDefault
public interface ActorSystemInstance {
    /**
     * {@return the ActorSystem}
     */
    ActorSystem actorSystem();
}
