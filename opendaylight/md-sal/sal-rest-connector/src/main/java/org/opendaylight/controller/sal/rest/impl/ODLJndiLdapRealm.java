/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.rest.impl;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapContext;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.ldap.JndiLdapContextFactory;
import org.apache.shiro.realm.ldap.JndiLdapRealm;
import org.apache.shiro.realm.ldap.LdapContextFactory;
import org.apache.shiro.realm.ldap.LdapUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An extended implementation of the default <code>JdniLdapRealm</code>, which
 * is tailored specifically to ODL.
 *
 * TODO Fix the <code>getRoleNamesForUser()</code> method once groups can be extracted
 * from LDAP.  This method will effectively map LDAP groups to Shiro roles.  This logic
 * will be hard-coded first pass, then pushed into a model later.
 *
 * @author ryandgoulding@gmail.com
 * @see JndiLdapRealm
 */
public class ODLJndiLdapRealm extends JndiLdapRealm {
    private static final Logger LOG = LoggerFactory.getLogger(ODLJndiLdapRealm.class);

    private static final String LDAP_CONNECTION_MESSAGE = "AAA LDAP connection from ";

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(
            AuthenticationToken token) throws AuthenticationException {

        try {
            String username = (String) token.getPrincipal();
            String message = String.format(LDAP_CONNECTION_MESSAGE + username);
            LOG.info(message);
            return super.doGetAuthenticationInfo(token);
        } catch (ClassCastException e) {
            LOG.info("Couldn't service the LDAP connection", e);
        }
        return null;
    }

    /**
     * utility method to return a JdniContextFactory
     *
     * @return
     */
    private JndiLdapContextFactory getFactory() {
        JndiLdapContextFactory jlcf = new JndiLdapContextFactory();

        jlcf.setUrl("ldap://freeipa.brcd-sssd-tb.com:389");

        return jlcf;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        Set<String> lhs = new LinkedHashSet<String>();
        lhs.add("admin");
        return new SimpleAuthorizationInfo(lhs);
        /*
        JndiLdapContextFactory jlcf = this.getFactory();
        AuthorizationInfo ai = null;
        try {
            ai = this.queryForAuthorizationInfo(principals, jlcf);
        } catch (NamingException e) {
            e.printStackTrace();
        }
        return ai;
        */
    }
    
    @Override
    protected AuthorizationInfo queryForAuthorizationInfo(
            PrincipalCollection principals,
            LdapContextFactory ldapContextFactory) throws NamingException {

        String username = (String) getAvailablePrincipal(principals);
        LdapContext ldapContext = ldapContextFactory.getSystemLdapContext();
        Set<String> roleNames;

        try {
            roleNames = getRoleNamesForUser(username, ldapContext);
        } finally {
            LdapUtils.closeContext(ldapContext);
        }

        return buildAuthorizationInfo(roleNames);
    }

    protected AuthorizationInfo buildAuthorizationInfo(Set<String> roleNames) {
        return new SimpleAuthorizationInfo(roleNames);
    }
    
    /**
     * extracts the Set of roles associated with a user based on the username and ldap context (server).
     * 
     * @param username
     * @param ldapContext
     * @return
     * @throws NamingException
     */
    protected Set<String> getRoleNamesForUser(String username,
            LdapContext ldapContext) throws NamingException {

        Set<String> roleNames = new LinkedHashSet<String>();
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);


        // TODO fix the searchFilter & searchBase so that groups are properly returned from LDAP
        String searchFilter = "(&(objectClass=*)(CN={0}))";
        Object []searchArguments = new Object[]{ username };
        String searchBase = "cn=users,cn=accounts,dc=brcd-sssd-tb,dc=com";
        NamingEnumeration answer = ldapContext.search(searchBase,
                searchFilter, searchArguments, searchControls);

//        while (answer.hasMoreElements()) {
//            SearchResult sr = (SearchResult) answer.next();
//            Attributes attrs = sr.getAttributes();
//            if (attrs != null) {
//                NamingEnumeration ae = attrs.getAll();
//                while (ae.hasMore()) {
//                    Attribute attr = (Attribute) ae.next();
//                    if (attr.getID().equals("memberOf")) {
//                        Collection<String> groupNames = LdapUtils.getAllAttributeValues(attr);
//                        Collection<String> rolesForGroups = groupNames;//getRoleNamesForGroups(groupNames);
//                        roleNames.addAll(rolesForGroups);
//                    }
//                }
//            }
//        }
        // TODO remove this code once the former is complete
        roleNames.add("admin");
        return roleNames;
    }
}
