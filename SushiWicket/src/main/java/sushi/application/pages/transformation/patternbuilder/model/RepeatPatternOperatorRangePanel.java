package sushi.application.pages.transformation.patternbuilder.model;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;

import sushi.application.pages.transformation.AdvancedTransformationRuleEditorPanel;
import sushi.transformation.element.PatternOperatorElement;
import sushi.transformation.element.RangeElement;

public class RepeatPatternOperatorRangePanel extends Panel {
	
	private static final long serialVersionUID = 1L;
	private Form<Void> layoutForm;
	private int matchCount;
	private TextField<Integer> matchCountInput;

	public RepeatPatternOperatorRangePanel(String id, PatternOperatorElement element, final AdvancedTransformationRuleEditorPanel panel) {
		super(id);
		
		layoutForm = new Form<Void>("layoutForm");
		
		final RangeElement rangeElement = element.getRangeElement();
		
		matchCount = rangeElement.getLeftEndpoint();
		
		matchCountInput = new TextField<Integer>("matchCountInput", new PropertyModel<Integer>(this, "matchCount"));
		OnChangeAjaxBehavior onChangeAjaxBehavior = new OnChangeAjaxBehavior() {
			private static final long serialVersionUID = 2251803290291534439L;
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				rangeElement.setLeftEndpoint(matchCount);
				target.add(panel.getAttributeTreePanel().getAttributeTreeTable());
			}
        };
        matchCountInput.add(onChangeAjaxBehavior);
        matchCountInput.setOutputMarkupId(true);
		layoutForm.add(matchCountInput);
		
		add(layoutForm);
	}
}
