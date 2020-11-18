package edu.jhuapl.sbmt.dem.gui.action;

import java.util.Collection;
import java.util.List;

import javax.swing.JMenuItem;

import edu.jhuapl.saavtk.color.provider.ColorProvider;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemManager;

import glum.gui.action.PopAction;

/**
 * {@link PopAction} that defines the action: "Reset Colors".
 *
 * @author lopeznr1
 */
class ResetExteriorColorAction extends PopAction<Dem>
{
	// Ref vars
	private final DemManager refManager;

	/** Standard Constructor */
	public ResetExteriorColorAction(DemManager aManager)
	{
		refManager = aManager;
	}

	@Override
	public void executeAction(List<Dem> aItemL)
	{
		// Clear out the (Custom) ColorProviders
		refManager.setColorProviderExterior(aItemL, ColorProvider.Invalid);
	}

	@Override
	public void setChosenItems(Collection<Dem> aItemC, JMenuItem aAssocMI)
	{
		super.setChosenItems(aItemC, aAssocMI);

		// Determine if any of the lidar colors can be reset
		boolean isResetAvail = false;
		for (Dem aItem : aItemC)
			isResetAvail |= refManager.hasCustomExteriorColorProvider(aItem) == true;

		aAssocMI.setEnabled(isResetAvail);
	}
}