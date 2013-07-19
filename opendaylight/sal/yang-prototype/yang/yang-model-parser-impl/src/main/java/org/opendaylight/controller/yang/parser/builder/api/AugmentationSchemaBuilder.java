/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.api;

import org.opendaylight.controller.yang.model.api.AugmentationSchema;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;

/**
 * Interface for builders of 'augment' statement.
 */
public interface AugmentationSchemaBuilder extends DataNodeContainerBuilder {

    String getWhenCondition();

    void addWhenCondition(String whenCondition);

    void setDescription(String description);

    void setReference(String reference);

    void setStatus(Status status);

    String getTargetPathAsString();

    SchemaPath getTargetPath();

    void setTargetPath(SchemaPath path);

    AugmentationSchema build();

    boolean isResolved();

    void setResolved(boolean resolved);

}
