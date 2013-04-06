/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.web;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.opendaylight.controller.configuration.IConfigurationService;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.usermanager.IUserManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/")
public class DaylightWeb {
    @RequestMapping(value = "")
    public String index(Model model, HttpServletRequest request) {
        IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);
        if (userManager == null) {
            return "User Manager is not available";
        }

        String username = request.getUserPrincipal().getName();

        model.addAttribute("username", username);
        model.addAttribute("role", userManager.getUserLevel(username)
                .toNumber());

        return "main";
    }

    @RequestMapping(value = "web.json")
    @ResponseBody
    public Map<String, Map<String, Object>> bundles(HttpServletRequest request) {
        Object[] instances = ServiceHelper.getGlobalInstances(
                IDaylightWeb.class, this, null);
        Map<String, Map<String, Object>> bundles = new HashMap<String, Map<String, Object>>();
        Map<String, Object> entry;
        IDaylightWeb bundle;
        String username = request.getUserPrincipal().getName();
        IUserManager userManger = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);
        for (Object instance : instances) {
            bundle = (IDaylightWeb) instance;
            if (userManger != null
                    && bundle.isAuthorized(userManger.getUserLevel(username))) {
                entry = new HashMap<String, Object>();
                entry.put("name", bundle.getWebName());
                entry.put("order", bundle.getWebOrder());
                bundles.put(bundle.getWebId(), entry);
            }
        }
        return bundles;
    }

    @RequestMapping(value = "save", method = RequestMethod.POST)
    @ResponseBody
    public String save(HttpServletRequest request) {
        String username = request.getUserPrincipal().getName();
        IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);
        if (userManager == null) {
            return "User Manager is not available";
        }

        UserLevel level = userManager.getUserLevel(username);
        if (level == UserLevel.NETWORKOPERATOR) {
            return "Save not permitted for Operator";
        }

        Status status = new Status(StatusCode.UNAUTHORIZED,
                "Operation not allowed for current user");
        if (level == UserLevel.NETWORKADMIN || level == UserLevel.SYSTEMADMIN) {
            IConfigurationService configService = (IConfigurationService) ServiceHelper
                    .getGlobalInstance(IConfigurationService.class, this);
            if (configService != null) {
                status = configService.saveConfigurations();
            }
        }

        return status.getDescription();
    }

    @RequestMapping(value = "logout")
    public String login(Map<String, Object> model,
            final HttpServletRequest request) {

        IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);
        if (userManager == null) {
            return "User Manager is not available";
        }
        String username = request.getUserPrincipal().getName();
        HttpSession session = request.getSession(false);
        if (session != null) {
            if (username != null) {
                userManager.userLogout(username);
            }
            session.invalidate();

        }
        return "redirect:" + "/";
    }

    @RequestMapping(value = "login")
    public String login(Model model, final HttpServletRequest request,
            final HttpServletResponse response) {
        // response.setHeader("X-Page-Location", "/login");
        IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);
        if (userManager == null) {
            return "User Manager is not available";
        }

        String username = request.getUserPrincipal().getName();

        model.addAttribute("username", username);
        model.addAttribute("role", userManager.getUserLevel(username)
                .toNumber());
        return "forward:" + "/";
    }

}
