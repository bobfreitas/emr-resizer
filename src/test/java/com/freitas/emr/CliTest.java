package com.freitas.emr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;

import com.freitas.emr.ProcessArgs.OpTypeEnum;

public class CliTest {

	private static final String CID = "j-2R4BS1IADIJY4";

	@Test
	public void testValidIncrease() throws Exception {
		String[] args = new String[] { 
				"-cid=" + CID, 
				"-optype=increase", 
				"-insttype=" + Const.InstanceEnum.M4_2XL.getName(),
				"-instcnt=10"
		};
		ProcessArgs processArgs = new ProcessArgs();
		Map<String,String> map = processArgs.process(args);
		assertEquals(CID, map.get(ProcessArgs.CLUSTER_ID));
		assertEquals(OpTypeEnum.INCREASE.getName(), map.get(ProcessArgs.OPTYPE));
		assertEquals(Const.InstanceEnum.M4_2XL.getName(), map.get(ProcessArgs.INSTANCE_TYPE));
		assertEquals("10", map.get(ProcessArgs.INSTANCE_CNT));
		assertEquals(Const.DEFAULT_MAX_WAIT_MINS, map.get(ProcessArgs.TIMEOUT));
	}
	
	@Test
	public void testValidIncreaseWithTimeout() throws Exception {
		String[] args = new String[] { 
				"-cid=" + CID, 
				"-optype=increase", 
				"-insttype=" + Const.InstanceEnum.M4_2XL.getName(),
				"-instcnt=10",
				"-timeout=15"
		};
		ProcessArgs processArgs = new ProcessArgs();
		Map<String,String> map = processArgs.process(args);
		assertEquals(CID, map.get(ProcessArgs.CLUSTER_ID));
		assertEquals(OpTypeEnum.INCREASE.getName(), map.get(ProcessArgs.OPTYPE));
		assertEquals(Const.InstanceEnum.M4_2XL.getName(), map.get(
			ProcessArgs.INSTANCE_TYPE));
		assertEquals("10", map.get(ProcessArgs.INSTANCE_CNT));
		assertEquals("15", map.get(ProcessArgs.TIMEOUT));
	}
	
	@Test(expected = Exception.class)
	public void testNoParams() throws Exception {
		ProcessArgs processArgs = new ProcessArgs();
		processArgs.process(new String[] { });
	}

	@Test(expected = Exception.class)
	public void testInvalidOpType() throws Exception {
		String[] args = new String[] { 
				"-cid=" + CID, 
				"-optype=trash", 
				"-insttype=" + Const.InstanceEnum.M4_2XL.getName(),
				"-instcnt=10"
		};
		ProcessArgs processArgs = new ProcessArgs();
		processArgs.process(args);
	}
	
	@Test
	public void testMissingArg() throws Exception {
		String[] args = new String[] { 
				"-optype=increase", 
				"-instcnt=10"
		};
		ProcessArgs processArgs = new ProcessArgs();
		try {
			processArgs.process(args);
			fail("Exception expected");
		} catch (Exception e) {
			if (!e.getMessage().equals("Wrong number of arguments")) {
				fail("Incorrect exception thrown");
			}
		}
	}
	
	@Test
	public void testMissingCIDValue() throws Exception {
		String[] args = new String[] { 
				"-cid=",
				"-optype=increase", 
				"-insttype=" + Const.InstanceEnum.M4_2XL.getName(),
				"-instcnt=10"
		};
		ProcessArgs processArgs = new ProcessArgs();
		try {
			processArgs.process(args);
			fail("Exception expected");
		} catch (Exception e) {
			if (!e.getMessage().contains("Missing required option")) {
				fail("Incorrect exception thrown");
			}
		}
	}
	
	@Test(expected = Exception.class)
	public void testNegativeInstCnt() throws Exception {
		String[] args = new String[] { 
				"-cid=" + CID, 
				"-optype=increase", 
				"-insttype=" + Const.InstanceEnum.M4_2XL.getName(),
				"-instcnt=-10"
		};
		ProcessArgs processArgs = new ProcessArgs();
		processArgs.process(args);
	}
	
	@Test(expected = Exception.class)
	public void testNegativeTimeout() throws Exception {
		String[] args = new String[] { 
				"-cid=" + CID, 
				"-optype=increase", 
				"-insttype=" + Const.InstanceEnum.M4_2XL.getName(),
				"-instcnt=10",
				"-timeout=-15"
		};
		ProcessArgs processArgs = new ProcessArgs();
		processArgs.process(args);
	}
	
	@Test(expected = Exception.class)
	public void testInvalidTimeout() throws Exception {
		String[] args = new String[] { 
				"-cid=" + CID, 
				"-optype=increase", 
				"-insttype=" + Const.InstanceEnum.M4_2XL.getName(),
				"-instcnt=10",
				"-timeout=ab"
		};
		ProcessArgs processArgs = new ProcessArgs();
		processArgs.process(args);
	}
	
	@Test
	public void testValidIncreaseWithIAMRole() throws Exception {
		String[] args = new String[] { 
				"-cid=" + CID, 
				"-optype=increase", 
				"-insttype=" + Const.InstanceEnum.M4_2XL.getName(),
				"-instcnt=10",
				"-timeout=15",
				"-iam"
		};
		ProcessArgs processArgs = new ProcessArgs();
		Map<String,String> map = processArgs.process(args);
		assertEquals(CID, map.get(ProcessArgs.CLUSTER_ID));
		assertEquals(OpTypeEnum.INCREASE.getName(), map.get(ProcessArgs.OPTYPE));
		assertEquals(Const.InstanceEnum.M4_2XL.getName(), map.get(ProcessArgs.INSTANCE_TYPE));
		assertEquals("10", map.get(ProcessArgs.INSTANCE_CNT));
		assertEquals("15", map.get(ProcessArgs.TIMEOUT));
		assertTrue(map.containsKey(ProcessArgs.IAM));
	}

}
