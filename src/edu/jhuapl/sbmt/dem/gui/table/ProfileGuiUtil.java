package edu.jhuapl.sbmt.dem.gui.table;

import java.awt.Component;
import java.util.Collection;
import java.util.Set;

import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.saavtk.model.structure.LineModel;
import edu.jhuapl.saavtk.structure.PolyLine;
import edu.jhuapl.sbmt.dem.gui.analyze.SaveGravityProfileAction;

import glum.gui.action.PopupMenu;
import glum.gui.panel.generic.PromptPanel;

/**
 * Utility class that provides a collection of methods useful for working with
 * the (dem) profile related GUIs.
 *
 * @author lopeznr1
 */
public class ProfileGuiUtil
{
	/**
	 * Forms the popup menu associated with (dem) profiles.
	 */
	public static PopupMenu<PolyLine> formPopupMenu(LineModel<PolyLine> aManager, Component aParent,
			PolyhedralModel aSmallBody)
	{
		PopupMenu<PolyLine> retPM = new PopupMenu<>(aManager);

		retPM.installPopAction(new SaveGravityProfileAction(aManager, aParent, aSmallBody), "Save Profile...");

		return retPM;
	}

	/**
	 * Utility method that prompts the user for confirmation of deleting the
	 * specified items.
	 */
	public static boolean promptDeletionConfirmation(Component aParent, LineModel<PolyLine> aManager,
			Collection<PolyLine> aItemC)
	{
		Set<PolyLine> pickS = aManager.getSelectedItems();
		String infoMsg = "Are you sure you want to delete " + pickS.size() + " profiles?\n";

		PromptPanel tmpPanel = new PromptPanel(aParent, "Confirm Deletion", 400, 160);
		tmpPanel.setInfo(infoMsg);
		tmpPanel.setSize(400, 150);
		tmpPanel.setVisibleAsModal();
		return tmpPanel.isAccepted();
	}

}
