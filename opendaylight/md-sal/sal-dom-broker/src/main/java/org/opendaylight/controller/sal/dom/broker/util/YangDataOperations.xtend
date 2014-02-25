    /*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.util

import org.opendaylight.controller.sal.dom.broker.util.operations.DataOperations
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode

class YangDataOperations {

    static def CompositeNode merge(DataSchemaNode schema, CompositeNode stored, CompositeNode modified, boolean config) {
        if (stored === null) {
            return modified;
        }

        if (schema instanceof ListSchemaNode) {
            return DataOperations.modify(schema as ListSchemaNode, stored, modified).orNull();
        } else if (schema instanceof ContainerSchemaNode) {
            return DataOperations.modify(schema as ContainerSchemaNode, stored, modified).orNull();
        }
        throw new IllegalArgumentException("Supplied node is not data node container.");
    }

}
