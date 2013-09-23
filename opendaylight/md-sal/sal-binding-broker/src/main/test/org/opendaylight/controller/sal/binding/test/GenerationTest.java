package org.opendaylight.controller.sal.binding.test;
import static org.junit.Assert.*;

import java.util.concurrent.Future;

import org.junit.Test;
import org.opendaylight.controller.sal.binding.impl.ProxyFactoryGenerator;
import org.opendaylight.controller.sal.binding.impl.RpcServiceProxy;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.RpcResult;


public class GenerationTest {

	public interface MockService extends RpcService {
		
	    Future<RpcResult<java.lang.Void>> cancelToast();
	    
	    Future<RpcResult<java.lang.Void>> makeToast(String input);
	}
	
	@Test
	public void test() {
		ProxyFactoryGenerator generator = new ProxyFactoryGenerator();
		Class<? extends RpcServiceProxy<MockService>> ret = generator.generate(MockService.class);
		
		assertTrue(RpcServiceProxy.class.isAssignableFrom(ret));
		assertTrue(MockService.class.isAssignableFrom(ret));
	}

}
