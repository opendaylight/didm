/*
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.didm.tools.utils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.BucketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class DriverUtil {

    private static final Logger LOG = LoggerFactory
            .getLogger(DriverUtil.class);
    private static AtomicLong flowIdInc = new AtomicLong();
    private static AtomicLong keyInc = new AtomicLong();
     /**
      * This utility method installs the flows to the switch.
      * @param flowService reference of SalFlowService.
      * @param flows Collection of flows to be installed.
      * @param node Flows to be deleted from the node.
     */

     public static void install_flows(SalFlowService flowService, Collection<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> flows, InstanceIdentifier<Node> node) {
         for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow flow : flows) {
             NodeConnectorRef nodeConnectorRef = new NodeConnectorRef(node);
             // create the input for the addFlow
             TableKey flowTableKey = new TableKey(flow.getTableId());
             // create the flow path
             InstanceIdentifier<Flow> flowPath = buildFlowPath(nodeConnectorRef, flowTableKey);
             final InstanceIdentifier<Table> tableInstanceId = flowPath.<Table>firstIdentifierOf(Table.class);
             final InstanceIdentifier<Node> nodeInstanceId = flowPath.<Node>firstIdentifierOf(Node.class);

             // need to rebuild the flow of the correct type to send to salFlowService
             FlowBuilder flowBuilder = new FlowBuilder(flow);
             String id = String.valueOf(keyInc.getAndIncrement());
             FlowKey key = new FlowKey(new FlowId(id));
             flowBuilder.setKey(key);
             flowBuilder.setId(new FlowId(id));
             Flow flowToSend = flowBuilder.build();

             final AddFlowInputBuilder builder = new AddFlowInputBuilder(flowToSend);
             builder.setNode(new NodeRef(nodeInstanceId));
             builder.setFlowRef(new FlowRef(flowPath));
             builder.setFlowTable(new FlowTableRef(tableInstanceId));
             builder.setTransactionUri(new Uri(flowToSend.getId().getValue()));

             final Future<RpcResult<AddFlowOutput>> rpcResultFuture =  flowService.addFlow(builder.build());
             try{
             if(rpcResultFuture != null){
                 final RpcResult<?> addFlowOutputRpcResult = rpcResultFuture.get();
                 if(addFlowOutputRpcResult != null)
                     if (addFlowOutputRpcResult.isSuccessful())
                         LOG.info("Pushed the flow successfully", flow.getFlowName());

             }
             }catch (Exception ex){
                 LOG.error("Exception while pushing flow to the device", ex.getMessage());
             }
         }

     }

     private static InstanceIdentifier<Flow> buildFlowPath(NodeConnectorRef nodeConnectorRef, TableKey flowTableKey) {
         // generate unique flow key
         FlowId flowId = new FlowId(String.valueOf(flowIdInc.getAndIncrement()));
         FlowKey flowKey = new FlowKey(flowId);

         return generateFlowInstanceIdentifier(nodeConnectorRef, flowTableKey, flowKey);
     }

     private static InstanceIdentifier<Flow> generateFlowInstanceIdentifier(final NodeConnectorRef nodeConnectorRef,
         final TableKey flowTableKey,
         final FlowKey flowKey) {
            return generateFlowTableInstanceIdentifier(nodeConnectorRef, flowTableKey).child(Flow.class, flowKey);
     }
     private static InstanceIdentifier<Table> generateFlowTableInstanceIdentifier(final NodeConnectorRef nodeConnectorRef, final TableKey flowTableKey) {
         return generateNodeInstanceIdentifier(nodeConnectorRef).builder()
             .augmentation(FlowCapableNode.class)
             .child(Table.class, flowTableKey)
             .build();
     }
     private static InstanceIdentifier<Node> generateNodeInstanceIdentifier(final NodeConnectorRef nodeConnectorRef) {
         return nodeConnectorRef.getValue().firstIdentifierOf(Node.class);
     }

     /**
      * This utility method deletes the flows as per the match.
      * @param flowService reference of SalFlowService.
      * @param flows Collection of flows to be deleted.
      * @param node Flows to be deleted from the node.
     */
     public static void deleteFlows(SalFlowService flowService,
          Collection<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> flows,
          InstanceIdentifier<Node> node) {

          for(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow flow : flows) {
               RemoveFlowInputBuilder inputBuilder = new RemoveFlowInputBuilder()
                                                     .setMatch(flow.getMatch())
                                                     .setTableId(flow.getTableId())
                                                     .setNode(new NodeRef(node));
               flowService.removeFlow(inputBuilder.build());

          }
     }

     /**
      * This utility method adds the group entry to the group table.
      * @param salGrpService instance of sal group service.
      * @param actions List of action to be included in the group bucket.
      * @param nodeID node identifier to add the group entry.
      * @param grpID Group entry will be identified by the this ID.
      */
      public static void addGroup(SalGroupService salGrpService,
                                           List<Action> actions,
                                           InstanceIdentifier<Node> nodeID,
                                           Integer grpID) {

          AddGroupInputBuilder grpInputBuilder = new AddGroupInputBuilder();

          BucketBuilder buckerBuilder = new BucketBuilder();
          buckerBuilder.setAction(actions)
                       .setBucketId(new BucketId(grpID.longValue()));

          grpInputBuilder.setBuckets(new BucketsBuilder()
                                         .setBucket(ImmutableList.of(buckerBuilder.build()))
                                         .build())
                                         .setNode(new NodeRef(nodeID))
                                         .setGroupRef(new GroupRef(nodeID))
                                         .setGroupId(new GroupId(Long.valueOf(grpID.longValue())))
                                         .setGroupType(GroupTypes.GroupIndirect)
                                         .setGroupName("Foo");

          AddGroupInput input = grpInputBuilder.build();
          salGrpService.addGroup(input);
      }
}

