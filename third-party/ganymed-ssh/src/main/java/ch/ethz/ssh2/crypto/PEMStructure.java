/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.crypto;

/**
 * Parsed PEM structure.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */

public class PEMStructure
{
	int pemType;
	String dekInfo[];
	String procType[];
	byte[] data;
}