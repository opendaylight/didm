package org.opendaylight.yang.gen.v1.urn.opendaylight.didm.flowmodtest.rev140814;

import org.opendaylight.didm.fmt.FlowModTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FMTModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.didm.flowmodtest.rev151015.AbstractFMTModule {
	private static final Logger LOG = LoggerFactory.getLogger(FMTModule.class);
	
	public FMTModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public FMTModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.didm.flowmodtest.rev140814.FMTModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
    	LOG.error("Creating FlowModTest instance");

        RpcProviderRegistry rpcRegistryDependency = getRpcRegistryDependency();
        SalFlowService salFlowService = rpcRegistryDependency.getRpcService(SalFlowService.class);

        return new FlowModTest(getDataBrokerDependency(), getRpcRegistryDependency(), salFlowService);
    }

}
