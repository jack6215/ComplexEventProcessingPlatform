package sushi.application.pages.simulator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.TreeColumn;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.agilecoders.wicket.markup.html.bootstrap.tabs.Collapsible;

import sushi.application.components.form.BlockingAjaxButton;
import sushi.application.components.form.WarnOnExitForm;
import sushi.application.components.table.SelectEntryPanel;
import sushi.application.components.tree.SushiLabelTreeTable;
import sushi.application.pages.AbstractSushiPage;
import sushi.application.pages.correlation.AdvancedCorrelationPanel;
import sushi.application.pages.correlation.SimpleCorrelationPanel;
import sushi.application.pages.correlation.SimpleCorrelationWithRulesPanel;
import sushi.application.pages.simulator.model.SimulationTreeTableElement;
import sushi.application.pages.simulator.model.SimulationTreeTableExpansion;
import sushi.application.pages.simulator.model.SimulationTreeTableExpansionModel;
import sushi.application.pages.simulator.model.SimulationTreeTableProvider;
import sushi.application.pages.simulator.model.SimulationTreeTableToModelConverter;
import sushi.bpmn.decomposition.ANDComponent;
import sushi.bpmn.decomposition.IPattern;
import sushi.bpmn.decomposition.LoopComponent;
import sushi.bpmn.decomposition.SequenceComponent;
import sushi.bpmn.decomposition.XORComponent;
import sushi.bpmn.element.AbstractBPMNElement;
import sushi.bpmn.element.BPMNProcess;
import sushi.bpmn.element.BPMNXORGateway;
import sushi.event.SushiEventType;
import sushi.event.attribute.SushiAttribute;
import sushi.event.collection.SushiTree;
import sushi.process.SushiProcess;
import sushi.simulation.DerivationType;
import sushi.simulation.SimulationUtils;
import sushi.simulation.Simulator;
import sushi.simulation.ValueRule;
import sushi.util.Tuple;

/**
 * Panel representing the content panel for the first tab.
 */
public class SimpleSimulationPanel extends SimulationPanel {
	
	private static final long serialVersionUID = -7896431319431474548L;
	private DropDownChoice<String> processSelect;
	private DropDownChoice<String> eventTypeSelect;
	private List<String> processNameList;
	private List<String> eventTypeAndPatternList = new ArrayList<String>();
	private SushiProcess selectedProcess;
	private SushiLabelTreeTable<SimulationTreeTableElement<Object>, String> treeTable;
	private UnexpectedEventPanel unexpectedEventPanel;

	public SimpleSimulationPanel(String id, final AbstractSushiPage abstractSushiPage) {
		super(id, abstractSushiPage);
		
		createProcessList();
		createEventTypeList(selectedProcess);
		
		buildMainLayout();
	}

	private void buildMainLayout() {
		layoutForm = new WarnOnExitForm("layoutForm");
		
		addProcessSelect(layoutForm);
		
		addEventTypeSelect(layoutForm);
		
		addButtons(layoutForm);
		
		createTreeTable(layoutForm);
		
		instanceNumberInput = new TextField<String>("instanceNumberInput", Model.of(""));
		layoutForm.add(instanceNumberInput);
		
		daysNumberInput = new TextField<String>("daysNumberInput", Model.of(""));
		layoutForm.add(daysNumberInput);
		
		add(layoutForm);
		
		addTabs();
	}

	private void addButtons(Form<Void> layoutForm) {
		AjaxButton addButton = new AjaxButton("addButton", layoutForm) {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onSubmit(AjaxRequestTarget target, Form form) {
				if(treeTableProvider.getSelectedTreeTableElements().size() > 1){
					abstractSushiPage.getFeedbackPanel().error("Please select only one value to add elements!");
					abstractSushiPage.getFeedbackPanel().setVisible(true);
					target.add(abstractSushiPage.getFeedbackPanel());
				} else if(!eventTypeSelect.getValue().isEmpty()){
					//TODO: Element im TreeTableProvider hinzufügen
					String eventTypeSelectValue = eventTypeSelect.getChoices().get(Integer.parseInt(eventTypeSelect.getValue()));
					SimulationTreeTableElement<Object> treeTableElement;
					if(IPattern.contains(eventTypeSelectValue)){
						AbstractBPMNElement component = null;
						if(eventTypeSelectValue.equals(IPattern.AND.value)){
							component = new ANDComponent(null, null, null, null);
						} else if(eventTypeSelectValue.equals(IPattern.XOR.value)){
							component = new XORComponent(null, null, null, null);
						} else if(eventTypeSelectValue.equals(IPattern.SEQUENCE.value)){
							component = new SequenceComponent(null, null, null, null);
						} else if(eventTypeSelectValue.equals(IPattern.LOOP.value)){
							component = new LoopComponent(null, null, null, null);
						}
						treeTableElement = new SimulationTreeTableElement<Object>(treeTableProvider.getNextID(), component);
						treeTableProvider.addTreeTableElement(treeTableElement);
					} else {
						SushiEventType eventType = SushiEventType.findByTypeName(eventTypeSelectValue);
						if(eventType != null){
							treeTableElement = new SimulationTreeTableElement<Object>(treeTableProvider.getNextID(), eventType);
							treeTableProvider.addTreeTableElement(treeTableElement);
							SimulationTreeTableElement<Object> childTreeTableElement;
							for(SushiAttribute attribute : eventType.getValueTypes()){
								childTreeTableElement = new SimulationTreeTableElement<Object>(treeTableProvider.getNextID(), attribute);
								treeTableProvider.addTreeTableElementWithParent(childTreeTableElement, treeTableElement);
							}
						}
					}
					advancedValuesPanel.refreshAttributeChoice();
					target.add(advancedValuesPanel);
					unexpectedEventPanel.refreshUsedEventTypes();
					target.add(unexpectedEventPanel);
					target.add(treeTable);
				}
	        }
	    };
	    layoutForm.add(addButton);
	    
	    AjaxButton editButton = new AjaxButton("editButton", layoutForm) {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onSubmit(AjaxRequestTarget target, Form form) {
	        }
	    };
	    layoutForm.add(editButton);
	    
	    AjaxButton deleteButton = new AjaxButton("deleteButton", layoutForm) {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onSubmit(AjaxRequestTarget target, Form form) {
				treeTableProvider.deleteSelectedEntries();
				target.add(treeTable);
			}
	    };
	    layoutForm.add(deleteButton);
	    
	    AjaxButton simulateButton = new BlockingAjaxButton("simulateButton", layoutForm) {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onSubmit(AjaxRequestTarget target, Form form) {
				super.onSubmit(target, form);
				String instanceNumber = instanceNumberInput.getValue();
				int numberOfInstances;
				if(instanceNumber != null && !instanceNumber.isEmpty()){
					numberOfInstances = Integer.parseInt(instanceNumberInput.getValue());
				} else {
					numberOfInstances = 1;
				}
				
				String daysNumber = daysNumberInput.getValue();
				int numberOfDays;
				if(daysNumber != null && !instanceNumber.isEmpty()){
					numberOfDays = Integer.parseInt(daysNumberInput.getValue());
				} else {
					numberOfDays = 1;
				}
				
				SushiTree<Object> modelTree = treeTableProvider.getModelAsTree();
				
				Map<SushiAttribute, List<Serializable>> attributeValues = treeTableProvider.getAttributeValuesFromModel();
				BPMNProcess model = new SimulationTreeTableToModelConverter().convertTreeToModel(modelTree);
				
				//TODO: wahrscheinlichkeiten auslesen
				Map<BPMNXORGateway, List<Tuple<AbstractBPMNElement, String>>> xorSplitsWithSuccessorProbabilityStrings = getProbabilityForXorSuccessors(model);
				System.out.println(xorSplitsWithSuccessorProbabilityStrings);
				Map<BPMNXORGateway, List<Tuple<AbstractBPMNElement, Integer>>> xorSplitsWithSuccessorProbabilities = SimulationUtils.convertProbabilityStrings(xorSplitsWithSuccessorProbabilityStrings);
				System.out.println(xorSplitsWithSuccessorProbabilities);
				Map<SushiEventType, String> eventTypesDurationStrings = treeTableProvider.getEventTypesWithDuration();
				Map<AbstractBPMNElement, String> tasksDurationString = SimulationUtils.getBPMNElementsFromEventTypes(eventTypesDurationStrings, model);
				
				Map<SushiEventType, String> eventTypesDerivationStrings = treeTableProvider.getEventTypesWithDuration();
				Map<AbstractBPMNElement, String> tasksDerivationString = SimulationUtils.getBPMNElementsFromEventTypes(eventTypesDerivationStrings, model);
				
				Map<SushiEventType, DerivationType> eventTypesDerivationTypes = treeTableProvider.getEventTypesWithDerivationType();
				Map<AbstractBPMNElement, DerivationType> tasksDerivationTypes = SimulationUtils.getBPMNElementsFromEventTypes2(eventTypesDerivationTypes, model);
				
				SushiProcess process = SushiProcess.findByName(processSelect.getModelObject()).get(0);
				Simulator simulator = new Simulator(process, model, attributeValues, tasksDurationString, tasksDerivationString, tasksDerivationTypes, xorSplitsWithSuccessorProbabilities);
				simulator.addAdvancedValueRules(advancedValuesPanel.getValueRules());
				simulator.simulate(numberOfInstances, numberOfDays);
			}

			private Map<BPMNXORGateway, List<Tuple<AbstractBPMNElement, String>>> getProbabilityForXorSuccessors(BPMNProcess model) {
				Map<BPMNXORGateway, List<Tuple<AbstractBPMNElement, SushiEventType>>> xorPathProbabilityEvents = SimulationUtils.getXORSplitsWithFollowingEventTypes(model);
				System.out.println(xorPathProbabilityEvents);
				Map<BPMNXORGateway, List<Tuple<AbstractBPMNElement, String>>> xorSuccessorsProbability = new HashMap<BPMNXORGateway, List<Tuple<AbstractBPMNElement, String>>>();
				//TODO: eventTypes finden --> elternelement besuchen bis xor gefunden -> probability auslesen
				List<SimulationTreeTableElement<Object>> eventTypeElements = treeTableProvider.getEventTypeElements();
				//Paare von Nachfolgeelementen des XOR-Splits und 1. folgendem eventTyp durchlaufen
				for(BPMNXORGateway xorGateway : xorPathProbabilityEvents.keySet()){
					List<Tuple<AbstractBPMNElement, SushiEventType>> listOfTuples = xorPathProbabilityEvents.get(xorGateway);
					List<Tuple<AbstractBPMNElement, String>> oneXorSuccessors = new ArrayList<Tuple<AbstractBPMNElement, String>>();
					for(Tuple<AbstractBPMNElement, SushiEventType> tuple : listOfTuples){
						if(tuple.x != null){
							 SimulationTreeTableElement<Object> targetElement = findEventTypeElementWithEventType(eventTypeElements, tuple);
							 while(!(targetElement.getParent().getContent() instanceof XORComponent)){
								 targetElement = targetElement.getParent();
							 }
							 Tuple<AbstractBPMNElement, String> successorProbability = new Tuple<AbstractBPMNElement, String>(tuple.x, targetElement.getProbability());
							 oneXorSuccessors.add(successorProbability);
							 xorSuccessorsProbability.put(xorGateway, oneXorSuccessors);
						}
						else{
							//leerer Pfad
						}
					}
				}
				return xorSuccessorsProbability;
			}
	    };
	    layoutForm.add(simulateButton);
	}

	private void createTreeTable(Form<Void> layoutForm) {
		List<IColumn<SimulationTreeTableElement<Object>, String>> columns = createColumns();
		
		treeTable = new SushiLabelTreeTable<SimulationTreeTableElement<Object>, String>(
					"sequenceTree", 
					columns, 
					treeTableProvider, 
					Integer.MAX_VALUE, 
					new SimulationTreeTableExpansionModel<Object>());
		
		treeTable.setOutputMarkupId(true);

		treeTable.getTable().addTopToolbar(new HeadersToolbar<String>(treeTable.getTable(), treeTableProvider));
		
		SimulationTreeTableExpansion.get().expandAll();
		
		layoutForm.add(treeTable);
	}
	
	private List<IColumn<SimulationTreeTableElement<Object>, String>> createColumns() {
		List<IColumn<SimulationTreeTableElement<Object>, String>> columns = new ArrayList<IColumn<SimulationTreeTableElement<Object>, String>>();
    
		columns.add(new PropertyColumn<SimulationTreeTableElement<Object>, String>(Model.of("ID"), "ID"));
		columns.add(new TreeColumn<SimulationTreeTableElement<Object>, String>(Model.of("Sequence"), "content"));
		
		columns.add(new AbstractColumn(new Model("Probability")) {
			@Override
			public void populateItem(Item cellItem, String componentId, IModel rowModel) {
				
				int entryId = ((SimulationTreeTableElement<Object>) rowModel.getObject()).getID();
				if(((SimulationTreeTableElement<Object>) rowModel.getObject()).hasParent() && ((SimulationTreeTableElement<Object>) rowModel.getObject()).getParent().getContent() instanceof XORComponent){
					ProbabilityEntryPanel probabilityEntryPanel = new ProbabilityEntryPanel(componentId, entryId, treeTableProvider);
					cellItem.add(probabilityEntryPanel);
					probabilityEntryPanel.setTable(treeTable);
				}
				else{
					cellItem.add(new EmptyPanel(componentId, entryId, treeTableProvider));
				}
			}
		});
		
		columns.add(new AbstractColumn<SimulationTreeTableElement<Object>, String>(new Model("Select")) {
			@Override
			public void populateItem(Item cellItem, String componentId, IModel rowModel) {
				
				int entryId = ((SimulationTreeTableElement<Object>) rowModel.getObject()).getID();
				if(((SimulationTreeTableElement<Object>) rowModel.getObject()).canHaveSubElements()){
					cellItem.add(new SelectEntryPanel(componentId, entryId, treeTableProvider));
				}
				else{
					cellItem.add(new EmptyPanel(componentId, entryId, treeTableProvider));
				}
			}
		});
		
		columns.add(new AbstractColumn<SimulationTreeTableElement<Object>, String>(new Model("Value")) {
			@Override
			public void populateItem(Item cellItem, String componentId, IModel rowModel) {
				
				int entryId = ((SimulationTreeTableElement<Object>) rowModel.getObject()).getID();
				if(((SimulationTreeTableElement<Object>) rowModel.getObject()).editableColumnsVisible()){
					TextFieldEntryPanel textFieldEntryPanel = new TextFieldEntryPanel(componentId, entryId, treeTableProvider);
					cellItem.add(textFieldEntryPanel);
					textFieldEntryPanel.setTable(treeTable);
				}
				else{
					cellItem.add(new EmptyPanel(componentId, entryId, treeTableProvider));
				}
			}
		});
		
		columns.add(new AbstractColumn(new Model("Derivation-Type")) {
			@Override
			public void populateItem(Item cellItem, String componentId, IModel rowModel) {
				
				int entryId = ((SimulationTreeTableElement<Object>) rowModel.getObject()).getID();
				if(((SimulationTreeTableElement<Object>) rowModel.getObject()).getContent() instanceof SushiEventType){
					DerivationTypeDropDownChoicePanel derivationChoicePanel = new DerivationTypeDropDownChoicePanel(componentId, entryId, treeTableProvider);
					cellItem.add(derivationChoicePanel);
					derivationChoicePanel.setTable(treeTable);
				}
				else{
					cellItem.add(new EmptyPanel(componentId, entryId, treeTableProvider));
				}
			}
		});
		
		
		columns.add(new AbstractColumn(new Model("Duration / Time difference to previous")) {
			@Override
			public void populateItem(Item cellItem, String componentId, IModel rowModel) {
				
				int entryId = ((SimulationTreeTableElement<Object>) rowModel.getObject()).getID();
				if(((SimulationTreeTableElement<Object>) rowModel.getObject()).getContent() instanceof SushiEventType){
					DurationEntryPanel textFieldEntryPanel = new DurationEntryPanel(componentId, entryId, treeTableProvider);
					cellItem.add(textFieldEntryPanel);
					textFieldEntryPanel.setTable(treeTable);
				}
				else{
					cellItem.add(new EmptyPanel(componentId, entryId, treeTableProvider));
				}
			}
		});
		
		return columns;
	}
	
	private void addProcessSelect(Form<Void> layoutForm) {
		processSelect = new DropDownChoice<String>("processSelect", new Model<String>(), processNameList);
		processSelect.setOutputMarkupId(true);
		processSelect.add(new AjaxFormComponentUpdatingBehavior("onchange") {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				String processValue = processSelect.getValue();
				if(processValue != null && !processValue.isEmpty()){
					List<SushiProcess> processList = SushiProcess.findByName(processSelect.getChoices().get(Integer.parseInt(processSelect.getValue())));
					if(processList.size() > 0){
						selectedProcess = processList.get(0);
						
						createEventTypeList(selectedProcess);
						target.add(eventTypeSelect);
					}
				}
				treeTableProvider.setCorrelationAttributes(selectedProcess.getCorrelationAttributes());
			}
		});
		
		layoutForm.add(processSelect);
	}

	private void addEventTypeSelect(Form<Void> layoutForm) {
		eventTypeSelect = new DropDownChoice<String>("eventTypeSelect", new Model<String>(), eventTypeAndPatternList);
		eventTypeSelect.setOutputMarkupId(true);
		layoutForm.add(eventTypeSelect);
	}
	
	private void createProcessList() {
		processNameList = new ArrayList<String>();
		for (SushiProcess process : SushiProcess.findAll()) {
			processNameList.add(process.getName());
		}
	}
	
	private void createEventTypeList(SushiProcess selectedProcess) {
		eventTypeAndPatternList.clear();
		if(selectedProcess != null){
			for (SushiEventType eventType : selectedProcess.getEventTypes()) {
				eventTypeAndPatternList.add(eventType.getTypeName());
			}
			eventTypeAndPatternList.addAll(IPattern.getValues());
		}
	}

	private SimulationTreeTableElement<Object> findEventTypeElementWithEventType(List<SimulationTreeTableElement<Object>> eventTypeElements, Tuple<AbstractBPMNElement, SushiEventType> tuple) {
		for(SimulationTreeTableElement<Object> eventTypeElement : eventTypeElements){
			if(eventTypeElement.getContent() == tuple.y){
				return eventTypeElement;
			}
		}
		return null;
	}

	@Override
	protected void addUnexpectedEventPanel(List<ITab> tabs) {
		
		tabs.add(new AbstractTab(new Model<String>("Unexpected Events (instance-dependent)")) {

			public Panel getPanel(String panelId) {
				unexpectedEventPanel = new UnexpectedEventPanel(panelId, simulationPanel);
				return unexpectedEventPanel;
			}
		});
		
	}
};
