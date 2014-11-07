/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.md.sal.binding.api;

import java.util.Collection;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

/**
 * Modified Data Object.
 *
 * Represents modification of Data Object.
 *
 */
// FIXME: Could be generic interface? Or generic interface will be helper class?

public interface ModifiedDataObject extends Identifiable<PathArgument> {

    enum ModificationType {
        SUBTREE_MODIFIED,
        WRITE,
        DELETE
    }

    @Override
    PathArgument getIdentifier();

    ModificationType getModificationType();

    @Nullable DataObject getDataBefore();

    @Nullable DataObject getDataAfter();

    Collection<ModifiedDataObject> getModifiedChildren();

}
