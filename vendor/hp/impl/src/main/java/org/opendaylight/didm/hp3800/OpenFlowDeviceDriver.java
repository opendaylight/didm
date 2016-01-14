/*
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.didm.hp3800;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.ArrayList;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.didm.flowmod.FlowModChassisV2;
import org.opendaylight.didm.tools.utils.DriverUtil;
import org.opendaylight.didm.tools.utils.FlowCreator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.AdjustFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.AdjustFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.AdjustFlowOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.OpenflowFeatureService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.adjust.flow.output.FlowBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.util.concurrent.Futures;


/**
 * The HP 3800 OF driver does the following:
 *
 * 1. listen for node added/removed in inventory (future: filtered by device type)
 * 2. when a HP 3800 node is added, register the routed RPCs (other driver types may register as DCLs for a feature such as vlan)
 * 3. when a HP 3800 node is removed, close the RPC registration (and/or DCLs for other driver types)
 */
public class OpenFlowDeviceDriver implements OpenflowFeatureService {
    private static final Logger LOG = LoggerFactory.getLogger(OpenFlowDeviceDriver.class);
    DataBroker dataBroker;
    private final RpcProviderRegistry rpcRegistry;


    public OpenFlowDeviceDriver(DataBroker dataBroker, RpcProviderRegistry rpcRegistry) {
        this.dataBroker = dataBroker;
        this.rpcRegistry = rpcRegistry;
    }

    @Override
    public Future<RpcResult<AdjustFlowOutput>> adjustFlow(AdjustFlowInput input) {
        Set<Flow> adjustedFlows = new HashSet<Flow>();
        LOG.info("HP 3800 adjustFlow");
        NodeRef nodeRef = input.getNode();
        InstanceIdentifier<Node> nodePath = (InstanceIdentifier<Node>)nodeRef.getValue();
        FlowModChassisV2 flowModDriver =  new FlowModChassisV2(nodePath, dataBroker);

        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.adjust.flow.input.Flow flow = input.getFlow();
        org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow fb = new org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder(flow).build();
        adjustedFlows = flowModDriver.adjustFlowMod(fb);
        //Test ARP flow using flow creator test code
        //FlowCreator flowCreator = new FlowCreator();
        //Flow arpFlow = flowCreator.buildArpFlowMod();
        //adjustedFlows = flowModDriver.adjustFlowMod(arpFlow);

        //Push flows to device
        SalFlowService salFlowService = rpcRegistry.getRpcService(SalFlowService.class);
        DriverUtil.install_flows(salFlowService, adjustedFlows, nodePath);

        //Send the output flow to the RPC listener
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.adjust.flow.output.Flow> adjustedOutputFlows = new ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.adjust.flow.output.Flow>();
        for(Flow oFflow : adjustedFlows){
            adjustedOutputFlows.add(new FlowBuilder(oFflow).build());
        }
        AdjustFlowOutputBuilder outputBuilder = new AdjustFlowOutputBuilder();
        outputBuilder.setFlow(adjustedOutputFlows);
        AdjustFlowOutput rpcResultType = outputBuilder.build();
        return Futures.immediateFuture(RpcResultBuilder
                .<AdjustFlowOutput>status(true).withResult(rpcResultType).build());
    }
}
