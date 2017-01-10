/*
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.didm.identification.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.DeviceTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.device.types.DeviceTypeInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.rev150202.DeviceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.rev150202.DeviceTypeBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// TODO: maintain closed state
public class DeviceIdentificationManager implements DataChangeListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceIdentificationManager.class);
    private static final InstanceIdentifier<Node> NODE_IID = InstanceIdentifier.builder(Nodes.class).child(Node.class).build();
    private static final InstanceIdentifier<DeviceTypes> DEVICE_TYPES_IID = InstanceIdentifier.builder(DeviceTypes.class).build();
    private static final ScheduledExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(1));
    private static final String DEFAULT_OPENFLOW_SWITCH = "Default Openflow Switch";
    private static final String DEFAULT_SWITCH = "Default Switch";

    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;
    private ListenerRegistration<DataChangeListener> dataChangeListenerRegistration;

    public DeviceIdentificationManager(DataBroker dataBroker, RpcProviderRegistry rpcProviderRegistry) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.rpcProviderRegistry = Preconditions.checkNotNull(rpcProviderRegistry);

        dataChangeListenerRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, NODE_IID, this, AsyncDataBroker.DataChangeScope.BASE);
        if (dataChangeListenerRegistration != null) {
             LOG.info("Listener registered");
        }
        else {

            LOG.error("Failed to register onDataChanged Listener");
        }
    }

    private List<DeviceTypeInfo> readDeviceTypeInfoFromMdsalDataStore() {
        ReadTransaction readTx = dataBroker.newReadOnlyTransaction();

        try {
            Optional<DeviceTypes> data = readTx.read(LogicalDatastoreType.CONFIGURATION, DEVICE_TYPES_IID).get();

            if (data.isPresent()) {
                DeviceTypes devTypes = data.get();
                return devTypes.getDeviceTypeInfo();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to read Device Info from data store", e);
        }
        return new ArrayList<>(0);
    }

    @Override
    public void close() throws Exception {
        if(dataChangeListenerRegistration != null) {
            LOG.info("closing onDataChanged listener registration");
            dataChangeListenerRegistration.close();
            dataChangeListenerRegistration = null;
        }
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        LOG.info("data change event");

        handleDataCreated(change.getCreatedData());
        handleDataUpdated(change.getUpdatedData());
        handleDataRemoved(change.getRemovedPaths());
    }

    /***
     * Looks through all new devicetype elements and calls registerDrivers for any devicetype supported by this provider
     * @param createdData map of paths to data
     */
    private void handleDataCreated(Map<InstanceIdentifier<?>, DataObject> createdData) {
        Preconditions.checkNotNull(createdData);
        if(!createdData.isEmpty()) {
            // TODO: put this on a new thread, or make the algorithm below fully async
            for (Map.Entry<InstanceIdentifier<?>, DataObject> dataObjectEntry : createdData.entrySet()) {
                final InstanceIdentifier<Node> path = (InstanceIdentifier<Node>) dataObjectEntry.getKey();

                // sleep 250ms and then re-read the Node information to give OF a change to update with FlowCapable
                // TODO: Figure out why FlowCapableNode is attached later. Who adds the original Node?
                Future<Void> submit = executorService.schedule(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                        ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
                        final CheckedFuture<Optional<Node>, ReadFailedException> readFuture = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, path);
                        Futures.addCallback(readFuture, new FutureCallback<Optional<Node>>() {
                            @Override
                            public void onSuccess(Optional<Node> result) {
                                if (result.isPresent()) {
                                    identifyDevice(path, result.get());
                                } else {
                                    LOG.error("Read succeeded, node doesn't exist: {}", path);
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                LOG.error("Failed to read Node: {}", path, t);
                            }
                        });

                        return null;
                        }
                    }, 250, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void identifyDevice(final InstanceIdentifier<Node> path, Node node) {
        LOG.debug("Attempting to identify '{}'", node.getId().toString());
        String hardware = null;
        String manufacturer = null;
        String serialNumber = null;
        String software = null;
        String sysOid = null;

        LOG.info("Read updated node");

        // 1) check for OF match
        FlowCapableNode flowCapableNode = node.getAugmentation(FlowCapableNode.class);
        if (flowCapableNode != null) {
            // TODO: this needs to register for data change on hardware or something because it's really still empty at this point

            hardware = flowCapableNode.getHardware();
            manufacturer = flowCapableNode.getManufacturer();
            serialNumber = flowCapableNode.getSerialNumber();
            software = flowCapableNode.getSoftware();

            // ***************************************************
                // TODO:
            // This method needs to be written to try OF info first,
            // if that fails, then try sysOid,  If that fails, then
            // assign unknown for default switch.
                // ****************************************************
                if (flowCapableNode.getIpAddress() != null) {
                        IpAddress ip;
                        ip = flowCapableNode.getIpAddress();
                        String ipStr = ip.getIpv4Address().getValue();

                    FetchSysOid fetchSysOid = new FetchSysOid(rpcProviderRegistry);
                    LOG.info("IpAddres in string format: " + ipStr);
                    sysOid = fetchSysOid.fetch(ipStr);
                    if (sysOid == null) {
                        LOG.info("SNMP sysOid could not be obtained");
                    }
                    else {
                        LOG.info("Found SNMP sysOid: {}", sysOid);
                    }
                }

            LOG.info("Found new FlowCapable node (\"{}\", \"{}\", \"{}\", \"{}\", \"{}\")",
                        hardware, manufacturer, serialNumber, software, ((sysOid == null)?"No SysOid":sysOid));
        } else {
                // TODO: Write the code to get the IP address for a non-OF device.
                //       We still need to define how we get the IP address for a
                //       non-OF device
        }
        // 2) check for sysOID match
        // read all device type info from the data store and use it to look for a match
        List<DeviceTypeInfo> dtiInfoList = readDeviceTypeInfoFromMdsalDataStore();
        for(DeviceTypeInfo dti: dtiInfoList) {
                // first check sysoid
                if (sysOid != null && !dti.getSysoid().isEmpty()) {
                        for (String sysObjectId : dti.getSysoid()) {
                                if (sysOid.equals(sysObjectId)) {
                                        setDeviceType(dti.getDeviceTypeName(), path);
                                return;
                                }
                        }
                }

                // next check for match on Openflow info
                if (hardware != null && !dti.getOpenflowHardware().isEmpty() &&
                                manufacturer != null && (manufacturer.equals(dti.getOpenflowManufacturer())) ) {
                        for (String hw : dti.getOpenflowHardware()) {
                                if (hardware.equals(hw)) {
                                        setDeviceType(dti.getDeviceTypeName(), path);
                                return;
                                }
                        }
                }
        }

        // 3) cannot identify device type with OF description info or sysOid
        if (hardware != null) {
                setDeviceType(DEFAULT_OPENFLOW_SWITCH, path);
        }
        else {
                setDeviceType(DEFAULT_SWITCH, path);
        }

    }

    /***
     * No-op
     * @param updatedData
     */
    private void handleDataUpdated(Map<InstanceIdentifier<?>, DataObject> updatedData) {
        Preconditions.checkNotNull(updatedData);
        if(!updatedData.isEmpty()) {
            LOG.info("{} Node(s) updated", updatedData.size());
        }
    }

    /***
     * No-op
     * @param removedPaths paths to the deviceinfo elements that were removed
     */
    private void handleDataRemoved(Set<InstanceIdentifier<?>> removedPaths) {
        Preconditions.checkNotNull(removedPaths);
        if(!removedPaths.isEmpty()) {
            LOG.info("{} Node(s) removed", removedPaths.size());
        }
    }

    private void setDeviceType(String deviceType, InstanceIdentifier<Node> path) {
        final InstanceIdentifier<DeviceType> deviceTypePath = path.augmentation(DeviceType.class);
        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();

        tx.merge(LogicalDatastoreType.OPERATIONAL, deviceTypePath, new DeviceTypeBuilder().setDeviceType(deviceType).build());

        LOG.debug("Setting node '{}' device type to '{}'", path, deviceType);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = tx.submit();

        // chain the result of the write with the expected rpc future.
        ListenableFuture<RpcResult<Void>> transform = Futures.transformAsync(submitFuture, new AsyncFunction<Void, RpcResult<Void>>() {
            @Override
            public ListenableFuture<RpcResult<Void>> apply(Void result) throws Exception {
                return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
            }
        });

        // add a callback so we can log any exceptions on the write attempt
        Futures.addCallback(submitFuture, new FutureCallback<Object>() {
            @Override
            public void onSuccess(Object result) {}

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Failed to write DeviceType to: {}", deviceTypePath, t);
            }
        });

        try {
            transform.get();
        } catch (Exception e) {
            LOG.error("Failed to write DeviceType to path: {}", path, e);
        }
    }

}
