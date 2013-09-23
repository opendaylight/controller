/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen.impl

import org.opendaylight.yangtools.yang.binding.BaseIdentity
import javassist.CtMethod
import java.lang.reflect.Method

@Data
class RoutingPair {

    @Property
    val Class<? extends BaseIdentity> context;
    @Property
    val CtMethod getter;
}
