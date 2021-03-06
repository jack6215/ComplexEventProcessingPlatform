package sushi.query.bpmn;

import sushi.bpmn.decomposition.Component;
import sushi.bpmn.element.AbstractBPMNElement;
import sushi.query.PatternQueryType;
import sushi.query.SushiPatternQuery;
import sushi.query.SushiQueryTypeEnum;


/**
 * This query factory creates queries for components of type AND.
 * @author micha
 */
public class AndQueryFactory extends AbstractPatternQueryFactory {

	/**
	 * Constructor to create AND queries with a query factory.
	 * @param patternQueryGenerator
	 */
	public AndQueryFactory(PatternQueryGenerator patternQueryGenerator) {
		super(patternQueryGenerator);
	}
	
	@Override
	protected SushiPatternQuery generateQuery(AbstractBPMNElement element, AbstractBPMNElement catchingMonitorableElement, SushiPatternQuery parentQuery) {
		if(element instanceof Component){
			Component component = (Component) element;
			//Component sollte mehrere Polygone beinhalten
			//Operator: AND
			SushiPatternQuery query = new SushiPatternQuery(generateQueryName("And"), null, SushiQueryTypeEnum.LIVE, PatternQueryType.AND, this.orderElements(component));
			
			String queryString = generateQueryString(component, EsperPatternOperators.AND, catchingMonitorableElement, query);
			query.setQueryString(queryString);
			addQueryRelationship(parentQuery, query);
			
			System.out.println(query.getTitle() + ": " + query.getQueryString());
			
			registerQuery(query);
			if(query != null && query.getListener() != null){
				query.getListener().setCatchingElement(catchingMonitorableElement);
			}
			
			return query;
		} else {
			System.err.println("Input element should be a component for an AND-query!");
			return null;
		}
	}

}
