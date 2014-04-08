/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.controller.sal.binding.codegen.impl;

import org.eclipse.xtext.xbase.lib.Exceptions;

@SuppressWarnings("all")
public class BrokerImplClassLoader extends ClassLoader {
  private final ClassLoader spiClassLoader;

  public BrokerImplClassLoader(final ClassLoader model, final ClassLoader spi) {
    super(model);
    this.spiClassLoader = spi;
  }

  public Class<? extends Object> loadClass(final String name) throws ClassNotFoundException {
    try {
      return super.loadClass(name);
    } catch (ClassNotFoundException e) {
        return this.spiClassLoader.loadClass(name);
    }
  }
}
