package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.mininet.rev150211;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.didm.mininet.OpenFlowDeviceDriver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.DeviceTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.device.types.DeviceTypeInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.device.types.DeviceTypeInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.device.types.DeviceTypeInfoKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import org.opendaylight.didm.mininet.DidmMininetProviderImpl;


/**
 * The Mininet config subsystem module manages:
 *   1) the mininet device type info
 *   2) the mininet OF driver
 */
public class DidmMininetProviderModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.mininet.rev150211.AbstractDidmMininetProviderModule {
    private static final Logger LOG = LoggerFactory.getLogger(DidmMininetProviderModule.class);

    public DidmMininetProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DidmMininetProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.mininet.rev150211.DidmMininetProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }
    
    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("Creating MininetDriverProvider");
        return new DidmMininetProviderImpl(/*getDeviceIdentificationProviderRegistryDependency(),
                                             getDeviceDriverProviderRegistryDependency(),*/
                                                     getDataBrokerDependency(), getRpcRegistryDependency());

    }

}
