package org.opendaylight.controller.cluster.datastore.node.utils;

public class PathUtils {
    public static String getParentPath(String currentElementPath){
        String parentPath = "";

        if(currentElementPath != null){
            String[] parentPaths = currentElementPath.split("/");
            if(parentPaths.length > 2){
                for(int i=0;i<parentPaths.length-1;i++){
                    if(parentPaths[i].length() > 0){
                        parentPath += "/" + parentPaths[i];
                    }
                }
            }
        }
        return parentPath;
    }
}
