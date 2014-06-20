/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree.spi;

/**
 * The concept of a version, either node version, or a subtree version. The
 * only interface contract this class has is that no two versions are the
 * same.
 */
public final class Version {
    private Version() {

    }

    /**
     * Create a new version, distinct from any other version.
     *
     * @return a new version.
     */
    public Version next() {
        return new Version();
    }

    /**
     * Create an initial version.
     *
     * @return a new version.
     */
    public static final Version initial() {
        return new Version();
    }
}
