/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api;

/**
 * The "anyxml" interface defines an interior node in the schema tree. It takes
 * one argument, which is an identifier represented by QName inherited from
 * {@link SchemaNode}, followed by a block of substatements that holds detailed
 * anyxml information. The substatements are defined in {@link DataSchemaNode} <br>
 * <br>
 * This interface was modeled according to definition in <a
 * href="https://tools.ietf.org/html/rfc6020#section-7.10">[RFC-6020] The anyxml
 * Statement</a>
 * 
 * 
 */
public interface AnyXmlSchemaNode extends DataSchemaNode {

}
