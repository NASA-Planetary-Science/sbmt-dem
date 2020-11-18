package edu.jhuapl.sbmt.dem.gui.popup;

import java.util.Collection;
import java.util.List;

import javax.swing.JMenuItem;

import edu.jhuapl.saavtk.gui.util.MessageUtil;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemManager;

import glum.gui.action.PopAction;

/**
 * Object that defines the action: "Hide/Show Boundaries".
 *
 * @author lopeznr1
 */
class HideShowExteriorAction extends PopAction<Dem>
{
	// Ref vars
	private final DemManager refManager;

	// Attributes
	private final String itemLabelStr;

	/** Standard Constructor */
	public HideShowExteriorAction(DemManager aManager, String aItemLabelStr)
	{
		refManager = aManager;

		itemLabelStr = aItemLabelStr;
	}

	@Override
	public void executeAction(List<Dem> aItemL)
	{
		// Determine if all tracks are shown
		boolean isAllShown = true;
		for (Dem aItem : aItemL)
			isAllShown &= refManager.getIsVisibleExterior(aItem) == true;

		// Update the tracks visibility based on whether they are all shown
		boolean tmpBool = isAllShown == false;
		refManager.setIsVisibleExterior(aItemL, tmpBool);
	}

	@Override
	public void setChosenItems(Collection<Dem> aItemC, JMenuItem aAssocMI)
	{
		super.setChosenItems(aItemC, aAssocMI);

		// Determine if all items are shown
		boolean isAllShown = true;
		for (Dem aItem : aItemC)
			isAllShown &= refManager.getIsVisibleExterior(aItem) == true;

		// Determine the display string
		String displayStr = "Hide " + itemLabelStr;
		if (isAllShown == false)
			displayStr = "Show " + itemLabelStr;
		displayStr = MessageUtil.toPluralForm(displayStr, aItemC);

		// Update the text of the associated MenuItem
		aAssocMI.setText(displayStr);
	}

}
