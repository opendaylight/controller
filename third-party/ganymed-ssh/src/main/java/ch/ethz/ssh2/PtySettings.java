/*
 * Copyright (c) 2012-2013 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2;

/**
 * PTY settings for a SSH session. Zero dimension parameters are ignored. The character/row dimensions
 * override the pixel dimensions (when nonzero). Pixel dimensions refer to
 * the drawable area of the window. The dimension parameters are only
 * informational. The encoding of terminal modes (parameter
 * <code>terminal_modes</code>) is described in RFC4254.
 * 
 * @author Christian
 */
public class PtySettings
{
	/**
	 * TERM environment variable value (e.g., vt100)
	 */
	public String term;

	/**
	 * Terminal width, characters (e.g., 80)
	 */
	public int term_width_characters;

	/**
	 * Terminal height, rows (e.g., 24)
	 */
	public int term_height_characters;

	/**
	 * Terminal width, pixels (e.g., 640)
	 */
	public int term_width_pixels;

	/**
	 * Terminal height, pixels (e.g., 480)
	 */
	public int term_height_pixels;

	/**
	 * Encoded terminal modes
	 */
	public byte[] terminal_modes;
}
