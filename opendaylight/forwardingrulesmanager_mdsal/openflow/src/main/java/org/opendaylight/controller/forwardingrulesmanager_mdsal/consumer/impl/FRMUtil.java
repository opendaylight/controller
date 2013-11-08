package org.opendaylight.controller.forwardingrulesmanager_mdsal.consumer.impl;

import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.bucket.Actions;

public class FRMUtil {
    private static final String NAMEREGEX = "^[a-zA-Z0-9]+$";
    public enum operation {ADD, DELETE, UPDATE, GET};
    
    
    public static boolean isNameValid(String name) {
    
        //  Name validation 
        if (name == null || name.trim().isEmpty() || !name.matches(NAMEREGEX)) {
            return false;
        }
        return true;
        
    }
    
    public static boolean areActionsValid(Actions actions) {
     //   List<Action> actionList;
       // Action actionRef;
      //  if (null != actions && null != actions.getAction()) {
       //     actionList = actions.getAction();
            

 
               
       // }
        
        return true;
    }
}
