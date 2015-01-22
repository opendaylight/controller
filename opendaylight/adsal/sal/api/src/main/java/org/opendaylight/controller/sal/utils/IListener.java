/*
 * Copyright (c) 2011 Big Switch Networks, Inc.
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

package org.opendaylight.controller.sal.utils;

/**
 * This interface defines the methods for callback ordering
 *
 */
@Deprecated
public interface IListener<T> {
    public enum Command {
        CONTINUE, STOP
    }

    /**
     * The name assigned to this listener
     *
     * @return the name string
     */
    public String getName();

    /**
     * Check if the module called name is a callback ordering prerequisite for
     * this module. In other words, if this function returns true for the given
     * name, then this listener will be called after that message listener.
     *
     * @param type
     *            the object type to which this applies
     * @param name
     *            the name of the module
     * @return whether name is a prerequisite.
     */
    public boolean isCallbackOrderingPrereq(T type, String name);

    /**
     * Check if the module called name is a callback ordering post-requisite for
     * this module. In other words, if this function returns true for the given
     * name, then this listener will be called before that message listener.
     *
     * @param type
     *            the object type to which this applies
     * @param name
     *            the name of the module
     * @return whether name is a post-requisite.
     */
    public boolean isCallbackOrderingPostreq(T type, String name);
}
