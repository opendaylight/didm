/*
 * (c) Copyright 2015 Hewlett-Packard Development Company, L.P.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.didm.tools.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.openflowplugin.api.OFConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FlowCreator {
	
	private static final Logger LOG = LoggerFactory.getLogger(FlowCreator.class);
	
	private final static Long ARP = 0x806L;
	
	private static final short TABLE_ID = (short) 0;
    private static final String ARP_FLOW = "ARP_FLOW";
    private static final String DEFAULT_FLOW_ID = "12345";
    private static final int HARD_TIMEOUT = 1800;
    private static final int IDLE_TIMEOUT = 180;
	
	public FlowCreator() {
	}

	public Flow buildArpFlowMod() {
		
		FlowBuilder flowBuilder = new FlowBuilder();
		
		// create a match condition for ARP reply packets
		MatchBuilder matchBuilder = new MatchBuilder();
		EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(ARP));
        ethernetMatchBuilder.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethernetMatchBuilder.build());
        flowBuilder.setMatch(matchBuilder.build());
		
        // create an instruction with two actions (output controller, output normal)
        List<Action> actionList = new ArrayList<Action>();

        // create an action to output to the controller port
        ActionBuilder abCtlr = new ActionBuilder();
        OutputActionBuilder outputCtlr = new OutputActionBuilder();
        outputCtlr.setMaxLength(OFConstants.OFPCML_NO_BUFFER);
        Uri valueCtlr = new Uri(OutputPortValues.CONTROLLER.toString());
        outputCtlr.setOutputNodeConnector(valueCtlr);
        abCtlr.setAction(new OutputActionCaseBuilder().setOutputAction(outputCtlr.build()).build());
        abCtlr.setOrder(0);
        abCtlr.setKey(new ActionKey(0));
        actionList.add(abCtlr.build());
        
        // create an action to output to normal port
        ActionBuilder abNorm = new ActionBuilder();
        OutputActionBuilder outputNorm = new OutputActionBuilder();
        outputNorm.setMaxLength(OFConstants.OFPCML_NO_BUFFER);
        Uri valueNorm = new Uri(OutputPortValues.NORMAL.toString());
        outputNorm.setOutputNodeConnector(valueNorm);
        abNorm.setAction(new OutputActionCaseBuilder().setOutputAction(outputNorm.build()).build());
        abNorm.setOrder(0);
        abNorm.setKey(new ActionKey(0));
        actionList.add(abNorm.build());
               
        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        InstructionBuilder ib = new InstructionBuilder();
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));

        // Put our Instruction in a list of Instructions
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = new ArrayList<Instruction>();
        instructions.add(ib.build());
        isb.setInstruction(instructions);
        flowBuilder.setInstructions(isb.build());
        
        // set other fields in flow
        flowBuilder.setPriority(0);

        FlowKey key = new FlowKey(new FlowId(DEFAULT_FLOW_ID));
        flowBuilder.setKey(key);
        flowBuilder.setBarrier(Boolean.FALSE);
        BigInteger value = new BigInteger("10", 10);
        flowBuilder.setCookie(new FlowCookie(value));
        flowBuilder.setCookieMask(new FlowCookie(value));
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        flowBuilder.setInstallHw(false);
        flowBuilder.setStrict(false);
        flowBuilder.setContainerName(null);
        flowBuilder.setFlags(new FlowModFlags(false, false, false, false, true));
        flowBuilder.setId(new FlowId(DEFAULT_FLOW_ID));
        flowBuilder.setTableId(TABLE_ID);
        flowBuilder.setFlowName(ARP_FLOW);
        flowBuilder.setHardTimeout(HARD_TIMEOUT);
        flowBuilder.setIdleTimeout(IDLE_TIMEOUT);
        
		Flow flow = flowBuilder.build();
		
		LOG.info("FlowCreator: Flow: " + flow.toString());
		
		return flow;
	}
	
	
}
