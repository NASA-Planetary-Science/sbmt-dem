package edu.jhuapl.sbmt.dem.gui.popup;

import java.util.Collection;
import java.util.List;

import javax.swing.JMenuItem;

import edu.jhuapl.saavtk.gui.util.MessageUtil;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemManager;

import glum.gui.action.PopAction;

/**
 * Object that defines the action: "Analyze".
 * <p>
 * A UI element will be displayed which allows for detailed analysis of the
 * selected {@link Dem}s.
 *
 * @author lopeznr1
 */
public class AnalyzeAction extends PopAction<Dem>
{
	// Ref vars
	private final DemManager refManager;

	/** Standard Constructor */
	public AnalyzeAction(DemManager aManager)
	{
		refManager = aManager;
	}

	@Override
	public void executeAction(List<Dem> aItemL)
	{
		// Delegate
		refManager.setIsDemAnalyzed(aItemL, true);
	}

	@Override
	public void setChosenItems(Collection<Dem> aItemC, JMenuItem aAssocMI)
	{
		super.setChosenItems(aItemC, aAssocMI);

		boolean isEnabled = aItemC.size() > 0;
		aAssocMI.setEnabled(isEnabled);

		// Determine the display string
		String displayStr = "Analyze DTM";
		displayStr = MessageUtil.toPluralForm(displayStr, aItemC);

		// Update the text of the associated MenuItem
		aAssocMI.setText(displayStr);
	}

}
