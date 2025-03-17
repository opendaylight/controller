/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import com.google.common.annotations.Beta;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Interface implemented by Actors, who can schedule invocation of a {@link Runnable} in their context.
 */
@Beta
@NonNullByDefault
@FunctionalInterface
public interface ExecuteInSelfActor {
    /**
     * Run a Runnable in the context of this actor.
     *
     * @param runnable Runnable to run
     */
    void executeInSelf(Runnable runnable);
}
