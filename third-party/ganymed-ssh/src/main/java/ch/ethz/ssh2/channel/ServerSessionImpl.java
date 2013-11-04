
package ch.ethz.ssh2.channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ch.ethz.ssh2.ServerSession;
import ch.ethz.ssh2.ServerSessionCallback;

public class ServerSessionImpl implements ServerSession
{
	Channel c;
	public ServerSessionCallback sscb;

	public ServerSessionImpl(Channel c)
	{
		this.c = c;
	}

	public int getState()
	{
		return c.getState();
	}

	public InputStream getStdout()
	{
		return c.getStdoutStream();
	}

	public InputStream getStderr()
	{
		return c.getStderrStream();
	}

	public OutputStream getStdin()
	{
		return c.getStdinStream();
	}

	public void close()
	{
		try
		{
			c.cm.closeChannel(c, "Closed due to server request", true);
		}
		catch (IOException ignored)
		{
		}
	}

	public synchronized ServerSessionCallback getServerSessionCallback()
	{
		return sscb;
	}

	public synchronized void setServerSessionCallback(ServerSessionCallback sscb)
	{
		this.sscb = sscb;
	}
}
