package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.discovery.impl.rev150224;
public class DeviceDiscoveryManagerModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.discovery.impl.rev150224.AbstractDeviceDiscoveryManagerModule {
    public DeviceDiscoveryManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DeviceDiscoveryManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.discovery.impl.rev150224.DeviceDiscoveryManagerModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new org.opendaylight.didm.discovery.impl.DeviceDiscoveryManager(getDataBrokerDependency(), getRpcRegistryDependency());
    }
}
