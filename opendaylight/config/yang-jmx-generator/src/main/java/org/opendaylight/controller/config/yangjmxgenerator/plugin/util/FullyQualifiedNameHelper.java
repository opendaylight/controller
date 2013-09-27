/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.util;

public class FullyQualifiedNameHelper {
    public static String getFullyQualifiedName(String packageName,
            String className) {
        if (packageName.isEmpty())
            return className;
        return packageName + "." + className;
    }
}
