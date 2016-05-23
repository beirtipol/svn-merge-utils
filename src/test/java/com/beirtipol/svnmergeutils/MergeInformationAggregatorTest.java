package com.beirtipol.svnmergeutils;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;

public class MergeInformationAggregatorTest {
	private static final String	SVN_REPO	= MergeInformationAggregatorTest.class.getClassLoader().getResource(".").getPath() + File.separator + "test_svnrepo";
	private static final String	WC_PATH		= MergeInformationAggregatorTest.class.getClassLoader().getResource(".").getPath() + File.separator + "test_workingcopy";
	private static SVNURL		localRepoURL;

	@Before
	public void setUp() throws Exception {
		clearWorkingDirectories();//Just in case the test failed to tear down previously and we're left with a dodgy directory.
		SVNRepositoryFactoryImpl.setup();
		localRepoURL = SVNRepositoryFactory.createLocalRepository(new File(SVN_REPO), true, false);
	}

	@After
	public void tearDown() throws IOException {
		clearWorkingDirectories();
	}

	private void clearWorkingDirectories() throws IOException {
		FileUtils.deleteDirectory(new File(SVN_REPO));
		FileUtils.deleteDirectory(new File(WC_PATH));
	}

	@SuppressWarnings("serial")
	@Test
	public void testAggregateMergeInformation() throws SVNException {
		SVNRepository repo = SVNClientManager.newInstance().createRepository(localRepoURL, true);
		createRepoStructure(repo.getCommitEditor("Adding Base Structure", null));

		SVNClientManager clientManager = SVNClientManager.newInstance(null, repo.getAuthenticationManager());
		SVNUpdateClient updateClient = clientManager.getUpdateClient();
		updateClient.setIgnoreExternals(false);
		updateClient.doCheckout(SVNURL.fromFile(new File(SVN_REPO)), new File(WC_PATH), SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true);
		TreeMap<String, Set<Long>> expected = new TreeMap<>();
		expected.put("/branches/branch1", new TreeSet<Long>() {
			{
				add(1l);
				add(2l);
			}
		});
		expected.put("/branches/branch2", new TreeSet<Long>() {
			{
				add(3l);
			}
		});
		expected.put("/branches/branch3", new TreeSet<Long>() {
			{
				add(4l);
			}
		});

		File trunkPath = new File(WC_PATH, "trunk");
		MergeInformationAggregator.main(new String[] { "--workingCopyRoot", trunkPath.getAbsolutePath(), "--mergeSourcesRoot", "/branches/" });

		SVNPropertyData workingCopyProps = clientManager.getWCClient().doGetProperty(trunkPath, SVNUtils.SVN_MERGEINFO_PROP, SVNRevision.WORKING, SVNRevision.WORKING);
		SortedMap<String, Set<Long>> actual = SVNUtils.parseMergeInfoPropertyData(workingCopyProps);

		Assert.assertEquals(expected, actual);
	}

	@SuppressWarnings("serial")
	private SVNCommitInfo createRepoStructure(ISVNEditor commitEditor) throws SVNException {
		commitEditor.openRoot(-1);
		{
			commitEditor.addDir("trunk", null, -1);
			{
				commitEditor.addDir("trunk/folder1", null, -1);
				TreeMap<String, Set<Long>> trunk_folder1_mergedRevisions = new TreeMap<>();
				trunk_folder1_mergedRevisions.put("branches/branch1", new TreeSet<Long>() {
					{
						add(1l);
					}
				});
				commitEditor.changeDirProperty(SVNUtils.SVN_MERGEINFO_PROP, SVNUtils.createMergeInfoPropertyValue(trunk_folder1_mergedRevisions));
				commitEditor.closeDir();
			}
			{
				String folderName = "trunk/folder1/innerfolder1";
				commitEditor.addDir(folderName, null, -1);
				TreeMap<String, Set<Long>> trunk_folder1_innerfolder1_mergedRevisions = new TreeMap<>();
				trunk_folder1_innerfolder1_mergedRevisions.put("branches/branch1", new TreeSet<Long>() {
					{
						add(2l);
					}
				});
				commitEditor.changeDirProperty(SVNUtils.SVN_MERGEINFO_PROP, SVNUtils.createMergeInfoPropertyValue(trunk_folder1_innerfolder1_mergedRevisions));
				commitEditor.closeDir();
			}
			{
				String folderName = "trunk/folder2";
				commitEditor.addDir(folderName, null, -1);
				TreeMap<String, Set<Long>> trunk_folder1_innerfolder1_mergedRevisions = new TreeMap<>();
				trunk_folder1_innerfolder1_mergedRevisions.put("branches/branch2", new TreeSet<Long>() {
					{
						add(3l);
					}
				});
				commitEditor.changeDirProperty(SVNUtils.SVN_MERGEINFO_PROP, SVNUtils.createMergeInfoPropertyValue(trunk_folder1_innerfolder1_mergedRevisions));
				commitEditor.closeDir();
			}
			TreeMap<String, Set<Long>> trunk_folder1_mergedRevisions = new TreeMap<>();
			trunk_folder1_mergedRevisions.put("branches/branch3", new TreeSet<Long>() {
				{
					add(4l);
				}
			});
			commitEditor.changeDirProperty(SVNUtils.SVN_MERGEINFO_PROP, SVNUtils.createMergeInfoPropertyValue(trunk_folder1_mergedRevisions));
			commitEditor.closeDir();
		}
		commitEditor.closeDir();

		return commitEditor.closeEdit();
	}

}
