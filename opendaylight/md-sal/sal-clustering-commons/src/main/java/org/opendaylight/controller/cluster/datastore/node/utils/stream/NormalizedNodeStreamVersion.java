/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.annotations.Beta;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Enumeration of all stream versions this implementation supports on both input and output.
 */
@Beta
@NonNullByDefault
public enum NormalizedNodeStreamVersion {
    LITHIUM,
    NEON_SR2,
    SODIUM_SR1,
    MAGNESIUM;
}
