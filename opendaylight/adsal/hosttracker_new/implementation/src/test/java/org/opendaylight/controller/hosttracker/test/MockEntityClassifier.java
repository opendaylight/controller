/*
 * Copyright (c) 2013 Big Switch Networks, Inc.
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
 */

package org.opendaylight.controller.hosttracker.test;

import static org.opendaylight.controller.hosttracker.IDeviceService.DeviceField.MAC;
import static org.opendaylight.controller.hosttracker.IDeviceService.DeviceField.SWITCHPORT;
import static org.opendaylight.controller.hosttracker.IDeviceService.DeviceField.VLAN;

import java.util.EnumSet;

import org.opendaylight.controller.hosttracker.Entity;
import org.opendaylight.controller.hosttracker.IDeviceService;
import org.opendaylight.controller.hosttracker.IDeviceService.DeviceField;
import org.opendaylight.controller.hosttracker.IEntityClass;
import org.opendaylight.controller.hosttracker.internal.DefaultEntityClassifier;

/**
 * A simple IEntityClassifier. Useful for tests that need IEntityClassifiers and
 * IEntityClass'es with switch and/or port key fields
 */
public class MockEntityClassifier extends DefaultEntityClassifier {
    public static class TestEntityClass implements IEntityClass {
        @Override
        public EnumSet<DeviceField> getKeyFields() {
            return EnumSet.of(MAC, VLAN, SWITCHPORT);
        }

        @Override
        public String getName() {
            return "TestEntityClass";
        }
    }

    public static IEntityClass testEC = new MockEntityClassifier.TestEntityClass();

    @Override
    public IEntityClass classifyEntity(Entity entity) {
        if (((Long) entity.getPort().getNode().getID()) >= 10L) {
            return testEC;
        }
        return DefaultEntityClassifier.entityClass;
    }

    @Override
    public EnumSet<IDeviceService.DeviceField> getKeyFields() {
        return EnumSet.of(MAC, VLAN, SWITCHPORT);
    }

}