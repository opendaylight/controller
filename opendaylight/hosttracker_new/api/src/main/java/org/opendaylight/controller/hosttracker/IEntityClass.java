/*
 * Copyright (c) 2011,2012 Big Switch Networks, Inc.
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the
 * "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *    Originally created by David Erickson, Stanford University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the
 *    License. You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an "AS
 *    IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *    express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 */

package org.opendaylight.controller.hosttracker;

import java.util.EnumSet;

import org.opendaylight.controller.hosttracker.IDeviceService.DeviceField;

/**
 * Entities within an entity class are grouped into {@link Device} objects based
 * on the {@link IEntityClass}, and the key fields specified by the entity
 * class. A set of entities are considered to be the same device if and only if
 * they belong to the same entity class and they match on all key fields for
 * that entity class. A field is effectively wildcarded by not including it in
 * the list of key fields returned by {@link IEntityClassifierService} and/or
 * {@link IEntityClass}.
 *
 * Note that if you're not using static objects, you'll need to override
 * {@link Object#equals(Object)} and {@link Object#hashCode()}.
 *
 * @author readams
 *
 */
public interface IEntityClass {
    /**
     * Return the set of key fields for this entity class. Entities belonging to
     * this class that differ in fields not included in this collection will be
     * considered the same device. The key fields for an entity class must not
     * change unless associated with a flush of that entity class.
     *
     * @return a set containing the fields that should not be wildcarded. May be
     *         null to indicate that all fields are key fields.
     */
    EnumSet<DeviceField> getKeyFields();

    /**
     * Returns a user-friendly, unique name for this EntityClass
     *
     * @return the name of the entity class
     */
    String getName();
}
