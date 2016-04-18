/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors;

import java.io.Serializable;

/**
 * A snapshot of FrontendActor's generation number.
 */
final class FrontendGenerationSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long generation;

    public FrontendGenerationSnapshot(final long generation) {
        this.generation = generation;
    }

    long getGeneration() {
        return generation;
    }
}
