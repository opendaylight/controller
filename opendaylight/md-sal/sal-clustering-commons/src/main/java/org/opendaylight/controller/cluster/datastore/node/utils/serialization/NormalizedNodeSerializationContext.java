/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import java.net.URI;
import java.util.Date;

/**
 * NormalizedNodeSerializationContext provides methods which help in encoding
 * certain components of a NormalizedNode properly
 */
public interface NormalizedNodeSerializationContext {
    int addNamespace(URI namespace);
    int addRevision(Date revision);
    int addLocalName(String localName);
}
