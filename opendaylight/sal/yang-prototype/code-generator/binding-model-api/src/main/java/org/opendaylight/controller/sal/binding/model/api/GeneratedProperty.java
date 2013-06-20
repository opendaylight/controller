/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.model.api;

/**
 * Generated Property extends interface {@link MethodSignature} interface.
 * <br>
 * The Generated Property interface is designed to store information of
 * fields (or members) declared in Java Transfer Objects (or any java
 * classes) and their access counterparts (getters and setters).
 *
 * @see MethodSignature
 */
public interface GeneratedProperty extends TypeMember {

    /**
     * Returns <code>true</code> if the property si declared as read-only.
     * <br>
     * If the property has flag <code>isReadOnly == true</code> the property
     * SHOULD be generated as getter only.
     *
     * @return <code>true</code> if the property si declared as read-only.
     */
    public boolean isReadOnly();
}
