/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;

public class IdentityMapping {
    private final Map<String, IdentitySchemaNode> identityNameToSchemaNode;

    public IdentityMapping() {
        this.identityNameToSchemaNode = Maps.newHashMap();
    }

    public void addIdSchemaNode(final IdentitySchemaNode node) {
        String name = node.getQName().getLocalName();
        Preconditions.checkState(!identityNameToSchemaNode.containsKey(name));
        identityNameToSchemaNode.put(name, node);
    }

    public boolean containsIdName(final String idName) {
        return identityNameToSchemaNode.containsKey(idName);
    }

}
