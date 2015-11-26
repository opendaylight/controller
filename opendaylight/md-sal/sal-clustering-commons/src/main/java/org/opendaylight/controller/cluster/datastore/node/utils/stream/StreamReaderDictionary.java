/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;

/**
 * Dictionary for use with {@link NormalizedNodeStreamWriter}s. An instance can be owned by a single writer at any
 * given time.
 */
@NotThreadSafe
public final class StreamReaderDictionary extends AbstractStreamDictionary {
    StreamReaderDictionary() {
        // Hidden on purpose
    }
}
