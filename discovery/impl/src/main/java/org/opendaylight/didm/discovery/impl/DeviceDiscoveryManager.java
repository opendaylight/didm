package org.opendaylight.didm.discovery.impl;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class DeviceDiscoveryManager implements DataChangeListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceDiscoveryManager.class);
    private static final InstanceIdentifier<Node> NODE_IID = InstanceIdentifier.builder(Nodes.class).child(Node.class)
            .build();

    private final DataBroker dataBroker;
    @SuppressWarnings("unused")
    private final RpcProviderRegistry rpcRegistry;
    private ListenerRegistration<DataChangeListener> dataChangeListenerRegistration = null;

    public DeviceDiscoveryManager(DataBroker dataBrokerDependency, RpcProviderRegistry rpcRegistryDependency) {
        this.dataBroker = Preconditions.checkNotNull(dataBrokerDependency);
        this.rpcRegistry = Preconditions.checkNotNull(rpcRegistryDependency);

        dataChangeListenerRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                NODE_IID, this, AsyncDataBroker.DataChangeScope.BASE);
        if (dataChangeListenerRegistration == null) {
            LOG.error("Failed to register onDataChanged Listener");
        }
    }

    @Override
    public void close() throws IOException {
        if (dataChangeListenerRegistration != null) {
            LOG.error("closing onDataChanged listener registration");
            dataChangeListenerRegistration.close();
            dataChangeListenerRegistration = null;
        }
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> data) {
        handleNodeCreated(data.getCreatedData());
        handleNodeUpdated(data.getUpdatedData());
        handleNodeRemoved(data.getRemovedPaths());
    }

    private void handleNodeRemoved(Set<InstanceIdentifier<?>> removedPaths) {
        if (!removedPaths.isEmpty()) {
            // TODO: implement
        }
    }

    private void handleNodeUpdated(Map<InstanceIdentifier<?>, DataObject> updatedData) {
        if (!updatedData.isEmpty()) {
            // TODO: implement
        }
    }

    private void handleNodeCreated(Map<InstanceIdentifier<?>, DataObject> createdData) {
        if (!createdData.isEmpty()) {
            // TODO: implement
        }
    }
}
