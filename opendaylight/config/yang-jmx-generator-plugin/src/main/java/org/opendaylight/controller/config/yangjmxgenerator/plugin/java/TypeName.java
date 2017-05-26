/*
 * Copyright (c) 2013, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yangjmxgenerator.plugin.java;

public enum TypeName {

    classType("class"), interfaceType("interface"), enumType("enum"), absClassType("abstract class"), finalClassType("final class");

    private final String value;

    TypeName(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
