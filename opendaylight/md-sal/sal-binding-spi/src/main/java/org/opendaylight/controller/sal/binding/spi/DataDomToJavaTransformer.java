/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.spi;

import org.opendaylight.controller.concepts.lang.Transformer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public interface DataDomToJavaTransformer<P extends DataObject> extends Transformer<CompositeNode, P> {

    /**
     * Returns a QName of valid input composite node.
     * 
     * @return
     */
    QName getQName();
}
