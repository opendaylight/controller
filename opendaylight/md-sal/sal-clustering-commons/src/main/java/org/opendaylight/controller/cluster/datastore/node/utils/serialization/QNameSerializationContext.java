/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import java.net.URI;
import java.util.Date;

/**
 * Interface that provides methods which help in encoding components of a QName.
 *
 * @author Thomas Pantelis
 */
public interface QNameSerializationContext {
    int addNamespace(URI namespace);

    int addRevision(Date revision);

    int addLocalName(String localName);

}
