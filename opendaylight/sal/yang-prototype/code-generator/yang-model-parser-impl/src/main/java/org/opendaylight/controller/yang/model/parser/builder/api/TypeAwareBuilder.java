/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.api;

import org.opendaylight.controller.yang.model.api.TypeDefinition;

/**
 * Builders of all nodes, which can have 'type' statement must implement this interface.
 * [typedef, type, leaf, leaf-list, deviate]
 */
public interface TypeAwareBuilder {

	TypeDefinition<?> getType();
	void setType(TypeDefinition<?> type);

}
