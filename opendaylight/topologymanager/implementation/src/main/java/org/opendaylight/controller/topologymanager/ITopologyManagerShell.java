package org.opendaylight.controller.topologymanager;

import java.util.List;

public interface ITopologyManagerShell {
    public List<String> printUserLink();
    public List<String> addUserLink(String name, String ncStr1, String ncStr2);
    public List<String> deleteUserLinkShell(String name);
    public List<String> printNodeEdges();
}
