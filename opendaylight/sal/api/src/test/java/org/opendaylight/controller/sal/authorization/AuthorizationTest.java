
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.authorization;
		
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.NodeCreator;
	
	public class AuthorizationTest {
	
	@Test
	public void testResources () {
	Privilege p = Privilege.WRITE;
	ResourceGroup resourceGroup = new ResourceGroup("NodeGroup", p);
	Map<ResourceGroup, ArrayList<Resource>> resourceMap = new HashMap<ResourceGroup, ArrayList<Resource>>();
	ArrayList<Resource> resourceList = new ArrayList<Resource>();
	
		for (int i = 0; i < 5; i++) {
			Node node = NodeCreator.createOFNode((long)i);
			Resource resource = new Resource (node, p);	
			resourceList.add(resource);
		}
		
		resourceMap.put(resourceGroup, resourceList);
		
		ArrayList<Resource> retrievedResourceList = resourceMap.get(resourceGroup);
		for (Entry<ResourceGroup, ArrayList<Resource>> entry : resourceMap.entrySet()) {
			ResourceGroup rGroup = entry.getKey();
			Assert.assertTrue(rGroup.getGroupName().equals(resourceGroup.getGroupName()));
			for (int i = 0; i < 5; i++) {
				Resource resource = retrievedResourceList.get(i);
				Assert.assertTrue(resource.getPrivilege().equals(Privilege.WRITE));
				Assert.assertTrue(((Long)((Node)resource.getResource()).getID()).equals((long)i));
			}
		}
	}
	
	@Test
	public void testAppRoleLevel() {
		AppRoleLevel appRoleLevel = AppRoleLevel.APPOPERATOR;
		Assert.assertTrue(appRoleLevel.toString().equals("App-Operator"));
		Assert.assertTrue(appRoleLevel.toNumber() == 2);
		Assert.assertTrue(appRoleLevel.toStringPretty().equals("Application Operator"));
	}
	
	@Test
	public void testUserLevel() {
		UserLevel userLevel = UserLevel.SYSTEMADMIN;
		Assert.assertTrue(userLevel.toString().equals("System-Admin"));
		Assert.assertTrue(userLevel.toNumber() == 0);
		Assert.assertTrue(userLevel.toStringPretty().equals("System Administrator"));
	}
	
	@Test
	public void testAppRoleLevelFromString() {
		Assert.assertTrue(AppRoleLevel.fromString("App-Admin") == AppRoleLevel.APPADMIN);
		Assert.assertTrue(AppRoleLevel.fromString("App-User") == AppRoleLevel.APPUSER);
		Assert.assertTrue(AppRoleLevel.fromString("App-Operator") == AppRoleLevel.APPOPERATOR);
		Assert.assertTrue(AppRoleLevel.fromString(" ") == null);
		Assert.assertTrue(AppRoleLevel.fromString("") == null);
		Assert.assertTrue(AppRoleLevel.fromString("App-Admini") == null);		
	}
}
