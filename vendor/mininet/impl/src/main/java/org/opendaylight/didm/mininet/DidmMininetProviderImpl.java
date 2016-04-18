/*
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.didm.mininet;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.OpenflowFeatureService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.DeviceTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.rev150202.DeviceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.device.types.DeviceTypeInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.device.types.DeviceTypeInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.device.types.DeviceTypeInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/***
 * Provider of mininet device drivers
 */
public class DidmMininetProviderImpl implements DataChangeListener, AutoCloseable {
	
    private static final Logger LOG = LoggerFactory.getLogger(DidmMininetProviderImpl.class);
    
    private static final InstanceIdentifier<DeviceType> PATH = InstanceIdentifier.builder(Nodes.class).child(Node.class).augmentation(DeviceType.class).build();

    private static String MININET_DEVICE_TYPE = "mininet";
    private static List<String> SUPPORTED_DEVICE_TYPES = Lists.newArrayList(MININET_DEVICE_TYPE);
    private static String MANUFACTURER = "Nicira, Inc.";
    private static List<String> HARDWARE = Lists.newArrayList("Open vSwitch");
    private static List<String> SYSOID = Lists.newArrayList("No sysOid");
    
    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcRegistry;
    private ListenerRegistration<DataChangeListener> dataChangeListenerRegistration;
    private Map<InstanceIdentifier<?>, List<BindingAwareBroker.RoutedRpcRegistration<RpcService>>> rpcRegistrations = new HashMap<>();

    public DidmMininetProviderImpl(DataBroker dataBroker, RpcProviderRegistry rpcRegistry) {
    	Preconditions.checkNotNull(dataBroker);
    	Preconditions.checkNotNull(rpcRegistry);
    	this.dataBroker = dataBroker;
    	this.rpcRegistry = rpcRegistry;
    	
    	// subscribe to be notified when a device-type augmentation is applied to an inventory node
    	dataChangeListenerRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, PATH, this, AsyncDataBroker.DataChangeScope.BASE);
        LOG.info("Device-type Listener registered");
    	
    	writeTestDataToDeviceTypeDataStore();
    }
    
    private void writeTestDataToDeviceTypeDataStore () {
    	InstanceIdentifier<DeviceTypeInfo> path = createPath(MININET_DEVICE_TYPE);
    	
    	DeviceTypeInfoBuilder dtiBuilder = new DeviceTypeInfoBuilder();
    	dtiBuilder.setDeviceTypeName(MININET_DEVICE_TYPE);
    	dtiBuilder.setOpenflowManufacturer(MANUFACTURER);
    	dtiBuilder.setOpenflowHardware(HARDWARE);	
    	dtiBuilder.setSysoid(SYSOID);
    	DeviceTypeInfo dti = dtiBuilder.build();
    	
    	WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
    	writeTx.merge(LogicalDatastoreType.CONFIGURATION, path, dti, true);

        CheckedFuture<Void, TransactionCommitFailedException> future = writeTx.submit();

        Futures.addCallback(future, new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                LOG.debug("successfully wrote the dti to data store");
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("failed to write device type info to md-sal data store: {}", t.getMessage());
            }
        });
    }

    private InstanceIdentifier<DeviceTypeInfo> createPath(String name) {
        return InstanceIdentifier.<DeviceTypes>builder(DeviceTypes.class)
                .<DeviceTypeInfo, DeviceTypeInfoKey>child(DeviceTypeInfo.class, new DeviceTypeInfoKey(name))
                .build();
    }

    public List<String> getDeviceTypes() {
        return SUPPORTED_DEVICE_TYPES;
    }

    public ClassToInstanceMap<RpcService> getDrivers(String deviceType) {
        Preconditions.checkNotNull(deviceType);
        Preconditions.checkArgument(deviceType.equals(MININET_DEVICE_TYPE), "Only the '{}' device type is supported!", MININET_DEVICE_TYPE);
        
        ClassToInstanceMap<RpcService> drivers = MutableClassToInstanceMap.create();
        drivers.putInstance(OpenflowFeatureService.class, new OpenFlowDeviceDriver());
        return drivers;
    }

    public void close() throws Exception {
    	if(dataChangeListenerRegistration != null) {
            dataChangeListenerRegistration.close();
            dataChangeListenerRegistration = null;
        }
    	
    	for (Map.Entry<InstanceIdentifier<?>, List<BindingAwareBroker.RoutedRpcRegistration<RpcService>>> entry : rpcRegistrations.entrySet()) {
            // TODO: does this work if we're removing entries inside the iterator?
            unregisterRpcs(entry.getKey());
        }
    }

	@Override
	public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
		handleDataCreated(change.getCreatedData());
		handleDataUpdated(change.getUpdatedData());
        handleDataRemoved(change.getRemovedPaths());
	}
	
	private void handleDataCreated(Map<InstanceIdentifier<?>, DataObject> createdData) {
		Preconditions.checkNotNull(createdData);
        if(!createdData.isEmpty()) {
            LOG.info("Data was created ({})", createdData.size());

            // iid will be of the form \nodes\node[id="<uuid>"]\devicetype
            // this should really only be a map of 1 entry
            for (Map.Entry<InstanceIdentifier<?>, DataObject> dataObjectEntry : createdData.entrySet()) {
                InstanceIdentifier<?> path = dataObjectEntry.getKey();
                DeviceType dt = (DeviceType) dataObjectEntry.getValue();
                
                if (dt.getDeviceType().equals(MININET_DEVICE_TYPE)) {

	                String deviceType = dt.getDeviceType();
	                LOG.info("DeviceTypeInfo is {}", deviceType);
	
	                // register the drivers
                    InstanceIdentifier<Node> nodePath = path.firstIdentifierOf(Node.class);
                    List<BindingAwareBroker.RoutedRpcRegistration<RpcService>> registrations = registerDrivers(deviceType, nodePath);

                    // save the registrations so we can cleanup on device removal
                    // TODO: what if an entry already exists?
                    rpcRegistrations.put(path, registrations);
                }
            }
        }
	}
	
	/***
     * Registers all drivers for a devicetype
     * @param deviceType The devicetype
     * @param path The node the drivers apply to
     * @return List of successful routed RPC registrations
     */
	private List<BindingAwareBroker.RoutedRpcRegistration<RpcService>> registerDrivers(String deviceType, InstanceIdentifier<Node> path) {
        LOG.info("onNodeIdentified!");
        
        List<BindingAwareBroker.RoutedRpcRegistration<RpcService>> list = new ArrayList<>();
        
        // get all the drivers to be registered as routed RPCs
        ClassToInstanceMap<RpcService> map = getDrivers(deviceType);
        
        Set<Class<? extends RpcService>> driverTypes = map.keySet();
        
        for (Class<? extends RpcService> clazz: driverTypes) {
            BindingAwareBroker.RoutedRpcRegistration<RpcService> registration = registerRpcService(clazz, map.getInstance(clazz), path);
            if(registration != null) {
                list.add(registration);
            }
            else {
                // TODO: do we need a onRegistrationFailed() extension?
            }
        }
        return list;    
	}
	
	/***
     * Registers a routed RPC service (aka driver)
     *
     * @param driverInterface The RPC service interface
     * @param driverImplementation The RPC service implementation
     * @param path The path to the node which the RPCs apply to
     * @return
     */
	private BindingAwareBroker.RoutedRpcRegistration<RpcService> registerRpcService(Class<? extends RpcService> driverInterface, RpcService driverImplementation, InstanceIdentifier<Node> path) {
        LOG.info("Registering RPC {} as {}", driverImplementation, driverInterface);

        BindingAwareBroker.RoutedRpcRegistration<RpcService> registration = null;
        try {
            registration = rpcRegistry.addRoutedRpcImplementation((Class<RpcService>) driverInterface, driverImplementation);
            registration.registerPath(NodeContext.class, path);
        } catch (IllegalStateException e) {
            LOG.info("Failed to register {} as {}", driverImplementation, driverInterface, e);
        }
        return registration;
    }
	
	private void handleDataUpdated(Map<InstanceIdentifier<?>, DataObject> updatedData) {
        Preconditions.checkNotNull(updatedData);
        if(!updatedData.isEmpty()) {
            LOG.info("Data was updated ({})", updatedData.size());
        }
    }
	
	private void handleDataRemoved(Set<InstanceIdentifier<?>> removedPaths) {
        Preconditions.checkNotNull(removedPaths);
        if(!removedPaths.isEmpty()) {
            LOG.info("Data was removed ({})", removedPaths.size());

            // iid will be of the form \nodes\node[id="<uuid>"]\devicetype
            for (InstanceIdentifier<?> path : removedPaths) {
                LOG.info("Path removed: {}", path);
                unregisterRpcs(path);
            }
        }
    }
	
	private void unregisterRpcs(InstanceIdentifier<?> path) {
        List<BindingAwareBroker.RoutedRpcRegistration<RpcService>> routedRpcRegistrations = rpcRegistrations.get(path);
        if(routedRpcRegistrations != null) {
            LOG.info("Unregistering all RPCs for path: {}", path);
            for (BindingAwareBroker.RoutedRpcRegistration<RpcService> routedRpcRegistration : routedRpcRegistrations) {
                LOG.info("Unregistering RPC implementation {} for {} ", routedRpcRegistration.getInstance(), routedRpcRegistration.getServiceType());
                routedRpcRegistration.close();
            }
            rpcRegistrations.remove(path);
        }
    }
	
}
