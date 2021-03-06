package sushi.monitoring.querycreation.timer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import sushi.event.SushiEvent;
import sushi.event.SushiEventType;
import sushi.event.attribute.SushiAttribute;
import sushi.event.attribute.SushiAttributeTree;
import sushi.event.attribute.SushiAttributeTypeEnum;
import sushi.event.collection.SushiMapTree;
import sushi.eventhandling.Broker;
import sushi.monitoring.bpmn.BPMNQueryMonitor;
import sushi.monitoring.bpmn.ProcessInstanceStatus;
import sushi.monitoring.querycreation.AbstractQueryCreationTest;
import sushi.persistence.Persistor;
import sushi.process.SushiProcess;
import sushi.xml.importer.BPMNParser;

/**
 * This class tests the import of a BPMN process with intermediate events, 
 * the creation of queries for this BPMN process and 
 * simulates the execution of the process to monitor the execution.
 * @author micha
 */
public class IntermediateTimerTest extends AbstractQueryCreationTest {
	
	@Before
	public void setup(){
		Persistor.useTestEnviroment();
		filePath = System.getProperty("user.dir")+"/src/test/resources/bpmn/Pizzalieferung.bpmn20.xml";
	}
	
	public static void afterQueriesTests(SushiProcess process){
		BPMNQueryMonitor queryMonitor = BPMNQueryMonitor.getInstance();
		
		assertNotNull(queryMonitor.getProcessMonitorForProcess(process));
		assertTrue(queryMonitor.getProcessMonitorForProcess(process).getProcessInstances(ProcessInstanceStatus.Finished).size() == 3);
	}

	@Test
	@Override
	public void testImport() {
		BPMNProcess = BPMNParser.generateProcessFromXML(filePath);
		assertNotNull(BPMNProcess);
		assertTrue(BPMNProcess.getBPMNElementsWithOutSequenceFlows().size() == 11);
	}

	@Test
	@Override
	public void testQueryCreation() {
		queryCreationTemplateMethod(filePath, "MessageAndTimer", Arrays.asList(new SushiAttribute("Location", SushiAttributeTypeEnum.INTEGER)));
		IntermediateTimerTest.afterQueriesTests(process);
	}

	@Override
	protected List<SushiEventType> createEventTypes() {
		List<SushiEventType> eventTypes = new ArrayList<SushiEventType>();
		
		SushiAttributeTree values;
		
		values = createAttributeTree();
		SushiEventType pizzaAuswahl = new SushiEventType("PizzaAuswahl", values, "Timestamp");
		values = createAttributeTree();
		SushiEventType pizzaBestellen = new SushiEventType("PizzaBestellen", values, "Timestamp");
		values = createAttributeTree();
		SushiEventType pizzaNachfragen = new SushiEventType("PizzaNachfragen", values, "Timestamp");
		values = createAttributeTree();
		SushiEventType pizzaEssen = new SushiEventType("PizzaEssen", values, "Timestamp");
		values = createAttributeTree();
		SushiEventType pizzaErhalten = new SushiEventType("PizzaErhalten", values, "Timestamp");
		values = createAttributeTree();
		SushiEventType pizzaErhaltenDelay = new SushiEventType("PizzaErhaltenDelay", values, "Timestamp");
		
		
		eventTypes.add(pizzaAuswahl);
		eventTypes.add(pizzaBestellen);
		eventTypes.add(pizzaErhalten);
		eventTypes.add(pizzaNachfragen);
		eventTypes.add(pizzaErhaltenDelay);
		eventTypes.add(pizzaEssen);
		
		return eventTypes;
	}

	@Override
	protected void simulate(List<SushiEventType> eventTypes) {
		//Events für verschiedene Prozessinstanzen erzeugen und an Broker senden
		for(SushiEventType eventType : eventTypes){
			//Nur den Timer-Pfad ausführen
			if(eventType.getTypeName().equals("PizzaErhalten")){
				continue;
			}
			
			//Warten, um den Timer zu testen
			if(eventType.getTypeName().equals("PizzaNachfragen")){
				try {
					Thread.sleep(15 * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			SushiMapTree<String, Serializable> values;
			SushiEvent event;
			
			values = new SushiMapTree<String, Serializable>();
			values.put("Location", 1);
			values.put("Movie", "Movie Name");
			event = new SushiEvent(eventType, new Date(), values);
			Broker.send(event);
			
			values = new SushiMapTree<String, Serializable>();
			values.put("Location", 2);
			values.put("Movie", "Movie Name");
			event = new SushiEvent(eventType, new Date(), values);
			Broker.send(event);
			
			values = new SushiMapTree<String, Serializable>();
			values.put("Location", 3);
			values.put("Movie", "Movie Name");
			event = new SushiEvent(eventType, new Date(), values);
			Broker.send(event);
		}
	}
	
	@AfterClass
	public static void tearDown() {
		AbstractQueryCreationTest.resetDatabase();
	}
}
