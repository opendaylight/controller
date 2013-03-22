/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.usermanager.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import org.opendaylight.controller.usermanager.ISessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;

public class SessionManager implements ISessionManager {

    private static final Logger logger = LoggerFactory
            .getLogger(SessionManager.class);

    private Map<ServletContext, Set<HttpSession>> sessionMap = new HashMap<ServletContext, Set<HttpSession>>();
    public static final String SPRING_SECURITY_CONTEXT_KEY = "SPRING_SECURITY_CONTEXT";

    @Override
    public void sessionCreated(HttpSessionEvent se) {

        ServletContext ctx = se.getSession().getServletContext();
        String path = ctx.getContextPath();

        logger.debug("Servlet Context Path created " + path);
        logger.debug("Session Id created for ctxt path " + se.getSession().getId());

        synchronized (sessionMap) {
            Set<HttpSession> set = sessionMap.get(ctx);
            if (set == null) {
                set = new HashSet<HttpSession>();
                sessionMap.put(ctx, set);
            }
            set.add(se.getSession());
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        ServletContext ctx = se.getSession().getServletContext();
        String path = ctx.getContextPath();
        logger.debug("Servlet Context Path of destroyed session - " + path);
        logger.debug("Session Id destroyed " + se.getSession().getId());

        synchronized (sessionMap) {
            Set<HttpSession> set = sessionMap.get(ctx);
            if (set != null) {
                set.remove(se.getSession());
            }
        }
    }

    @Override
    public void invalidateSessions(String username, String sessionId) {

        synchronized (sessionMap) {
            List<HttpSession> sessionsList = new ArrayList<HttpSession>();
            Iterator<Map.Entry<ServletContext, Set<HttpSession>>> sessMapIterator = sessionMap
                    .entrySet().iterator();
            while (sessMapIterator.hasNext()) {

                Entry<ServletContext, Set<HttpSession>> val = sessMapIterator
                        .next();
                Iterator<HttpSession> sessIterator = val.getValue().iterator();

                while (sessIterator.hasNext()) {
                    HttpSession session = sessIterator.next();
                    if (session != null && sessionId != null && session.getId() != null && !session.getId().equals(sessionId)) {
                        Object contextFromSession = session
                                .getAttribute(SPRING_SECURITY_CONTEXT_KEY);
                        if (contextFromSession != null
                                && contextFromSession instanceof SecurityContext) {
                            String storedUserName = ((SecurityContext) contextFromSession)
                                    .getAuthentication().getName();
                            if (storedUserName != null && storedUserName.equals(username)) {                                
                                sessionsList.add(session);                                
                                sessIterator.remove();
                            }
                            else {
                                logger.debug("storedUserName is null or did not match username " + username);
                            }
                        } else {
                            logger.debug("contextFromSession is null or not instance of SecurityContext");
                        }
                    }
                    else {
                        logger.debug(" session or sessionId is null ");
                    }
                }
            }

            Iterator<HttpSession> sessionIt = sessionsList.iterator();
            while (sessionIt.hasNext()) {
                HttpSession session = sessionIt.next();
                sessionIt.remove();
                session.invalidate();
            }
        }
    }

}
