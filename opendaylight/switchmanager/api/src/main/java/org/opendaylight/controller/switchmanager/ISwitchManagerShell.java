/**
* Copyright (c) 2014 Inocybe Technologies, and others. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.switchmanager;

import java.util.List;

public interface ISwitchManagerShell {
    public List<String> pencs(String st);
    public List<String> pdm(String st);
}
