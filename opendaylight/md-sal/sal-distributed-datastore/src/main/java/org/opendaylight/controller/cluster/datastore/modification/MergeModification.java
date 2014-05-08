/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.modification;

import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * MergeModification stores all the parameters required to merge data into the specified path
 */
public class MergeModification extends AbstractModification{
  private final NormalizedNode data;


  public MergeModification(InstanceIdentifier path, NormalizedNode data) {
    super(path);
    this.data = data;
  }

  @Override
  public void apply(DOMStoreWriteTransaction transaction) {
    transaction.merge(path, data);
  }
}
