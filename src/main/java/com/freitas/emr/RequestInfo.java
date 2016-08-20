package com.freitas.emr;

public class RequestInfo {
	
	private String clusterId;
	private String instType;
	private String instGrpName;
	private Integer instCnt;
	private int waitTime;
	private int maxInstances;
	
	public RequestInfo() {
	}
	
	public RequestInfo(String clusterId, String instType, String instGrpName, 
			Integer instCnt, int waitTime) {
		super();
		this.clusterId = clusterId;
		this.instType = instType;
		this.instCnt = instCnt;
		this.waitTime = waitTime;
	}

	public String getClusterId() {
		return clusterId;
	}
	public void setClusterId(String clusterId) {
		this.clusterId = clusterId;
	}

	public String getInstType() {
		return instType;
	}
	public void setInstType(String instType) {
		this.instType = instType;
	}

	public String getInstGrpName() {
		return instGrpName;
	}
	public void setInstGrpName(String instGrpName) {
		this.instGrpName = instGrpName;
	}

	public Integer getInstCnt() {
		return instCnt;
	}
	public void setInstCnt(Integer instCnt) {
		this.instCnt = instCnt;
	}

	public int getWaitTime() {
		return waitTime;
	}
	public void setWaitTime(int waitTime) {
		this.waitTime = waitTime;
	}

	public int getMaxInstances() {
		return maxInstances;
	}
	public void setMaxInstances(int maxInstances) {
		this.maxInstances = maxInstances;
	}
	
}
