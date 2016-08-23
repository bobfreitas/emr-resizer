package com.freitas.emr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClient;
import com.amazonaws.services.elasticmapreduce.model.AddInstanceGroupsRequest;
import com.amazonaws.services.elasticmapreduce.model.AddInstanceGroupsResult;
import com.amazonaws.services.elasticmapreduce.model.DescribeClusterRequest;
import com.amazonaws.services.elasticmapreduce.model.DescribeClusterResult;
import com.amazonaws.services.elasticmapreduce.model.Instance;
import com.amazonaws.services.elasticmapreduce.model.InstanceGroup;
import com.amazonaws.services.elasticmapreduce.model.InstanceGroupConfig;
import com.amazonaws.services.elasticmapreduce.model.InstanceGroupModifyConfig;
import com.amazonaws.services.elasticmapreduce.model.InstanceRoleType;
import com.amazonaws.services.elasticmapreduce.model.InstanceState;
import com.amazonaws.services.elasticmapreduce.model.JobFlowExecutionState;
import com.amazonaws.services.elasticmapreduce.model.ListInstanceGroupsRequest;
import com.amazonaws.services.elasticmapreduce.model.ListInstanceGroupsResult;
import com.amazonaws.services.elasticmapreduce.model.ListInstancesRequest;
import com.amazonaws.services.elasticmapreduce.model.ListInstancesResult;
import com.amazonaws.services.elasticmapreduce.model.MarketType;
import com.amazonaws.services.elasticmapreduce.model.ModifyInstanceGroupsRequest;
import com.freitas.emr.Const.InstanceEnum;
import com.freitas.emr.ProcessArgs.OpTypeEnum;

public class EmrResizer {
	
	static final List<String> SEARCH_STATES = new ArrayList<String>();
	static {
		SEARCH_STATES.add(InstanceState.RUNNING.name());
	}

	private static final int ONE_MINUTE = 60*1000;
	private final AmazonElasticMapReduce client;

	public static void main(String[] args) {
		ProcessArgs processArgs = new ProcessArgs();
		Map<String,String> argsMap = null;
		try {
			argsMap = processArgs.process(args);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
		EmrResizer awsTools = new EmrResizer(argsMap);
		System.exit(awsTools.process(argsMap));
	}
	
	@SuppressWarnings("unused")
	private EmrResizer() {
		client = null;
	}
	
	public EmrResizer(Map<String,String> argsMap) {
		if (argsMap.containsKey(ProcessArgs.IAM)) {
			// use IAM roles on the local instance
			client = new AmazonElasticMapReduceClient(new InstanceProfileCredentialsProvider());
		} else {
			//FIXME: not really okay, for demo purposes only
			String accessKey = "some_access_key";
			String secretKey = "some_secret_key";
			AWSCredentials credentials = new BasicAWSCredentials(
	                accessKey,  secretKey);
			client = new AmazonElasticMapReduceClient(credentials);
		}
		client.setEndpoint(Const.ENDPOINT);
	}

	private int process(Map<String,String> argsMap) {
		String clusterId = argsMap.get(ProcessArgs.CLUSTER_ID);
		if (!clusterActive(clusterId)) {
			System.err.println("Selected cluster, " + clusterId + 
				", is not active");
			return 1;
		}
		
		Integer instCnt = convertToInt(argsMap.get(ProcessArgs.INSTANCE_CNT));
		if (instCnt == 0 ){
			System.err.println("Invalid instance count provided: " + instCnt);
			return 1;
		}
		
		Integer waitTime = convertToInt(argsMap.get(ProcessArgs.TIMEOUT));
		if (waitTime == 0 ){
			System.err.println("Invalid timeout provided: " + waitTime);
			return 1;
		}
		
		RequestInfo requestInfo = new RequestInfo();
		requestInfo.setClusterId(clusterId);
		requestInfo.setInstCnt(instCnt);
		requestInfo.setWaitTime(waitTime);
		String instType = argsMap.get(ProcessArgs.INSTANCE_TYPE);
		requestInfo.setInstType(instType);
		InstanceEnum instanceEnum = InstanceEnum.getByName(instType);
		requestInfo.setMaxInstances(instanceEnum.getMaxInstances());
		requestInfo.setInstGrpName(instanceEnum.getInstODGrpName());
		
		String opType = argsMap.get(ProcessArgs.OPTYPE);
		if (opType.equals(OpTypeEnum.INCREASE.getName())){
			return processIncrease(requestInfo);
		}
		else if (opType.equals(OpTypeEnum.DECREASE.getName())){
			return processDecrease(requestInfo);
		}
		else {
			// should not happen, but just in case
			System.err.println("Unexpected optype: " + opType);
			return 1;
		}
	}
	
	private int processIncrease(RequestInfo requestInfo) {
		if (requestInfo.getMaxInstances() < requestInfo.getInstCnt()) {
			System.err.println("Exceeding maximum allowed instance in group, " + 
				requestInfo.getMaxInstances());
			return 1;
		}
		try {
			// first get instance group for the requested type
			String autoInstGrpId = getAutoInstGrpId(requestInfo.getClusterId(), 
					requestInfo.getInstGrpName());
			int numCurrentlyRunning = 0;
			if (autoInstGrpId != null) {
				// instance group exists so need to modify it
				numCurrentlyRunning = countRunningInstancesInGroup(
						requestInfo.getClusterId(), autoInstGrpId);
				int expectedCnt = numCurrentlyRunning + requestInfo.getInstCnt();
				modifyInstanceGroup(autoInstGrpId, expectedCnt, 
						requestInfo.getInstGrpName());
				waitForInstances(requestInfo.getClusterId(), autoInstGrpId, 
						expectedCnt, requestInfo.getWaitTime());
			} else {
				// no instance group yet, create one
				autoInstGrpId = addInstanceGroup(requestInfo);
				if (autoInstGrpId != null) {
					int expectedCnt = requestInfo.getInstCnt();
					waitForInstances(requestInfo.getClusterId(), autoInstGrpId, 
							expectedCnt, requestInfo.getWaitTime());
				} else {
					System.err.println(
						"Unable to create instance group and add spot instances");
					return 1;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return 1;
		}
		incrOkMsg(requestInfo.getClusterId(), requestInfo.getInstType(), 
				requestInfo.getInstCnt());
		return 0;
	}
	
	private int processDecrease(RequestInfo requestInfo) {
		try {
			// first get instance group for the requested type
			String autoInstGrpId = getAutoInstGrpId(requestInfo.getClusterId(), 
					requestInfo.getInstGrpName());
			if (autoInstGrpId == null) {
				System.err.println("Selected cluster, " + requestInfo.getClusterId() + 
					", does not have the spot instance group, do increase first");
				return 1;
			}
			int numCurrentlyRunning = countRunningInstancesInGroup(
					requestInfo.getClusterId(), autoInstGrpId);
			if (numCurrentlyRunning < requestInfo.getInstCnt()) {
				System.err.println(
					"Unable to remove more spot instances than are active, active: " + 
					numCurrentlyRunning + ", requested decrease: " + 
					requestInfo.getInstCnt());
				return 1;
			}
			int expectedCnt = numCurrentlyRunning - requestInfo.getInstCnt();
			modifyInstanceGroup(autoInstGrpId, expectedCnt, 
					requestInfo.getInstGrpName());
			waitForInstances(requestInfo.getClusterId(), autoInstGrpId, 
					expectedCnt, requestInfo.getWaitTime());
		} catch (Exception e) {
			e.printStackTrace();
			return 1;
		}
		decrOkMsg(requestInfo.getClusterId(), requestInfo.getInstType(), 
				requestInfo.getInstCnt());
		return 0;
	}
	
	private boolean clusterActive(String clusterId) {
		DescribeClusterResult clusterDetail = client.describeCluster(
                new DescribeClusterRequest().withClusterId(clusterId));
		String clusterState = clusterDetail.getCluster().getStatus().getState();
		if (clusterState.equals(JobFlowExecutionState.WAITING.toString())){
			return true;
		}
		return false;
	}
	
	private String getAutoInstGrpId(String clusterId, String instGrpName) {
		ListInstanceGroupsRequest request = new ListInstanceGroupsRequest();
		request.setClusterId(clusterId);
		ListInstanceGroupsResult result = client.listInstanceGroups(request);
		for (InstanceGroup group : result.getInstanceGroups()) {
			if (group.getName().contains(instGrpName)) {
				return group.getId();
			}
		}
		return null;
	}
	
	private String addInstanceGroup(RequestInfo requestInfo) {
		InstanceGroupConfig instGrpConfig = new InstanceGroupConfig();
		instGrpConfig.setName(requestInfo.getInstGrpName());
		instGrpConfig.setInstanceType(requestInfo.getInstType());
		instGrpConfig.setInstanceCount(requestInfo.getInstCnt());
		instGrpConfig.setInstanceRole(InstanceRoleType.TASK.name());
		instGrpConfig.setMarket(MarketType.ON_DEMAND.name());
		
		List<InstanceGroupConfig> instanceGroups = new ArrayList<>();
		instanceGroups.add(instGrpConfig);
		
		AddInstanceGroupsRequest addInstanceGroupsRequest = 
				new AddInstanceGroupsRequest();
		addInstanceGroupsRequest.setJobFlowId(requestInfo.getClusterId());
		addInstanceGroupsRequest.setInstanceGroups(instanceGroups);
		
		AddInstanceGroupsResult result = client.addInstanceGroups(
				addInstanceGroupsRequest);
		if (result.getInstanceGroupIds().size() == 1) {
			System.out.println("Created instance group, " + 
					requestInfo.getInstGrpName() + ", for " + 
					requestInfo.getInstCnt() + " instances");
			return result.getInstanceGroupIds().get(0);
		}
		return null;
	}

	private void modifyInstanceGroup(String autoInstGrpId, Integer newInstCnt, 
			String instGrpName) {
		InstanceGroupModifyConfig instGrpConfig = new InstanceGroupModifyConfig();
		instGrpConfig.setInstanceGroupId(autoInstGrpId);
		instGrpConfig.setInstanceCount(newInstCnt);
		
		List<InstanceGroupModifyConfig> instanceGroups = 
				new ArrayList<InstanceGroupModifyConfig>();
		instanceGroups.add(instGrpConfig);
		
		ModifyInstanceGroupsRequest request = new ModifyInstanceGroupsRequest();
		request.setInstanceGroups(instanceGroups);
		client.modifyInstanceGroups(request);
		System.out.println("Modified instance group, " + instGrpName + 
				", to have " + newInstCnt + " instances");
		// no reason to use the returned status here
		// AWS API returns nothing useful
	}
	
	private void waitForInstances(String clusterId, String autoInstGrpId, 
			int expectedCnt, int maxWaitMinutes) {
		TimeWatch watch = TimeWatch.start();
		boolean waiting = true;
		do {
			if (watch.time(TimeUnit.MINUTES) >= maxWaitMinutes) {
				System.err.println("Timeout minutes have expired, " + 
					maxWaitMinutes);
				System.exit(2);
			}
			int numRunning = 0;
			try {
				numRunning = countRunningInstancesInGroup(clusterId, autoInstGrpId);
			} catch (AmazonServiceException ase) {
				System.err.println("AWS error: " + ase.getMessage());
				waitOneMin();
			}
			if (numRunning == expectedCnt) {
				waiting = false;
			} else {
				System.out.println("Waiting for instances, currently running: " + 
						numRunning + ", required: " + expectedCnt);
			}
			waitOneMin();
		} while (waiting);
	}
	
	private void waitOneMin() {
		try {
			// Sleep for one minute
			Thread.sleep(ONE_MINUTE);
		} catch (Exception e) {
			// Do nothing because it woke up early.
		}
	}
	
	private int countRunningInstancesInGroup(String clusterId, 
			String autoInstGrpId) {
		int cnt = 0;
		ListInstancesRequest request = new ListInstancesRequest();
		request.setClusterId(clusterId);
		request.setInstanceGroupId(autoInstGrpId);
		request.setInstanceStates(SEARCH_STATES);
		String marker = null;
		do {
			ListInstancesResult results = client.listInstances(request);
			marker = results.getMarker();
			for (Instance inst : results.getInstances()) {
				cnt++;
			}
			request.setMarker(marker);
		} while (marker != null);
		return cnt;
	}
	
	private Integer convertToInt(String instCntStr) {
		try {
	      return Integer.parseInt(instCntStr.trim());
	    } catch (NumberFormatException nfe) {
	      return 0;
	    }
	}
	
	private void incrOkMsg(String clusterId, String instType, Integer instCnt) {
		System.out.println("Successfully added " + instCnt + " " + instType + 
				" instances to the cluster " + clusterId);
	}
	
	private void decrOkMsg(String clusterId, String instType, Integer instCnt) {
		System.out.println("Successfully removed " + instCnt + " " + instType + 
				" instances from the cluster " + clusterId);
	}
	
}
