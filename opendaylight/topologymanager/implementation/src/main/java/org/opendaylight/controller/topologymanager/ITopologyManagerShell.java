/**
* Copyright (c) 2014 Inocybe Technologies, and others. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.topologymanager;

import java.util.List;

public interface ITopologyManagerShell {
    public List<String> printUserLink();
    public List<String> addUserLink(String name, String ncStr1, String ncStr2);
    public List<String> deleteUserLinkShell(String name);
    public List<String> printNodeEdges();
}
