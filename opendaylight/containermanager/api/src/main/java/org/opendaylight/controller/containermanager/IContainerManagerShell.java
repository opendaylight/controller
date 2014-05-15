package org.opendaylight.controller.containermanager;

import java.util.List;

public interface IContainerManagerShell {
    public List<String> psc();
    public List<String> pfc();
    public List<String> psd();
    public List<String> psp();
    public List<String> psm();
    public List<String> addContainer(String arg1, String arg2);
    public List<String> createContainer(String arg1, String arg2);
    public List<String> removeContainerShell(String arg1);
    public List<String> addContainerEntry(String arg1, String arg2, String arg3);
    public List<String> removeContainerEntry(String arg1, String arg2, String arg3);
    public List<String> addContainerFlow(String arg1, String arg2, String arg3);
    public List<String> removeContainerFlow(String arg1, String arg2);
    public List<String> containermgrGetRoles();
    public List<String> containermgrGetAuthorizedGroups(String arg1);
    public List<String> containermgrGetAuthorizedResources(String arg1);
    public List<String> containermgrGetResourcesForGroup(String arg1);
    public List<String> containermgrGetUserLevel(String arg1);
    public List<String> containermgrGetUserResources(String arg1);
    public List<String> saveConfig();
}