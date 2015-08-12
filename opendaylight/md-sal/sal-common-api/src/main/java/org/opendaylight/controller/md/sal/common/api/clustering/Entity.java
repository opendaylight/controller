/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.api.clustering;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * <p></p>
 * A clustered Entity is something which is shared by multiple applications across a cluster. An Entity has a type
 * and an identifier.
 * </p>
 * <p>
 * The type describes the type of the Entity where examples of a type maybe "openflow" or "netconf"
 * etc. An Entity type could be tied to how exactly an application shares and "owns" an entity. For example we may want
 * an application which deals with the openflow entity to be assigned ownership of that entity based on a first come
 * first served basis. On the other hand for netconf entity types we may want applications to gain ownership based on
 * a load balancing approach. While this mechanism of assigning a ownership acquisition strategy is not finalized the
 * intention is that the entity type will play a role in determining the strategy and thus should be put in place.
 * </p>
 * <p>
 * The identifier is a YangInstanceIdentifier. The reason for the choice of YangInstanceIdentifier is because it
 * can easily be used to represent a data node. For example an inventory node represents a shared entity and it is best
 * referenced by the YangInstanceIdentifier if the inventory node is stored in the data store.
 * </p>
 * Note that an entity identifier must conform to a valid yang schema. If there is no existing yang schema to
 * represent an entity, the general-entity yang model can be used.
 * <p>
 * </p>
 */
public final class Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final QName ENTITY_QNAME =
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.core.general.entity.rev150820.Entity.QNAME;
    private static final QName ENTITY_NAME = QName.create(ENTITY_QNAME, "name");

    private final String type;
    private final YangInstanceIdentifier id;

    /**
     * Construct an Entity with a YangInstanceIdentifier.
     *
     * @param type the type of the entity
     * @param id the identifier of the entity
     */
    public Entity(@Nonnull String type, @Nonnull YangInstanceIdentifier id) {
        this.type = Preconditions.checkNotNull(type, "type should not be null");
        this.id = Preconditions.checkNotNull(id, "id should not be null");
    }

    /**
     * Construct an Entity with an with a name. The general-entity schema is used to construct the
     * YangInstanceIdentifier.
     *
     * @param type the type of the entity
     * @param entityName the name of the entity used to construct a general-entity YangInstanceIdentifier
     */
    public Entity(@Nonnull String type, @Nonnull String entityName) {
        this.type = Preconditions.checkNotNull(type, "type should not be null");
        this.id = YangInstanceIdentifier.builder().node(ENTITY_QNAME).nodeWithKey(ENTITY_QNAME, ENTITY_NAME,
                        Preconditions.checkNotNull(entityName, "entityName should not be null")).build();
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
        return 31 * type.hashCode() + id.hashCode();
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
