package com.freitas.emr;

public class Const {

	public static final String ENDPOINT = "elasticmapreduce.us-west-2.amazonaws.com";
	public static final String DEFAULT_MAX_WAIT_MINS = "20";
	
	public enum InstanceEnum {
		M4_2XL("m4.2xlarge", "AG_OD_M4_2XL", 40),
		M4_4XL("m4.4xlarge", "AG_OD_M4_4XL", 10),
		M4_10XL("m4.10xlarge", "AG_OD_M4_10XL", 5);
		
		private String name;
		private String instODGrpName;
		private int maxInstances;
		
		InstanceEnum(String name, String instODGrpName, int maxInstances) {
			this.name = name;
			this.instODGrpName = instODGrpName;
			this.maxInstances = maxInstances;
		}
		
		public String getName() {
			return this.name;
		}
		public String getInstODGrpName() {
			return this.instODGrpName;
		}
		
		public int getMaxInstances() {
			return maxInstances;
		}

		public static boolean isValid(String instTypeName) {
			switch (instTypeName){
			case "m4.2xlarge":
				return true;
			case "m4.4xlarge":
				return true;
			case "m4.10xlarge":
				return true;
			default: 
				return false;
			}
		}
		
		public static InstanceEnum getByName(String instTypeName) {
			switch (instTypeName){
			case "m4.2xlarge":
				return InstanceEnum.M4_2XL;
			case "m4.4xlarge":
				return InstanceEnum.M4_4XL;
			case "m4.10xlarge":
				return InstanceEnum.M4_10XL;
			default: 
				return null;
			}
		}
		
	}
	
}
