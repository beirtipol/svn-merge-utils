package com.beirtipol.svnmergeutils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.core.wc.SVNClientManager;

public class MissingMergeCheckerTest {
	private static final String	path	= MissingMergeCheckerTest.class.getClassLoader().getResource(".").getPath() + File.separator + "svnrepo";
	private static SVNURL		localRepoURL;

	@Before
	public void setUp() throws Exception {
		tearDown();//Just in case the test failed to tear down previously and we're left with a dodgy directory.
		SVNRepositoryFactoryImpl.setup();
		localRepoURL = SVNRepositoryFactory.createLocalRepository(new File(path), true, false);
	}

	@After
	public void tearDown() throws IOException {
		FileUtils.deleteDirectory(new File(path));
	}

	@Test
	public void testStandardMissingMerge() throws SVNException {
		SVNRepository repo = SVNClientManager.newInstance().createRepository(localRepoURL, true);
		createRepositoryBaseStructure(repo.getCommitEditor("Adding Base Structure", null));
		createFile(repo.getCommitEditor("Adding file to trunk", null), "trunk/file1.txt", "Some File Contents\nOn Multiple Lines");

		long revision = repo.getLatestRevision();
		copyDir(repo.getCommitEditor("Creating a branch", null), "trunk", "branches/branch1", revision);
		String missingMergeCommitMessage = "Changing the file on branch1";
		SVNCommitInfo missingMergeInfo = addLineToFile(repo.getCommitEditor(missingMergeCommitMessage, null), "branches/branch1/file1.txt", "Some More Stuff");

		SVNURL mergeTarget = localRepoURL.appendPath("trunk", false);
		SVNURL mergeSource = localRepoURL.appendPath("branches/branch1", false);

		MissingMergeWorker worker = new MissingMergeWorker(mergeSource, mergeTarget, false);
		List<SVNLogEntry> result = worker.getMissingMerges();

		Assert.assertEquals(1, result.size());
		Assert.assertEquals(missingMergeInfo.getAuthor(), result.get(0).getAuthor());
		Assert.assertEquals(missingMergeInfo.getNewRevision(), result.get(0).getRevision());
		Assert.assertEquals(missingMergeCommitMessage, result.get(0).getMessage());
	}

	@Test
	public void testMissingMergeIgnoredWithQuietTime() throws SVNException, InterruptedException {
		SVNRepository repo = SVNClientManager.newInstance().createRepository(localRepoURL, true);
		createRepositoryBaseStructure(repo.getCommitEditor("Adding Base Structure", null));
		createFile(repo.getCommitEditor("Adding file to trunk", null), "trunk/file1.txt", "Some File Contents\nOn Multiple Lines");

		long revision = repo.getLatestRevision();
		copyDir(repo.getCommitEditor("Creating a branch", null), "trunk", "branches/branch1", revision);
		String missingMergeCommitMessage = "Changing the file on branch1";
		SVNCommitInfo missingMergeInfo = addLineToFile(repo.getCommitEditor(missingMergeCommitMessage, null), "branches/branch1/file1.txt", "Some More Stuff");

		Thread.sleep(3000);
		addLineToFile(repo.getCommitEditor(missingMergeCommitMessage, null), "branches/branch1/file1.txt", "This just happened and should be ignored");
		long quietTime = 2000;
		SVNURL mergeTarget = localRepoURL.appendPath("trunk", false);
		SVNURL mergeSource = localRepoURL.appendPath("branches/branch1", false);

		MissingMergeWorker worker = new MissingMergeWorker(mergeSource, mergeTarget, false, null, new Predicate<SVNLogEntry>() {
			@Override
			public boolean test(SVNLogEntry entry) {
				return System.currentTimeMillis() - entry.getDate().getTime() > quietTime;
			}
		});
		List<SVNLogEntry> result = worker.getMissingMerges();

		Assert.assertEquals(1, result.size());
		Assert.assertEquals(missingMergeInfo.getAuthor(), result.get(0).getAuthor());
		Assert.assertEquals(missingMergeInfo.getNewRevision(), result.get(0).getRevision());
		Assert.assertEquals(missingMergeCommitMessage, result.get(0).getMessage());
	}

	private SVNCommitInfo addLineToFile(ISVNEditor commitEditor, String atPath, String fileContents) throws SVNException {
		commitEditor.openRoot(-1);
		commitEditor.openFile(atPath, -1);
		commitEditor.applyTextDelta(atPath, null);
		SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
		String checksum = deltaGenerator.sendDelta(atPath, new ByteArrayInputStream(fileContents.getBytes()), commitEditor, true);
		commitEditor.closeFile(atPath, checksum);
		commitEditor.closeDir();
		return commitEditor.closeEdit();
	}

	private SVNCommitInfo copyDir(ISVNEditor commitEditor, String sourcePath, String destPath, long revision) throws SVNException {
		commitEditor.openRoot(-1);

		commitEditor.addDir(destPath, sourcePath, revision);
		commitEditor.closeDir();

		commitEditor.closeDir();

		return commitEditor.closeEdit();
	}

	private SVNCommitInfo createFile(ISVNEditor commitEditor, String atPath, String fileContents) throws SVNException {
		commitEditor.openRoot(-1);
		commitEditor.addFile(atPath, null, -1);
		commitEditor.applyTextDelta(atPath, null);
		SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
		String checksum = deltaGenerator.sendDelta(atPath, new ByteArrayInputStream(fileContents.getBytes()), commitEditor, true);
		commitEditor.closeFile(atPath, checksum);
		commitEditor.closeDir();
		return commitEditor.closeEdit();
	}

	private SVNCommitInfo createRepositoryBaseStructure(ISVNEditor commitEditor) throws SVNException {
		commitEditor.openRoot(-1);

		commitEditor.addDir("trunk", null, -1);
		commitEditor.closeDir();

		commitEditor.addDir("branches", null, -1);
		commitEditor.closeDir();

		commitEditor.addDir("tags", null, -1);
		commitEditor.closeDir();

		commitEditor.closeDir();

		return commitEditor.closeEdit();
	}
}
