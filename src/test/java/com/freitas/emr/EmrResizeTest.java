package com.freitas.emr;

import org.junit.Ignore;
import org.junit.Test;


public class EmrResizeTest {

	@Ignore
	@Test
	public void testTokboxAWSTools() throws Exception {
		String[] args = new String[] { 
				"-cid=j-1KN2EU47VJXP5", 
				"-insttype=" + Const.InstanceEnum.M4_2XL.getName(),
				"-optype=increase",
				"-instcnt=40"
		};
		EmrResizer.main(args);
	}
	
}
