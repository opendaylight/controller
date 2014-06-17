/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class UpdateSchemaContext {
  private final SchemaContext schemaContext;

  public UpdateSchemaContext(SchemaContext schemaContext) {
    this.schemaContext = schemaContext;
  }

  public SchemaContext getSchemaContext() {
    return schemaContext;
  }
}
