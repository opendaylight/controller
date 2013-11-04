/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2;

import java.io.IOException;

/**
 * May be thrown upon connect() if a HTTP proxy is being used.
 * 
 * @see Connection#connect()
 * @see Connection#setProxyData(ProxyData)
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */

public class HTTPProxyException extends IOException
{
	private static final long serialVersionUID = 2241537397104426186L;

	public final String httpResponse;
	public final int httpErrorCode;

	public HTTPProxyException(String httpResponse, int httpErrorCode)
	{
		super("HTTP Proxy Error (" + httpErrorCode + " " + httpResponse + ")");
		this.httpResponse = httpResponse;
		this.httpErrorCode = httpErrorCode;
	}
}
