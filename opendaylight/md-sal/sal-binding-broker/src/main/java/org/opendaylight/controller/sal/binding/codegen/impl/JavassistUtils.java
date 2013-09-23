/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen.impl;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;

public class JavassistUtils {

    public static interface ClassGenerator {
        void process(CtClass cls);
    }

    public static interface MethodGenerator {
        void process(CtMethod method);
    }

    public static interface FieldGenerator {
        void process(CtField field);
    }
}
