/*
 * (c) Copyright 2015 Hewlett-Packard Development Company, L.P.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.didm.drivertest;


import com.google.common.base.Optional;
import com.google.common.util.concurrent.*;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.didm.drivertest.rev151015.DidmDriverTestService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.didm.drivertest.rev151015.CallDriversInput;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeContextRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import java.util.concurrent.Future;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.didm.api.openflow.rev151015.AdjustFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.didm.api.openflow.rev151015.AdjustFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.didm.api.openflow.rev151015.AdjustFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.didm.api.openflow.rev151015.OpenflowFeatureService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.didm.tools.utils.FlowCreator;


public class DriverTest implements DidmDriverTestService, AutoCloseable {
	private static final Logger LOG = LoggerFactory.getLogger(DriverTest.class);
	
	private final DataBroker dataBroker;
	private final RpcProviderRegistry rpcProviderRegistry;
	private final BindingAwareBroker.RpcRegistration<DidmDriverTestService> rpcRegistration;

	public DriverTest(DataBroker dataBroker, RpcProviderRegistry rpcProviderRegistry)
    {
        this.dataBroker = dataBroker;
        this.rpcProviderRegistry = rpcProviderRegistry;

        // register as the test RPC implementation
        rpcRegistration = rpcProviderRegistry.addRpcImplementation(DidmDriverTestService.class, this);
    }
	
	@Override
    public Future<RpcResult<Void>> callDrivers(CallDriversInput input) {
		LOG.error("callDrivers called.");
		
		 // create path to the node
        final InstanceIdentifier<Node> path = createPath(input.getNodeId());

        ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();

        // read the node
        CheckedFuture<Optional<Node>, ReadFailedException> dataFuture = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, path);
		
        // chain the future so we can call the RPC on the read node.
        ListenableFuture<RpcResult<Void>> transform = Futures.transform(dataFuture, new AsyncFunction<Optional<Node>, RpcResult<Void>>() {
            @Override
            public ListenableFuture<RpcResult<Void>> apply(Optional<Node> result) throws Exception {
                LOG.error("Read completed.");

                if (result.isPresent()) {
                    OpenflowFeatureService rpcService = rpcProviderRegistry.getRpcService(OpenflowFeatureService.class);

                    // create a flow
                    FlowCreator fc = new FlowCreator();
                    Flow flow = fc.buildArpFlowMod();

                    NodeRef ref = new NodeRef(path);
                    AdjustFlowInput input = new AdjustFlowInputBuilder()
                            .setNode(ref)
                            .setFlow(new org.opendaylight.yang.gen.v1.urn.opendaylight.didm.api.openflow.rev141114.adjust.flow.input.FlowBuilder(flow).build()).build();

                    Future<RpcResult<AdjustFlowOutput>> adjustFlowFuture = rpcService.adjustFlow(input);
                    if (adjustFlowFuture != null) {
                        adjustFlowFuture.get(); // TODO: shouldn't have to call get() here.
                    }

                } else {
                    throw new Exception("node not found");
                }

                return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
            }
        });
        
     // add a callback so we can log any exceptions for the read or RPC call
        Futures.addCallback(transform, new FutureCallback<RpcResult<Void>>() {
            @Override
            public void onSuccess(RpcResult<Void> result) {}

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Unable to execute RPC for path: {}", path);
            }
        });

        return transform;
	}
	
	private static InstanceIdentifier<Node> createPath(String nodeId) {
        return createPath(new NodeKey(new NodeId(nodeId)));
    }

    private static InstanceIdentifier<Node> createPath(NodeKey nodeKey) {
        return InstanceIdentifier.builder(Nodes.class).child(Node.class, nodeKey).build();
    }
	
	
	@Override
	public void close() throws Exception {
		if(rpcRegistration != null) rpcRegistration.close();		
	}

}
