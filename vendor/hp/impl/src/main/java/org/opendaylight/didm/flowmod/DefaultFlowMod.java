/*
 * Copyright (c) 2015 Hewlett Packard Enterprise Development LP and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.didm.flowmod;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.opendaylight.didm.tools.utils.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.openflowplugin.api.openflow.md.util.OpenflowVersion;
import org.opendaylight.openflowplugin.openflow.md.core.sal.convertor.match.MatchConvertorImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.TunnelIpv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.SctpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.MatchField;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.OxmMatchType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.entries.grouping.MatchEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.features.TableFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.features.table.features.TableProperties;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.features.table.features.table.properties.TableFeatureProperties;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.TableFeaturePropType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.NextTableMiss;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.match.MatchSetfield;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.set.field.match.SetFieldMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.EthSrc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.EthDst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.EthType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.InPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.InPhyPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.VlanVid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.VlanPcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.IpDscp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.IpEcn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.IpProto;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Ipv4Src;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Ipv4Dst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Ipv6Src;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Ipv6Dst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Ipv6Flabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Ipv6NdTarget;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Ipv6NdSll;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Ipv6NdTll;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Ipv6Exthdr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.ArpSha;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.ArpSpa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.ArpTha;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.ArpTpa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.ArpOp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TunnelIpv4Src;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TunnelIpv4Dst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.SctpSrc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.SctpDst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TcpSrc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TcpDst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.UdpSrc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.UdpDst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TcpFlag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Icmpv4Code;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Icmpv4Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Icmpv6Code;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Icmpv6Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Metadata;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.MplsBos;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.MplsTc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.PbbIsid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ClearActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.MeterCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.next.table.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.next.table.miss.TablesMiss;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.apply.actions.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.apply.actions.miss.ApplyActionsMiss;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.write.actions.WriteActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.write.actions.miss.WriteActionsMiss;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.ControllerActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.CopyTtlInCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.CopyTtlOutCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DecMplsTtlCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DecNwTtlCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.FloodActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.FloodAllActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.HwPathActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.LoopbackActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopMplsActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopPbbActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushMplsActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushPbbActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlTypeActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetMplsTtlActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNextHopActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwDstActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwSrcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwTosActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwTtlActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetQueueActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpDstActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpSrcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanCfiActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanIdActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanPcpActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.StripVlanActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SwPathActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetField;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

public class DefaultFlowMod {

    protected static final String E_FLOW_UNSUPPORTED = "\u3053\u306e\u30d5\u30ed\u30fc {} \u306f\u30c7\u30d0\u30a4\u30b9\u30bf\u30a4\u30d7 {} \u3067\u306f\u30b5\u30dd\u30fc\u30c8\u3055\u308c\u3066\u3044\u307e\u305b\u3093\u3002";
    protected static final String E_FLOW_DUPLICATE = "\u3053\u306e\u30d5\u30ed\u30fc {} \u306f\u3059\u3067\u306b\u30c7\u30d0\u30a4\u30b9\u306b\u30d7\u30c3\u30b7\u30e5\u3055\u308c\u305f\u30c7\u30d5\u30a9\u30eb\u30c8\u30d5\u30ed\u30fc\u306e\u8907\u88fd\u3067\u3059\u3002";
    protected static final String E_FLOW_TABLE_MISS = "\u30c6\u30fc\u30d6\u30eb {} \u306f\u30c7\u30d0\u30a4\u30b9 {} (\u30bf\u30a4\u30d7 {})\u306e\u30c6\u30fc\u30d6\u30eb\u30df\u30b9\u69cb\u6210\u3092\u30b5\u30dd\u30fc\u30c8\u3057\u3066\u3044\u307e\u305b\u3093\u3002";
    protected static final String MSG_FLOW_SPLIT = "Flow {} is being split between table {} and {}";
    protected static final int FLOW_IDLE_TIMEOUT = 0;
    protected static final int FLOW_HARD_TIMEOUT = 0;
    protected static final int DEFAULT_PRIORITY = 0;
    protected static final long DEFAULT_COOKIE = 0xffff000000000000L;
    protected static final int SRC_MAC_GRP_TABLE = 40;
    protected static final int DST_MAC_GRP_TABLE = 41;
    protected static final Short BASE_TABLE = 0;
    private static final String OF_URI_PREFIX = "openflow:";

    private static final Logger LOG = LoggerFactory
            .getLogger(DefaultFlowMod.class);
    private InstanceIdentifier<Node> nodePath;
    DataBroker dataBroker;

    List<Table> tables = null;
    ProtocolVersion version = null;
    boolean hybridMode = true;
    BigInteger dataPathId;
    FlowCapableNode flowCapableNode;
    Node node;

    /*
     * Constructor
     */
    public DefaultFlowMod(InstanceIdentifier<Node> nodePath,
            DataBroker dataBroker) {
        this.nodePath = nodePath;
        this.dataBroker = dataBroker;
        this.tables = getTableList(nodePath);
        this.dataPathId = getDataPathId(nodePath);
        this.node = DefaultFlowMod.getDataObject(
                dataBroker.newReadOnlyTransaction(), nodePath);
        this.flowCapableNode = node.getAugmentation(FlowCapableNode.class);

        // TESTING: ********************************
        // FlowCreator flowCreator = new FlowCreator();
        // Flow flow = flowCreator.buildArpFlowMod();

        // TableFeatureCreator tableCreator = new TableFeatureCreator();
        // this.tables = tableCreator.create3800Table();

        // Match match = flow.getMatch();
        // Table mfTable = getTableForMatch(match);
        // Table insTable = getTableIdForInstructions(flow);
        // TESTING: end of testing code *************
    }

    /* InventoryDataServiceUtil.getDataObject() */
    protected static <T extends DataObject> T getDataObject(
            final ReadTransaction readOnlyTransaction,
            final InstanceIdentifier<T> identifier) {
        Optional<T> optionalData = null;
        try {
            optionalData = readOnlyTransaction.read(
                    LogicalDatastoreType.OPERATIONAL, identifier).get();
            if (optionalData.isPresent()) {
                return optionalData.get();
            }
        } catch (ExecutionException | InterruptedException e) {
            LOG.error("Read transaction for identifier {} failed.", identifier,
                    e);
        }
        return null;
    }

    private BigInteger getDataPathId(InstanceIdentifier<Node> nodePath) {
        Node node = DefaultFlowMod.getDataObject(
                dataBroker.newReadOnlyTransaction(), nodePath);
        NodeId nodeId = node.getId();
        String dpids = nodeId.getValue().replace(OF_URI_PREFIX, "");
        return new BigInteger(dpids);
    }

    private List<Table> getTableList(InstanceIdentifier<Node> nodePath) {
        Node node = DefaultFlowMod.getDataObject(
                dataBroker.newReadOnlyTransaction(), nodePath);
        return getTableList(node);
    }

    private List<Table> getTableList(Node node) {
        FlowCapableNode flowCapableNode = node
                .getAugmentation(FlowCapableNode.class);
        List<Table> tableList = flowCapableNode.getTable();
        Collections.sort(tableList, new Comparator<Table>() {
            @Override
            public int compare(Table o1, Table o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
        return tableList;
    }

    public void setTableFeatures(List<Table> tables, ProtocolVersion pv,
            boolean isHybrid) {
        if (tables == null || pv == null) {
            throw new NullPointerException(/* E_NULL_PARAMS */"e_null_parms");
        }
        this.tables = tables;
        version = pv;
        hybridMode = isHybrid;
    }

    /**
     * Generic flow creator with default fields.
     *
     * @param tableId
     *            of the flow
     * @return OfmMutableFlowMod of the basic flow
     */
    protected FlowBuilder createBasicFlow(Short tableId) {
        FlowBuilder fb = createAddFlow();
        if (tableId != null) {
            fb.setTableId(tableId);
        }
        return fb;
    }

    /**
     * Create a mutable ADD flow.
     *
     * @return Mutable ADD flow
     */
    protected FlowBuilder createAddFlow() {
        FlowBuilder fb = new FlowBuilder();
        fb.setBufferId(0xffffffffL);
        fb.setIdleTimeout(0);
        fb.setHardTimeout(0);
        fb.setFlags(new FlowModFlags(false, false, false, false, true));
        fb.setPriority(0);
        fb.setCookie(new FlowCookie(BigInteger.valueOf(0xffff000000000000L)));
        return fb;
    }

    protected List<Action> createNormalOutputAction() {

        List<Action> actions = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();
        OutputActionBuilder output = new OutputActionBuilder();
        output.setMaxLength(30);
        Uri value = new Uri(OutputPortValues.NORMAL.toString());
        output.setOutputNodeConnector(value);
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(
                output.build()).build());
        ab.setKey(new ActionKey(0));
        actions.add(ab.build());
        return actions;
    }

    protected InstructionsBuilder createAppyActionInstruction() {

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(createNormalOutputAction());

        // Wrap our Apply Action in an Instruction
        InstructionBuilder ib = new InstructionBuilder();
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(
                aab.build()).build());

        // Put our Instruction in a list of Instructions
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = new ArrayList<Instruction>();
        instructions.add(ib.build());
        isb.setInstruction(instructions);
        return isb;
    }

    private List<TableFeatureProperties> getTableFeatureProperties(Table table) {
        if (table.getTableFeatures().isEmpty())
            return Collections.emptyList();
        TableFeatures tableFeatures = table.getTableFeatures().get(0);
        return tableFeatures.getTableProperties().getTableFeatureProperties();
    }

    private List<Short> getNextTablesMiss(Table table) {
        for (TableFeatureProperties tableFeatureProperties : getTableFeatureProperties(table)) {
            if (tableFeatureProperties.getTableFeaturePropType() instanceof NextTableMiss) {
                NextTableMiss nextTableMiss = (NextTableMiss) tableFeatureProperties
                        .getTableFeaturePropType();
                return nextTableMiss.getTablesMiss().getTableIds();
            }
        }
        return Collections.emptyList();
    }

    /**
     * Create a match that matches all packets.
     * @param fb FlowBuilder wih flow matching default flow
     * @return Match that matches all packets
     */
    protected void matchAll(FlowBuilder fb) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.grouping.MatchBuilder mb = new org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.grouping.MatchBuilder();
        mb.setType(OxmMatchType.class);
        MatchBuilder Ofmb = MatchConvertorImpl.fromOFMatchToSALMatch(
                mb.build(), this.dataPathId, OpenflowVersion.OF13);
        fb.setMatch(Ofmb.build());
    }

    /**
     * Create a table miss flow for the given tableContext. This will be called
     * only for v1.3 flow mods and should only be called if it is expected that
     * the table context should support table misses.
     *
     * @param table
     *            of the table for which a miss flow will be created
     * @return goto flow mod
     * @throws FlowUnsupportedException
     *             if the table does not support this
     */
    protected Flow createTableMissFlow(Table table) {

        if (getNextTablesMiss(table).isEmpty()) {
            throw new FlowUnsupportedException(StringUtils.format(
                    E_FLOW_TABLE_MISS, table.getId(),
                    flowCapableNode.getIpAddress()));
        }
        FlowBuilder fb = createBasicFlow(table.getId());
        matchAll(fb);
        Instruction instr = createGotoInstruction(getNextTablesMiss(table).get(
                0));
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> tableInstructions = new ArrayList<Instruction>();
        tableInstructions.add(instr);
        fb.setInstructions(isb.build());

        return fb.build();
    }

    protected Table getTable(Short tableId) {
        List<Table> tableList = getTableList(nodePath);
        Table tableContext = null;
        for (Table table : tableList) {
            if (table.getId() == tableId)
                tableContext = table;
        }
        return tableContext;
    }

    /**
     * Wrap a default action in an instruction.
     *
     * @param tableId
     *            for which this default instruction is being created; may not
     *            be needed
     * @return Instruction representing default action
     */
    protected Instructions createDefaultInstruction(Short tableId) {
        Table table = getTable(tableId);
        if (table != null) {
            List<Short> tableIds = getNextTablesMiss(table);
            if (!tableIds.isEmpty()) {
                Instruction instr = createGotoInstruction(tableIds.get(0));
                InstructionsBuilder isb = new InstructionsBuilder();
                List<Instruction> tableInstructions = new ArrayList<Instruction>();
                tableInstructions.add(instr);
                isb.setInstruction(tableInstructions);
                return isb.build();
            }
        }
        InstructionsBuilder isb = createAppyActionInstruction();
        return isb.build();
    }

    /**
     * Returns a simple match all flow mod.
     *
     * @param tableId
     *            for flow
     * @return Set of default flow mods
     */
    protected Set<Flow> createDefaultFlow(Short tableId) {
        FlowBuilder fb = createBasicFlow(tableId);
        matchAll(fb);
        fb.setInstructions(createDefaultInstruction(tableId));
        return Sets.newHashSet(fb.build());
    }

    // Just the defaults since unknown what kind of device it is
    public Set<Flow> generateDefaultFlows() {

        Set<Flow> defaultFlows = new HashSet<Flow>();

        // Special case for mininet
        if (!tables.isEmpty())
            defaultFlows.addAll(createDefaultFlow(BASE_TABLE));
        else {
            for (Table table : tables)
                defaultFlows.addAll(createDefaultFlow(table.getId()));
        }
        return defaultFlows;
    }

    protected Flow assignToTable(Flow origFlow, Table mfTableId) {
        FlowBuilder fb = new FlowBuilder(origFlow);
        fb.setTableId(mfTableId.getId());
        return fb.build();
    }

    /**
     * Go through the list of newly added flows and make sure they are not
     * duplicated with existing flows.
     *
     * @param currentFlows
     *            represents the flows already being pushed
     * @param newFlows
     *            represents the new flows we want to add
     * @return Set of new flows that are not duplicated
     */
    protected Set<Flow> removeDuplicates(Set<Flow> currentFlows,
            Set<Flow> newFlows) {
        Set<Flow> notDuplicates = new HashSet<Flow>();
        for (Flow flow : newFlows) {
            if (!alreadyContains(flow, currentFlows)) {
                notDuplicates.add(flow);
            }
        }

        return notDuplicates;
    }

    /**
     * Does a simple comparison for nulls.
     *
     * @param first
     *            object to compare
     * @param second
     *            object to compare
     * @return true if their both null or neither null
     */
    protected boolean sameNull(Object first, Object second) {
        return (((first == null) && (second == null)) || ((first != null) && (second != null)));
    }

    /**
     * Compares only the things the flow mod adjustments care about to determine
     * if the two flows are the same.
     *
     * @param firstFlow
     *            to be compared
     * @param secondFlow
     *            to be compared
     * @return boolean indicating whether they are the same
     */
    protected boolean sameFlow(Flow firstFlow, Flow secondFlow) {

        // if one of the table ids is null and other is not, then they are
        // not the same
        if (!sameNull(firstFlow.getTableId(), secondFlow.getTableId())) {
            return false;
        }

        // if the table ids don't match, they are not the same
        if ((firstFlow.getTableId() != null)
                && (!firstFlow.getTableId().equals(secondFlow.getTableId()))) {
            return false;
        }

        // if one of the matches is null and the other is not, not the same
        if (!sameNull(firstFlow.getMatch(), secondFlow.getMatch())) {
            return false;
        }

        // if the matches aren't null, make sure they are the same
        if (firstFlow.getMatch() != null) {
            if (!firstFlow.getMatch().equals(secondFlow.getMatch())) {
                return false;
            }
        }

        // if one of the instructions is null and the others aren't, not the
        // same
        if (!sameNull(firstFlow.getInstructions(), secondFlow.getInstructions())) {
            return false;
        }

        // if they are both null, then this is equal
        if (firstFlow.getInstructions() == null) {
            return true;
        }

        // else make sure the size of each is the same
        if (firstFlow.getInstructions().getInstruction().size() != secondFlow
                .getInstructions().getInstruction().size()) {
            return false;
        }

        // then make sure the instructions match
        for (Instruction instr : firstFlow.getInstructions().getInstruction()) {
            boolean found = false;

            for (Instruction secInstr : secondFlow.getInstructions()
                    .getInstruction()) {
                if (instr.getInstruction().equals(secInstr.getInstruction())) {

                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }

    /**
     * Determines whether a single flow is already in a list of flows.
     *
     * @param flow
     *            to be examined
     * @param currentFlows
     *            to be compared against
     * @return boolean to indicate whether this flow is already there
     */
    protected boolean alreadyContains(Flow flow, Set<Flow> currentFlows) {
        for (Flow currentFlow : currentFlows) {
            if (sameFlow(flow, currentFlow)) {
                LOG.trace("Found same flow ", flow, currentFlow);
                return true;
            }
        }

        return false;
    }

    /**
     * Try to find the best table to put this flow into. Depending on which kind
     * of table it is in (if we have that knowledge), create a goto flow to
     * connect one to the other.
     *
     * @param flow
     *            being inspected
     * @param mfTableId
     *            that was best for the match
     * @param instrTableId
     *            that was best for the instruction
     * @return TableId that's the best for this flow
     * @throws FlowUnsupportedException
     *             if things go wrong
     */
    protected Table bestGuess(Flow flow, Table mfTableId, Table instrTableId) {
        // At this point, either the match fields or the instruction actions
        // can only be supported in one of the tables. As this is a best-
        // guess implementation, figure out which one supports both.
        boolean isTableMiss = (flow.getMatch() == null) ? true
                : doesMatchFieldsEmpty(flow.getMatch());
        Table bestGuess;
        if (doesTableSupportMatch(mfTableId, flow.getMatch())
                && doesTableSupportInstructions(mfTableId, flow
                        .getInstructions().getInstruction(), isTableMiss)) {
            bestGuess = mfTableId;
        } else if (doesTableSupportMatch(instrTableId, flow.getMatch())
                && doesTableSupportInstructions(instrTableId, flow
                        .getInstructions().getInstruction(), isTableMiss)) {
            bestGuess = instrTableId;
        } else {
            throw new FlowUnsupportedException(StringUtils.format(
                    E_FLOW_UNSUPPORTED, flow));
        }

        return bestGuess;
    }

    protected boolean doesMatchFieldsEmpty(Match match) {

        MatchConvertorImpl matchConvertor = new MatchConvertorImpl();
        List<MatchEntry> matchEntryList = matchConvertor.convert(match, null);
        if (matchEntryList.isEmpty())
            return true;
        return false;
    }

    public Set<Flow> adjustFlowMod(Flow origFlow) {
        Set<Flow> adjustedFlows = new HashSet<Flow>();

        // When adjusting a flow mod, make sure that the resulting flows are
        // not duplicates of the basic flows that are laid down by default
        Set<Flow> baseFlows = generateDefaultFlows();

        Table mfTable = getTableForMatch(origFlow.getMatch());
        Table insTable = getTableIdForInstructions(origFlow);

        Short mfTableId = mfTable.getId();
        Short insTableId = insTable.getId();

        // Something in this flow is not supported at all
        if (mfTableId == null || insTableId == null)
            throw new FlowUnsupportedException(StringUtils.format(
                    E_FLOW_UNSUPPORTED, origFlow, nodePath));

        // If both the match and the instructions can go straight into the
        // same table, just use that.
        if (mfTableId.equals(insTableId)) {
            adjustedFlows.add(assignToTable(origFlow, mfTable));
            return returnOrThrow(removeDuplicates(baseFlows, adjustedFlows),
                    origFlow);
        }

        // At this point, either the match fields or the instruction actions
        // can only be supported in one of the tables. As this is a best-
        // guess implementation, figure out which one supports both.
        Table bestGuess = bestGuess(origFlow, mfTable, insTable);
        adjustedFlows.add(assignToTable(origFlow, bestGuess));
        Table other = (bestGuess.getId() == mfTableId) ? insTable : mfTable;
        // TODO: review - this is dangerous: what if other > bestGuess??
        adjustedFlows.add(createGotoFlow(origFlow, other, bestGuess));
        LOG.debug(MSG_FLOW_SPLIT, origFlow, bestGuess, other);

        return returnOrThrow(removeDuplicates(baseFlows, adjustedFlows),
                origFlow);
    }

    /**
     * Create a goto instruction to a specific table.
     *
     * @param tableId
     *            to go to
     * @return Instruction that is a goto
     */
    protected Instruction createGotoInstruction(Short tableId) {
        GoToTableBuilder gb = new GoToTableBuilder();
        gb.setTableId(tableId);
        InstructionBuilder ib = new InstructionBuilder();
        ib.setInstruction(new GoToTableCaseBuilder().setGoToTable(gb.build())
                .build());
        Instruction newInstr = ib.build();
        return newInstr;
    }

    /**
     * Create a goto flow from the given firstTableId to the given
     * secondTableId. This will be called when a flow must be split between two
     * tables. It is assumed that this will only be called for Openflow version
     * 1.3 and above flow mods.
     *
     * @param flow
     *            the original FlowMod requested to send to the device
     * @param firstTable
     *            of the first table into which the Goto will be placed
     * @param secondTable
     *            of the second table to which the Goto will be pointing
     * @return FlowMod representing the goto
     */
    protected Flow createGotoFlow(Flow flow, Table firstTable, Table secondTable) {

        short firstTableId = firstTable.getId();
        // Create Goto Instruction
        Instruction newInstr = createGotoInstruction(secondTable.getId());
        InstructionsBuilder insb = new InstructionsBuilder();
        List<Instruction> hwTableInstructions = new ArrayList<Instruction>();
        hwTableInstructions.add(newInstr);
        insb.setInstruction(hwTableInstructions);

        // For the hardware table, we need to extract any match fields that
        // aren't supported
        // Clear instructions from the new flow.
        FlowBuilder fb = new FlowBuilder(flow);
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = new ArrayList<Instruction>();
        isb.setInstruction(instructions);
        fb.setInstructions(isb.build());
        fb.setTableId(firstTableId);
        fb.setInstructions(insb.build());

        // TODO: dangerous. What if newMatch becomes a matchAll?
        Match newMatch = removeInvalidMatchesForTable(flow.getMatch(),
                firstTable);
        fb.setMatch(newMatch);
        return fb.build();
    }

    /**
     * Add all the match fields if they are all valid.
     *
     * @param match
     *            from which fields will be analyzed
     * @param table
     *            to which match is destined
     * @return newly created match with valid match fields
     */
    protected List<MatchEntry> findValidMatchFields(Table table, Match match) {
        List<MatchEntry> validMatchEntries = new ArrayList<MatchEntry>();
        List<SetFieldMatch> supportedMatchList = getMatchList(table);
        MatchConvertorImpl matchConvertor = new MatchConvertorImpl();
        List<MatchEntry> matchEntryList = matchConvertor.convert(match, null);
        for (MatchEntry matchEntry : matchEntryList) {
            if (isFieldSupported(matchEntry.getOxmMatchField(),
                    supportedMatchList))
                validMatchEntries.add(matchEntry);
        }
        return validMatchEntries;
    }

    /**
     * Make sure the device allows all the match fields in the given table.
     * Remove them if needed.
     *
     * @param match
     *            to check for invalid match fields
     * @param table
     *            for which the match is targeted
     * @return Match representing valid match fields only
     */
    private Match removeInvalidMatchesForTable(Match match, Table table) {
        if (match == null) {
            return match;
        }
        List<MatchEntry> validFields = findValidMatchFields(table, match);
        MatchConvertorImpl matchConvertor = new MatchConvertorImpl();
        List<MatchEntry> matchEntryList = matchConvertor.convert(match, null);
        if (validFields.size() == matchEntryList.size()) {
            return match;
        }
        org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.grouping.MatchBuilder mbb = new org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.grouping.MatchBuilder();
        mbb.setMatchEntry(validFields);
        MatchBuilder mb = MatchConvertorImpl.fromOFMatchToSALMatch(mbb.build(),
                this.dataPathId, OpenflowVersion.OF13);

        return mb.build();
    }

    protected boolean isFieldSupported(Class<? extends MatchField> field,
            List<SetFieldMatch> supportedFields) {
        for (SetFieldMatch supportedField : supportedFields) {
            if (isFieldMatch(field, supportedField.getMatchType()))
                return true;
        }

        return false;
    }

    private boolean isFieldMatch(
            Class<? extends MatchField> field,
            Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.MatchField> matchType) {
        return field.getSimpleName().equals(matchType.getSimpleName());
    }

    protected List<SetFieldMatch> getMatchList(Table table) {
        if (table.getTableFeatures().isEmpty())
            return Collections.emptyList();
        TableFeatures tableFeatures = table.getTableFeatures().get(0);
        List<TableFeatureProperties> tableFeaturePropertiesList = tableFeatures
                .getTableProperties().getTableFeatureProperties();
        for (TableFeatureProperties tableFeatureProperties : tableFeaturePropertiesList) {
            if (tableFeatureProperties.getTableFeaturePropType() instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.Match) {
                org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.Match match = (org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.Match) tableFeatureProperties
                        .getTableFeaturePropType();
                return match.getMatchSetfield().getSetFieldMatch();
            }
        }
        return Collections.emptyList();
    }

    /**
     * Decide whether to return the set of flows or throw an exception
     * indicating that the flow list is empty.
     *
     * @param returningFlows
     *            flows wanting to be returned
     * @param flow
     *            that was to be added
     * @return Set of flow mods that aren't duplicates
     * @throws DuplicateFlowException
     *             if the list is empty
     */
    protected Set<Flow> returnOrThrow(Set<Flow> returningFlows, Flow flow) {
        if (returningFlows.isEmpty()) {
            throw new DuplicateFlowException(StringUtils.format(
                    E_FLOW_DUPLICATE, flow));
        }

        return returningFlows;
    }

    /**
     * Determine the best table for the match in the flow. "Best" is defined by
     * each implementation of this facet.
     *
     * @param match
     *            to be analyzed
     * @return Table "best" table to use
     */
    protected Table getTableForMatch(Match match) {
        for (Table table : tables) {
            // If this is a not a wildcard match, see if all the fields
            // are supported in this match
            if (doesTableSupportMatch(table, match)) {
                return table;
            }
        }
        // return null if no table supports it
        return null;
    }

    /**
     * Determines if the given table supports the match.
     *
     * @param table
     *            table being examined
     * @param match
     *            to be analyzed
     * @return boolean indicating whether the table supports the match
     */
    protected boolean doesTableSupportMatch(
            Table table,
            org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.Match match) {
        // TODO - verify that passing the model version of the Match class
        // works. This work
        // today because the flow version of Match extends from the model
        // version of match,
        // and the flow version doesn't do anything (ie, not methods defined).
        // If flow version
        // of Match changes (methods added), then the new methods won't be
        // accessible here.
        if (match == null) {
            // All tables should support no matches
            return true;
        }

        // check Ethernet matches
        if (match.getEthernetMatch() != null) {
            if (match.getEthernetMatch().getEthernetDestination() != null) {
                if (doesTableSupportMatchField(table, EthSrc.class)) {
                    return true;
                }
            }
            if (match.getEthernetMatch().getEthernetSource() != null) {
                if (doesTableSupportMatchField(table, EthDst.class)) {
                    return true;
                }
            }
            if (match.getEthernetMatch().getEthernetType() != null) {
                if (doesTableSupportMatchField(table, EthType.class)) {
                    return true;
                }
            }
        }

        // check Input Port
        if (match.getInPort() != null) {
            if (doesTableSupportMatchField(table, InPort.class)) {
                return true;
            }
        }

        // check physical input port
        if (match.getInPhyPort() != null) {
            if (doesTableSupportMatchField(table, InPhyPort.class)) {
                return true;
            }
        }

        // check vlan matches
        if (match.getVlanMatch() != null) {
            if (match.getVlanMatch().getVlanId() != null) {
                if (doesTableSupportMatchField(table, VlanVid.class)) {
                    return true;
                }
            }
            if (match.getVlanMatch().getVlanPcp() != null) {
                if (doesTableSupportMatchField(table, VlanPcp.class)) {
                    return true;
                }
            }
        }

        // check IP matches
        if (match.getIpMatch() != null) {
            if (match.getIpMatch().getIpDscp() != null) {
                if (doesTableSupportMatchField(table, IpDscp.class)) {
                    return true;
                }
            }
            if (match.getIpMatch().getIpEcn() != null) {
                if (doesTableSupportMatchField(table, IpEcn.class)) {
                    return true;
                }
            }
            if (match.getIpMatch().getIpProto() != null) {
                if (doesTableSupportMatchField(table, IpProto.class)) {
                    return true;
                }
            }
        }

        // check layer 3 matches
        Layer3Match l3m = match.getLayer3Match();
        if (l3m instanceof Ipv4Match) {
            if (((Ipv4Match) l3m).getIpv4Source() != null) {
                if (doesTableSupportMatchField(table, Ipv4Src.class)) {
                    return true;
                }
            }
            if (((Ipv4Match) l3m).getIpv4Destination() != null) {
                if (doesTableSupportMatchField(table, Ipv4Dst.class)) {
                    return true;
                }
            }
        } else if (l3m instanceof Ipv6Match) {
            if (((Ipv6Match) l3m).getIpv6Source() != null) {
                if (doesTableSupportMatchField(table, Ipv6Src.class)) {
                    return true;
                }
            }
            if (((Ipv6Match) l3m).getIpv6Destination() != null) {
                if (doesTableSupportMatchField(table, Ipv6Dst.class)) {
                    return true;
                }
            }
            if (((Ipv6Match) l3m).getIpv6Label() != null) {
                if (doesTableSupportMatchField(table, Ipv6Flabel.class)) {
                    return true;
                }
            }
            if (((Ipv6Match) l3m).getIpv6NdTarget() != null) {
                if (doesTableSupportMatchField(table, Ipv6NdTarget.class)) {
                    return true;
                }
            }
            if (((Ipv6Match) l3m).getIpv6NdSll() != null) {
                if (doesTableSupportMatchField(table, Ipv6NdSll.class)) {
                    return true;
                }
            }
            if (((Ipv6Match) l3m).getIpv6NdTll() != null) {
                if (doesTableSupportMatchField(table, Ipv6NdTll.class)) {
                    return true;
                }
            }
            if (((Ipv6Match) l3m).getIpv6ExtHeader() != null) {
                if (doesTableSupportMatchField(table, Ipv6Exthdr.class)) {
                    return true;
                }
            }
        } else if (l3m instanceof ArpMatch) {
            if (((ArpMatch) l3m).getArpSourceHardwareAddress() != null) {
                if (doesTableSupportMatchField(table, ArpSha.class)) {
                    return true;
                }
            }
            if (((ArpMatch) l3m).getArpSourceTransportAddress() != null) {
                if (doesTableSupportMatchField(table, ArpSpa.class)) {
                    return true;
                }
            }
            if (((ArpMatch) l3m).getArpTargetHardwareAddress() != null) {
                if (doesTableSupportMatchField(table, ArpTha.class)) {
                    return true;
                }
            }
            if (((ArpMatch) l3m).getArpTargetTransportAddress() != null) {
                if (doesTableSupportMatchField(table, ArpTpa.class)) {
                    return true;
                }
            }
            if (((ArpMatch) l3m).getArpOp() != null) {
                if (doesTableSupportMatchField(table, ArpOp.class)) {
                    return true;
                }
            }
        } else if (l3m instanceof TunnelIpv4Match) {
            if (((TunnelIpv4Match) l3m).getTunnelIpv4Source() != null) {
                if (doesTableSupportMatchField(table, TunnelIpv4Src.class)) {
                    return true;
                }
            }
            if (((TunnelIpv4Match) l3m).getTunnelIpv4Destination() != null) {
                if (doesTableSupportMatchField(table, TunnelIpv4Dst.class)) {
                    return true;
                }
            }
        }

        // check layer 4 matches
        Layer4Match l4m = match.getLayer4Match();
        if (l4m instanceof SctpMatch) {
            if (((SctpMatch) l4m).getSctpSourcePort() != null) {
                if (doesTableSupportMatchField(table, SctpSrc.class)) {
                    return true;
                }
            }
            if (((SctpMatch) l4m).getSctpDestinationPort() != null) {
                if (doesTableSupportMatchField(table, SctpDst.class)) {
                    return true;
                }
            }
        } else if (l4m instanceof TcpMatch) {
            if (((TcpMatch) l4m).getTcpSourcePort() != null) {
                if (doesTableSupportMatchField(table, TcpSrc.class)) {
                    return true;
                }
            }
            if (((TcpMatch) l4m).getTcpDestinationPort() != null) {
                if (doesTableSupportMatchField(table, TcpDst.class)) {
                    return true;
                }
            }
        } else if (l4m instanceof UdpMatch) {
            if (((UdpMatch) l4m).getUdpSourcePort() != null) {
                if (doesTableSupportMatchField(table, UdpSrc.class)) {
                    return true;
                }
            }
            if (((UdpMatch) l4m).getUdpDestinationPort() != null) {
                if (doesTableSupportMatchField(table, UdpDst.class)) {
                    return true;
                }
            }
        }

        // check TCP flag match
        if (match.getTcpFlagMatch() != null) {
            if (match.getTcpFlagMatch().getTcpFlag() != null) {
                if (doesTableSupportMatchField(table, TcpFlag.class)) {
                    return true;
                }
            }
        }

        // check ICMP V4 matches
        if (match.getIcmpv4Match() != null) {
            if (match.getIcmpv4Match().getIcmpv4Code() != null) {
                if (doesTableSupportMatchField(table, Icmpv4Code.class)) {
                    return true;
                }
            }
            if (match.getIcmpv4Match().getIcmpv4Type() != null) {
                if (doesTableSupportMatchField(table, Icmpv4Type.class)) {
                    return true;
                }
            }
        }

        // check ICMP V4 matches
        if (match.getIcmpv6Match() != null) {
            if (match.getIcmpv6Match().getIcmpv6Code() != null) {
                if (doesTableSupportMatchField(table, Icmpv6Code.class)) {
                    return true;
                }
            }
            if (match.getIcmpv6Match().getIcmpv6Type() != null) {
                if (doesTableSupportMatchField(table, Icmpv6Type.class)) {
                    return true;
                }
            }
        }

        // check Tunnel ID match
        if (match.getTunnel() != null) {
            if (match.getTunnel().getTunnelId() != null) {
                if (doesTableSupportMatchField(table, TunnelId.class)) {
                    return true;
                }
            }
        }

        // check metadata match
        if (match.getMetadata() != null) {
            if (match.getMetadata().getMetadata() != null) {
                if (doesTableSupportMatchField(table, Metadata.class)) {
                    return true;
                }
            }
        }

        // check protocol matches
        if (match.getProtocolMatchFields() != null) {
            if (match.getProtocolMatchFields().getMplsBos() != null) {
                if (doesTableSupportMatchField(table, MplsBos.class)) {
                    return true;
                }
            }
            if (match.getProtocolMatchFields().getMplsLabel() != null) {
                if (doesTableSupportMatchField(table, MplsLabel.class)) {
                    return true;
                }
            }
            if (match.getProtocolMatchFields().getMplsTc() != null) {
                if (doesTableSupportMatchField(table, MplsTc.class)) {
                    return true;
                }
            }
            if (match.getProtocolMatchFields().getPbb() != null) {
                if (doesTableSupportMatchField(table, PbbIsid.class)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean doesTableSupportMatchField(Table table, Class<?> clazz) {
        List<TableFeatures> tableFeatures = table.getTableFeatures();

        for (TableFeatures tf : tableFeatures) {
            TableProperties tableProperties = tf.getTableProperties();
            List<TableFeatureProperties> tableFeatureProperties = tableProperties
                    .getTableFeatureProperties();

            for (TableFeatureProperties tfp : tableFeatureProperties) {
                TableFeaturePropType tfpt = tfp.getTableFeaturePropType();

                if (tfpt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.Match) {
                    MatchSetfield matchSetField = ((org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.Match) tfpt)
                            .getMatchSetfield();
                    List<SetFieldMatch> setFieldMatch = null;

                    if (null != matchSetField) {
                        setFieldMatch = matchSetField.getSetFieldMatch();

                        // NOTE: the match types (eg, EthSrc.class) are defined
                        // in model-flow-base project
                        for (SetFieldMatch sfMatch : setFieldMatch) {
                            if (clazz.equals(EthSrc.class)
                                    && sfMatch.getMatchType() == EthSrc.class) {
                                return true;
                            }
                            if (clazz.equals(EthDst.class)
                                    && sfMatch.getMatchType() == EthDst.class) {
                                return true;
                            }
                            if (clazz.equals(EthType.class)
                                    && sfMatch.getMatchType() == EthType.class) {
                                return true;
                            }
                            if (clazz.equals(InPort.class)
                                    && sfMatch.getMatchType() == InPort.class) {
                                return true;
                            }
                            if (clazz.equals(InPhyPort.class)
                                    && sfMatch.getMatchType() == InPhyPort.class) {
                                return true;
                            }
                            if (clazz.equals(VlanVid.class)
                                    && sfMatch.getMatchType() == VlanVid.class) {
                                return true;
                            }
                            if (clazz.equals(VlanPcp.class)
                                    && sfMatch.getMatchType() == VlanPcp.class) {
                                return true;
                            }
                            if (clazz.equals(IpDscp.class)
                                    && sfMatch.getMatchType() == IpDscp.class) {
                                return true;
                            }
                            if (clazz.equals(IpEcn.class)
                                    && sfMatch.getMatchType() == IpEcn.class) {
                                return true;
                            }
                            if (clazz.equals(IpProto.class)
                                    && sfMatch.getMatchType() == IpProto.class) {
                                return true;
                            }
                            if (clazz.equals(Ipv4Src.class)
                                    && sfMatch.getMatchType() == Ipv4Src.class) {
                                return true;
                            }
                            if (clazz.equals(Ipv4Dst.class)
                                    && sfMatch.getMatchType() == Ipv4Dst.class) {
                                return true;
                            }
                            if (clazz.equals(Ipv6Src.class)
                                    && sfMatch.getMatchType() == Ipv6Src.class) {
                                return true;
                            }
                            if (clazz.equals(Ipv6Dst.class)
                                    && sfMatch.getMatchType() == Ipv6Dst.class) {
                                return true;
                            }
                            if (clazz.equals(Ipv6Flabel.class)
                                    && sfMatch.getMatchType() == Ipv6Flabel.class) {
                                return true;
                            }
                            if (clazz.equals(Ipv6NdTarget.class)
                                    && sfMatch.getMatchType() == Ipv6NdTarget.class) {
                                return true;
                            }
                            if (clazz.equals(Ipv6NdSll.class)
                                    && sfMatch.getMatchType() == Ipv6NdSll.class) {
                                return true;
                            }
                            if (clazz.equals(Ipv6NdTll.class)
                                    && sfMatch.getMatchType() == Ipv6NdTll.class) {
                                return true;
                            }
                            if (clazz.equals(Ipv6Exthdr.class)
                                    && sfMatch.getMatchType() == Ipv6Exthdr.class) {
                                return true;
                            }
                            if (clazz.equals(ArpSha.class)
                                    && sfMatch.getMatchType() == ArpSha.class) {
                                return true;
                            }
                            if (clazz.equals(ArpSpa.class)
                                    && sfMatch.getMatchType() == ArpSpa.class) {
                                return true;
                            }
                            if (clazz.equals(ArpTha.class)
                                    && sfMatch.getMatchType() == ArpTha.class) {
                                return true;
                            }
                            if (clazz.equals(ArpTpa.class)
                                    && sfMatch.getMatchType() == ArpTpa.class) {
                                return true;
                            }
                            if (clazz.equals(ArpOp.class)
                                    && sfMatch.getMatchType() == ArpOp.class) {
                                return true;
                            }
                            if (clazz.equals(TunnelIpv4Src.class)
                                    && sfMatch.getMatchType() == TunnelIpv4Src.class) {
                                return true;
                            }
                            if (clazz.equals(TunnelIpv4Dst.class)
                                    && sfMatch.getMatchType() == TunnelIpv4Dst.class) {
                                return true;
                            }
                            if (clazz.equals(SctpSrc.class)
                                    && sfMatch.getMatchType() == SctpSrc.class) {
                                return true;
                            }
                            if (clazz.equals(SctpDst.class)
                                    && sfMatch.getMatchType() == SctpDst.class) {
                                return true;
                            }
                            if (clazz.equals(TcpSrc.class)
                                    && sfMatch.getMatchType() == TcpSrc.class) {
                                return true;
                            }
                            if (clazz.equals(TcpDst.class)
                                    && sfMatch.getMatchType() == TcpDst.class) {
                                return true;
                            }
                            if (clazz.equals(UdpSrc.class)
                                    && sfMatch.getMatchType() == UdpSrc.class) {
                                return true;
                            }
                            if (clazz.equals(UdpDst.class)
                                    && sfMatch.getMatchType() == UdpDst.class) {
                                return true;
                            }
                            if (clazz.equals(TcpFlag.class)
                                    && sfMatch.getMatchType() == TcpFlag.class) {
                                return true;
                            }
                            if (clazz.equals(Icmpv4Code.class)
                                    && sfMatch.getMatchType() == Icmpv4Code.class) {
                                return true;
                            }
                            if (clazz.equals(Icmpv4Type.class)
                                    && sfMatch.getMatchType() == Icmpv4Type.class) {
                                return true;
                            }
                            if (clazz.equals(Icmpv6Code.class)
                                    && sfMatch.getMatchType() == Icmpv6Code.class) {
                                return true;
                            }
                            if (clazz.equals(Icmpv6Type.class)
                                    && sfMatch.getMatchType() == Icmpv6Type.class) {
                                return true;
                            }
                            if (clazz.equals(TunnelId.class)
                                    && sfMatch.getMatchType() == TunnelId.class) {
                                return true;
                            }
                            if (clazz.equals(Metadata.class)
                                    && sfMatch.getMatchType() == Metadata.class) {
                                return true;
                            }
                            if (clazz.equals(MplsBos.class)
                                    && sfMatch.getMatchType() == MplsBos.class) {
                                return true;
                            }
                            if (clazz.equals(MplsTc.class)
                                    && sfMatch.getMatchType() == MplsTc.class) {
                                return true;
                            }
                            if (clazz.equals(MplsLabel.class)
                                    && sfMatch.getMatchType() == MplsLabel.class) {
                                return true;
                            }
                            if (clazz.equals(PbbIsid.class)
                                    && sfMatch.getMatchType() == PbbIsid.class) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    protected Table getTableIdForInstructions(Flow flow) {
        boolean isTableMiss = false;
        if (flow.getMatch() == null) {
            isTableMiss = true;
        }

        // just return null if there are no instructions
        if (flow.getInstructions() == null
                || flow.getInstructions().getInstruction().isEmpty()) {
            return null;
        }

        for (Table table : tables) {
            if (doesTableSupportInstructions(table, flow.getInstructions()
                    .getInstruction(), isTableMiss)) {
                return table;
            }
        }
        // return null if no table supports it
        return null;
    }

    protected boolean doesTableSupportInstructions(Table table,
            List<Instruction> instructions, boolean isTableMiss) {
        for (Instruction instr : instructions) {

            // Goto Table
            if (instr.getInstruction() instanceof GoToTableCase) {
                if (doesTableSupportInstGotoTable(table, isTableMiss,
                        ((GoToTableCase) instr.getInstruction()).getGoToTable()
                                .getTableId())) {
                    return true;
                }
            }
            // Apply Actions
            else if (instr.getInstruction() instanceof ApplyActionsCase) {
                // debug
                org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions ap = ((ApplyActionsCase) instr
                        .getInstruction()).getApplyActions();

                if (doesTableSupportInstApplyActions(table, isTableMiss,
                        ((ApplyActionsCase) instr.getInstruction())
                                .getApplyActions().getAction())) {
                    return true;
                }
            }
            // Write Actions
            else if (instr.getInstruction() instanceof WriteActionsCase) {
                if (doesTableSupportInstWriteActions(table, isTableMiss,
                        ((WriteActionsCase) instr.getInstruction())
                                .getWriteActions().getAction())) {
                    return true;
                }
            }
            // write metadata, clear actions, meter, experimenter (note: I don't
            // see experimenter in yang model
            else if ((instr.getInstruction() instanceof MeterCase)
                    || (instr.getInstruction() instanceof WriteMetadataCase)
                    || (instr.getInstruction() instanceof ClearActionsCase)) {
                if (doesTableSupportInstruction(table, isTableMiss,
                        instr.getInstruction())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     *
     * @param table table to be checked for matching instruction
     * @param isTableMiss is this a table miss or not
     * @param instruction instruction to be matched against
     * @return boolean variable to represent if table supports instruction or not
     */
    protected boolean doesTableSupportInstruction(
            Table table,
            boolean isTableMiss,
            org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction instruction) {
        List<TableFeatures> tableFeatures = table.getTableFeatures();

        for (TableFeatures tf : tableFeatures) {
            TableProperties tableProperties = tf.getTableProperties();
            List<TableFeatureProperties> tableFeatureProperties = tableProperties
                    .getTableFeatureProperties();

            for (TableFeatureProperties tfp : tableFeatureProperties) {
                TableFeaturePropType tfpt = tfp.getTableFeaturePropType();

                if (!isTableMiss
                        && tfpt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.Instructions) {
                    List<Instruction> instList = ((org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.Instructions) tfpt)
                            .getInstructions().getInstruction();

                    for (Instruction inst : instList) {
                        if (areInstructionsEqual(instruction,
                                inst.getInstruction())) {
                            return true;
                        }
                        // if (inst.getInstruction().equals(instruction)) {
                        // return true;
                        // }
                    }
                    return false;
                }

                if (isTableMiss
                        && tfpt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.InstructionsMiss) {
                    List<Instruction> instList = ((org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.InstructionsMiss) tfpt)
                            .getInstructionsMiss().getInstruction();

                    for (Instruction inst : instList) {
                        if (areInstructionsEqual(instruction,
                                inst.getInstruction())) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        }
        return false;
    }

    private boolean areInstructionsEqual(
            org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction flowInst,
            org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction tableInst) {
        if (flowInst instanceof ClearActionsCase
                && tableInst instanceof ClearActionsCase) {
            return true;
        }
        if (flowInst instanceof MeterCase && tableInst instanceof MeterCase) {
            return true;
        }
        if (flowInst instanceof WriteMetadataCase
                && tableInst instanceof WriteMetadataCase) {
            return true;
        }
        return false;
    }

    /**
     *
     * @param table table to be checked or matching instruction
     * @param isTableMiss is this a table miss or not variable
     * @param flowmodActions
     *            contains the actions in the flow we are validating and
     *            possibly adjusting
     * @return boolean variable to represent instruction support
     */
    protected boolean doesTableSupportInstWriteActions(Table table,
            boolean isTableMiss, List<Action> flowmodActions) {
        List<TableFeatures> tableFeatures = table.getTableFeatures();

        for (TableFeatures tf : tableFeatures) {
            TableProperties tableProperties = tf.getTableProperties();
            List<TableFeatureProperties> tableFeatureProperties = tableProperties
                    .getTableFeatureProperties();

            for (TableFeatureProperties tfp : tableFeatureProperties) {
                TableFeaturePropType tfpt = tfp.getTableFeaturePropType();

                if (!isTableMiss
                        && tfpt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.WriteActions) {
                    WriteActions writeActions = ((org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.WriteActions) tfpt)
                            .getWriteActions();
                    List<Action> actionList = writeActions.getAction();

                    for (Action flowmodAction : flowmodActions) {
                        for (Action tableAction : actionList) {
                            // TODO verify that the if statement below is
                            // correct
                            if (areActionsEqual(flowmodAction.getAction(),
                                    tableAction.getAction())) {
                                return true;
                            }
                        }

                        // Action SET_FIELD needs to validate supported fields
                        // as well
                        if (flowmodAction.getAction() instanceof SetFieldCase) {
                            SetField setField = ((SetFieldCase) flowmodAction
                                    .getAction()).getSetField();
                            // TODO verify that the statement below works. Can
                            // we use the method doesTableSuportMatch
                            // verify that all match field are supported by the
                            // table
                            if (doesTableSupportMatch(table, setField)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }

                if (isTableMiss
                        && tfpt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.WriteActionsMiss) {
                    WriteActionsMiss writeActionsMiss = ((org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.WriteActionsMiss) tfpt)
                            .getWriteActionsMiss();
                    List<Action> actionList = writeActionsMiss.getAction();

                    for (Action flowmodAction : flowmodActions) {
                        for (Action tableAction : actionList) {
                            // TODO verify that the if statement below is
                            // correct
                            if (areActionsEqual(flowmodAction.getAction(),
                                    tableAction.getAction())) {
                                return true;
                            }
                        }

                        // Action SET_FIELD needs to validate supported fields
                        // as well
                        if (flowmodAction.getAction() instanceof SetFieldCase) {
                            SetField setField = ((SetFieldCase) flowmodAction
                                    .getAction()).getSetField();
                            // TODO verify that the statement below works. Can
                            // we use the method doesTableSuportMatch
                            // verify that all match field are supported by the
                            // table
                            if (doesTableSupportMatch(table, setField)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            }
        }
        return false;
    }

    /**
     *
     * @param table table to check if the apply instruction is supported or not
     * @param isTableMiss boolean variable representing tableMiss
     * @param flowmodActions
     *            contains the actions in the flow we are validating and
     *            possibly adjusting
     * @return boolean variable representing if table supports apply actions or not
     */
    protected boolean doesTableSupportInstApplyActions(Table table,
            boolean isTableMiss, List<Action> flowmodActions) {
        List<TableFeatures> tableFeatures = table.getTableFeatures();

        for (TableFeatures tf : tableFeatures) {
            TableProperties tableProperties = tf.getTableProperties();
            List<TableFeatureProperties> tableFeatureProperties = tableProperties
                    .getTableFeatureProperties();

            for (TableFeatureProperties tfp : tableFeatureProperties) {
                TableFeaturePropType tfpt = tfp.getTableFeaturePropType();

                if (!isTableMiss
                        && tfpt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.ApplyActions) {
                    ApplyActions applyActions = ((org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.ApplyActions) tfpt)
                            .getApplyActions();
                    List<Action> actionList = applyActions.getAction();

                    for (Action flowmodAction : flowmodActions) {
                        for (Action tableAction : actionList) {
                            // TODO verify that the if statement below is
                            // correct
                            // debug
                            // org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action
                            // fa = flowmodAction.getAction();
                            // org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action
                            // ta = tableAction.getAction();

                            if (areActionsEqual(flowmodAction.getAction(),
                                    tableAction.getAction())) {
                                return true;
                            }
                        }

                        // validate the apply actions specially
                        if (flowmodAction.getAction() instanceof OutputActionCase) {
                            if (!analyzeOutputType(flowmodAction.getAction())) {
                                return false;
                            }
                        } else if (flowmodAction.getAction() instanceof SetFieldCase) {
                            SetField setField = ((SetFieldCase) flowmodAction
                                    .getAction()).getSetField();
                            // TODO verify that the statement below works. Can
                            // we use the method doesTableSuportMatch
                            // verify that all match field are supported by the
                            // table
                            if (doesTableSupportMatch(table, setField)) {
                                return true;
                            }
                        }

                        // Check further analysis if necessary
                        if (!analyzeSetType(flowmodAction.getAction())) {
                            return false;
                        }
                    }
                    return false;
                }
                if (isTableMiss
                        && tfpt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.ApplyActionsMiss) {
                    ApplyActionsMiss applyActionsMiss = ((org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.ApplyActionsMiss) tfpt)
                            .getApplyActionsMiss();
                    List<Action> actionList = applyActionsMiss.getAction();

                    for (Action flowmodAction : flowmodActions) {
                        for (Action tableAction : actionList) {
                            // TODO verify that the if statement below is
                            // correct
                            if (areActionsEqual(flowmodAction.getAction(),
                                    tableAction.getAction())) {
                                return true;
                            }

                            // TODO do we need to check the output action and
                            // set field action as
                            // wee did for the non-miss case. We appear to be
                            // doing both for
                            // WRITE_ACTION case (see the method
                            // doesTableSupportInstWriteActions)
                        }
                    }
                    return false;
                }
            }
        }
        return false;
    }

    private boolean areActionsEqual(
            org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action flowAction,
            org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action tableAction) {

        if (flowAction instanceof ControllerActionCase
                && tableAction instanceof ControllerActionCase) {
            return true;
        }
        if (flowAction instanceof CopyTtlInCase
                && tableAction instanceof CopyTtlInCase) {
            return true;
        }
        if (flowAction instanceof CopyTtlOutCase
                && tableAction instanceof CopyTtlOutCase) {
            return true;
        }
        if (flowAction instanceof DecMplsTtlCase
                && tableAction instanceof DecMplsTtlCase) {
            return true;
        }
        if (flowAction instanceof DecNwTtlCase
                && tableAction instanceof DecNwTtlCase) {
            return true;
        }
        if (flowAction instanceof DropActionCase
                && tableAction instanceof DropActionCase) {
            return true;
        }
        if (flowAction instanceof FloodActionCase
                && tableAction instanceof FloodActionCase) {
            return true;
        }
        if (flowAction instanceof FloodAllActionCase
                && tableAction instanceof FloodAllActionCase) {
            return true;
        }
        if (flowAction instanceof GroupActionCase
                && tableAction instanceof GroupActionCase) {
            return true;
        }
        if (flowAction instanceof HwPathActionCase
                && tableAction instanceof HwPathActionCase) {
            return true;
        }
        if (flowAction instanceof LoopbackActionCase
                && tableAction instanceof LoopbackActionCase) {
            return true;
        }
        if (flowAction instanceof OutputActionCase
                && tableAction instanceof OutputActionCase) {
            return true;
        }
        if (flowAction instanceof PopMplsActionCase
                && tableAction instanceof PopMplsActionCase) {
            return true;
        }
        if (flowAction instanceof PopPbbActionCase
                && tableAction instanceof PopPbbActionCase) {
            return true;
        }
        if (flowAction instanceof PopVlanActionCase
                && tableAction instanceof PopVlanActionCase) {
            return true;
        }
        if (flowAction instanceof PushMplsActionCase
                && tableAction instanceof PushMplsActionCase) {
            return true;
        }
        if (flowAction instanceof PushPbbActionCase
                && tableAction instanceof PushPbbActionCase) {
            return true;
        }
        if (flowAction instanceof PushVlanActionCase
                && tableAction instanceof PushVlanActionCase) {
            return true;
        }
        if (flowAction instanceof SetDlDstActionCase
                && tableAction instanceof SetDlDstActionCase) {
            return true;
        }
        if (flowAction instanceof SetDlSrcActionCase
                && tableAction instanceof SetDlSrcActionCase) {
            return true;
        }
        if (flowAction instanceof SetDlTypeActionCase
                && tableAction instanceof SetDlTypeActionCase) {
            return true;
        }
        if (flowAction instanceof SetFieldCase
                && tableAction instanceof SetFieldCase) {
            return true;
        }
        if (flowAction instanceof SetMplsTtlActionCase
                && tableAction instanceof SetMplsTtlActionCase) {
            return true;
        }
        if (flowAction instanceof SetNextHopActionCase
                && tableAction instanceof SetNextHopActionCase) {
            return true;
        }
        if (flowAction instanceof SetNwDstActionCase
                && tableAction instanceof SetNwDstActionCase) {
            return true;
        }
        if (flowAction instanceof SetNwSrcActionCase
                && tableAction instanceof SetNwSrcActionCase) {
            return true;
        }
        if (flowAction instanceof SetNwTosActionCase
                && tableAction instanceof SetNwTosActionCase) {
            return true;
        }
        if (flowAction instanceof SetNwTtlActionCase
                && tableAction instanceof SetNwTtlActionCase) {
            return true;
        }
        if (flowAction instanceof SetQueueActionCase
                && tableAction instanceof SetQueueActionCase) {
            return true;
        }
        if (flowAction instanceof SetTpDstActionCase
                && tableAction instanceof SetTpDstActionCase) {
            return true;
        }
        if (flowAction instanceof SetTpSrcActionCase
                && tableAction instanceof SetTpSrcActionCase) {
            return true;
        }
        if (flowAction instanceof SetVlanCfiActionCase
                && tableAction instanceof SetVlanCfiActionCase) {
            return true;
        }
        if (flowAction instanceof SetVlanIdActionCase
                && tableAction instanceof SetVlanIdActionCase) {
            return true;
        }
        if (flowAction instanceof SetVlanPcpActionCase
                && tableAction instanceof SetVlanPcpActionCase) {
            return true;
        }
        if (flowAction instanceof StripVlanActionCase
                && tableAction instanceof StripVlanActionCase) {
            return true;
        }
        if (flowAction instanceof SwPathActionCase
                && tableAction instanceof SwPathActionCase) {
            return true;
        }
        return false;
    }

    /**
     * Verify whether the given output action is permitted on this device.
     *
     * @param action
     *            the output action being analyzed
     * @return boolean indicating whether the action is supported or not
     */
    protected boolean analyzeOutputType(
            org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action action) {
        return true;
    }

    /**
     * Verify whether the given set field action is permitted on this device.
     *
     * @param action
     *            the set action being analyzed
     * @return boolean indicating whether the action is supported or not
     */
    protected boolean analyzeSetType(
            org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action action) {
        return true;
    }

    /**
     * Indicates whether we should create table misses for the default flows.
     * The default is to create table misses.
     *
     * @return boolean true if we should
     */
    protected boolean createTableMisses() {
        return true;
    }

    /**
     *
     * @param table table to be checked for goto instruction
     * @param isTableMiss table miss or not
     * @param tableId
     *            contains the next table (table miss) in the flow we are
     *            validating and possibly adjusting
     * @return boolean variable representing goto instruction support
     */
    protected boolean doesTableSupportInstGotoTable(Table table,
            boolean isTableMiss, Short tableId) {
        List<TableFeatures> tableFeatures = table.getTableFeatures();

        for (TableFeatures tf : tableFeatures) {
            TableProperties tableProperties = tf.getTableProperties();
            List<TableFeatureProperties> tableFeatureProperties = tableProperties
                    .getTableFeatureProperties();

            for (TableFeatureProperties tfp : tableFeatureProperties) {
                TableFeaturePropType tfpt = tfp.getTableFeaturePropType();

                if (!isTableMiss
                        && tfpt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.NextTable) {
                    Tables nextTables = ((org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.NextTable) tfpt)
                            .getTables();
                    List<Short> tableIds = nextTables.getTableIds();
                    for (Short ids : tableIds) {
                        if (ids.equals(tableId)) {
                            return true;
                        }
                    }
                    return false;
                }
                if (isTableMiss
                        && tfpt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.NextTableMiss) {
                    TablesMiss nextTableMisses = ((org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.NextTableMiss) tfpt)
                            .getTablesMiss();
                    List<Short> tableIds = nextTableMisses.getTableIds();
                    for (Short ids : tableIds) {
                        if (ids.equals(tableId)) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        }
        return false;
    }

}
