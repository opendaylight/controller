/*
 * Copyright (c) 2012 Big Switch Networks, Inc.
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

package org.opendaylight.controller.hosttracker.internal;

import java.util.Iterator;

/**
 * An iterator for handling device index queries
 */
public class DeviceIndexInterator implements Iterator<Device> {
    private DeviceManagerImpl deviceManager;
    private Iterator<Long> subIterator;

    /**
     * Construct a new device index iterator referring to a device manager
     * instance and an iterator over device keys
     * 
     * @param deviceManager the device manager
     * @param subIterator an iterator over device keys
     */
    public DeviceIndexInterator(DeviceManagerImpl deviceManager,
                                Iterator<Long> subIterator) {
        super();
        this.deviceManager = deviceManager;
        this.subIterator = subIterator;
    }

    @Override
    public boolean hasNext() {
        return subIterator.hasNext();
    }

    @Override
    public Device next() {
        Long next = subIterator.next();
        return deviceManager.deviceMap.get(next);
    }

    @Override
    public void remove() {
        subIterator.remove();
    }

}
