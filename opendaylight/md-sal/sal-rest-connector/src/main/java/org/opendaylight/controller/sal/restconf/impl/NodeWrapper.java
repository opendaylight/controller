/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import java.net.URI;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;

/**
 * @deprecated class will be removed in Lithium release
 */
@Deprecated
public interface NodeWrapper<T extends Node<?>> {

    void setQname(QName name);

    QName getQname();

    T unwrap();

    boolean isChangeAllowed();

    URI getNamespace();

    void setNamespace(URI namespace);

    String getLocalName();
}
