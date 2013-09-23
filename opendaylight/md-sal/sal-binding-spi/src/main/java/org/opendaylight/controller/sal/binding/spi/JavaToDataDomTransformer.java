/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.spi;

import org.opendaylight.controller.concepts.lang.InputClassBasedTransformer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public interface JavaToDataDomTransformer<I extends DataObject> extends
        InputClassBasedTransformer<DataObject, I, CompositeNode> {
}
