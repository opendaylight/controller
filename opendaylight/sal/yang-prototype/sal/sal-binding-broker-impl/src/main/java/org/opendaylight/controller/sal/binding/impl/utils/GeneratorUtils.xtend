/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl.utils

import javassist.ClassPool

class GeneratorUtils {

    static val PREFIX = "_gen.";

    public static def generatedName(Class<?> cls, String suffix) {
        '''«PREFIX»«cls.package.name».«cls.simpleName»$«suffix»'''.toString()
    }
    
    public static def get(ClassPool pool,Class<?> cls) {
        pool.get(cls.name);
    }
}
