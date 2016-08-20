package com.freitas.emr;

import java.util.HashMap;
import java.util.Map;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class ProcessArgs {
	
	public static final String CLUSTER_ID = "cid";
	public static final String OPTYPE = "optype";
	public static final String INSTANCE_TYPE = "insttype";
	public static final String INSTANCE_CNT = "instcnt";
	public static final String TIMEOUT = "timeout";
	public static final String IAM = "iam";
	
	public enum OpTypeEnum {
		INCREASE(1, "increase"),
		DECREASE(2, "decrease");

		private int id;
		private String name;

		OpTypeEnum(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public int getId() {
			return this.id;
		}
		
		public String getName() {
			return this.name;
		}
	}
	
	public Map<String, String> process(String[] args) throws Exception {
		
		if (args.length < 3){
			help();
			throw new Exception("Wrong number of arguments");
		}
		
		OptionParser parser = new OptionParser();
		parser.accepts(CLUSTER_ID).withRequiredArg().required();
		parser.accepts(OPTYPE).withRequiredArg().required();
		parser.accepts(INSTANCE_TYPE).withRequiredArg().required();
		parser.accepts(INSTANCE_CNT).withRequiredArg().required();
		parser.accepts(TIMEOUT).withRequiredArg().defaultsTo(
				Const.DEFAULT_MAX_WAIT_MINS);
		parser.accepts(IAM);
		
		OptionSet options = parser.parse(args);
		Map<String, String> map = new HashMap<String, String>();
		
		if (!options.hasArgument(CLUSTER_ID)){
			help();
			throw new Exception("Missing " + CLUSTER_ID);
		}
		map.put(CLUSTER_ID, (String)options.valueOf(CLUSTER_ID));
		
		if (!options.hasArgument(OPTYPE)){
			help();
			throw new Exception("Missing " + OPTYPE);
		}
		if (!checkOpType((String)options.valueOf(OPTYPE))) {
			help();
			throw new Exception("Invalid " + OPTYPE);
		}
		map.put(OPTYPE, (String)options.valueOf(OPTYPE));
		
		if (!options.hasArgument(INSTANCE_TYPE)){
			help();
			throw new Exception("Missing " + INSTANCE_TYPE);
		}
		if (!Const.InstanceEnum.isValid((String)options.valueOf(INSTANCE_TYPE))) {
			help();
			throw new Exception("Invalid " + INSTANCE_TYPE);
		}
		map.put(INSTANCE_TYPE, (String)options.valueOf(INSTANCE_TYPE));
		
		if (!options.hasArgument(INSTANCE_CNT)){
			help();
			throw new Exception("Missing " + INSTANCE_CNT);
		}
		if (!checkValidInt((String)options.valueOf(INSTANCE_CNT))) {
			help();
			throw new Exception("Invalid " + INSTANCE_CNT);
		}
		map.put(INSTANCE_CNT, (String)options.valueOf(INSTANCE_CNT));
		
		if (options.hasArgument(TIMEOUT)){
			if (!checkValidInt((String)options.valueOf(TIMEOUT))) {
				help();
				throw new Exception("Invalid " + TIMEOUT);
			}
			map.put(TIMEOUT, (String)options.valueOf(TIMEOUT));
		} else {
			map.put(TIMEOUT, (String)options.valueOf(TIMEOUT));
		}
		
		if (options.has(IAM)){
			map.put(IAM, IAM);
		}

		return map;
	}
	
	private boolean checkOpType(String opType) {
		if (opType.equals(OpTypeEnum.INCREASE.getName())){
			return true;
		}
		if (opType.equals(OpTypeEnum.DECREASE.getName())){
			return true;
		}
		return false;
	}
	
	private boolean checkValidInt(String numStr) {
		int num = 0;
		try {
			num = Integer.parseInt(numStr);
		} catch (NumberFormatException ex) {
			// not an integer, okay to swallow
			return false;
		}
		// make sure its not negative
		if (num < 0) {
			return false;
		}
		return true;
	}
	
	private void help() {
		System.out.println("EMR Resizer");
		System.out.println("");
		System.out.println("SYNOPSIS");
		System.out.println("  -cid=<id> -optype=<operation> -insttype=<type> -instcnt=<num> [-timeout=<num>] [-iam]");
		System.out.println("");
		System.out.println("DESCRIPTION");
		System.out.println("  This tool will allow for instances to be dynamically added/removed to/from a cluster.");
		System.out.println("  It is intended to be used as part of processing workflow to increase and decrease the");
		System.out.println("  number of nodes. The first step in the workflow would be to increase the number of nodes");
		System.out.println("  and the last step would be to decrease the number of nodes.");
		System.out.println("");
		System.out.println("  -cid=<id>");
		System.out.println("    The Amazon cluster identification string, such as 'j-34QCX7S1KA9DB'");
		System.out.println("");
		System.out.println("  -optype=<operation>");
		System.out.println("    Where <operation> will be either 'increase' or 'decrease'.");
		System.out.println("");
		System.out.println("  -insttype=<type>");
		System.out.println("    The Amazon instance type identification string. Currently, 'm4.2xlarge', 'm4.4xlarge'");
		System.out.println("    and 'm4.10xlarge' are supported.");
		System.out.println("");
		System.out.println("  -instcnt=<num>");
		System.out.println("    Where <num> will be the number if instance to added or removed. There are limits on");
		System.out.println("    the number of instances that can be allocated for each instance type:");
		System.out.println("      'm4.2xlarge' - 40 instances");
		System.out.println("      'm4.4xlarge' - 10 instances");
		System.out.println("      'm4.10xlarge' - 5 instances");
		System.out.println("");
		System.out.println("  -timeout=<num>");
		System.out.println("    An optional parameter for the number of minutes to wait for the instance operation");
		System.out.println("    to complete before timing out, defaults to 20");
		System.out.println("");
		System.out.println("  -iam");
		System.out.println("    An optional parameter to use the instance IAM role to authenticate");
		System.out.println("");
		System.out.println("EXIT STATUS");
		System.out.println("  0  the operation completed");
		System.out.println("  1  An error occurred in processing");
		System.out.println("  2  Operation exceeded the timeout");
		System.out.println("");
	}


}
