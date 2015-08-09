/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils;

import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class NodeIdentifierFactoryTest {

  @Test
  public void validateAugmentationIdentifier() {
    YangInstanceIdentifier.PathArgument argument =
        NodeIdentifierFactory
            .getArgument("AugmentationIdentifier{childNames=[(urn:opendaylight:flow:table:statistics?revision=2013-12-15)flow-table-statistics]}");

    assertTrue(argument instanceof YangInstanceIdentifier.AugmentationIdentifier);


  }

}
