/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.Module;

public class ModuleUtil {

    private ModuleUtil() {
    }

    public static QName getQName(final Module currentModule) {
        return QName.create(currentModule.getNamespace(), currentModule.getRevision(), currentModule.getName());
    }
}
