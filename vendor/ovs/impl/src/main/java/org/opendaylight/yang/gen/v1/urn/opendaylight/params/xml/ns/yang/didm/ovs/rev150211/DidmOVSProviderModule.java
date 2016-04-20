package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.ovs.rev150211;

import org.opendaylight.didm.ovs.DidmOVSProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The OVS config subsystem module manages: 
 * 1. the OVS device type info 
 * 2. the OVS OF driver
 */
public class DidmOVSProviderModule extends
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.ovs.rev150211.AbstractDidmOVSProviderModule {

    static final Logger LOG = LoggerFactory.getLogger(DidmOVSProviderModule.class);

    public DidmOVSProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DidmOVSProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.ovs.rev150211.DidmOVSProviderModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("Creating MininetDriverProvider");
        return new DidmOVSProviderImpl(getDataBrokerDependency(), getRpcRegistryDependency());
    }

}
