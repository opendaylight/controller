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
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.hosttracker.Entity;
import org.opendaylight.controller.hosttracker.IDeviceService;
import org.opendaylight.controller.hosttracker.IDeviceService.DeviceField;
import org.opendaylight.controller.hosttracker.IEntityClass;
import org.opendaylight.controller.hosttracker.internal.DefaultEntityClassifier;

/**
 * Extension to simple entity classifier to help in unit tests to provide table
 * based multiple entity classification mock for reclassification tests
 *
 */
public class MockFlexEntityClassifier extends DefaultEntityClassifier {
    Map<Long, IEntityClass> switchEntities;
    Map<Short, IEntityClass> vlanEntities;

    public static class TestEntityClass implements IEntityClass {
        String name;

        public TestEntityClass(String name) {
            this.name = name;
        }

        @Override
        public EnumSet<DeviceField> getKeyFields() {
            return EnumSet.of(MAC);
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public static IEntityClass defaultClass = new TestEntityClass("default");

    public MockFlexEntityClassifier() {
        switchEntities = new HashMap<Long, IEntityClass>();
        vlanEntities = new HashMap<Short, IEntityClass>();
    }

    public IEntityClass createTestEntityClass(String name) {
        return new TestEntityClass(name);
    }

    public void addSwitchEntity(Long dpid, IEntityClass entityClass) {
        switchEntities.put(dpid, entityClass);
    }

    public void removeSwitchEntity(Long dpid) {
        switchEntities.remove(dpid);
    }

    public void addVlanEntities(Short vlan, IEntityClass entityClass) {
        vlanEntities.put(vlan, entityClass);
    }

    public void removeVlanEntities(Short vlan) {
        vlanEntities.remove(vlan);
    }

    @Override
    public IEntityClass classifyEntity(Entity entity) {
        if (switchEntities.containsKey((Long) entity.getPort().getNode()
                .getID()))
            return switchEntities
                    .get((Long) entity.getPort().getNode().getID());
        if (vlanEntities.containsKey(entity.getVlan()))
            return vlanEntities.get(entity.getVlan());
        return defaultClass;
    }

    @Override
    public EnumSet<IDeviceService.DeviceField> getKeyFields() {
        return EnumSet.of(MAC, VLAN, SWITCHPORT);
    }
}
