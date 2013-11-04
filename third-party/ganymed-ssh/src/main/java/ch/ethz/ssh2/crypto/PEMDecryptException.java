
package ch.ethz.ssh2.crypto;

/*
 * Copyright (c) 2002-2010 David Kocher. All rights reserved.
 *
 * http://cyberduck.ch/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Bug fixes, suggestions and comments should be sent to:
 * dkocher@cyberduck.ch
 */

import java.io.IOException;

/**
 * @version $Id: PEMDecryptException.java 5865 2010-03-27 03:28:11Z dkocher $
 */
public class PEMDecryptException extends IOException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PEMDecryptException(String message)
	{
		super(message);
	}
}
