package com.beirtipol.svnmergeutils;

import java.io.ByteArrayInputStream;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;

public class SVNTestHelper {
	public static SVNCommitInfo addDir(ISVNEditor editor, String dirPath, String filePath, byte[] data) throws SVNException {
		editor.openRoot(-1);
		editor.addDir(dirPath, null, -1);
		editor.addFile(filePath, null, -1);
		editor.applyTextDelta(filePath, null);

		SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
		String checksum = deltaGenerator.sendDelta(filePath, new ByteArrayInputStream(data), editor, true);

		editor.closeFile(filePath, checksum);

		editor.closeDir();
		editor.closeDir();

		return editor.closeEdit();
	}

	private static SVNCommitInfo copyDir(ISVNEditor editor, String srcDirPath, String dstDirPath, long revision) throws SVNException {
		editor.openRoot(-1);

		editor.addDir(dstDirPath, srcDirPath, revision);

		editor.closeDir();
		editor.closeDir();

		return editor.closeEdit();
	}
}
