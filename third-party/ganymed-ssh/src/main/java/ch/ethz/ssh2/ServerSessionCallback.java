/*
 * Copyright (c) 2012-2013 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */

package ch.ethz.ssh2;

import java.io.IOException;

/**
 * The interface for an object that receives events based on requests that
 * a client sends on a session channel. Once the session channel has been set up,
 * a program is started at the server end. The program can be a shell, an application
 * program, or a subsystem with a host-independent name. Only one of these requests
 * can succeed per channel.
 * <p/>
 * <b>CAUTION: These event methods are being called from the receiver thread. The receiving of
 * messages will be blocked until your event handler returns. To signal to the client that you
 * are willing to accept its request, return a {@link Runnable} object which will be executed
 * in a new thread <i>after</i> the acknowledgment has been sent back to the client.</b>
 * <p/>
 * <b>If a method does not allow you to return a {@link Runnable}, then the
 * SSH protocol does not allow to send a status back to the client (more exactly, the client cannot
 * request an acknowledgment). In these cases, if you need to invoke
 * methods on the {@link ServerSession} or plan to execute long-running activity, then please do this from within a new {@link Thread}.</b>
 * <p/>
 * If you want to signal a fatal error, then please throw an <code>IOException</code>. Currently, this will
 * tear down the whole SSH connection.
 * 
 * @see ServerSession
 * 
 * @author Christian
 *
 */
public interface ServerSessionCallback
{
	public Runnable requestPtyReq(ServerSession ss, PtySettings pty) throws IOException;

	public Runnable requestEnv(ServerSession ss, String name, String value) throws IOException;

	public Runnable requestShell(ServerSession ss) throws IOException;

	public Runnable requestExec(ServerSession ss, String command) throws IOException;

	public Runnable requestSubsystem(ServerSession ss, String subsystem) throws IOException;

	/**
	 * When the window (terminal) size changes on the client side, it MAY send a message to the other side to inform it of the new dimensions.
	 * 
	 * @param ss the corresponding session
	 * @param term_width_columns
	 * @param term_height_rows
	 * @param term_width_pixels
	 */
	public void requestWindowChange(ServerSession ss, int term_width_columns, int term_height_rows,
			int term_width_pixels, int term_height_pixels) throws IOException;

	/**
	 * A signal can be delivered to the remote process/service. Some systems may not implement signals, in which case they SHOULD ignore this message.
	 * 
	 * @param ss the corresponding session
	 * @param signal (a string without the "SIG" prefix)
	 * @return
	 * @throws IOException
	 */

}
