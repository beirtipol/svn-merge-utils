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

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;
import org.slf4j.Logger;

/**
 * 
 * @author beirtipol@gmail.com
 *
 */
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