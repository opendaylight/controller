
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.web;

import java.util.List;

import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.usermanager.IUserManager;
import org.opendaylight.controller.usermanager.internal.UserConfig;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;

@Controller
@RequestMapping("/admin")
public class OneWebAdmin {
    @RequestMapping("/users")
    @ResponseBody
    public List<UserConfig> getUsers() {
        IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);
        if (userManager == null) {
            return null;
        }

        List<UserConfig> userConfList = userManager.getLocalUserList();

        return userConfList;
    }

    /*
     * Password in clear text, moving to HTTP/SSL soon
     */
    @RequestMapping(value = "/users", method = RequestMethod.POST)
    @ResponseBody
    public String saveLocalUserConfig(
            @RequestParam(required = true) String json,
            @RequestParam(required = true) String action) {

    	IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);
        if (userManager == null) {
        	return "Internal Error";
        }
        
        if (!authorize(userManager, UserLevel.NETWORKADMIN)) {
			return "Operation not permitted";
        }
    	
        Gson gson = new Gson();
        UserConfig config = gson.fromJson(json, UserConfig.class);
        
        Status result = (action.equals("add")) ? 
        		userManager.addLocalUser(config)
                   : userManager.removeLocalUser(config);

        return result.getDescription();
    }
    
    @RequestMapping(value = "/users/{username}", method = RequestMethod.POST)
    @ResponseBody
    public String removeLocalUser(@PathVariable("username") String userName) {
    	if(SecurityContextHolder.getContext().getAuthentication()
    			.getName().equals(userName)) {
    		return "Invalid Request: User cannot delete itself";
    	}
    	
    	IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);
        if (userManager == null) {
        	return "Internal Error";
        }
        
        if (!authorize(userManager, UserLevel.NETWORKADMIN)) {
			return "Operation not permitted";
        }
        
        return userManager.removeLocalUser(userName).getDescription();
    }
    
    /**
     * Is the operation permitted for the given level
     * 
     * @param level
     */
    private boolean authorize(IUserManager userManager, UserLevel level) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserLevel userLevel = userManager.getUserLevel(username);
        return userLevel.toNumber() <= level.toNumber();
    }
}
