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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;

/**
 * 
 * @author beirtipol@gmail.com
 *
 */
public class SVNUtils {

	/**
	 * Name of the SVN Property for merge information
	 */
	public static final String	SVN_MERGEINFO_PROP		= "svn:mergeinfo";

	/**
	 * Regular Expression for ranges of revisions as used in SVN Properties
	 */
	private static Pattern		REVISION_RANGE_PATTERN	= Pattern.compile("(\\d*)-(\\d*)");

	/**
	 * Create an SVNPropertyValue object representing merge information for the given branches and revisions.
	 * 
	 * @param mergedRevisions
	 * @return
	 */
	public static SVNPropertyValue createMergeInfoPropertyValue(SortedMap<String, Set<Long>> mergedRevisions) {
		List<String> lines = new ArrayList<>();
		for (Entry<String, Set<Long>> entry : mergedRevisions.entrySet()) {
			StringBuilder sb = new StringBuilder();
			sb.append(entry.getKey());
			sb.append(":");
			sb.append(StringUtils.join(entry.getValue(), ","));
			lines.add(sb.toString());
		}
		StringBuilder prop = new StringBuilder();
		prop.append(StringUtils.join(lines, "\n"));
		prop.append("\r");
		return SVNPropertyValue.create(prop.toString());
	}

	public static SVNPropertyData getMergeInformation(SVNClientManager clientManager, File path) throws SVNException {
		if (!path.isDirectory()) {
			return null;
		} else {
			SVNWCClient wcClient = clientManager.getWCClient();
			return wcClient.doGetProperty(path, SVN_MERGEINFO_PROP, SVNRevision.HEAD, SVNRevision.HEAD);
		}
	}

	public static SortedMap<String, Set<Long>> parseMergeInfoPropertyData(SVNPropertyData propData) {
		SortedMap<String, Set<Long>> result = new TreeMap<>();
		if (propData == null) {
			return result;
		}

		SVNPropertyValue value = propData.getValue();
		if (value != null) {
			String propString = value.getString();
			for (String line : propString.split("\n")) {
				String[] branchMerges = line.split(":");
				String branch = branchMerges[0];
				Set<Long> revisions = new TreeSet<>();
				for (String mergedRevision : branchMerges[1].split(",")) {
					mergedRevision = StringUtils.strip(mergedRevision);
					Matcher matcher = REVISION_RANGE_PATTERN.matcher(mergedRevision);
					if (matcher.matches()) {
						for (long l = Long.parseLong(matcher.group(1)); l <= Long.parseLong(matcher.group(2)); l++) {
							revisions.add(l);
						}
					} else {
						revisions.add(Long.parseLong(mergedRevision));
					}
				}
				result.put(branch, revisions);
			}
		}
		return result;
	}
}
