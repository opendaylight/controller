/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility;

import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.INodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MDSalNodeFactory implements INodeFactory{
    private Logger logger = LoggerFactory.getLogger(MDSalNodeFactory.class);

    @Override
    public Node fromString(String type, String id) {

        try {
            return new Node(type, id);
        } catch (ConstructionException e) {
            logger.error("Could not construct Node", e);
        }
        return null;
    }
}
