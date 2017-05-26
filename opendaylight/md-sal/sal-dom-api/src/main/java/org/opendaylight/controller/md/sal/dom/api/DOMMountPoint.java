/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.api;

import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import com.google.common.base.Optional;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public interface DOMMountPoint extends Identifiable<YangInstanceIdentifier> {

    <T extends DOMService> Optional<T> getService(Class<T> cls);

    SchemaContext getSchemaContext();
}
