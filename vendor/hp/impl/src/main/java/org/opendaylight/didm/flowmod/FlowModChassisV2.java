/*
 * Copyright (c) 2015 Hewlett Packard Enterprise Development LP and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.didm.flowmod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.openflowplugin.openflow.md.core.sal.convertor.match.MatchConvertorImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanPcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.EthDst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.EthSrc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.InPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.VlanVid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.entries.grouping.MatchEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.entry.value.grouping.match.entry.value.EthTypeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.set.field.match.SetFieldMatch;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An implementation of the flow mod facet for ProVision chassis switches
 * configured with v2 modules only. It overrides only those aspects of the
 * facet affected by module compatibility.
 */


public class FlowModChassisV2 extends FlowModProvision {

    protected static final String MSG_FIELD_NOT_SUPPORTED =
            "Field {} is not supported with ethType {} on {} with fw {}";

    protected enum EthType {is800, non800, wild}
    private InstanceIdentifier<Node> nodePath;
    DataBroker dataBroker;

    private static final Logger LOG = LoggerFactory.getLogger(FlowModProvision.class);
    /**
     * Constructs a flow mod facet specific to a chassis device supporting
     * v2-only modules.
     *
     * @param nodepath the device info of which this is a facet
     * @dataBroker handle to rpc registry provider
     */
    public FlowModChassisV2(InstanceIdentifier<Node> nodePath, DataBroker dataBroker) {
        super(nodePath, dataBroker);
        this.nodePath = nodePath;
        this.dataBroker = dataBroker;
    }

    @Override
    protected Set<Flow> specializedFlows() {
        // No specialized flows needed for v2 modules
        return new HashSet<Flow>();
    }

    @Override
    protected boolean createTableMisses() {
        // Only create table miss rules if we are not in hybrid mode
        return !hybridMode;
    }

    /**
     * This is the implementation of the DefaultFlowMod.  However, because this
     * class inherits from provision which overrides this method, the v2 code
     * needs a way to access the default implementation.
     *
     * @param match from which fields will be analyzed
     * @param table to which match is destined
     * @return newly created match with valid match fields
     */
    protected List<MatchEntry> findDefaultValidMatchFields(Table table, Match match) {
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

    @Override
    protected List<MatchEntry>  findValidMatchFields(Table table, Match match) {

        // Take first pass by weeding out the context-rejected fields
        List<MatchEntry>  validFields = findDefaultValidMatchFields(table, match);
        List<MatchEntry>  revised = new ArrayList<>();
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
     * Analyze the given match field based on ethType criteria and the fact that
     * it is a hardware table.  This should only be called for OF versions 1.3.
     *
     * @param eType enum indicating 800, non-800 or wildcard
     * @param field to be analyzed
     * @return boolean indicating whether it is allow or not
     */
    protected boolean analyzeHwField(EthType eType, MatchEntry field) {

        float fwVersion = makeNumeric(getFirmwareVersion());

        if(!isOxmBasicFieldType(field))
            return false;

        if(field.getOxmMatchField().equals(VlanVid.class) ||
                field.getOxmMatchField().equals(EthType.class) ||
                field.getOxmMatchField().equals(InPort.class))
                return true;

        if(field.getOxmMatchField().equals(VlanPcp.class)){
            if (fwVersion >= FW_VERSION_CHANGE1) {
                if (eType != EthType.wild) {
                    LOG.trace(MSG_FIELD_NOT_SUPPORTED, field, eType, node.getAugmentation(FlowCapableNode.class).getManufacturer());
                    return false;
                }
            }
        }

        if(field.getOxmMatchField().equals(EthSrc.class) ||
                field.getOxmMatchField().equals(EthDst.class)){
            if (fwVersion < FW_VERSION_CHANGE1) {
                if (eType != EthType.non800) {
                    LOG.trace(MSG_FIELD_NOT_SUPPORTED, field, eType, node.getAugmentation(FlowCapableNode.class).getManufacturer());
                    return false;
                }
            }
            else {
                if (eType == EthType.wild) {
                    LOG.trace(MSG_FIELD_NOT_SUPPORTED, field, eType, node.getAugmentation(FlowCapableNode.class).getManufacturer());
                    return false;
                }
            }
        }
        // Others can only be specified if ethType is 800
        if (eType != EthType.is800) {
              return false;
        }

        return true;
    }

    @Override
    protected boolean doesTableSupportInstWriteActions(Table table, boolean isTableMiss, List<Action> flowmodActions) {
        return super.doesTableSupportInstWriteActions(table, isTableMiss, flowmodActions);
    }
}

