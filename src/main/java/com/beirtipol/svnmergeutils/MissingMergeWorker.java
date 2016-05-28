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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnLogMergeInfo;
import org.tmatesoft.svn.core.wc2.SvnTarget;

/**
 * 
 * @author beirtipol@gmail.com
 *
 */
public class MissingMergeWorker {

	private static final Logger			LOGGER	= LoggerFactory.getLogger(MissingMergeWorker.class);
	private SVNURL						mergeSource;
	private SVNURL						mergeTarget;
	private SVNClientManager			clientManager;
	private Predicate<SVNLogEntry>[]	logEntryValidators;
	private boolean						verbose;

	@SuppressWarnings("unchecked")
	public MissingMergeWorker(SVNURL mergeSource, SVNURL mergeTarget, boolean verbose) {
		this(mergeSource, mergeTarget, verbose, null);

	}

	@SuppressWarnings("unchecked")
	public MissingMergeWorker(SVNURL mergeSource, SVNURL mergeTarget, boolean verbose, SVNClientManager clientManager, Predicate<SVNLogEntry>... logEntryValidators) {
		this.mergeSource = mergeSource;
		this.mergeTarget = mergeTarget;
		this.verbose = verbose;
		if (clientManager == null) {
			clientManager = SVNClientManager.newInstance();
		}
		this.clientManager = clientManager;
		this.logEntryValidators = logEntryValidators;
	}

	public List<SVNLogEntry> getMissingMerges() throws SVNException {
		List<SVNLogEntry> result = new ArrayList<>();

		SvnLogMergeInfo mergeInfo = clientManager.getDiffClient().getOperationsFactory().createLogMergeInfo();
		mergeInfo.addTarget(SvnTarget.fromURL(mergeTarget, SVNRevision.HEAD));
		mergeInfo.setSource(SvnTarget.fromURL(mergeSource, SVNRevision.HEAD));
		mergeInfo.setDiscoverChangedPaths(true);
		mergeInfo.setRevisionProperties(null);
		mergeInfo.setFindMerged(false);
		mergeInfo.setDepth(SVNDepth.INFINITY);
		mergeInfo.setReceiver(new ISvnObjectReceiver<SVNLogEntry>() {
			public void receive(SvnTarget target, SVNLogEntry logEntry) throws SVNException {
				for (Predicate<SVNLogEntry> validator : logEntryValidators) {
					if (!validator.test(logEntry)) {
						return;
					}
				}

				if (verbose) {
					LOGGER.info("Missing Merge: r{}: {}: {}: {}", logEntry.getRevision(), logEntry.getAuthor(), logEntry.getMessage(), logEntry.getDate());
				}
				result.add(logEntry);
			}
		});

		mergeInfo.run();
		return result;
	}
}
