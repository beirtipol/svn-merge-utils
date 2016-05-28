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

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.xml.SVNXMLSerializer;
import org.xml.sax.SAXException;

/**
 * 
 * @author beirtipol@gmail.com
 *
 */
public class MissingMergeChecker extends AbstractWorker {
	private static final Logger	LOGGER		= LoggerFactory.getLogger(MissingMergeChecker.class);

	@Option(name = "--user", usage = "SVN Username")
	protected String			user;

	@Option(name = "--pass", usage = "SVN Password")
	protected String			pass;

	@Option(name = "--baseUrl", usage = "Common base url of 'from' and 'to'", required = true)
	protected String			baseUrl;

	@Option(name = "--mergeSources", usage = "Semicolon-delimited list of merge source paths, relative to the baseUrl. This must have the same number of paths as 'toPaths'", required = true)
	protected String			mergeSources;
	protected String[]			mergeSourceArray;

	@Option(name = "--mergeTargets", usage = "Semicolon-delimited list of merge target paths, relative to the baseUrl. This must have the same number of paths as 'fromPaths'", required = true)
	protected String			mergeTargets;
	protected String[]			mergeTargetArray;

	@Option(name = "--ignoreRegex", usage = "Regular expression. If the svn log entry comment matches this regex, it will not be reported.")
	private String				ignoreRegex;

	@Option(name = "--outputFile", usage = "File to write missing merge revision information. Written as xml.", required = true)
	private File				outputFile;

	@Option(name = "--usersOutputFile", usage = "File to write list of users who are present in the missing merge report. If not specified, will write to standard out")
	private File				usersOutputFile;

	@Option(name = "--outputAsHTML", usage = "Transform the output file xml to human-readable HTML.")
	private boolean				outputAsHTML;

	@Option(name = "--quietTime", usage = "Number of seconds prior to execution to ignore commits. This allows people time to merge their changes, and will help prevent unnecessary noise.")
	private Integer				quietTime	= 0;

	public static void main(String[] args) throws Exception {
		new MissingMergeChecker().doMain(args);
	}

	protected boolean handleArgs(String[] args) {
		super.handleArgs(args);

		mergeSourceArray = mergeSources.split(";");
		mergeTargetArray = mergeTargets.split(";");

		if (mergeSourceArray.length != mergeTargetArray.length) {
			getLogger().error("The number of 'fromPaths' must match the number of 'toPaths'.");
			return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private void doMain(String[] args) {
		if (!handleArgs(args)) {
			return;
		}

		SVNClientManager clientManager = createClientManager();
		List<Predicate<SVNLogEntry>> logEntryValidators = getValidators();
		OutputStream out = null;

		try {
			boolean startedSerializing = false;
			out = new FileOutputStream(outputFile);
			SVNXMLSerializer xmlSerializer = new SVNXMLSerializer(out);
			for (int i = 0; i < mergeSourceArray.length; i++) {
				String mergeTargetPath = mergeTargetArray[i];
				String mergeSourcePath = mergeSourceArray[i];
				SVNURL baseSVNURL = SVNURL.parseURIEncoded(baseUrl);
				SVNURL mergeTarget = baseSVNURL.appendPath(mergeTargetPath, false);
				SVNURL mergeSource = baseSVNURL.appendPath(mergeSourcePath, false);

				MissingMergeWorker worker = new MissingMergeWorker(mergeSource, mergeTarget, verbose, clientManager, logEntryValidators.toArray(new Predicate[0]));
				List<SVNLogEntry> missingMerges = worker.getMissingMerges();
				BranchAwareXMLLogHandler handler = new BranchAwareXMLLogHandler(xmlSerializer, mergeTargetPath, mergeSourcePath, startedSerializing);
				for (SVNLogEntry entry : missingMerges) {
					handler.handleLogEntry(entry);
				}
				startedSerializing = handler.started();
			}
			if (startedSerializing) {
				xmlSerializer.endDocument();
				xmlSerializer.flush();
			}

			if (outputAsHTML) {
				File outputHTMLFile = new File(outputFile.getAbsolutePath() + ".html");
				if (startedSerializing) {
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

		} catch (SVNException e) {
			LOGGER.error("Error checking merge information", e);
		} catch (SAXException | IOException e) {
			LOGGER.error("Error writing merge information to file", e);
		} catch (TransformerException e) {
			LOGGER.error("Error transforming merge output xml to html", e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}

	}

	private SVNClientManager createClientManager() {
		SVNClientManager clientManager = SVNClientManager.newInstance();
		if (user != null && pass != null) {
			clientManager.setAuthenticationManager(BasicAuthenticationManager.newInstance(user, pass.toCharArray()));
		}
		return clientManager;
	}

	private List<Predicate<SVNLogEntry>> getValidators() {
		List<Predicate<SVNLogEntry>> logEntryValidators = new ArrayList<>();
		if (StringUtils.isNotBlank(ignoreRegex)) {
			logEntryValidators.add(new IgnoreRegexMergeCheckerPredicate(ignoreRegex, verbose));
		}
		if (quietTime > 0) {
			logEntryValidators.add(new QuietTimeMergeCheckerPredicate(quietTime * 1000, verbose));
		}
		return logEntryValidators;
	}

	@Override
	protected Logger getLogger() {
		return LOGGER;
	}
}
