/*
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.didm.mininet;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.AdjustFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.AdjustFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.AdjustFlowOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.OpenflowFeatureService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.adjust.flow.output.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.adjust.flow.output.FlowBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.ArrayList;

/**
 * The mininet OF driver does the following:
 *
 * 1. listen for node added/removed in inventory (future: filtered by device type)
 * 2. when a mininet node is added, register the routed RPCs (other driver types may register as DCLs for a feature such as vlan)
 * 3. when a mininet node is removed, close the RPC registration (and/or DCLs for other driver types)
 */
public class OpenFlowDeviceDriver implements OpenflowFeatureService  {
    private static final Logger LOG = LoggerFactory.getLogger(OpenFlowDeviceDriver.class);

    @Override
    public Future<RpcResult<AdjustFlowOutput>> adjustFlow(AdjustFlowInput input) {
        List<Flow> adjustedFlows = new ArrayList<Flow>();

        LOG.info("Mininet adjustFlow");

        // TODO: finish this method, but for now just return the same flow that was received
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.adjust.flow.input.Flow flow = input.getFlow();

        adjustedFlows.add(new FlowBuilder(flow).build());
        AdjustFlowOutputBuilder outputBuilder = new AdjustFlowOutputBuilder();
        outputBuilder.setFlow(adjustedFlows);
        AdjustFlowOutput rpcResultType = outputBuilder.build();
        return Futures.immediateFuture(RpcResultBuilder
                .<AdjustFlowOutput>status(true).withResult(rpcResultType).build());

    }

}
