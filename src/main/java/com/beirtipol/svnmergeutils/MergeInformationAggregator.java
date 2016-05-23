package com.beirtipol.svnmergeutils;

import java.io.File;
import java.io.FileFilter;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNWCClient;

/**
 * When using SVN merge and recording merge information, it is suggested to always record merge information at the
 * project level. This is because when you attempt to merge,or use the MissingMergeChecker to detect eligible merges,
 * you will sometimes see classes which appear to have not been merged but in fact have. This is due to merge
 * information being duplicated at different levels of the tree.
 * 
 * This program aggregates all merge information in a directory tree up to the level specified as the workingCopyRoot.
 * 
 * @author Beirti
 *
 */
public class MergeInformationAggregator extends AbstractWorker {
	private static final Logger	LOGGER				= LoggerFactory.getLogger(MergeInformationAggregator.class);

	public static final String	SVN_MERGEINFO_PROP	= "svn:mergeinfo";

	@Option(name = "--workingCopyRoot", usage = "Path to aggregate merge information to.", required = true)
	private File				workingCopyRoot;

	@Option(name = "--mergeSourcesRoot", usage = "Root folder of merge source directories, relative to the repository root. Should include leading and trailing slashes. e.g. '/myproject/branches/'", required = true)
	private String				mergeSourcesRoot;

	private SVNClientManager	clientManager;

	public static void main(String[] args) throws SVNException {
		new MergeInformationAggregator().doMain(args);
	}

	public MergeInformationAggregator() throws SVNException {
		// noargs constructor for command line execution
		clientManager = SVNClientManager.newInstance();
	}

	private void walkFileTree(File path, SortedMap<String, Set<Long>> mergedRevisions) throws SVNException {
		File[] files = path.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return !pathname.getName().equals(".svn");
			}
		});
		if (files == null) {
			return;
		}
		for (File file : files) {
			SortedMap<String, Set<Long>> fileMergedRevisions = SVNUtils.parseMergeInfoPropertyData(SVNUtils.getMergeInformation(clientManager, file));
			joinMergeInfo(mergedRevisions, fileMergedRevisions);
			SVNWCClient wcClient = clientManager.getWCClient();
			wcClient.doSetProperty(file, SVN_MERGEINFO_PROP, null, true, SVNDepth.EMPTY, null, null);
			walkFileTree(file, mergedRevisions);
		}
	}

	private void joinMergeInfo(SortedMap<String, Set<Long>> to, SortedMap<String, Set<Long>> from) {
		for (Entry<String, Set<Long>> entry : from.entrySet()) {
			String branch = entry.getKey();
			if (branch.startsWith(mergeSourcesRoot)) {
				String branchName = branch.replaceFirst(mergeSourcesRoot, "").split("/")[0];
				branch = mergeSourcesRoot + branchName;
			}

			Set<Long> revisions = to.get(branch);
			if (revisions == null) {
				revisions = new TreeSet<>();
				to.put(branch, revisions);
			}
			revisions.addAll(entry.getValue());
		}
	}

	private void doMain(String[] args) throws SVNException {
		if (!handleArgs(args)) {
			return;
		}
		SortedMap<String, Set<Long>> rootMergeInfo = SVNUtils.parseMergeInfoPropertyData(SVNUtils.getMergeInformation(clientManager, workingCopyRoot));
		walkFileTree(workingCopyRoot, rootMergeInfo);
		SVNPropertyValue propValue = SVNUtils.createMergeInfoPropertyValue(rootMergeInfo);
		clientManager.getWCClient().doSetProperty(workingCopyRoot, SVN_MERGEINFO_PROP, propValue, true, SVNDepth.EMPTY, null, null);
	}

	@Override
	protected Logger getLogger() {
		return LOGGER;
	}

}
