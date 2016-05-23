package com.beirtipol.svnmergeutils;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;

public abstract class AbstractMergeWorker extends AbstractWorker {
	@Option(name = "--user", usage = "SVN Username")
	protected String	user;

	@Option(name = "--pass", usage = "SVN Password")
	protected String	pass;

	@Option(name = "--baseUrl", usage = "Common base url of 'from' and 'to'", required = true)
	protected String	baseUrl;

	@Option(name = "--mergeSources", usage = "Semicolon-delimited list of merge source paths, relative to the baseUrl. This must have the same number of paths as 'toPaths'", required = true)
	protected String	mergeSources;
	protected String[]	mergeSourceArray;

	@Option(name = "--mergeTargets", usage = "Semicolon-delimited list of merge target paths, relative to the baseUrl. This must have the same number of paths as 'fromPaths'", required = true)
	protected String	mergeTargets;
	protected String[]	mergeTargetArray;

	protected abstract Logger getLogger();

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
}