/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.topology;

import com.google.common.annotations.Beta;

/**
 * A listener that recieves {@link #onRoleChanged(RoleChangeDTO)} callbacks when a role change occurs
 */
@Beta
public interface RoleChangeListener {

    /**
     * Called when a role change occurs
     *
     * @param roleChangeDTO a DTO that wraps the current ownership status
     */
    void onRoleChanged(RoleChangeDTO roleChangeDTO);

    /**
     * A DTO that wraps an ownership change status
     */
    class RoleChangeDTO {

        private final boolean wasOwner;
        private final boolean isOwner;
        private final boolean hasOwner;

        public RoleChangeDTO(final boolean wasOwner, final boolean isOwner, final boolean hasOwner) {
            this.wasOwner = wasOwner;
            this.isOwner = isOwner;
            this.hasOwner = hasOwner;
        }

        public boolean wasOwner() {
            return wasOwner;
        }

        public boolean isOwner() {
            return isOwner;
        }

        public boolean hasOwner() {
            return hasOwner;
        }
    }
}

