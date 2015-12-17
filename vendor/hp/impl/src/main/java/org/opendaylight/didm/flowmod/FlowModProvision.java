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

import org.opendaylight.didm.tools.utils.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.types.rev130731.PortNumberValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.ArpOp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.ArpSha;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.ArpSpa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.ArpTha;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.ArpTpa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.EthSrc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.Icmpv4Code;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.Icmpv4Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.Icmpv6Code;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.Icmpv6Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.InPhyPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.InPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.IpDscp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.IpEcn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.IpProto;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.Ipv4Dst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.Ipv4Src;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.Ipv6Dst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.Ipv6Exthdr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.Ipv6Flabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.Ipv6NdSll;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.Ipv6NdTarget;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.Ipv6NdTll;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.Ipv6Src;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.MatchField;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.MplsBos;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.MplsTc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.PbbIsid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.SctpDst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.SctpSrc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.TcpDst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.TcpSrc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.TunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.UdpDst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.UdpSrc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.VlanVid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.Metadata;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.entries.grouping.MatchEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.entry.value.grouping.match.entry.value.EthTypeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.entry.value.grouping.match.entry.value.eth.dst._case.EthDst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanPcp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.openflowplugin.api.openflow.md.util.OpenflowVersion;
import org.opendaylight.openflowplugin.openflow.md.util.InventoryDataServiceUtil;


public class FlowModProvision extends DefaultFlowMod{

    private static final List<Short> HW_TABLES = new ArrayList<>();
    private static final List<Short> SW_TABLES = new ArrayList<>();
    private static final int SPECIALIZED_PRIORITY = 1;

    protected static final String MSG_ETH_TYPE_NOT_SUPPORTED =
            "Field {} is not supported with ethType {} on {}; must be IPv4";
    protected static final String MSG_WILDCARD_NOT_SUPPORTED =
            "Field {} is not supported with ethType {} on {} because " +
            "wildcards are only supported with ethType IPv4";
    protected static final String MSG_SPECIFIC_FIELDS_NOT_SUPPORTED =
            "Field {} with ethType {} is not supported on {}";
    private InstanceIdentifier<Node> nodePath;
    DataBroker dataBroker;

    protected enum EthType {
        is800, non800, wild
    }

    protected float FW_VERSION_CHANGE1 = 15.14F;
    protected float FW_VERSION_CHANGE2 = 15.16F;
    private static final Logger LOG = LoggerFactory.getLogger(FlowModProvision.class);

    public FlowModProvision(InstanceIdentifier<Node> nodePath, DataBroker dataBroker) {
        super(nodePath, dataBroker);
        this.nodePath = nodePath;
        this.dataBroker = dataBroker;
        HW_TABLES.add((short) 100);
        SW_TABLES.add((short) 200);
    }

    /**
     * Based on this device's specifics, is the given tableId HW?
     *
     * @param tableId to be analyzed
     * @return boolean indicating whether it is HW or not
     */
    protected boolean isHardwareTable(Short tableId) {
        return (HW_TABLES.contains(tableId));
    }

    /**
     * Is this the last hardware table?
     *
     * @param tableId to be analyzed
     * @return boolean indicating whether it is or not
     */
    protected boolean isLastHardwareTable(Short tableId) {
        Short last = getLastHwTable();
        return (last != null && last.equals(tableId));
    }

    /**
     * Sometimes we will need a default HW table in which to program a goto
     * flow.
     *
     * @return TableId of the first HW table in the list
     */
    protected Short getFirstHwTable() {
        if (HW_TABLES.isEmpty()) {
            return null;
        }

        return (HW_TABLES.iterator().next());
    }

    /**
     * There might be times when we want to identify the last hw table.
     *
     * @return TableId of the first HW table in the list
     */
    protected Short getLastHwTable() {
        if (HW_TABLES.isEmpty()) {
            return null;
        }

        return HW_TABLES.get(HW_TABLES.size()-1);
    }

    /**
     * Based on this device's specifics, is the given tableId SW?
     *
     * @param tableId to be analyzed
     * @return boolean indicating whether it is SW or not
     */
    protected boolean isSoftwareTable(Short tableId) {
        return (SW_TABLES.contains(tableId));
    }

    /**
     * Sometimes we will need a default SW table to which to program a goto
     * flow.
     *
     * @return TableId of the first SW table in the list
     */
    protected Short getFirstSwTable() {

        if (SW_TABLES.isEmpty()) {
            return null;
        }

        return (SW_TABLES.iterator().next());
    }

    /**
     * There might be times when we want to identify the last sw table.
     *
     * @return TableId of the first SW table in the list
     */
    protected Short getLastSwTable() {
        if (SW_TABLES.isEmpty()) {
            return null;
        }

        return SW_TABLES.get(SW_TABLES.size()-1);
    }

    @Override
    public Set<Flow> generateDefaultFlows() {

        Set<Flow> defaultFlows = new HashSet<>();
        defaultFlows.addAll(defaultFlows());

        if (hybridMode) {
            defaultFlows.addAll(removeDuplicates(defaultFlows, specializedFlows()));
        }
        return defaultFlows;
    }

    @Override
    public Set<Flow> adjustFlowMod(Flow origFlow) {
        Set<Flow> adjustedFlows = new HashSet<>();

        // When adjusting a flow mod, make sure that the resulting flows are
        // not duplicates of the basic flows that are laid down by default
        Set<Flow> baseFlows = generateDefaultFlows();

        // If the caller has already populated table ID, we should leave as is
        if (origFlow.getTableId() != null) {
            adjustedFlows.add(origFlow);
            return returnOrThrow(removeDuplicates(baseFlows, adjustedFlows), origFlow);
        }

        // Find the best table to use for the given match fields and
        // instructions
        Table mfTable = getTableForMatch(origFlow.getMatch());
        Table insTable = getTableIdForInstructions(origFlow);

        Short mfTableId = mfTable.getId();
        Short insTableId = insTable.getId();

        // Something in this flow is not supported at all
        if (mfTableId == null || insTableId == null)
            throw new FlowUnsupportedException(StringUtils.format(
                    E_FLOW_UNSUPPORTED, origFlow, nodePath));

        // If both the matches and the instructions can go straight into the
        // hardware table then no adjustment is needed. If the match and
        // instruction actions belong in two different hardware tables, choose
        // the lower numbered one to put it in.
        if (isHardwareTable(mfTableId) && isHardwareTable(insTableId)) {
            // If they are both hw tables, but not the same number,
            // choose the highest number (random)
            adjustedFlows
                .add(assignToTable(origFlow,
                                   ((mfTableId.equals(insTableId)) ? mfTable
                                           : ((mfTableId
                                               .compareTo(insTableId) < 0) ? mfTable
                                                   : insTable))));
            return returnOrThrow(removeDuplicates(baseFlows, adjustedFlows), origFlow);
        }

        // At this point, either the match fields or the instruction actions
        // can only be supported in the sw table. It doesn't matter which one
        // is the reason. (Although this does assume that if the matches or
        // instruction actions are supported in the hw table, they would also
        // be supported in the sw table.) In order to do this, the entire flow
        // goes into the sw table and an additional flow is added to go from
        // the hw table to the sw table, meaning that the existing actions are
        // not used in the hw table at all. Although all the match fields will
        // be used in the sw table, only the valid ones will be matched in the
        // hw table for the goto statement. This means that the hw table will
        // be redirecting more general matches that may or may not be used in
        // the sw table. This runs the risk of eclipsing some other very
        // similar flows. This needs to be monitored.
        Table swTable = null;
        Short hwTableId = getFirstHwTable();
        Table hwTable = getTable(hwTableId);


        if (isSoftwareTable(mfTableId) && isSoftwareTable(insTableId)) {
            swTable = (mfTableId.equals(insTableId)) ? mfTable
                    : ((mfTableId.compareTo(insTableId) == 1) ? mfTable
                            : insTable);
        } else {
            swTable = isSoftwareTable(mfTableId) ? mfTable : insTable;
            hwTable = isHardwareTable(mfTableId) ? mfTable : insTable;
        }

        adjustedFlows.add(assignToTable(origFlow, swTable));
        adjustedFlows.add(createGotoFlow(origFlow, hwTable, swTable));

        return returnOrThrow(removeDuplicates(baseFlows, adjustedFlows), origFlow);
    }

    protected String getFirmwareVersion(){
        String software = null;
        Node node = DefaultFlowMod.getDataObject(dataBroker.newReadOnlyTransaction(), nodePath);
        FlowCapableNode flowCapableNode = node.getAugmentation(FlowCapableNode.class);
        if (flowCapableNode != null)
            software = flowCapableNode.getSoftware();
        return software;
    }

    /**
     * Creates the default flows based on OF protocol version and specialized
     * configuration information of the device itself.
     *
     * @return Set of flow mods describing the default flows
     * @throws FlowUnsupportedException if flows cannot be handled by this device
     */
    protected Set<Flow> defaultFlows() {
        Set<Flow> flows = new HashSet<>();

        // Special case for mininet
            if (!tables.isEmpty()) {
            return createDefaultFlow(BASE_TABLE);
        }
        float fwVersion = makeNumeric(getFirmwareVersion());

        for (Table table : tables) {
            if (table.getId() == BASE_TABLE) {
                if (fwVersion < FW_VERSION_CHANGE2) {
                    // Skip base table on firmware prior to 15.16
                    continue;
                }

                // After 15.16, the ONLY flow that can be put into the base
                // table is a miss to the first hw table
                flows.add(createTableMissFlow(table));
            } else {
                flows.addAll(createDefaultFlow(table.getId()));
            }
        }

        return flows;
    }

    /**
     * Some facets need to build specialized flows to add to the default
     * rules.  For provision, the default assumption is that it is a v1
     * module (if it is a chassis).  Therefore, it needs a specialized flow.
     *
     * @return Set of flow mods describing the specialized flows
     */
    protected Set<Flow> specializedFlows() {
        Set<Flow> mods = new HashSet<>();

        if (!hybridMode) {
            return mods;
        }

        Long[] types = new Long[] {0x0800L, 0x86DDL};
        for (Long type : types) {

            FlowBuilder fb = createBasicFlow(getFirstHwTable());

            // Create match condition for appropriate packets in OF 1.3
            fb.setMatch(createEthTypeMatch(type).build());

            // Instruction with forward normal action
            fb.setInstructions(createAppyActionInstruction().build());
            fb.setPriority(SPECIALIZED_PRIORITY);
            fb.setCookie(new FlowCookie(BigInteger.valueOf(DEFAULT_COOKIE)));

            mods.add(fb.build());
        }

        return mods;
    }

    private static MatchBuilder createEthTypeMatch(Long type) {
        MatchBuilder match = new MatchBuilder();
        EthernetMatchBuilder eth = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(type));
        eth.setEthernetType(ethTypeBuilder.build());
        match.setEthernetMatch(eth.build());
        return match;
    }

    /**
     * Turns the firmware version string into a float that can be compared
     * for equality.  Very specific to the Provision way of encoding version
     * numbers.
     *
     * @param fw String representing the firmware version
     * @return float representation of the string, or 0 if not parsable
     */
    protected float makeNumeric(String fw) {

        if ((fw == null) || fw.isEmpty()) {
            return 0F;
        }

        String str = fw.replaceAll("[^\\d_.]", "");
        str = str.replaceAll("_", ".");
        int ind = str.indexOf(".");
        if (ind == 0) {
            str = str.substring(1, str.length());
            ind = str.indexOf(".");
        }
        if (ind != -1) {
            if ( str.lastIndexOf(".") != ind) {
                String firstPart = str.substring(0, ind);
                if (ind < str.length()) {
                    String secondPart = str.substring(ind+1, str.length());
                    secondPart = secondPart.replace(".", "");
                    str = firstPart + "." + secondPart;
                } else {
                    str = firstPart;
                }
            }
        }

        return Float.parseFloat(str);
    }

    @Override
    protected List<MatchEntry> findValidMatchFields(Table table, Match match) {

        // Take first pass by weeding out the context-rejected fields
        List<MatchEntry> validFields = super
            .findValidMatchFields(table, match);
        List<MatchEntry> revised = new ArrayList<MatchEntry>();
        EthType eType = EthType.wild;

        // Everything is based on ethType, so extract that first
        for (MatchEntry me : validFields) {
            if (me.getOxmMatchField().equals(EthType.class)) {
                org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.entry.value.grouping.match.entry.value.eth.type._case.EthType ethTypeCase = ((EthTypeCase) me.getMatchEntryValue()).getEthType();
                if (ethTypeCase.getEthType().getValue() == 0x0800) {
                    eType = EthType.is800;
                    break;
                }

                eType = EthType.non800;
                break;
            }
        }

        for (MatchEntry me : validFields) {
            if (isHardwareTable(table.getId())) {
                if (!analyzeHwField(eType, me)) {
                    continue;
                }
            }

            revised.add(me);
        }

        return revised;
    }

    /**
     * Analyze the given match field based on ethType criteria and the fact
     * that it is a hardware table.
     *
     * @param eType enum indicating 800, non-800 or wildcard
     * @param field to be analyzed
     * @return boolean indicating whether it is allow or not
     */
    protected boolean analyzeHwField(EthType eType, MatchEntry field) {

        if(!isOxmBasicFieldType(field))
            return false;

        if(field.getOxmMatchField().equals(EthType.class)) {
            // If specified must be 800
            org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.entry.value.grouping.match.entry.value.eth.type._case.EthType ethTypeCase = ((EthTypeCase) field.getMatchEntryValue()).getEthType();
            if (ethTypeCase.getEthType().getValue() != 0x0800){
                LOG.trace(MSG_ETH_TYPE_NOT_SUPPORTED, field, eType, node.getAugmentation(FlowCapableNode.class).getManufacturer());
                return false;
            }
        }
        if(field.getOxmMatchField().equals(VlanVid.class) ||
                field.getOxmMatchField().equals(InPort.class))
                return true;

        if(field.getOxmMatchField().equals(EthSrc.class) ||
                field.getOxmMatchField().equals(EthDst.class) ||
                field.getOxmMatchField().equals(VlanPcp.class)){
            // These must be wildcarded, so they shouldn't show up here
            LOG.trace(MSG_SPECIFIC_FIELDS_NOT_SUPPORTED, field, eType,node.getAugmentation(FlowCapableNode.class).getManufacturer());
            return false;
        }
        // If they are wildcarded, shouldn't show up here
        // If they are not wildcards, can only be specified if there
        // is an ethType of 800
        if (eType != EthType.is800) {
           LOG.trace(MSG_WILDCARD_NOT_SUPPORTED, field, eType,node.getAugmentation(FlowCapableNode.class).getManufacturer());
           return false;
        }

        return true;
    }

    protected boolean isOxmBasicFieldType(MatchEntry field){

        Class <? extends MatchField> mfClass = field.getOxmMatchField();
        if (!(mfClass.equals(InPhyPort.class) ||
                mfClass.equals(InPort.class) ||
                mfClass.equals(InPhyPort.class) ||
                mfClass.equals(Metadata.class) ||
                mfClass.equals(EthDst.class) ||
                mfClass.equals(EthSrc.class) ||
                mfClass.equals(EthType.class) ||
                mfClass.equals(VlanVid.class) ||
                mfClass.equals(VlanPcp.class) ||
                mfClass.equals(IpDscp.class) ||
                mfClass.equals(IpEcn.class) ||
                mfClass.equals(IpProto.class) ||
                mfClass.equals(Ipv4Src.class) ||
                mfClass.equals(Ipv4Dst.class) ||
                mfClass.equals(TcpSrc.class) ||
                mfClass.equals(TcpDst.class) ||
                mfClass.equals(UdpSrc.class) ||
                mfClass.equals(UdpDst.class) ||
                mfClass.equals(SctpSrc.class) ||
                mfClass.equals(SctpDst.class) ||
                mfClass.equals(Icmpv4Type.class) ||
                mfClass.equals(Icmpv4Code.class) ||
                mfClass.equals(ArpOp.class) ||
                mfClass.equals(ArpSpa.class) ||
                mfClass.equals(ArpTpa.class) ||
                mfClass.equals(ArpSha.class) ||
                mfClass.equals(ArpTha.class) ||
                mfClass.equals(Ipv6Src.class) ||
                mfClass.equals(Ipv6Dst.class) ||
                mfClass.equals(Ipv6Flabel.class) ||
                mfClass.equals(Icmpv6Type.class) ||
                mfClass.equals(Icmpv6Code.class) ||
                mfClass.equals(Ipv6NdTarget.class) ||
                mfClass.equals(Ipv6NdSll.class) ||
                mfClass.equals(Ipv6NdTll.class) ||
                mfClass.equals(MplsLabel.class) ||
                mfClass.equals(MplsTc.class) ||
                mfClass.equals(MplsBos.class) ||
                mfClass.equals(PbbIsid.class) ||
                mfClass.equals(TunnelId.class) ||
                mfClass.equals(Ipv6Exthdr.class)))
            return false;

        return true;
    }

    protected List<Table> getSortedList(List<Table> tables){
        Collections.sort(tables, new Comparator<Table>(){
            @Override
            public int compare(Table o1, Table o2){
                if(o1.getId() == o2.getId())
                    return 0;
                return o1.getId() < o2.getId() ? -1 : 1;
            }
       });
       return tables;
    }

    // Make it specific to the hw and sw tables we know about for PVOS
    @Override
    protected Table getTableIdForInstructions(Flow flow) {
        // If there are no instructions, obviously the first hw table will
        // suffice
        if (flow.getInstructions().getInstruction().isEmpty()) {
            return getTable(getFirstHwTable());
        }
        // The flow is a table miss if there is no match criteria specified
        boolean isTableMiss = true;
        if ((flow.getMatch() != null)
                && !doesMatchFieldsEmpty(flow.getMatch())) {
            isTableMiss = false;
        }
        // Get a sorted set of table IDs from the definition
        List<Table> sortedTableList = getSortedList(tables);

        // Find a table in which all instructions are supported
        for (Table table : sortedTableList) {

            if (doesTableSupportInstructions(table, flow.getInstructions().getInstruction(),
                                             isTableMiss)) {
                // NOTE: This method assumes that HW tables appear first in
                // the sorted list and SW tables appear last.
                return table;
            }
        }
        // No table supports these instructions
        return null;
    }

    @Override
    protected Table getTableForMatch(Match match) {
        Table swTable = null;
        Table hwTable = null;

        // If no match fields, then the first hw table will do
        if ((match == null) || doesMatchFieldsEmpty(match)) {
            return getTable(getFirstHwTable());
        }

        for (Table table : tables) {

            // If this is a not a wildcard match, see if all the fields
            // are supported in this match
            if (doesTableSupportMatch(table, match)) {
                // Look for a better match
                if (!table.getId().equals(getFirstHwTable())) {
                    if (isHardwareTable(table.getId())) {
                        hwTable = table;
                        if (getFirstHwTable().equals(hwTable)) {
                            // Can't do any better than this. Stop checking.
                            break;
                        }
                    } else {
                        // Choose the best swTable which would be the first
                        if ((swTable == null) || !getFirstSwTable().equals(swTable)) {
                            swTable = table;
                        }
                    }
                } else {
                    return table;
                }
            }
        }

        // Will return hwTable if possible; otherwise, defaults to swTable
        // Will return null if no table supports it
        return (hwTable != null ? hwTable : swTable);
    }

    // Implement hw-specific analysis
    @Override
    protected boolean doesTableSupportInstApplyActions(Table table,
                                                    boolean isTableMiss,
                                                    List<Action> actions) {
        if (!super.doesTableSupportInstApplyActions(table, isTableMiss, actions)) {
            return false;
        }

        boolean actionCopyController = false;
        boolean actionFwdNormal = false;

        for (Action action : actions) {
            // Copy is only allowed in the sw table for PVOS switches
            if (!isTableMiss) {
                if (action.getAction() instanceof OutputActionCase) {
                    //ctOutput acop = (ActOutput) action;
                    OutputActionCase outputActionCase = (OutputActionCase) action.getAction();
                    OutputAction opAction = outputActionCase.getOutputAction();
                    Uri uri = opAction.getOutputNodeConnector();
                    OpenflowVersion ofVersion = OpenflowVersion.OF13;
                    Long portNumber = InventoryDataServiceUtil.portNumberfromNodeConnectorId(ofVersion, uri.getValue());

                    if (portNumber == PortNumberValues.CONTROLLER.getIntValue()){
                        actionCopyController = true;
                    } else if (portNumber == PortNumberValues.NORMAL.getIntValue()) {
                        actionFwdNormal = true;
                    }

                    if ((actionCopyController) && (actionFwdNormal)
                            && (isHardwareTable(table.getId()))) {
                        return false;
                    }
                } else if (action.getAction() instanceof SetFieldCase) {
                    if (!analyzeSetType(action.getAction())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

}
