package edu.jhuapl.sbmt.dem.gui.popup;

import java.awt.Component;
import java.util.Collection;
import java.util.List;

import javax.swing.JMenuItem;

import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemManager;
import edu.jhuapl.sbmt.dem.io.CustomShapeModelUtil;

import glum.gui.action.PopAction;

/**
 * Object that defines the action: "Export as Custom Model".
 * <p>
 * This action allows the user to export a single selected {@link Dem} as a
 * custom shape model.
 * <p>
 * The actual implementation details of this export action is defined in
 * {@link CustomShapeModelUtil#exportDEMToCustomModel}
 *
 * @author lopeznr1
 */
public class ExportAsCustomModelAction extends PopAction<Dem>
{
	// Ref vars
	private final DemManager refManager;
	private final PolyhedralModel refSmallBody;
	private final Component refParent;

	/** Standard Constructor */
	public ExportAsCustomModelAction(DemManager aManager, PolyhedralModel aSmallBody, Component aParent)
	{
		refManager = aManager;
		refSmallBody = aSmallBody;
		refParent = aParent;
	}

	@Override
	public void executeAction(List<Dem> aItemL)
	{
		CustomShapeModelUtil.exportDemToCustomShapeModel(refParent, refManager, aItemL, refSmallBody);
	}

	@Override
	public void setChosenItems(Collection<Dem> aItemC, JMenuItem aAssocMI)
	{
		super.setChosenItems(aItemC, aAssocMI);

		boolean isEnabled = DemGuiUtil.isOneAndShown(refManager, aItemC);
		aAssocMI.setEnabled(isEnabled);
	}

}
