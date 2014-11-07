/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.md.sal.binding.api;

import java.util.Collection;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataRoot;

public interface ModifiedDataRoot {

    <T extends ChildOf<? extends DataRoot>> T getRootBefore(Class<T> root);

    <T extends ChildOf<? extends DataRoot>> T getRootAfter(Class<T> root);

    Collection<ModifiedDataObject> getModifiedChildren();
}
