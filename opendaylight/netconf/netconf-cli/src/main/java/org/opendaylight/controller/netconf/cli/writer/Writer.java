/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.writer;

import java.util.List;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

/**
 * Generic handler(writer) of output elements for commands
 */
public interface Writer<T extends DataSchemaNode> {

    void write(T dataSchemaNode, List<Node<?>> dataNodes) throws WriteException;

}
