/*
 * (c) Copyright 2015 Hewlett-Packard Development Company, L.P.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.didm.fmt;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.didm.flowmodtest.rev140814.FlowmodTestService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.didm.flowmodtest.rev140814.PushFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.didm.api.openflow.rev151015.AdjustFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.didm.api.openflow.rev151015.AdjustFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.didm.api.openflow.rev151015.AdjustFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.didm.api.openflow.rev151015.OpenflowFeatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;

import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.TableFeaturePropType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.features.TableFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.features.table.features.TableProperties;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.features.table.features.table.properties.TableFeatureProperties;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.NextTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.match.MatchSetfield;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.set.field.match.SetFieldMatch;

import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.EthSrc;
import org.opendaylight.didm.tools.utils.FlowCreator;
import org.opendaylight.didm.tools.utils.TableFeatureCreator;

// TODO: remove
import org.opendaylight.did.flowmod.DefaultFlowMod;


public class FlowModTest implements DataChangeListener, FlowmodTestService, AutoCloseable {
	
	private static final Logger LOG = LoggerFactory.getLogger(FlowModTest.class);
	
	private final DataBroker dataBroker;
	private final RpcProviderRegistry rpcProviderRegistry;
	private SalFlowService salFlowService;
	private final BindingAwareBroker.RpcRegistration<FlowmodTestService> rpcRegistration;
	
	private ListenerRegistration<DataChangeListener> listenerRegistration;

	private AtomicLong flowIdInc = new AtomicLong();
	private AtomicLong keyInc = new AtomicLong();
	
	protected final Class<Flow> clazz = Flow.class;
	
	public FlowModTest(DataBroker dataBroker, RpcProviderRegistry rpcProviderRegistry, SalFlowService salFlowService) {
		LOG.info("FlowModTest constructor called");
		
		this.dataBroker = Preconditions.checkNotNull(dataBroker);
		this.rpcProviderRegistry = Preconditions.checkNotNull(rpcProviderRegistry);
		this.salFlowService = Preconditions.checkNotNull(salFlowService);
		
		// register as the test RPC implementation
        rpcRegistration = rpcProviderRegistry.addRpcImplementation(FlowmodTestService.class, this);
        
        listenerRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                getWildCardPath(), this, DataChangeScope.SUBTREE);
        
        if (listenerRegistration == null) {
        	LOG.info("FlowModTest problem registering onDataChanged listener");
        }
        else {
        	LOG.info("FlowModTest registering onDataChanged listener successful");
        }
        
        // Test code to create Table Features and see if we can access field and 
        // that they contain the expected values
        TableFeatureCreator tfc = new TableFeatureCreator();
        List<Table> tables3800 = tfc.create3800Table();
        
        
        // *** Debug code - Demonstrates how Table Features are accessed                                      ***
        // *** This is just an example of how to check the capabilities of the device                         ***
        // *** An example can be found in the Openflowplugin code in TableFeaturesConvertor.toTableProperties ***
        LOG.info("Number of 3800 tables: " + tables3800.size());
        for (Table table: tables3800) {
        	List<TableFeatures> tableFeatures = table.getTableFeatures();
        	LOG.info("3800 table: Number of table features: "  + tableFeatures.size());
        	for (TableFeatures tf: tableFeatures) {
        		LOG.info("3800 table: table Features: tableID " + tf.getTableId());
        		LOG.info("3800 table: table Features: config " + tf.getConfig());
        		LOG.info("3800 table: table Features: maxEntries " + tf.getMaxEntries());
        		LOG.info("3800 table: table Features: MetadataMatch " + tf.getMetadataMatch());
        		LOG.info("3800 table: table Features: MetadataWrite " + tf.getMetadataWrite());
        		LOG.info("3800 table: table Features: Name " + tf.getName());
        		
        		TableProperties tableProperties = tf.getTableProperties();
        		List<TableFeatureProperties> tableFeatureProperties = tableProperties.getTableFeatureProperties();
        		LOG.info("3800 table: table Features: Number of Table Feature Properties " + tableFeatureProperties.size());
        		
        		for (TableFeatureProperties tfp: tableFeatureProperties) {
        			LOG.info("3800 table: table Features Properties: key: " + tfp.getKey());
        			LOG.info("3800 table: table Features Properties: order: " + tfp.getOrder());
        			
        			TableFeaturePropType tfpt = tfp.getTableFeaturePropType();
        			LOG.info("3800 table: table Features: TableFeaturePropType " + tfpt.toString());
        			
        			if (tfpt instanceof Instructions) {
        				Instructions instructionsType = (Instructions)tfpt;
        				
        				org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.instructions.Instructions instructions = ((Instructions) instructionsType)
                                .getInstructions();
        				
        				List<Instruction> instructionList = instructions.getInstruction();
        				
        				for (Instruction instruction: instructionList) {
        					LOG.info("3800 table: TableFeaturePropType=Instrucitons, Instruction: " 
        				        + instruction.getInstruction().toString());
        					
        					if (instruction.getInstruction() instanceof GoToTableCase) {
        						LOG.info("3800 table: TableFeaturePropType=Instrucitons, Instruction is GOTO");
        					}
        				}
        			}
        			else if (tfpt instanceof NextTable) {
        				NextTable nextTableType = (NextTable)tfpt;
        				org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.table.feature.prop.type.table.feature.prop.type.next.table.Tables tables = ((NextTable) nextTableType)
                                .getTables();
        				List<Short> tableIds = tables.getTableIds();
        				
        				for (Short id: tableIds) {
        					LOG.info("3800 table: TableFeaturePropType=NextTable, Next Table ID: " + id);
        				}
        			}
        			else if (tfpt instanceof Match) {
        				MatchSetfield matchSetField = ((Match) tfpt).getMatchSetfield();
                        List<SetFieldMatch> setFieldMatch = null;
                        
                        if ( null != matchSetField) {
                            setFieldMatch = matchSetField.getSetFieldMatch();
                            
                            LOG.info("3800 table: TableFeaturePropType=Match, SetFieldMatch size: " + setFieldMatch.size());
                            //LOG.info("3800 table: TableFeaturePropType=Match, SetFieldMatch: " + setFieldMatch.toString());
                            for (SetFieldMatch sfMatch: setFieldMatch) {
                            	if (sfMatch.getMatchType() == EthSrc.class ) {
                            		LOG.info("3800 table: TableFeaturePropType=Match, Matches EthSrc, hasMask: " + sfMatch.isHasMask());
                            	}
                            }
                        }
        			}
        		}
        	}
 
        }
	}
	
	private InstanceIdentifier<Flow> getWildCardPath() {
		InstanceIdentifier<Flow> iid = InstanceIdentifier.create(Nodes.class).child(Node.class)
                .augmentation(FlowCapableNode.class).child(Table.class).child(Flow.class);
        LOG.info("FlowModTest Path: " + iid.toString());
        return iid;
    }
		
	@Override
    public void close() throws Exception {
		if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (final Exception e) {
                LOG.error("Error in FlowModTest close", e);
            }
            listenerRegistration = null;
        }
	}
	
	@Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changeEvent) {
		LOG.info("FlowModTest onDataChanged called");
		final Map<InstanceIdentifier<?>, DataObject> createdData = changeEvent.getCreatedData() != null
                ? changeEvent.getCreatedData() : Collections.<InstanceIdentifier<?>, DataObject> emptyMap();
                
        createData(createdData);
	}
	
	private void createData(final Map<InstanceIdentifier<?>, DataObject> createdData) {
		
        LOG.info("FlowModTest createData called");
        
		final Set<InstanceIdentifier<?>> keys = createdData.keySet() != null
                ? createdData.keySet() : Collections.<InstanceIdentifier<?>> emptySet();
                
        for (InstanceIdentifier<?> key : keys) {
        	if (clazz.equals(key.getTargetType())) {
        		final Optional<DataObject> value = Optional.of(createdData.get(key));
        		Flow flow = (Flow)value.get();
        		LOG.info("FlowmodTest: flow - " + flow.toString());
            }
        }
    }

	@Override
	public Future<RpcResult<Void>> pushFlow(PushFlowInput input) {
		// TODO Remove
		DefaultFlowMod dfm = new DefaultFlowMod();
		
		
		LOG.info("FlowmodTest - pushFlow called wiht node-id: " + input.getNodeId()
				+ " table-id: " + input.getTableId());
        
        // create a table key
        TableKey flowTableKey = new TableKey(input.getTableId().shortValue());
        
        // create a instance identifier for the node and a node connector reference
        NodeKey nodeKey = new NodeKey(new NodeId(input.getNodeId()));
        InstanceIdentifier<Node> nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class).child(Node.class, nodeKey).build();
        NodeConnectorRef nodeConnectorRef = new NodeConnectorRef(nodeInstanceIdentifier);
        
        // create the flow path
        InstanceIdentifier<Flow> flowPath = buildFlowPath(nodeConnectorRef, flowTableKey);
        
        // call the FlowCreator class buildArpFlowMod method and pass the flowpath  
        FlowCreator fc = new FlowCreator();
        Flow flow = fc.buildArpFlowMod();
        
        // before pushing the flow, adjust it
        OpenflowFeatureService rpcService = rpcProviderRegistry.getRpcService(OpenflowFeatureService.class);
        NodeRef ref = new NodeRef(nodeInstanceIdentifier);
        // build input object
        AdjustFlowInputBuilder adjustFlowInputBuilder = new AdjustFlowInputBuilder();
        adjustFlowInputBuilder.setNode(ref);
        // the adjustFlowInputBuild.setFlow method expects the flow to be of type o.o.y.g.v.u.o.d.a.o.r.a.f.i.Flow, but the 
        // flow we build is different type, but both are extended from the same parent class. So, it need to be conveted.
        // this is why using FlowBuilder. It's to a new flow of the correct type from the flow of a different type
        adjustFlowInputBuilder.setFlow(new org.opendaylight.yang.gen.v1.urn.opendaylight.didm.api.openflow.rev151015.adjust.flow.input.FlowBuilder(flow).build()); 
        AdjustFlowInput adjustFlowInput = adjustFlowInputBuilder.build();
        // call the routed rpc adjustFlow method
        Future<RpcResult<AdjustFlowOutput>> adjustFlowFuture = rpcService.adjustFlow(adjustFlowInput);
        
        // push the adjusted flows to the device
        try {
        	// extract the result from the future returned by adjustFlow()
        	RpcResult<AdjustFlowOutput> result = adjustFlowFuture.get();
        	if (result.isSuccessful()) {
        		AdjustFlowOutput output = result.getResult();
        		// the list is of type o.o.y.g.v.u.o.d.a.o.r.a.f.o.Flow so we have to specify the full type name
        		List<org.opendaylight.yang.gen.v1.urn.opendaylight.didm.api.openflow.rev151015.adjust.flow.output.Flow> flows = output.getFlow();
        		
        		// send all adjusted flows
        		for (org.opendaylight.yang.gen.v1.urn.opendaylight.didm.api.openflow.rev151015.adjust.flow.output.Flow outFlow: flows) {
        			// need to rebuild the flow of the correct type to send to salFlowService
        			FlowBuilder flowBuilder = new FlowBuilder(outFlow);
        			// not sure why, but need to rebuild the flow and add a key and Id
        			String id = String.valueOf(keyInc.getAndIncrement());
        			FlowKey key = new FlowKey(new FlowId(id));
        			flowBuilder.setKey(key);
        			flowBuilder.setId(new FlowId(id));
        			Flow flowToSend = flowBuilder.build();
        			
        			pushFlowToDevice(flowPath, flowToSend);  
        		}
        	}
        } catch( InterruptedException | ExecutionException e ) {
            LOG.info( "An error occurred while extracting result from adjustFlow: " + e );
        }

		// for now always return success 
	    return Futures.immediateFuture(RpcResultBuilder.<Void> success().build());
	}
	
	private InstanceIdentifier<Flow> buildFlowPath(NodeConnectorRef nodeConnectorRef, TableKey flowTableKey) {

	    // generate unique flow key
	    FlowId flowId = new FlowId(String.valueOf(flowIdInc.getAndIncrement()));
	    FlowKey flowKey = new FlowKey(flowId);

	    return generateFlowInstanceIdentifier(nodeConnectorRef, flowTableKey, flowKey);
	}
	
	// cut and pasted from l2switch class called InstanceIdentifierUtils
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
	   * Starts and commits data change transaction which
	   * modifies provided flow path with supplied body.
	   *
	   * @param flowPath
	   * @param flow
	   * @return transaction commit
	   */
	private Future<RpcResult<AddFlowOutput>> pushFlowToDevice(InstanceIdentifier<Flow> flowPath,
			Flow flow) {
	    final InstanceIdentifier<Table> tableInstanceId = flowPath.<Table>firstIdentifierOf(Table.class);
	    final InstanceIdentifier<Node> nodeInstanceId = flowPath.<Node>firstIdentifierOf(Node.class);
	    final AddFlowInputBuilder builder = new AddFlowInputBuilder(flow);
	    builder.setNode(new NodeRef(nodeInstanceId));
	    builder.setFlowRef(new FlowRef(flowPath));
	    builder.setFlowTable(new FlowTableRef(tableInstanceId));
	    builder.setTransactionUri(new Uri(flow.getId().getValue()));
	    return salFlowService.addFlow(builder.build());
	}

}
