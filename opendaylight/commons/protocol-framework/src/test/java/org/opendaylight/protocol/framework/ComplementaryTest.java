/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

@Deprecated
public class ComplementaryTest {

	@Test
	public void testExceptions() {
		final DeserializerException de = new DeserializerException("some error");
		final DocumentedException ee = new DocumentedException("some error");

		assertEquals(de.getMessage(), ee.getMessage());
	}
}
