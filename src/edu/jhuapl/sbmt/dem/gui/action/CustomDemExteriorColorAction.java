package edu.jhuapl.sbmt.dem.gui.action;

import java.awt.Color;
import java.awt.Component;
import java.util.List;

import edu.jhuapl.saavtk.color.provider.ColorProvider;
import edu.jhuapl.saavtk.color.provider.ConstColorProvider;
import edu.jhuapl.saavtk.gui.dialog.ColorChooser;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemManager;

import glum.gui.action.PopAction;

/**
 * {@link PopAction} that defines the action: "Custom Color".
 *
 * @author lopeznr1
 */
class CustomDemExteriorColorAction extends PopAction<Dem>
{
	// Ref vars
	private final DemManager refManager;
	private final Component refParent;

	/** Standard Constructor */
	public CustomDemExteriorColorAction(DemManager aManager, Component aParent)
	{
		refManager = aManager;
		refParent = aParent;
	}

	@Override
	public void executeAction(List<Dem> aItemL)
	{
		Color tmpColor = refManager.getColorProviderExterior(aItemL.get(0)).getBaseColor();
		Color newColor = ColorChooser.showColorChooser(refParent, tmpColor);
		if (newColor == null)
			return;

		ColorProvider tmpCP = new ConstColorProvider(newColor);
		refManager.setColorProviderExterior(aItemL, tmpCP);
	}
}
