/*
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.did.flowmod;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.TunnelIpv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.SctpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.features.TableFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.features.table.features.TableProperties;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.features.table.features.table.properties.TableFeatureProperties;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.TableFeaturePropType;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteMetadataCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ClearActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.MeterCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.next.table.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.next.table.miss.TablesMiss;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.apply.actions.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.apply.actions.miss.ApplyActionsMiss;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.write.actions.WriteActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.write.actions.miss.WriteActionsMiss;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetField;


public class DefaultFlowMod {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultFlowMod.class);
    
    List<Table> tables = null;
    ProtocolVersion version = null;
    boolean hybridMode = true;
    
    /*
     * Constructor
     */
    public DefaultFlowMod () {
        // TESTING: ******************************** 
        //FlowCreator flowCreator = new FlowCreator();
        //Flow flow = flowCreator.buildArpFlowMod();
        
        //TableFeatureCreator tableCreator = new TableFeatureCreator();
        //this.tables = tableCreator.create3800Table();
        
        //Match match = flow.getMatch();
        //getTableForMatch(match);
        //getTableIdForInstructions(flow);
        // TESTING: end of testing code *************
    }
    
    public void setTableFeatures(List<Table> tables, ProtocolVersion pv, boolean isHybrid) {
        if (tables == null || pv == null) {
            throw new NullPointerException(/*E_NULL_PARAMS*/"e_null_parms");
        }
        this.tables = tables;
        version = pv;
        hybridMode = isHybrid;
    }
    
    public List<Flow> adjustFlowMod(Flow origFlow) {
        List<Flow> adjustedFlows = new ArrayList<Flow>();
        
        // TODO: write this method correctly.  currently just learning... 
        // DEBUG: Code to learn how to access the ODL table feature information
        // I want to see how much code is required to access the 
        // table features 
        Table mfTable = getTableForMatch(origFlow.getMatch());
        Table insTable = getTableIdForInstructions(origFlow);
        
        return adjustedFlows;
    }
    
    /**
     * Determine the best table for the match in the flow. "Best" is
     * defined by each implementation of this facet.
     * 
     * @param match to be analyzed
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
     * @param table table being examined
     * @param match to be analyzed
     * @return boolean indicating whether the table supports the match
     */
    protected boolean doesTableSupportMatch(Table table, org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.Match match) {
    	// TODO - verify that passing the model version of the Match class works. This work
    	// today because the flow version of Match extends from the model version of match,
    	// and the flow version doesn't do anything (ie, not methods defined). If flow version 
    	// of Match changes (metods added), then the new methods won't be accessible here.
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
            if (((Ipv4Match)l3m).getIpv4Source() != null) {
                if (doesTableSupportMatchField(table, Ipv4Src.class)) {
                    return true;
                }
            }
            if (((Ipv4Match)l3m).getIpv4Destination() != null) {
                if (doesTableSupportMatchField(table, Ipv4Dst.class)) {
                    return true;
                }
            }
        }
        else if(l3m instanceof Ipv6Match) {
            if (((Ipv6Match)l3m).getIpv6Source() != null) {
                if (doesTableSupportMatchField(table, Ipv6Src.class)) {
                    return true;
                }
            }
            if (((Ipv6Match)l3m).getIpv6Destination() != null) {
                if (doesTableSupportMatchField(table, Ipv6Dst.class)) {
                    return true;
                }
            }
            if (((Ipv6Match)l3m).getIpv6Label() != null) {
                if (doesTableSupportMatchField(table, Ipv6Flabel.class)) {
                    return true;
                }
            }
            if (((Ipv6Match)l3m).getIpv6NdTarget() != null) {
                if (doesTableSupportMatchField(table, Ipv6NdTarget.class)) {
                    return true;
                }
            }
            if (((Ipv6Match)l3m).getIpv6NdSll() != null) {
                if (doesTableSupportMatchField(table, Ipv6NdSll.class)) {
                    return true;
                }
            }
            if (((Ipv6Match)l3m).getIpv6NdTll() != null) {
                if (doesTableSupportMatchField(table, Ipv6NdTll.class)) {
                    return true;
                }
            }
            if (((Ipv6Match)l3m).getIpv6ExtHeader() != null) {
                if (doesTableSupportMatchField(table, Ipv6Exthdr.class)) {
                    return true;
                }
            }
        }
        else if(l3m instanceof ArpMatch) {
            if (((ArpMatch)l3m).getArpSourceHardwareAddress() != null)
            {
                if (doesTableSupportMatchField(table, ArpSha.class)) {
                    return true;
                }
            }
            if (((ArpMatch)l3m).getArpSourceTransportAddress() != null) {
                if (doesTableSupportMatchField(table, ArpSpa.class)) {
                    return true;
                }
            }
            if (((ArpMatch)l3m).getArpTargetHardwareAddress() != null) {
                if (doesTableSupportMatchField(table, ArpTha.class)) {
                    return true;
                }
            }
            if (((ArpMatch)l3m).getArpTargetTransportAddress() != null) {
                if (doesTableSupportMatchField(table, ArpTpa.class)) {
                    return true;
                }
            }
            if (((ArpMatch)l3m).getArpOp() != null) {
                if (doesTableSupportMatchField(table, ArpOp.class)) {
                    return true;
                }
            }
        }
        else if(l3m instanceof TunnelIpv4Match) {
            if (((TunnelIpv4Match)l3m).getTunnelIpv4Source() != null) {
                if (doesTableSupportMatchField(table, TunnelIpv4Src.class)) {
                    return true;
                }
            }
            if (((TunnelIpv4Match)l3m).getTunnelIpv4Destination() != null) {
                if (doesTableSupportMatchField(table, TunnelIpv4Dst.class)) {
                    return true;
                }
            }
        }
        
        // check layer 4 matches
        Layer4Match l4m = match.getLayer4Match();
        if (l4m instanceof SctpMatch) {
             if (((SctpMatch)l4m).getSctpSourcePort() != null) {
                if (doesTableSupportMatchField(table, SctpSrc.class)) {
                     return true;
                 } 
             }
             if (((SctpMatch)l4m).getSctpDestinationPort() != null) {
                 if (doesTableSupportMatchField(table, SctpDst.class)) {
                       return true;
                   }  
              }
        }
        else if (l4m instanceof TcpMatch) {
            if (((TcpMatch)l4m).getTcpSourcePort() != null) {
                if (doesTableSupportMatchField(table, TcpSrc.class)) {
                      return true;
                  } 
            }
            if (((TcpMatch)l4m).getTcpDestinationPort() != null) {
                if (doesTableSupportMatchField(table, TcpDst.class)) {
                      return true;
                  } 
            }
        }
        else if (l4m instanceof UdpMatch) {
            if (((UdpMatch)l4m).getUdpSourcePort() != null) {
                if (doesTableSupportMatchField(table, UdpSrc.class)) {
                      return true;
                  } 
            }
            if (((UdpMatch)l4m).getUdpDestinationPort() != null) {
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
            if (match.getIcmpv4Match().getIcmpv4Type()!= null) {
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
            if (match.getIcmpv6Match().getIcmpv6Type()!= null) {
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
        
        for (TableFeatures tf: tableFeatures) {
            TableProperties tableProperties = tf.getTableProperties();
            List<TableFeatureProperties> tableFeatureProperties = tableProperties.getTableFeatureProperties();
            
            for (TableFeatureProperties tfp: tableFeatureProperties) {
                TableFeaturePropType tfpt = tfp.getTableFeaturePropType();
                
                if (tfpt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.Match) {
                    MatchSetfield matchSetField = ((org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.Match) tfpt).getMatchSetfield();
                    List<SetFieldMatch> setFieldMatch = null;
                    
                    if ( null != matchSetField) {
                        setFieldMatch = matchSetField.getSetFieldMatch();
                        
                        // NOTE: the match types (eg, EthSrc.class) are defined in model-flow-base project
                        for (SetFieldMatch sfMatch: setFieldMatch) {
                            if (clazz.equals(EthSrc.class) && sfMatch.getMatchType() == EthSrc.class ) {
                                return true;
                            }
                            if (clazz.equals(EthDst.class) && sfMatch.getMatchType() == EthDst.class ) {
                                return true;
                            }
                            if (clazz.equals(EthType.class) && sfMatch.getMatchType() == EthType.class ) {
                                return true;
                            }
                            if (clazz.equals(InPort.class) && sfMatch.getMatchType() == InPort.class ) {
                                return true;
                            }
                            if (clazz.equals(InPhyPort.class) && sfMatch.getMatchType() == InPhyPort.class ) {
                                return true;
                            }
                            if (clazz.equals(VlanVid.class) && sfMatch.getMatchType() == VlanVid.class ) {
                                return true;
                            }
                            if (clazz.equals(VlanPcp.class) && sfMatch.getMatchType() == VlanPcp.class ) {
                                return true;
                            }
                            if (clazz.equals(IpDscp.class) && sfMatch.getMatchType() == IpDscp.class ) {
                                return true;
                            }
                            if (clazz.equals(IpEcn.class) && sfMatch.getMatchType() == IpEcn.class ) {
                                return true;
                            }
                            if (clazz.equals(IpProto.class) && sfMatch.getMatchType() == IpProto.class ) {
                                return true;
                            }
                            if (clazz.equals(Ipv4Src.class) && sfMatch.getMatchType() == Ipv4Src.class ) {
                                return true;
                            }
                            if (clazz.equals(Ipv4Dst.class) && sfMatch.getMatchType() == Ipv4Dst.class ) {
                                return true;
                            }
                            if (clazz.equals(Ipv6Src.class) && sfMatch.getMatchType() == Ipv6Src.class ) {
                                return true;
                            }
                            if (clazz.equals(Ipv6Dst.class) && sfMatch.getMatchType() == Ipv6Dst.class ) {
                                return true;
                            }
                            if (clazz.equals(Ipv6Flabel.class) && sfMatch.getMatchType() == Ipv6Flabel.class ) {
                                return true;
                            }
                            if (clazz.equals(Ipv6NdTarget.class) && sfMatch.getMatchType() == Ipv6NdTarget.class ) {
                                return true;
                            }
                            if (clazz.equals(Ipv6NdSll.class) && sfMatch.getMatchType() == Ipv6NdSll.class ) {
                                return true;
                            }
                            if (clazz.equals(Ipv6NdTll.class) && sfMatch.getMatchType() == Ipv6NdTll.class ) {
                                return true;
                            }
                            if (clazz.equals(Ipv6Exthdr.class) && sfMatch.getMatchType() == Ipv6Exthdr.class ) {
                                return true;
                            }
                            if (clazz.equals(ArpSha.class) && sfMatch.getMatchType() == ArpSha.class ) {
                                return true;
                            }
                            if (clazz.equals(ArpSpa.class) && sfMatch.getMatchType() == ArpSpa.class ) {
                                return true;
                            }
                            if (clazz.equals(ArpTha.class) && sfMatch.getMatchType() == ArpTha.class ) {
                                return true;
                            }
                            if (clazz.equals(ArpTpa.class) && sfMatch.getMatchType() == ArpTpa.class ) {
                                return true;
                            }
                            if (clazz.equals(ArpOp.class) && sfMatch.getMatchType() == ArpOp.class ) {
                                return true;
                            }
                            if (clazz.equals(TunnelIpv4Src.class) && sfMatch.getMatchType() == TunnelIpv4Src.class ) {
                                return true;
                            }
                            if (clazz.equals(TunnelIpv4Dst.class) && sfMatch.getMatchType() == TunnelIpv4Dst.class ) {
                                return true;
                            }
                            if (clazz.equals(SctpSrc.class) && sfMatch.getMatchType() == SctpSrc.class ) {
                                return true;
                            }
                            if (clazz.equals(SctpDst.class) && sfMatch.getMatchType() == SctpDst.class ) {
                                return true;
                            }
                            if (clazz.equals(TcpSrc.class) && sfMatch.getMatchType() == TcpSrc.class ) {
                                return true;
                            }
                            if (clazz.equals(TcpDst.class) && sfMatch.getMatchType() == TcpDst.class ) {
                                return true;
                            }
                            if (clazz.equals(UdpSrc.class) && sfMatch.getMatchType() == UdpSrc.class ) {
                                return true;
                            }
                            if (clazz.equals(UdpDst.class) && sfMatch.getMatchType() == UdpDst.class ) {
                                return true;
                            }
                            if (clazz.equals(TcpFlag.class) && sfMatch.getMatchType() == TcpFlag.class ) {
                                return true;
                            }
                            if (clazz.equals(Icmpv4Code.class) && sfMatch.getMatchType() == Icmpv4Code.class ) {
                                return true;
                            }
                            if (clazz.equals(Icmpv4Type.class) && sfMatch.getMatchType() == Icmpv4Type.class ) {
                                return true;
                            }
                            if (clazz.equals(Icmpv6Code.class) && sfMatch.getMatchType() == Icmpv6Code.class ) {
                                return true;
                            }
                            if (clazz.equals(Icmpv6Type.class) && sfMatch.getMatchType() == Icmpv6Type.class ) {
                                return true;
                            }
                            if (clazz.equals(TunnelId.class) && sfMatch.getMatchType() == TunnelId.class ) {
                                return true;
                            }
                            if (clazz.equals(Metadata.class) && sfMatch.getMatchType() == Metadata.class ) {
                                return true;
                            }
                            if (clazz.equals(MplsBos.class) && sfMatch.getMatchType() == MplsBos.class ) {
                                return true;
                            }
                            if (clazz.equals(MplsTc.class) && sfMatch.getMatchType() == MplsTc.class ) {
                                return true;
                            }
                            if (clazz.equals(MplsLabel.class) && sfMatch.getMatchType() == MplsLabel.class ) {
                                return true;
                            }
                            if (clazz.equals(PbbIsid.class) && sfMatch.getMatchType() == PbbIsid.class ) {
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
    	if (flow.getInstructions() == null || flow.getInstructions().getInstruction().isEmpty()) {
    		return null;
    	}
    	    		
    	for (Table table : tables) {
            if (doesTableSupportInstructions(table, flow.getInstructions().getInstruction(), isTableMiss)) {
                return table;
            }
        }
        // return null if no table supports it
    	return null;
    }
    
    protected boolean doesTableSupportInstructions(Table table, List<Instruction> instructions, boolean isTableMiss) {
    	for (Instruction instr : instructions) {
    		
    		// Goto Table
    		if (instr.getInstruction() instanceof GoToTableCase) {
    			if (doesTableSupportInstGotoTable(table, isTableMiss, 
    					                          ((GoToTableCase)instr.getInstruction()).getGoToTable().getTableId()))  {
    				return true;
    			}
    		}
    		// Apply Actions
    		else if (instr.getInstruction() instanceof ApplyActionsCase) {
    			// debug
    			org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions ap = ((ApplyActionsCase)instr.getInstruction()).getApplyActions();
    			    			
    			if (doesTableSupportInstApplyActions(table, isTableMiss, 
    					                             ((ApplyActionsCase)instr.getInstruction()).getApplyActions().getAction())) {
    				return true;
    			}
    		}
    		// Write Actions
    		else if (instr.getInstruction() instanceof WriteActionsCase) {
    			if (doesTableSupportInstWriteActions(table, isTableMiss,
    					                              ((WriteActionsCase)instr.getInstruction()).getWriteActions().getAction())) {
    				return true;
    			}
    		}
    		// write metadata, clear actions, meter, experimenter (note: I don't see experimenter in yang model
    		else if ((instr.getInstruction() instanceof MeterCase) || 
    				 (instr.getInstruction() instanceof WriteMetadataCase) || 
    				 (instr.getInstruction() instanceof ClearActionsCase)) {
    			if (doesTableSupportInstruction(table, isTableMiss, instr.getInstruction())) {
    				return true;
    			}
    		}
    	}
    	return false;
    }
    
    /**
     * 
     * @param table
     * @param isTableMiss
     * @param instruction
     * @return
     */
    protected boolean doesTableSupportInstruction(Table table, boolean isTableMiss, 
    		             org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction instruction) {
    	List<TableFeatures> tableFeatures = table.getTableFeatures();
    	
        for (TableFeatures tf: tableFeatures) {
            TableProperties tableProperties = tf.getTableProperties();
            List<TableFeatureProperties> tableFeatureProperties = tableProperties.getTableFeatureProperties();
            
            for (TableFeatureProperties tfp: tableFeatureProperties) {
                TableFeaturePropType tfpt = tfp.getTableFeaturePropType();
                
                if (!isTableMiss && tfpt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.Instructions) {
                	List<Instruction> instList = ((org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.Instructions)tfpt).getInstructions().getInstruction();
                	
                	for (Instruction inst : instList) {
                		if (areInstructionsEqual(instruction, inst.getInstruction())) {
                			return true;
                		}
                		//if (inst.getInstruction().equals(instruction)) {
                		//	return true;
                		//}
                	}
                	return false;
                }
                
                if (isTableMiss && tfpt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.InstructionsMiss) {
                	List<Instruction> instList = ((org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.InstructionsMiss)tfpt).getInstructionsMiss().getInstruction();
                
                	for (Instruction inst : instList) {
                		if (areInstructionsEqual(instruction, inst.getInstruction())) {
                			return true;
                		}
                	}
                	return false;
                }
            }
        }
        return false;
    }
    
    private boolean areInstructionsEqual(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction flowInst,
    		                             org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.Instruction tableInst) {
    	if (flowInst instanceof ClearActionsCase && tableInst instanceof ClearActionsCase) {
			return true;
		}
		if (flowInst instanceof MeterCase && tableInst instanceof MeterCase) {
			return true;
		}
		if (flowInst instanceof WriteMetadataCase && tableInst instanceof WriteMetadataCase) {
			return true;
		}
		return false;
    }
    
    /**
     * 
     * @param table
     * @param isTableMiss
     * @param flowmodActions contains the actions in the flow we are validating and possibly adjusting
     * @return
     */
    protected boolean doesTableSupportInstWriteActions(Table table, boolean isTableMiss, List<Action> flowmodActions) {
    	List<TableFeatures> tableFeatures = table.getTableFeatures();
    	
        for (TableFeatures tf: tableFeatures) {
            TableProperties tableProperties = tf.getTableProperties();
            List<TableFeatureProperties> tableFeatureProperties = tableProperties.getTableFeatureProperties();
            
            for (TableFeatureProperties tfp: tableFeatureProperties) {
                TableFeaturePropType tfpt = tfp.getTableFeaturePropType();
                
                if (!isTableMiss && tfpt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.WriteActions) {
                	WriteActions writeActions = ((org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.WriteActions)tfpt).getWriteActions();
                	List<Action> actionList = writeActions.getAction();
                	
                	for (Action flowmodAction : flowmodActions) {
                    	for (Action tableAction : actionList) {
                    		// TODO verify that the if statement below is correct
                    		if (areActionsEqual(flowmodAction.getAction(), tableAction.getAction())) {
                    			return true;
                    		}
                    	}
                    	
                    	// Action SET_FIELD needs to validate supported fields as well
                    	if (flowmodAction.getAction() instanceof SetFieldCase) {
                    		SetField setField = ((SetFieldCase)flowmodAction.getAction()).getSetField();
                    		// TODO verify that the statement below works. Can we use the method doesTableSuportMatch
                    		// verify that all match field are supported by the table
                    		if (doesTableSupportMatch(table, (org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.Match)setField)) {
                    			return true;
                    		}
                    	}
                	}
                	return false;
                }
                
                if (isTableMiss && tfpt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.WriteActionsMiss) { 
                	WriteActionsMiss writeActionsMiss = ((org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.WriteActionsMiss)tfpt).getWriteActionsMiss();
                	List<Action> actionList = writeActionsMiss.getAction();
                	
                	for (Action flowmodAction : flowmodActions) {
                    	for (Action tableAction : actionList) {
                    		// TODO verify that the if statement below is correct
                    		if (areActionsEqual(flowmodAction.getAction(), tableAction.getAction())) {
                    			return true;
                    		}
                    	}
                    	
                    	// Action SET_FIELD needs to validate supported fields as well
                    	if (flowmodAction.getAction() instanceof SetFieldCase) {
                    		SetField setField = ((SetFieldCase)flowmodAction.getAction()).getSetField();
                    		// TODO verify that the statement below works. Can we use the method doesTableSuportMatch
                    		// verify that all match field are supported by the table
                    		if (doesTableSupportMatch(table, (org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.Match)setField)) {
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
     * @param table
     * @param isTableMiss
     * @param flowmodActions contains the actions in the flow we are validating and possibly adjusting
     * @return
     */
    protected boolean doesTableSupportInstApplyActions(Table table, boolean isTableMiss, List<Action> flowmodActions) {
    	List<TableFeatures> tableFeatures = table.getTableFeatures();
        
        for (TableFeatures tf: tableFeatures) {
            TableProperties tableProperties = tf.getTableProperties();
            List<TableFeatureProperties> tableFeatureProperties = tableProperties.getTableFeatureProperties();
            
            for (TableFeatureProperties tfp: tableFeatureProperties) {
                TableFeaturePropType tfpt = tfp.getTableFeaturePropType();
                
                if (!isTableMiss && tfpt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.ApplyActions) {
                	ApplyActions applyActions = ((org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.ApplyActions)tfpt).getApplyActions();
                	List<Action> actionList = applyActions.getAction();
                    
                    for (Action flowmodAction : flowmodActions) {
                    	for (Action tableAction : actionList) {
                    		// TODO verify that the if statement below is correct
                    		// debug
                    		//org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action fa = flowmodAction.getAction();
                    		//org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action ta = tableAction.getAction();
                    		
                    		if (areActionsEqual(flowmodAction.getAction(), tableAction.getAction())) {
                    			return true;
                    		}
                        }
                    	
                    	// validate the apply actions specially
                    	if (flowmodAction.getAction() instanceof OutputActionCase) {
                    		if (!analyzeOutputType(flowmodAction.getAction())) {
                    			return false;
                    		}
                    	}
                    	else if (flowmodAction.getAction() instanceof SetFieldCase) {
                    		SetField setField = ((SetFieldCase)flowmodAction.getAction()).getSetField();
                    		// TODO verify that the statement below works. Can we use the method doesTableSuportMatch
                    		// verify that all match field are supported by the table
                    		if (doesTableSupportMatch(table, (org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.Match)setField)) {
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
                if (isTableMiss && tfpt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.ApplyActionsMiss) {
                	ApplyActionsMiss applyActionsMiss = ((org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.ApplyActionsMiss)tfpt).getApplyActionsMiss();
                	List<Action> actionList = applyActionsMiss.getAction();
                	
                	for (Action flowmodAction : flowmodActions) {
                    	for (Action tableAction : actionList) {
                    		// TODO verify that the if statement below is correct
                    		if (areActionsEqual(flowmodAction.getAction(), tableAction.getAction())) {
                    			return true;
                    		}
                    		
                    		// TODO do we need to check the output action and set field action as
                    		// wee did for the non-miss case.  We appear to be doing both for
                    		// WRITE_ACTION case (see the method doesTableSupportInstWriteActions)
                        }
                    }
                    return false;
                }
            }
        }
    	return false;
    }
    
    private boolean areActionsEqual(org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action flowAction, 
    		                       org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action tableAction) {
    	
    	if (flowAction instanceof ControllerActionCase && tableAction instanceof ControllerActionCase) {
       		return true;
        }
    	if (flowAction instanceof CopyTtlInCase && tableAction instanceof CopyTtlInCase) {
       		return true;
        }
    	if (flowAction instanceof CopyTtlOutCase && tableAction instanceof CopyTtlOutCase) {
       		return true;
        }
    	if (flowAction instanceof DecMplsTtlCase && tableAction instanceof DecMplsTtlCase) {
       		return true;
        }
    	if (flowAction instanceof DecNwTtlCase && tableAction instanceof DecNwTtlCase) {
       		return true;
        }
    	if (flowAction instanceof DropActionCase && tableAction instanceof DropActionCase) {
       		return true;
        }
    	if (flowAction instanceof FloodActionCase && tableAction instanceof FloodActionCase) {
       		return true;
        }
    	if (flowAction instanceof FloodAllActionCase && tableAction instanceof FloodAllActionCase) {
       		return true;
        }
    	if (flowAction instanceof GroupActionCase && tableAction instanceof GroupActionCase) {
       		return true;
        }
    	if (flowAction instanceof HwPathActionCase && tableAction instanceof HwPathActionCase) {
       		return true;
        }
    	if (flowAction instanceof LoopbackActionCase && tableAction instanceof LoopbackActionCase) {
       		return true;
        }
        if (flowAction instanceof OutputActionCase && tableAction instanceof OutputActionCase) {
       		return true;
        }
        if (flowAction instanceof PopMplsActionCase && tableAction instanceof PopMplsActionCase) {
       		return true;
        }
        if (flowAction instanceof PopPbbActionCase && tableAction instanceof PopPbbActionCase) {
       		return true;
        }
        if (flowAction instanceof PopVlanActionCase && tableAction instanceof PopVlanActionCase) {
       		return true;
        }
        if (flowAction instanceof PushMplsActionCase && tableAction instanceof PushMplsActionCase) {
       		return true;
        }
        if (flowAction instanceof PushPbbActionCase && tableAction instanceof PushPbbActionCase) {
       		return true;
        }
        if (flowAction instanceof PushVlanActionCase && tableAction instanceof PushVlanActionCase) {
       		return true;
        }
        if (flowAction instanceof SetDlDstActionCase && tableAction instanceof SetDlDstActionCase) {
       		return true;
        }
        if (flowAction instanceof SetDlSrcActionCase && tableAction instanceof SetDlSrcActionCase) {
       		return true;
        }
        if (flowAction instanceof SetDlTypeActionCase && tableAction instanceof SetDlTypeActionCase) {
       		return true;
        }
        if (flowAction instanceof SetFieldCase && tableAction instanceof SetFieldCase) {
       		return true;
        }
        if (flowAction instanceof SetMplsTtlActionCase && tableAction instanceof SetMplsTtlActionCase) {
       		return true;
        }
        if (flowAction instanceof SetNextHopActionCase && tableAction instanceof SetNextHopActionCase) {
       		return true;
        }
        if (flowAction instanceof SetNwDstActionCase && tableAction instanceof SetNwDstActionCase) {
       		return true;
        }
        if (flowAction instanceof SetNwSrcActionCase && tableAction instanceof SetNwSrcActionCase) {
       		return true;
        }
        if (flowAction instanceof SetNwTosActionCase && tableAction instanceof SetNwTosActionCase) {
       		return true;
        }
        if (flowAction instanceof SetNwTtlActionCase && tableAction instanceof SetNwTtlActionCase) {
       		return true;
        }
        if (flowAction instanceof SetQueueActionCase && tableAction instanceof SetQueueActionCase) {
       		return true;
        }
        if (flowAction instanceof SetTpDstActionCase && tableAction instanceof SetTpDstActionCase) {
       		return true;
        }
        if (flowAction instanceof SetTpSrcActionCase && tableAction instanceof SetTpSrcActionCase) {
       		return true;
        }
        if (flowAction instanceof SetVlanCfiActionCase && tableAction instanceof SetVlanCfiActionCase) {
       		return true;
        }
        if (flowAction instanceof SetVlanIdActionCase && tableAction instanceof SetVlanIdActionCase) {
       		return true;
        }
        if (flowAction instanceof SetVlanPcpActionCase && tableAction instanceof SetVlanPcpActionCase) {
       		return true;
        }
        if (flowAction instanceof StripVlanActionCase && tableAction instanceof StripVlanActionCase) {
       		return true;
        }
        if (flowAction instanceof SwPathActionCase && tableAction instanceof SwPathActionCase) {
       		return true;
        }
        return false;
    }
    
    /**
     * Verify whether the given output action is permitted on this device.
     * 
     * @param action the output action being analyzed
     * @return boolean indicating whether the action is supported or not
     */
    protected boolean analyzeOutputType(org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action action) {
        return true;
    }
    
    /**
     * Verify whether the given set field action is permitted on this device.
     * 
     * @param action the set action being analyzed
     * @return boolean indicating whether the action is supported or not
     */
    protected boolean analyzeSetType(org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action action) {
        return true;
    }
            
    /**
     * 
     * @param table
     * @param isTableMiss
     * @param tableId contains the next table (table miss) in the flow we are validating and possibly adjusting
     * @return
     */
    protected boolean doesTableSupportInstGotoTable(Table table, boolean isTableMiss, Short tableId) {
      	List<TableFeatures> tableFeatures = table.getTableFeatures();
        
        for (TableFeatures tf: tableFeatures) {
            TableProperties tableProperties = tf.getTableProperties();
            List<TableFeatureProperties> tableFeatureProperties = tableProperties.getTableFeatureProperties();
            
            for (TableFeatureProperties tfp: tableFeatureProperties) {
                TableFeaturePropType tfpt = tfp.getTableFeaturePropType();
                
                if (!isTableMiss && tfpt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.NextTable) {
                	Tables nextTables = ((org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.NextTable)tfpt).getTables();
                	List<Short> tableIds = nextTables.getTableIds();
                	for (Short ids : tableIds) {
                		if (ids.equals(tableId)) {
                			return true;
                		}
                	}
                	return false;
                }
                if (isTableMiss && tfpt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.NextTableMiss) {
                	TablesMiss nextTableMisses = ((org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.NextTableMiss)tfpt).getTablesMiss();
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
