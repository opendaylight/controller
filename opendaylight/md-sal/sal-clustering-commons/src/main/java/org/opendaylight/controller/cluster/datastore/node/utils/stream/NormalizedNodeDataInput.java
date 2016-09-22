/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.annotations.Beta;
import java.io.DataInput;
import java.io.IOException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Interface for reading {@link NormalizedNode}s, {@link YangInstanceIdentifier}s and {@link PathArgument}s.
 */
@Beta
public interface NormalizedNodeDataInput extends DataInput {
    /**
     * Read a normalized node from the reader.
     *
     * @return Next node from the stream, or null if end of stream has been reached.
     * @throws IOException if an error occurs
     * @throws IllegalStateException if the dictionary has been detached
     */
    NormalizedNode<?, ?> readNormalizedNode() throws IOException;

    YangInstanceIdentifier readYangInstanceIdentifier() throws IOException;

    PathArgument readPathArgument() throws IOException;
}
