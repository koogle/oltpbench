/*******************************************************************************
 * oltpbenchmark.com
 *  
 *  Project Info:  http://oltpbenchmark.com
 *  Project Members:  	Carlo Curino <carlo.curino@gmail.com>
 * 				Evan Jones <ej@evanjones.ca>
 * 				DIFALLAH Djellel Eddine <djelleleddine.difallah@unifr.ch>
 * 				Andy Pavlo <pavlo@cs.brown.edu>
 * 				CUDRE-MAUROUX Philippe <philippe.cudre-mauroux@unifr.ch>  
 *  				Yang Zhang <yaaang@gmail.com> 
 * 
 *  This library is free software; you can redistribute it and/or modify it under the terms
 *  of the GNU General Public License as published by the Free Software Foundation;
 *  either version 3.0 of the License, or (at your option) any later version.
 * 
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 ******************************************************************************/
package com.oltpbenchmark;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.log4j.Logger;

import com.oltpbenchmark.ThreadBench.Results;
import com.oltpbenchmark.api.BenchmarkModule;
import com.oltpbenchmark.api.TransactionType;
import com.oltpbenchmark.api.TransactionTypes;
import com.oltpbenchmark.api.Worker;
import com.oltpbenchmark.util.ClassUtil;
import com.oltpbenchmark.util.QueueLimitException;

public class DBWorkload {
    private static final Logger LOG = Logger.getLogger(DBWorkload.class);
    

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
	    // Initialize log4j
	    org.apache.log4j.PropertyConfigurator.configure(System.getProperty("log4j.configuration"));
		
		// create the command line parser
		CommandLineParser parser = new PosixParser();
		XMLConfiguration pluginConfig=null;
		try {
			pluginConfig = new XMLConfiguration("config/plugin.xml");
		} catch (ConfigurationException e1) {
			System.out.println("Plugin configuration file config/plugin.xml is missing");
			e1.printStackTrace();
		}
		pluginConfig.setExpressionEngine(new XPathExpressionEngine());
		Options options = new Options();
		options.addOption(
				"b",
				"bench",
				true,
				"[required] Benchmark class. Currently supported: "+ pluginConfig.getList("/plugin//@name"));
		options.addOption(
				"c", 
				"config", 
				true,
				"[required] Workload configuration file");
      options.addOption(
                null,
	            "create",
                true,
                "Initialize the database for this benchmark");
		options.addOption(
		        null,
		        "load",
		        true,
		        "Load data using the benchmark's data loader");
        options.addOption(
                null,
                "execute",
                true,
                "Execute the benchmark workload");
		
		options.addOption("v", "verbose", false, "Display Messages");
		options.addOption("h", "help", false, "Print this help");
		options.addOption("s", "sample", true, "Sampling window");
		options.addOption("o", "output", true, "Output file (default System.out)");		

		// parse the command line arguments
		CommandLine argsLine = parser.parse(options, args);
		if (argsLine.hasOption("h")) {
			printUsage(options);
			return;
		}

		// Load the Workload Configuration from the Config file
		WorkloadConfiguration wrkld = new WorkloadConfiguration();
		XMLConfiguration xmlConfig = null;
		int numTxnTypes = -1; 
		if (argsLine.hasOption("c")) {
			String configFile = argsLine.getOptionValue("c");
			xmlConfig = new XMLConfiguration(configFile);
			wrkld.setXmlConfig(xmlConfig);
			wrkld.setDBDriver(xmlConfig.getString("driver"));
			wrkld.setDBConnection(xmlConfig.getString("DBUrl"));
			wrkld.setDBName(xmlConfig.getString("DBName"));
			wrkld.setDBUsername(xmlConfig.getString("username"));
			wrkld.setDBPassword(xmlConfig.getString("password"));
			wrkld.setTerminals(xmlConfig.getInt("terminals"));			
			
			int size = xmlConfig.configurationsAt("works.work").size();
			for (int i = 0; i < size; i++){
			
				if((int) xmlConfig.getInt("works.work(" + i + ").rate")<1)
					throw new Exception("You cannot use less than 1 TPS in a Phase of your expeirment");

				wrkld.addWork(
						xmlConfig.getInt("works.work(" + i + ").time"),
						xmlConfig.getInt("works.work(" + i + ").rate"),
						xmlConfig.getList("works.work(" + i + ").weights"));
			}
			
			numTxnTypes = xmlConfig.configurationsAt("transactiontypes.transactiontype").size();
			//CHECKING INPUT PHASES
			int j =0;
			for(Phase p:wrkld.getAllPhases()){
				j++;
				if(p.weights.size()!=numTxnTypes){
					LOG.error("Configuration files is inconsistent, phase " + j + " contains " +p.weights.size() + " weights while you defined "+ numTxnTypes + " transaction types");
					System.exit(-1);
				}
			}		
			wrkld.init();
		} else {
		    LOG.error("Missing Configuration file");
		    printUsage(options);
            return;
		}
		assert(numTxnTypes >= 0);
		assert(xmlConfig != null);

		// Load the Benchmark Implementation
		BenchmarkModule bench = null;
        if (argsLine.hasOption("b")) {
            String plugin = argsLine.getOptionValue("b");
            String classname=pluginConfig.getString("/plugin[@name='"+plugin+"']");
            LOG.info("Loading BenchmarkModule " + classname);
            if(classname==null)
                    throw new ParseException("Plugin "+ plugin + " is undefined in config/plugin.xml");
            bench = ClassUtil.newInstance(classname,
                                          new Object[]{ wrkld },
                                          new Class<?>[]{ WorkloadConfiguration.class });
            assert(bench != null);
        }
        else {
            LOG.error("Missing Benchmark Class to load");
            printUsage(options);
            return;
        }

        // Load TransactionTypes
        List<TransactionType> ttypes = new ArrayList<TransactionType>();
        
        // Always add an INVALID type for Carlo
        ttypes.add(TransactionType.INVALID);
		for (int i = 0; i < numTxnTypes; i++) {
		    String key = "transactiontypes.transactiontype(" + i + ")";
		    String txnName = xmlConfig.getString(key + ".name");
		    int txnId = i+1;
		    if (xmlConfig.containsKey(key + ".id")) {
		        txnId = xmlConfig.getInt(key + ".id");
		    }
		    ttypes.add(bench.getTransactionType(txnName, txnId));
		} // FOR
		TransactionTypes tt = new TransactionTypes(ttypes);
		wrkld.setTransTypes(tt);
		LOG.debug("Using the following transaction types: " +tt);
			
		@Deprecated
		boolean verbose = argsLine.hasOption("v");
		
		// Create the Benchmark's Database
        if (argsLine.hasOption("create") && Boolean.parseBoolean(argsLine.getOptionValue("create"))) {
            runCreator(bench, verbose);
        }
		
		// Execute Loader
        if (argsLine.hasOption("load") && Boolean.parseBoolean(argsLine.getOptionValue("load"))) {
		    runLoader(bench, verbose);
		}
		
		// Execute Workload
        if (argsLine.hasOption("execute") && Boolean.parseBoolean(argsLine.getOptionValue("execute"))) {
    		// Bombs away!
            Results r = runWorkload(bench, verbose);
            PrintStream ps = System.out;
            if (argsLine.hasOption("o"))
                ps = new PrintStream(new File(argsLine.getOptionValue("o")));
            if (argsLine.hasOption("s")) {
                int windowSize = Integer.parseInt(argsLine.getOptionValue("s"));
                r.writeCSV(windowSize, ps);
            } else
                r.writeAllCSVAbsoluteTiming(ps);
            ps.close();
	    }
	}
	
	private static void runCreator(BenchmarkModule bench, boolean verbose) {
        LOG.info(String.format("Creating %s Database", bench.toString()));
        bench.createDatabase();
    }
	
	private static void runLoader(BenchmarkModule bench, boolean verbose) {
	    LOG.info(String.format("Loading %s Database", bench));
	    bench.loadDatabase();
	}
	
	private static Results runWorkload(BenchmarkModule bench, boolean verbose) throws QueueLimitException, IOException {
		List<Worker> workers = bench.makeWorkers(verbose);
		LOG.info(String.format("Launching the %s Benchmark with %s Phases...",
		                       bench.getBenchmarkName(), bench.getWorkloadConfiguration().size()));
		ThreadBench.setWorkConf(bench.getWorkloadConfiguration());
		ThreadBench.Results r = ThreadBench.runRateLimitedBenchmark(workers);
		System.out.println("Rate limited reqs/s: " + r);
		return r;
	}

	private static void printUsage(Options options) {
		HelpFormatter hlpfrmt = new HelpFormatter();
		hlpfrmt.printHelp("dbworkload", options);
	}
}
