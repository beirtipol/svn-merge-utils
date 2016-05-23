package com.beirtipol.svnmergeutils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.xml.SVNXMLLogHandler;
import org.tmatesoft.svn.core.wc.xml.SVNXMLSerializer;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnLogMergeInfo;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

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

	public void getMissingMerges(String user, String pass, String ignoreRegex, File outputFile, String[] mergeSourceArray, String[] mergeTargetArray, String baseUrl, int quietTime, boolean verbose, boolean outputAsHTML) {

		SVNClientManager clientManager = SVNClientManager.newInstance();
		if (user != null && pass != null) {
			clientManager.setAuthenticationManager(BasicAuthenticationManager.newInstance(user, pass.toCharArray()));
		}

		Pattern ignoreRegexPattern = null;
		if (StringUtils.isNotBlank(ignoreRegex)) {
			ignoreRegexPattern = Pattern.compile(ignoreRegex);
		}

		OutputStream out = null;
		try {
			out = new FileOutputStream(outputFile);
			SVNXMLSerializer xmlSerializer = new SVNXMLSerializer(out);
			final long now = System.currentTimeMillis();
			final long quietTimeMillis = quietTime * 1000l * 60l;

			boolean started = false;

			for (int i = 0; i < mergeSourceArray.length; i++) {
				String mergeTargetPath = mergeTargetArray[i];
				String mergeSourcePath = mergeSourceArray[i];
				TrackableXMLLogHandler thisBranchHandler = new TrackableXMLLogHandler(xmlSerializer, mergeSourcePath, mergeTargetPath, started);
				List<SVNLogEntry> missingMerges = getMissingMerges(baseUrl, quietTime, verbose, clientManager, ignoreRegexPattern, now, quietTimeMillis, mergeSourcePath, mergeTargetPath);
				started |= missingMerges.size() > 0;
				LOGGER.info("{} revisions have not been merged from '{}' to '{}'.", missingMerges.size(), mergeSourcePath, mergeTargetPath);

				if (!started && missingMerges.size() > 0) {
					started = true;
					thisBranchHandler.startDocument();
				}
			}

			if (started) {
				xmlSerializer.endDocument();
			}
			xmlSerializer.flush();

			if (outputAsHTML && outputFile != null) {
				File outputHTMLFile = new File(outputFile.getAbsolutePath() + ".html");
				if (started) {
					Result xmlOutput = new StreamResult(outputHTMLFile);

					Source xmlInput = new StreamSource(outputFile);
					InputStream is = getClass().getClassLoader().getResourceAsStream("svnlog.xsl");
					Source xsl = new StreamSource(is);
					xsl.setSystemId("A System ID");

					TransformerFactory factory = TransformerFactory.newInstance();
					Transformer transformer = factory.newTransformer(xsl);
					transformer.setParameter("baseUrl", baseUrl);

					transformer.transform(xmlInput, xmlOutput);
				} else {
					FileWriter fw = new FileWriter(outputHTMLFile);
					fw.write("<html><body><center><h3>");
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					fw.write(String.format("Missing Merge Report - %s", sdf.format(new Date())));
					fw.write("</h3>");
					fw.write("<h4>No Missing Merges</h4></center></body></html>");
					fw.close();
				}
			}

		} catch (FileNotFoundException e) {
			LOGGER.error("Could not open file '{}' for reading.", outputFile.getAbsolutePath(), e);
		} catch (SVNException e) {
			LOGGER.error("SVN error encountered", e);
		} catch (IOException e) {
			LOGGER.error("Error writing output", e);
		} catch (TransformerException e) {
			LOGGER.error("Error transforming xml to html", e);
		} catch (SAXException e) {
			LOGGER.error("Error handling log xml serialization", e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}

	}

	private List<SVNLogEntry> getMissingMerges(String baseUrl, int quietTime, boolean verbose, SVNClientManager clientManager, final Pattern finalIgnorePattern, final long now, final long quietTimeMillis, String mergeSourcePath, String mergeTargetPath) throws SVNException {
		List<SVNLogEntry> result = new ArrayList<>();

		SVNURL baseSVNURL = SVNURL.parseURIEncoded(baseUrl);
		SVNURL mergeTarget = baseSVNURL.appendPath(mergeTargetPath, false);
		SVNURL mergeSource = baseSVNURL.appendPath(mergeSourcePath, false);

		SvnLogMergeInfo mergeInfo = clientManager.getDiffClient().getOperationsFactory().createLogMergeInfo();
		mergeInfo.addTarget(SvnTarget.fromURL(mergeTarget, SVNRevision.HEAD));
		mergeInfo.setSource(SvnTarget.fromURL(mergeSource, SVNRevision.HEAD));
		mergeInfo.setDiscoverChangedPaths(true);
		mergeInfo.setRevisionProperties(null);
		mergeInfo.setFindMerged(false);
		mergeInfo.setDepth(SVNDepth.INFINITY);
		mergeInfo.setReceiver(new ISvnObjectReceiver<SVNLogEntry>() {

			public void receive(SvnTarget target, SVNLogEntry logEntry) throws SVNException {
				if (quietTime != 0) {
					long difference = now - logEntry.getDate().getTime();
					if (difference < quietTimeMillis) {
						if (verbose) {
							LOGGER.info("Ignoring revision {} as it was committed in the past {} minutes", logEntry.getRevision(), quietTime);
						}
						return;
					}
				}
				if (finalIgnorePattern != null) {
					if (finalIgnorePattern.matcher(logEntry.getMessage()).matches()) {
						if (verbose) {
							LOGGER.info("Ignoring revision {} as the regular expression matches it.", logEntry.getRevision());
						}
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

	/**
	 * Extending the base SVNXMLLogHandler allows us to include multiple merge source and targets in the same output
	 * file. This is useful for teams wishing to track multiple branches at the same time.
	 * 
	 * @author Beirti
	 *
	 */
	private class TrackableXMLLogHandler extends SVNXMLLogHandler {

		private String	mergeTargetPath;
		private String	mergeSourcePath;
		private boolean	started;
		private int		numberOfRevisions	= 0;

		public TrackableXMLLogHandler(ContentHandler contentHandler, String mergeTargetPath, String mergeSourcePath, boolean started) {
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
}
