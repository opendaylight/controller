package org.opendaylight.controller.connectionmanager;

import java.util.ArrayList;

public interface IConnectionManagerShell {

    public String scheme(String arg);
    public ArrayList<String> printNodes(String arg);
}
