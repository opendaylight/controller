/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen.impl

import java.lang.ClassLoader

class BrokerImplClassLoader extends ClassLoader {

    val ClassLoader spiClassLoader
    
    public new(ClassLoader model, ClassLoader spi) {
        super(model)
        spiClassLoader = spi;
    }

    override public loadClass(String name) throws ClassNotFoundException {
        try {
            return super.loadClass(name);
        } catch (ClassNotFoundException e) {
            return spiClassLoader.loadClass(name);
        }
    }

}
