package org.opendaylight.yang.gen.v1.urn.opendaylight.didm.drivertest.rev141114;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.didm.drivertest.DriverTest;

public class DriverTestModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.didm.drivertest.rev151015.AbstractDriverTestModule {
	private static final Logger LOG = LoggerFactory.getLogger(DriverTestModule.class);
	
	public DriverTestModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DriverTestModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.didm.drivertest.rev141114.DriverTestModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
    	LOG.error("Creating DriverTest instance");
    	return new DriverTest(getDataBrokerDependency(), getRpcRegistryDependency());
    }

}
