package edu.jhuapl.sbmt.dem.gui.popup;

import java.util.Collection;
import java.util.List;

import javax.swing.JMenuItem;

import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemManager;

import glum.gui.action.PopAction;

/**
 * Object that defines the action: "Center".
 * <p>
 * Action which will cause the {@link Dem} to be centered in the view of the
 * {@link Renderer}.
 *
 * @author lopeznr1
 */
class CenterAction extends PopAction<Dem>
{
	// Ref vars
	private final DemManager refManager;
	private final Renderer refRenderer;

	/** Standard Constructor */
	public CenterAction(DemManager aManager, Renderer aRenderer)
	{
		refManager = aManager;
		refRenderer = aRenderer;
	}

	@Override
	public void executeAction(List<Dem> aItemL)
	{
		Dem tmpItem = aItemL.get(0);

		// Delegate
		ActionUtil.centerOnDem(refManager, tmpItem, refRenderer);
	}

	@Override
	public void setChosenItems(Collection<Dem> aItemC, JMenuItem aAssocMI)
	{
		super.setChosenItems(aItemC, aAssocMI);

		boolean isEnabled = DemGuiUtil.isOneAndShown(refManager, aItemC);
		aAssocMI.setEnabled(isEnabled);
	}

}
