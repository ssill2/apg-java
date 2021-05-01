package apg;
// GeneratorGrammar header javadoc-documentation - just in case it gets overwritten on re-generation
/*
 * Sample of the Grammar class generated by APG.
 * <p>
 * The Generator is a Java APG Parser, just like the Parser's it generates.
 * The working version of GeneratorGrammar was generated from a bootstrap program
 * built with <a href="http://www.coasttocoastresearch.com/apg target="_blank">APG version 6.1</a>.
 * This file, however, has been regenerated with the Generator itself.
 * That is, the Generator can now re-generate GeneratorGrammar.java. 
 * <p>
 * This file is an extension of the Grammar class which 
 * contains a number of members, member functions
 * and enums not contained in the base class. 
 */
import java.io.File;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import apg.GeneratorCommandLine.Params;
import apg.GeneratorSyntax.*;
import apg.Parser.*;
import apg.Utilities.*;
/**
 * The Generator class has a static <code>main()</code> function which is APG, 
 * the ABNF Parser Generator.
 */
public class Generator {
	static Vector<String> 	errors = new Vector<String>();
	static Vector<String> 	warnings = new Vector<String>();
	static String 			fileName = null;
	static String 			workingDir = null;
	static String 			logFile = null;
	static String 			grammarDefinitionFile = null;
	static String 			grammarPackageName = null;
	static Grammar          grammar = null;
	static Parser 			parser = null;
	static Trace 			trace = null;
	static boolean 			displayTrace = false;
	static Statistics 		stats = null;
	private Generator(){}
	
	/**
	 * The <code>main()</code> function <i>is</i> APG, the ABNF Parser Generator. 
	 * Its command line arguments have two forms, flags and parameters.
	 * <p>
	 * Flags are unary names indicating simple true/false values.
	 * They are false by default. If they appear on the command line they are set to true.
	 * All flags are optional.
	 * </p>
	 * Parameters are binary with names and values. They are all given default values that
	 * may be changed by identifying them on the command line with name/value pairs. The input
	 * grammar definition file (/in=) is the only required parameter. All other parameters are optional.
	 * <p>
	 * All flags and parameters are defined below:
	 * </p>
<pre>
flags
/da        attributes, display grammar attributes
/de        errors, display parsing errors
/dg        grammar, display grammar file with line numbers and character indexes
?          help, display this help screen
/dm        metrics, display grammar metrics (opcode counts)
/na        no attributes, omit grammar attribute discovery
/dopcodes  opcodes, display opcodes in human readable format (may generate large amount of output)
/dstate    state, display the state of the grammar parser
/dstats    statistics, display the grammar parsing statistics
/doptions  values, display all option values
/dv        verbose, display all (except opcodes)
/dw        warnings, display parsing warnings

parameters
/java=     if not null, generate a Grammar class at value.java (default: null)
/javadoc=  if not null, generate a Grammar class with Javadoc documentation at value.java (default: null)
/in=       (required)the SABNF grammar definition file (default: null)
           Files from multiple /in= parameters will be concatenated.
/log=      name of log file, if null print log file to console (default: null)
/package=  package name for the generated grammar (default: package.name)
/dir=      working directory for input and output files (default: ./)

All flags are false by default. Specifying a flag sets it to true.
Multiple occurrences of a given flag are allowed but redundant. Resulting flag is true.
Parameters are of the form parameter value. eg. /in=value.
Parameter values may not be empty.
Parameter values containing spaces must be quoted.
Multiple occurrences of a given parameter are allowed.
Files from multiple /in= parameters will be concatenated.
For all other parameters, only the last value will be used.
All flags and parameter names and values are case sensitive.
The help flag may be ? or /help.
</pre>
	 * @param args The command line arguments that provision APG.
	 */
	public static void main(String[] args) {
		try{
			// parse the command line options
			GeneratorCommandLine cl = new GeneratorCommandLine(args);
			
			// version request 
			if(cl.flagValues[GeneratorCommandLine.Flags.VERSION.ordinal()]){
				displayVersion(System.out);
				return;
			}

			String inputParam = cl.paramValues[GeneratorCommandLine.Params.INPUT.ordinal()];
			if(inputParam == null || 
					inputParam.equals("")){
				throw new Exception("required input file parameter, "+Params.INPUT.prefix()+"filename, is missing");
			}
			// working directory
			workingDir = cl.paramValues[GeneratorCommandLine.Params.WORKING_DIR.ordinal()];
			if(workingDir == null || workingDir == ""){
				workingDir = null;
			} else{
				File file = new File(workingDir);
				if(!file.exists()){throw new AssertionError("requested working directory ("+workingDir+") does not exist");}
			}
			
			// log file
			fileName = cl.paramValues[GeneratorCommandLine.Params.LOGFILE.ordinal()];
			if(fileName == null || fileName == ""){
				System.out.println("console: using console screen");
			} else{
				File file = getFile(workingDir, fileName);
				if(file == null){throw new AssertionError("unable to open log file ("+fileName+")");}
				file.createNewFile();
				PrintStream out = new PrintStream(file);
				System.out.println("console: using log file: "+ file.getCanonicalPath());
				System.setOut(out);
			}
			
			// the grammar package name
			grammarPackageName = cl.paramValues[GeneratorCommandLine.Params.PACKAGE.ordinal()];
			
			// the input string
			fileName = cl.paramValues[GeneratorCommandLine.Params.INPUT.ordinal()];
			if((fileName == null || fileName == "")){
				throw new AssertionError("required input file name missing");
			} else{
				// RHA: check all input files
				for(int i=0; i<cl.inputFiles.size(); i++) {
					fileName = cl.inputFiles.get(i);
					File file = getFile(workingDir, fileName);
					if(file == null){throw new AssertionError("unable to open input file ("+fileName+")");}
					fileName = file.getCanonicalPath();
					if(!file.exists()){throw new AssertionError(
							"requested input grammar definition file ("+fileName+") does not exist");}
					cl.inputFiles.set(i,fileName);
				}
			}
			
			if(cl.flagValues[GeneratorCommandLine.Flags.VALUES.ordinal()]){
				// display all command line flags and parameters
				cl.displayOptions();
			}
			
			// convert input string to a char array
			// RHA: process all input files
			// TODO: include file name in error messages, probably via LineCatalog
			grammarDefinitionFile = "";
			for(String inputFile : cl.inputFiles){
				String inputFileContent = Utilities.getFileAsString(inputFile);
				if(inputFileContent == null){throw new AssertionError("can't read input grammar definition file ("+inputFile+")");}
				grammarDefinitionFile += inputFileContent;
			}
			
			// catalog the line numbers for error & information reporting
			LineCatalog catalog = new LineCatalog(grammarDefinitionFile);
			System.out.println("");
			System.out.println(catalog.toString());
			if(catalog.getWarningCount() > 0){
				System.out.println("catalog warnings:");
				catalog.displayWarnings(System.out);
			}
			if(catalog.getErrorCount() > 0){
				System.out.println("catalog errors:");
				catalog.displayErrors(System.out);
			}

			// set up the parser
			grammar = GeneratorGrammar.getInstance();
			parser = new Parser(grammar);
			if(displayTrace){
				trace = parser.enableTrace(true);
				setTraceOptions(trace);
			}
			stats = parser.enableStatistics(true);
			parser.setInputString(grammarDefinitionFile.toCharArray());
			parser.setStartRule(GeneratorGrammar.RuleNames.FILE.ruleID());
			
			// the callback function for the selected AST nodes
			parser.setRuleCallback(GeneratorGrammar.RuleNames.FILE.ruleID(), new GenFile(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.RULE.ruleID(), new GenRule(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.NAMEDEF.ruleID(), new NameDef(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.INCALT.ruleID(), new IncAlt(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.PROSVAL.ruleID(), new ProsVal(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.ALTERNATION.ruleID(), new Alternation(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.CONCATENATION.ruleID(), new Concatenation(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.REPETITION.ruleID(), new Repetition(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.REP.ruleID(), new Rep(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.REP_MIN.ruleID(), new RepMin(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.REP_MAX.ruleID(), new RepMax(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.REP_MIN_MAX.ruleID(), new RepMinMax(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.PREDICATE.ruleID(), new Predicate(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.ANDOP.ruleID(), new And(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.NOTOP.ruleID(), new Not(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.OPTION.ruleID(), new Option(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.RNMOP.ruleID(), new Rnm(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.UDTOP.ruleID(), new Udt(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.TLSOP.ruleID(), new Tls(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.TCSOP.ruleID(), new Tcs(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.TRGOP.ruleID(), new Trg(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.TBSOP.ruleID(), new Tbs(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.DNUM.ruleID(), new DNum(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.XNUM.ruleID(), new XNum(parser));
			parser.setRuleCallback(GeneratorGrammar.RuleNames.BNUM.ruleID(), new BNum(parser));
			
			// translate the input SABNF grammar
			GeneratorSyntax.initSyntax(errors, warnings, catalog);
			if(errors.size() > 0){throw new AssertionError("pre-parsing errors detected");}
			Result result = parser.parse();
			result.displayResult(System.out);
			System.out.println(result.toString());
			if(errors.size() > 0){throw new AssertionError("parsing errors detected");}
			if(warnings.size() > 0){
				System.out.println("catalog warnings:");
				for(String error : warnings){System.out.println(error);}
			}
			
			// prune the redundant opcodes (ALT(1), CAT(1), REP(1,1))
			boolean test = GeneratorSyntax.prune();
			if(!test){throw new AssertionError("error pruning intermediate opcodes");}
			
			
			// display opcodes for each rule
			if(cl.flagValues[GeneratorCommandLine.Flags.OPCODES.ordinal()]){
				System.out.println("pruned rules: "+GeneratorSyntax.rules.size());
				System.out.println("pruned opcodes: "+GeneratorSyntax.opcodes.size());
				System.out.println("SyntaxRules\n");
				for(SyntaxRule rule:GeneratorSyntax.rules){
					System.out.println(rule.toString());
					for(int i = rule.opcodeOffset; i < (rule.opcodeOffset + rule.opcodeCount); i++){
						System.out.print(GeneratorSyntax.opcodes.elementAt(i).toString());
					}
					System.out.println();
				}
			}
			
			// stats
			if(cl.flagValues[GeneratorCommandLine.Flags.STATISTICS.ordinal()]){
				stats.displayStats(System.out, "operators");
				if(grammar.getUdtCount() > 0){
					stats.displayStats(System.out, "udts", false);
				}
				stats.displayStats(System.out, "rules", false);
			}
			
			// display findings
			
			// 9. metrics
			if(cl.flagValues[GeneratorCommandLine.Flags.METRICS.ordinal()]){
				SyntaxMetrics mets = GeneratorSyntax.metrics(GeneratorSyntax.rules,
						GeneratorSyntax.udts, GeneratorSyntax.opcodes);
				System.out.println(mets.toString());
			}
			
			// 10. attributes
			if(cl.flagValues[GeneratorCommandLine.Flags.ATTRIBUTES.ordinal()]){
				GeneratorAttributes attrs = new GeneratorAttributes(errors, warnings,
						GeneratorSyntax.ruleMap, GeneratorSyntax.rules, GeneratorSyntax.opcodes);
				attrs.attributes();
				System.out.println(attrs.toString());
			}
			
			if(errors.size() > 0){
				throw new AssertionError("errors detected");
			}
			
			String outputFile;
			boolean javadoc = false;
			outputFile = cl.paramValues[GeneratorCommandLine.Params.JAVA.ordinal()];
			if(outputFile == null){
				outputFile = cl.paramValues[GeneratorCommandLine.Params.JAVADOC.ordinal()];
				javadoc = true;
			}
			if(outputFile != null){
				// 11. Java output the grammar
				fileName = outputFile + ".java";
				PrintStream lookOut = new PrintStream(workingDir + fileName);
				generateJava(
						javadoc,
						lookOut,
						outputFile,
						grammarPackageName,
						grammarDefinitionFile,
						GeneratorSyntax.ruleMap,
						GeneratorSyntax.rules,
						GeneratorSyntax.udtMap,
						GeneratorSyntax.udts,
						GeneratorSyntax.opcodes);
			}

			outputFile = cl.paramValues[GeneratorCommandLine.Params.JAVASCRIPT.ordinal()];
			if(outputFile != null){
				// 12. JavaScript output the grammar
				System.out.println("JavaScript output TBD");
			}

			outputFile = cl.paramValues[GeneratorCommandLine.Params.C.ordinal()];
			if(outputFile != null){
				// 13. C output the grammar
				System.out.println("C output TBD");
			}

			outputFile = cl.paramValues[GeneratorCommandLine.Params.CPP.ordinal()];
			if(outputFile != null){
				// 14. C++ output the grammar
				System.out.println("C++ output TBD");
			}
			
			if(errors.size() > 0){
				System.out.println("*** errors ***");
				for(String error : errors){System.out.println(error);}
			}
			if(warnings.size() > 0){
				System.out.println("*** warnings ***");
				for(String error : warnings){System.out.println(error);}
			}
			
			// success
		} catch(Exception e){
			if(errors.size() > 0){
				System.out.println("*** java.lang.Exception caught - errors ***");
				for(String error : errors){System.out.println(error);}
			}
			if(warnings.size() > 0){
				System.out.println("*** java.lang.Exception caught - warnings ***");
				for(String error : warnings){System.out.println(error);}
			}
			System.out.println(Utilities.displayException(e));
		}	catch(Error e){
			if(errors.size() > 0){
				System.out.println("*** java.lang.Error caught - errors ***");
				for(String error : errors){System.out.println(error);}
			}
			if(warnings.size() > 0){
				System.out.println("*** java.lang.Error caught - warnings ***");
				for(String error : warnings){System.out.println(error);}
			}
			System.out.println(Utilities.displayError(e));
		}
	}
	
	private static void displayVersion(PrintStream out){
		out.println("Java APG Version 1.1");
		out.println("Copyright (c) 2021 Lowell D. Thomas, all rights reserved");
		out.println("2-Clause BSD License");
	}
	private static void setTraceOptions(Trace trace){trace.enableAllNodes(false);}

	private static File getFile(String dir, String filename){
		File ret = null;
		while(true){
			if(filename == null || filename == ""){break;}
			File test = new File(filename);
			if(filename == test.getAbsolutePath()){
				// file name is absolute - use it
				ret = new File(filename);
				break;
			}
			
			if(dir == null || dir == ""){
				// dir is empty - use just the file name
				ret = new File(filename);
				break;
			}

			// use dir + file name
			ret = new File(dir, filename);
			break;
		}
		return ret;
	}
	
	private static String toUpperUnderscore(String string){
		StringBuffer buf = new StringBuffer();
		String ret = null;
		while(true){
			if(string == null || string.length() == 0){break;}
			String upper = string.toUpperCase();
			for(char c:upper.toCharArray()){
				if(c == 32){c = '_';}
				else if(c == 45){c = '_';}
				buf.append(c);
			}
			ret = buf.toString();
			break;
		}
		return ret;
	}

	private static class JavadocStrings{
		/*
/** This class has been generated automatically from an SABNF grammar by
 * Java APG, the {@link apg.Generator} class.<br>
 * It is an extension of the {@link apg.Grammar} class containing additional members and enums not found
 * in the base class.<br>
 * The function {@link #getInstance()} will return a reference to a static, singleton instance of the class.
 * <p>Licensed under the <a href=\"https://opensource.org/licenses/BSD-2-Clause\">2-Clause BSD License</a>.</p>
 * Copyright (c) 2021 Lowell D. Thomas, all rights reserved.
 */
		String header = "/** This class has been generated automatically from an SABNF grammar by\n" +
				 " * the {@link apg.Generator} class of Java APG, Version 1.1.<br>\n"+
				 " * It is an extension of the {@link apg.Grammar}\n" +
				 " * class containing additional members and enums not found\n"+
				 " * in the base class.<br>\n"+
				 " * The function {@link #getInstance()} will return a reference to a static,\n" +
				 " * singleton instance of the class.\n"+
				" * <br>Copyright (c) 2021 Lowell D. Thomas, all rights reserved<br>\n" +
				" * <a href=\"https://opensource.org/licenses/BSD-2-Clause\">2-Clause BSD License</a>\n" +
				" */";
		String singleton = "    /** Called to get a singleton instance of this class.\n" +
				"     * @return a singleton instance of this class, cast as the base class, Grammar. */";
		String numRules = "    /** The number of rules in the grammar */";
		String numUdts = "    /** The number of UDTs in the grammar */";
		String getEnumDoc(String type){
			return "    /** This enum provides easy to remember enum constants for locating the "+type+" identifiers and names.\n" +
				"     * The enum constants have the same spelling as the "+type+" names rendered in all caps with underscores replacing hyphens. */";
		}
		String getEnumConstant(int id, String name){
			return "/** id = <code>"+id+"</code>, name = <code>\""+name+"\"</code> */";
		}
		String display = "    /** Displays the original SABNF grammar on the output device.\n" +
				"     * @param out the output device to display on.*/";
		String ruleName = "        /** Associates the enum with the original grammar name of the rule it represents.\n"+
				"        * @return the original grammar rule name. */";
		String ruleId = "        /** Associates the enum with an identifier for the grammar rule it represents.\n"+
				"        * @return the rule name identifier. */";
		String udtName = "        /** Associates the enum with the original grammar name of the UDT it represents.\n"+
				"        * @return the original grammar UDT name. */";
		String udtId = "        /** Associates the enum with an identifier for the UDT it represents.\n"+
				"        * @return the UDT identifier. */";
		String udtEmpty = "        /** Associates the enum with the \"empty\" attribute of the UDT it represents.\n"+
				"        * @return the \"empty\" attribute.\n" +
				"        * True if the UDT name begins with <code>\"e_\"</code>, false if it begins with <code>\"u_\"</code>. */";
	}

	private static void generateJava(
			boolean javadoc,
			PrintStream out,
			String name,
			String packageName,
			String grammar,
			TreeMap<String, Integer> ruleMap,
			Vector<SyntaxRule> rules,
			TreeMap<String, Integer> udtMap,
			Vector<SyntaxRule> udts,
			Vector<SyntaxOpcode> opcodes){
		JavadocStrings jd = javadoc ? new JavadocStrings() : null;
		String header = "// This class has been generated automatically\n" +
				"// from an SABNF grammar by Java APG, Verision 1.1.\n" +
				"// Copyright (c) 2021 Lowell D. Thomas, all rights reserved.\n" +
				"// Licensed under the 2-Clause BSD License.\n";
		int udtCount;
		int ruleCount;
		String lineEnd;
		String javadocEnd;
		while(true){
			out.println(header);
			out.print("package ");
			out.print(packageName);
			out.print(";\n\n");
			out.print("import apg.Grammar;\n");
			out.print("import java.io.PrintStream;\n");
			if(javadoc){out.println(jd.header);}
			out.print("\n");
			out.print("public class "+name+" extends Grammar{\n");
			out.print("\n");
			Set<Map.Entry<String, Integer>> set = ruleMap.entrySet();
			ruleCount = set.size();
			out.print("    // public API\n");
			if(javadoc){out.println(jd.singleton);}
			out.print("    public static Grammar getInstance(){\n");
			out.print("        if(factoryInstance == null){\n");
			out.print("            factoryInstance = new ");
			out.print(name);
			out.print("(getRules(), getUdts(), getOpcodes());\n");
			out.print("        }\n");
			out.print("        return factoryInstance;\n");
			out.print("    }\n");
			out.print("\n");
			out.print("    // rule name enum\n");
			if(javadoc){out.println(jd.numRules);}
			out.print("    public static int ruleCount = ");
			out.print(ruleCount);
			out.print(";\n");
			lineEnd = "        ";
			javadocEnd = lineEnd;
			if(ruleCount > 0){
				if(javadoc){out.println(jd.getEnumDoc("rule"));}
				out.print("    public enum RuleNames{\n");
				for(Map.Entry<String, Integer> entry:set){
					String caps = toUpperUnderscore(entry.getKey());
					int index = entry.getValue();
					SyntaxRule rule = rules.elementAt(index);
					out.print(lineEnd);
					if(javadoc){
						out.println(jd.getEnumConstant(index, rule.name));
						out.print(javadocEnd);
					}
					out.print(caps);
					out.print("(");
					out.print("\"");
					out.print(rule.name);
					out.print("\", ");
					out.print(index);
					out.print(", ");
					out.print(rule.opcodeOffset);
					out.print(", ");
					out.print(rule.opcodeCount);
					out.print(")");
					if(lineEnd.length() == 8){lineEnd = ",\n        ";}
				}
				out.print(";\n");
				out.print("        private String name;\n");
				out.print("        private int id;\n");
				out.print("        private int offset;\n");
				out.print("        private int count;\n");
				out.print("        RuleNames(String string, int id, int offset, int count){\n");
				out.print("            this.name = string;\n");
				out.print("            this.id = id;\n");
				out.print("            this.offset = offset;\n");
				out.print("            this.count = count;\n");
				out.print("        }\n");
				if(javadoc){out.println(jd.ruleName);}
				out.print("        public  String ruleName(){return name;}\n");
				if(javadoc){out.println(jd.ruleId);}
				out.print("        public  int    ruleID(){return id;}\n");
				out.print("        private int    opcodeOffset(){return offset;}\n");
				out.print("        private int    opcodeCount(){return count;}\n");
				out.print("    }\n");
			}
			out.print("\n");
			set = udtMap.entrySet();
			udtCount = set.size();
			out.print("    // UDT name enum\n");
			if(javadoc){out.println(jd.numUdts);}
			out.print("    public static int udtCount = ");
			out.print(udtCount);
			out.print(";\n");
			if(javadoc){out.println(jd.getEnumDoc("UDT"));}
			out.print("    public enum UdtNames{\n");
			if(udtCount > 0){
				lineEnd = "        ";
				for(Map.Entry<String, Integer> entry:set){
					String caps = toUpperUnderscore(entry.getKey());
					int index = entry.getValue();
					SyntaxRule udt = udts.elementAt(index);
					out.print(lineEnd);
					if(javadoc){
						out.println(jd.getEnumConstant(index, udt.name));
						out.print(javadocEnd);
					}
					out.print(caps);
					out.print("(");
					out.print(index);
					out.print(", \"");				
					out.print(udt.name);
					out.print("\", ");
					out.print(udt.mayBeEmpty);
					out.print(")");
					if(lineEnd.length() == 8){lineEnd = ",\n        ";}
				}
				out.print(";\n");
				out.print("        private String name;\n");
				out.print("        private int id;\n");
				out.print("        private boolean empty;\n");
				out.print("        UdtNames(int id, String string, boolean empty){\n");
				out.print("            this.name = string;\n");
				out.print("            this.id = id;\n");
				out.print("            this.empty = empty;\n");
				out.print("        }\n");
				if(javadoc){out.println(jd.udtName);}
				out.print("        public String  udtName(){return name;}\n");
				if(javadoc){out.println(jd.udtId);}
				out.print("        public int     udtID(){return id;}\n");
				if(javadoc){out.println(jd.udtEmpty);}
				out.print("        public boolean udtMayBeEmpty(){return empty;}\n");
			}
			out.print("    }\n");
			out.print("\n");
			out.print("    // private\n");
			out.print("    private static ");
			out.print(name);
			out.print(" factoryInstance = null;\n");
			out.print("    private ");
			out.print(name);
			out.print("(Rule[] rules, Udt[] udts, Opcode[] opcodes){\n");
			out.print("        super(rules, udts, opcodes);\n");
			out.print("    }\n");
			out.print("\n");
			out.print("    private static Rule[] getRules(){\n");
			out.print("    	Rule[] rules = new Rule[");
			out.print(ruleCount);
			out.print("];\n");
			out.print("        for(RuleNames r : RuleNames.values()){\n");
			out.print("            rules[r.ruleID()] = getRule(r.ruleID(), r.ruleName(), r.opcodeOffset(), r.opcodeCount());\n");
			out.print("        }\n");
			out.print("        return rules;\n");
			out.print("    }\n");
			out.print("\n");
			set = udtMap.entrySet();
			out.print("    private static Udt[] getUdts(){\n");
			out.print("    	Udt[] udts = new Udt[");
			out.print(udtCount);
			out.print("];\n");
			if(udtCount > 0){
				out.print("        for(UdtNames r : UdtNames.values()){\n");
				out.print("            udts[r.udtID()] = getUdt(r.udtID(), r.udtName(), r.udtMayBeEmpty());\n");
				out.print("        }\n");
			}
			out.print("        return udts;\n");
			out.print("    }\n");
			out.print("\n");
			out.print("        // opcodes\n");
			out.print("    private static Opcode[] getOpcodes(){\n");
			out.print("    	Opcode[] op = new Opcode[");
			out.print(opcodes.size());
			out.print("];\n");
			
			// RHA split into several methods to avoid hitting method size limit
			final int rulesPerMethod = 250;
			for(int i=0, n=0;n<rules.size();i++,n+=rulesPerMethod){
				out.printf("    	addOpcodes%02d(op);\n",i);
			}
			
			out.print("        return op;\n");
			out.print("    }\n");
			out.print("\n");
			
			boolean first;
			int ruleNo = 0;
			for(SyntaxRule rule:rules){
				if(ruleNo % rulesPerMethod == 0){
					if(ruleNo > 0)
						out.print("    }\n");
					out.printf("    private static void addOpcodes%02d(Opcode[] op){\n",ruleNo/rulesPerMethod);
				}
				ruleNo++;
				
				for(int i = rule.opcodeOffset; i < (rule.opcodeOffset + rule.opcodeCount); i++){
					SyntaxOpcode op = opcodes.elementAt(i);
					switch(op.type){
					case ALT:
						out.print("        {int[] a = {");
						first = true;
						for(SyntaxOpcode child:op.childOpcodes){
							if(first){first = false;}
							else{out.print(",");}
							out.print(child.index);
						}
						out.print("}; op[");
						out.print(i);
						out.print("] = getOpcodeAlt(a);}\n");
						break;
					case CAT:
						out.print("        {int[] a = {");
						first = true;
						for(SyntaxOpcode child:op.childOpcodes){
							if(first){first = false;}
							else{out.print(",");}
							out.print(child.index);
						}
						out.print("}; op[");
						out.print(i);
						out.print("] = getOpcodeCat(a);}\n");
						break;
					case AND:
						out.print("        op[");
						out.print(op.index);
						out.print("] = getOpcodeAnd(");
						out.print(op.childOpcodes.elementAt(0).index);
						out.print(");\n");
						break;
					case NOT:
						out.print("        op[");
						out.print(op.index);
						out.print("] = getOpcodeNot(");
						out.print(op.childOpcodes.elementAt(0).index);
						out.print(");\n");
						break;
					case REP:
						out.print("        op[");
						out.print(op.index);
						out.print("] = getOpcodeRep(");
						if(op.min < Character.MAX_VALUE){out.print("(char)"+op.min);}
						else{out.print("Character.MAX_VALUE, ");}
						out.print(", ");
						if(op.max < Character.MAX_VALUE){out.print("(char)"+op.max);}
						else{out.print("Character.MAX_VALUE");}
						out.print(", ");
						out.print(op.childOpcodes.elementAt(0).index);
						out.print(");\n");
						break;
					case TRG:
						out.print("        op[");
						out.print(op.index);
						out.print("] = getOpcodeTrg(");
						if(op.min < Character.MAX_VALUE){out.print("(char)"+op.min);}
						else{out.print("Character.MAX_VALUE, ");}
						out.print(", ");
						if(op.max < Character.MAX_VALUE){out.print("(char)"+op.max);}
						else{out.print("Character.MAX_VALUE");}
						out.print(");\n");
						break;
					case TBS:
						out.print("        {char[] a = {");
						first = true;
						for(int j = 0; j < op.string.length; j++){
							if(first){first = false;}
							else{out.print(",");}
							out.print((int)op.string[j]);
						}
						out.print("}; op[");
						out.print(i);
						out.print("] = getOpcodeTbs(a);}\n");
						break;
					case TLS:
						out.print("        {char[] a = {");
						first = true;
						for(int j = 0; j < op.string.length; j++){
							if(first){first = false;}
							else{out.print(",");}
							out.print((int)op.string[j]);
						}
						out.print("}; op[");
						out.print(i);
						out.print("] = getOpcodeTls(a);}\n");
						break;
					case RNM:
						SyntaxRule thisRule = rules.elementAt(op.id);
						out.print("        op[");
						out.print(op.index);
						out.print("] = getOpcodeRnm(");
						out.print(thisRule.id);
						out.print(", ");
						out.print(thisRule.opcodeOffset);
						out.print("); // ");
						out.print(thisRule.name);
						out.print("\n");
						break;
					case UDT:
						out.print("        op[");
						out.print(op.index);
						out.print("] = getOpcodeUdt(");
						out.print(op.id);
						out.print("); // ");
						out.print(op.name);
						out.print("\n");
						break;
					}
				}
			}
			out.print("    }\n");
			out.print("\n");
			
			// generate a "display()" function which will display the original SABNF grammar
			if(javadoc){out.println(jd.display);}
			out.print("    public static void display(PrintStream out){\n");
			out.print("        out.println(\";\");\n");
			out.print("        out.println(\"; " + packageName + "." +name+ "\");\n");
			out.print("        out.println(\";\");\n");
			if(grammar != null && !grammar.equals("")){
				String[] lines = grammar.split("\n|\r\n|\r");
				for(String line : lines){
					out.print("        out.println(\"" + sanitizeLine(line) + "\");");
					out.print("\n");
				}
			}
			out.print("    }\n");
			
			// end
			out.print("}\n");
			break;
		}
	}
	private static String sanitizeLine(String line){
		StringBuffer buf = new StringBuffer();
		if(!(line == null || line.length() == 0)){
			char[] lineChars = line.toCharArray();
			for(char c : lineChars){
				if(c == '"'){buf.append("\\\"");}
				else if(c == '\\'){buf.append("\\\\");}
				else{buf.append(c);}
			}
		}
		return buf.toString();
	}
	// TODO: generate JavaScript language parsers
//	private static String generateJavaScript(TreeMap<String, Integer> ruleMap, String fileName){
//		StringBuffer buf = new StringBuffer();
//		String ret = null;
//		buf.append("TBD: generate JavaScript parser");
//		return buf.toString();
//	}

	// TODO: generate C-language parsers
//	private static String generateC(TreeMap<String, Integer> ruleMap, String fileName){
//		StringBuffer buf = new StringBuffer();
//		String ret = null;
//		buf.append("TBD: generate C-language parser");
//		return buf.toString();
//	}

	// TODO: generate C++ language parsers
//	private static String generateCPP(TreeMap<String, Integer> ruleMap, String fileName){
//		StringBuffer buf = new StringBuffer();
//		String ret = null;
//		buf.append("TBD: generate C++ parser");
//		return buf.toString();
//	}

}
