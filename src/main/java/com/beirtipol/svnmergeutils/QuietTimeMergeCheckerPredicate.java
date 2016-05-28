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

import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNLogEntry;

/**
 * 
 * @author beirtipol@gmail.com
 *
 */
public class QuietTimeMergeCheckerPredicate implements Predicate<SVNLogEntry> {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(QuietTimeMergeCheckerPredicate.class);
	private final long			quietTimeInMillis;
	private boolean				verbose;

	public QuietTimeMergeCheckerPredicate(long quietTimeInMillis, boolean verbose) {
		this.quietTimeInMillis = quietTimeInMillis;
		this.verbose = verbose;
	}

	@Override
	public boolean test(SVNLogEntry entry) {
		boolean result = System.currentTimeMillis() - entry.getDate().getTime() > quietTimeInMillis;
		if (!result && verbose) {
			LOGGER.info(String.format("Skipping Log Entry for revision %s as it has been committed within the quiet time window of %sms", entry.getRevision(), quietTimeInMillis));
		}
		return result;
	}
}