package com.beirtipol.svnmergeutils;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;
import org.slf4j.Logger;

public abstract class AbstractWorker {

	@Option(name = "-v", usage = "Verbose logging")
	protected boolean	verbose		= false;

	@Option(name = "-h", usage = "Print help", help = true)
	protected boolean	printHelp	= false;

	protected abstract Logger getLogger();

	protected boolean handleArgs(String[] args) {
		ParserProperties pp = ParserProperties.defaults().withUsageWidth(80);
		CmdLineParser parser = new CmdLineParser(this, pp);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			getLogger().error(e.getMessage());
			printUsage(parser);
			return false;
		}
		if (printHelp) {
			printUsage(parser);
			return false;
		}
		return true;
	}

	private void printUsage(CmdLineParser parser) {
		getLogger().info("java " + getClass().getName() + " [options...] arguments...");
		parser.printUsage(System.out);
	}
}