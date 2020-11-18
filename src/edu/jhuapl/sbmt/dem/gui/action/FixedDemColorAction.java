package edu.jhuapl.sbmt.dem.gui.action;

import java.awt.Color;
import java.util.List;

import edu.jhuapl.saavtk.color.provider.ColorProvider;
import edu.jhuapl.saavtk.color.provider.ConstColorProvider;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemManager;

import glum.gui.action.PopAction;

/**
 * {@link PopAction} that defines the action: "Fixed Color".
 *
 * @author lopeznr1
 */
class FixedDemColorAction extends PopAction<Dem>
{
	// Ref vars
	private final DemManager refManager;
	private final ColorProvider refCP;

	/** Standard Constructor */
	public FixedDemColorAction(DemManager aManager, Color aColor)
	{
		refManager = aManager;
		refCP = new ConstColorProvider(aColor);
	}

	/**
	 * Returns the color associated with this Action
	 */
	public Color getColor()
	{
		return refCP.getBaseColor();
	}

	@Override
	public void executeAction(List<Dem> aItemL)
	{
		refManager.setColorProviderExterior(aItemL, refCP);
	}

}