/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.stream;

/**
 * Interface for a class that can read serialized NormalizedNode instances from a stream.
 *
 * @deprecated Use {@link NormalizedNodeDataInput} instead.
 */
public interface NormalizedNodeStreamReader extends NormalizedNodeDataInput {
}
