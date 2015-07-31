/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.api.clustering;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 *
 */
public final class Entity {

    private final String type;
    private final YangInstanceIdentifier id;

    /**
     *
     * @param type
     * @param id
     */
    public Entity(@Nonnull String type, @Nonnull YangInstanceIdentifier id) {
        this.type = Preconditions.checkNotNull(type, "type should not be null");
        this.id = Preconditions.checkNotNull(id, "id should not be null");
    }

    /**
     *
     * @return id of entity
     */
    @Nonnull
    public YangInstanceIdentifier getId(){
        return id;
    }

    /**
     *
     * @return type of entity
     */
    @Nonnull
    public String getType(){
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Entity entity = (Entity) o;

        if (!id.equals(entity.id)) {
            return false;
        }

        if (!type.equals(entity.type)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Entity{");
        sb.append("type='").append(type).append('\'');
        sb.append(", id=").append(id);
        sb.append('}');
        return sb.toString();
    }
}
