/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

/**
 * NormalizedNodeDeSerializationContext provides methods which help in decoding
 * certain components of a NormalizedNode properly
 */

public interface NormalizedNodeDeSerializationContext {
    String getNamespace(int namespace);
    String getRevision(int revision);
    String getLocalName(int localName);
}
