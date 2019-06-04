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
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;

/**
 * Enumeration of all stream versions this implementation supports on both input and output.
 */
@Beta
@NonNullByDefault
public enum NormalizedNodeStreamVersion {
    /**
     * Original version, as shipped in Lithium.
     */
    LITHIUM,
    /**
     * Revised Sodium version. Differences from Lithium:
     * <ul>
     *   <li>{@link QName}s and {@link AugmentationIdentifier}s are encoded using a stream-built dictionary</li>
     *   <li>lists and maps provide explicit size hints</li>
     * <ul>
     */
    SODIUM;
}
