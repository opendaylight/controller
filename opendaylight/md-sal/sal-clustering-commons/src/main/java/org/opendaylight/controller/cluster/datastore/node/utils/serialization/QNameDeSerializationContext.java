/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

/**
 * Interface that provides methods which help in decoding components of a QName.
 *
 * @author Thomas Pantelis
 */
public interface QNameDeSerializationContext {
    String getNamespace(int namespace);

    String getRevision(int revision);

    String getLocalName(int localName);
}
