
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.test.internal;


import org.junit.Assert;
import org.junit.Test;

import junit.framework.TestCase;

public class TestClusteringTest extends TestCase {

	@Test
	public void testComplexClass() {
		ComplexClass cc = new ComplexClass("cplxc1");
		Assert.assertTrue(cc.whoAmI().equals("ComplexClass_cplxc1"));
		cc.IAm("cplxc2");
		Assert.assertTrue(cc.whoAmI().equals("ComplexClass_cplxc2"));
	}
		
	@Test
	public void testComplexClass1() {
		ComplexClass1 cc1 = new ComplexClass1("cplxc1a");
		Assert.assertTrue(cc1.whoAmI().equals("ComplexClass1_cplxc1a"));
		cc1.IAm("cplxc1b");
		Assert.assertTrue(cc1.whoAmI().equals("ComplexClass1_cplxc1b"));
	}
		
		
	@Test
	public void testComplexContainer() {
		ComplexContainer cplxcontnr1 = new ComplexContainer("cct1", 5);
		Assert.assertTrue(cplxcontnr1.getIdentity().equals("[ComplexClass_cct1]-[ComplexClass1_cct1]"));
		Assert.assertTrue(cplxcontnr1.getState() == 5);
		
		cplxcontnr1.setIdentity("cct2");
		Assert.assertTrue(cplxcontnr1.getIdentity().equals("[ComplexClass_cct2]-[ComplexClass1_cct2]"));
		
		Assert.assertTrue(cplxcontnr1.toString().equals(
				"{ID:[ComplexClass_cct2]-[ComplexClass1_cct2],STATE:5}"));
	}
		
	@Test
	public void testStringContainer() {
		StringContainer strcontainer1 = new StringContainer();
		Assert.assertTrue(strcontainer1.getMystring() == null);
		Assert.assertTrue(strcontainer1.hashCode() == 0);
		
		StringContainer strcontainer2 = new StringContainer("foo");
		Assert.assertTrue(strcontainer2.getMystring() != null);
		Assert.assertTrue(strcontainer2.hashCode() != 0);
		
		strcontainer1.setMystring("foo");
		Assert.assertTrue(strcontainer2.equals(strcontainer1));
		
		Assert.assertTrue(strcontainer2.toString().equals("{foo}"));
	}
		

}
