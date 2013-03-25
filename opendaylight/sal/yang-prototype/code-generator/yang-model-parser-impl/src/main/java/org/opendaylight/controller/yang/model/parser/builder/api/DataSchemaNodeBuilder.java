/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.api;

import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.parser.builder.impl.ConstraintsBuilder;

/**
 * Interface for all yang data-schema nodes [anyxml, case, container, grouping,
 * list, module, notification].
 */
public interface DataSchemaNodeBuilder extends SchemaNodeBuilder {

    DataSchemaNode build();

    void setAugmenting(boolean augmenting);

    void setConfiguration(boolean configuration);

    ConstraintsBuilder getConstraintsBuilder();

}
