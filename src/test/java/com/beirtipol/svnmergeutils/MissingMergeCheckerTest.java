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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

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
		deleteWorkingDirectories();
		SVNRepositoryFactoryImpl.setup();
		localRepoURL = SVNRepositoryFactory.createLocalRepository(new File(path), true, false);
	}

	@After
	public void tearDown() throws IOException {
		deleteWorkingDirectories();
	}

	private void deleteWorkingDirectories() throws IOException {
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

	@SuppressWarnings("unchecked")
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

		MissingMergeWorker worker = new MissingMergeWorker(mergeSource, mergeTarget, false, null, new QuietTimeMergeCheckerPredicate(quietTime, false));
		List<SVNLogEntry> result = worker.getMissingMerges();

		Assert.assertEquals(1, result.size());
		Assert.assertEquals(missingMergeInfo.getAuthor(), result.get(0).getAuthor());
		Assert.assertEquals(missingMergeInfo.getNewRevision(), result.get(0).getRevision());
		Assert.assertEquals(missingMergeCommitMessage, result.get(0).getMessage());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMissingMergeIgnoredWithRegex() throws SVNException, InterruptedException {
		SVNRepository repo = SVNClientManager.newInstance().createRepository(localRepoURL, true);
		createRepositoryBaseStructure(repo.getCommitEditor("Adding Base Structure", null));
		createFile(repo.getCommitEditor("Adding file to trunk", null), "trunk/file1.txt", "Some File Contents\nOn Multiple Lines");

		long revision = repo.getLatestRevision();
		copyDir(repo.getCommitEditor("Creating a branch", null), "trunk", "branches/branch1", revision);
		String missingMergeCommitMessage = "Changing the file on branch1";
		SVNCommitInfo missingMergeInfo = addLineToFile(repo.getCommitEditor(missingMergeCommitMessage, null), "branches/branch1/file1.txt", "Some More Stuff");

		addLineToFile(repo.getCommitEditor("[maven-release-plugin] do a maven thing", null), "branches/branch1/file1.txt", "a maven thing");
		SVNURL mergeTarget = localRepoURL.appendPath("trunk", false);
		SVNURL mergeSource = localRepoURL.appendPath("branches/branch1", false);

		MissingMergeWorker worker = new MissingMergeWorker(mergeSource, mergeTarget, false, null, new IgnoreRegexMergeCheckerPredicate("\\[maven-release-plugin\\].*", false));
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
