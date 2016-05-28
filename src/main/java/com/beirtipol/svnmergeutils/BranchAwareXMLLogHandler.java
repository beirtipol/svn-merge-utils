/**
 * MIT License
 * 
 * Copyright (c) 2016 Beirtí Ó'Nunáin
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.beirtipol.svnmergeutils;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.wc.xml.SVNXMLLogHandler;
import org.xml.sax.ContentHandler;

public class BranchAwareXMLLogHandler extends SVNXMLLogHandler {

	private String	mergeTargetPath;
	private String	mergeSourcePath;
	private boolean	started;
	private int		numberOfRevisions	= 0;

	public BranchAwareXMLLogHandler(ContentHandler contentHandler, String mergeTargetPath, String mergeSourcePath, boolean started) {
		super(contentHandler);
		this.mergeTargetPath = mergeTargetPath;
		this.mergeSourcePath = mergeSourcePath;
		this.started = started;
	}

	@Override
	public void handleLogEntry(SVNLogEntry arg0) throws SVNException {
		if (!started) {
			startDocument();
			started = true;
		}
		numberOfRevisions++;
		addAttribute("mergeSource", mergeSourcePath);
		addAttribute("mergeTarget", mergeTargetPath);
		super.handleLogEntry(arg0);
	}

	/**
	 * 
	 * @return the count of revisions which have been handled.
	 */
	public int numberOfRevisions() {
		return numberOfRevisions;
	}

	/**
	 * 
	 * @return true if at least one revision has been handled.
	 */
	public boolean started() {
		return started;
	}

}
