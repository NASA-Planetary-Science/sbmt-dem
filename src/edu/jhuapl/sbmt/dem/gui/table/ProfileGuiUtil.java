package edu.jhuapl.sbmt.dem.gui.table;

import java.awt.Component;

import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.saavtk.model.structure.LineModel;
import edu.jhuapl.saavtk.structure.PolyLine;
import edu.jhuapl.sbmt.dem.gui.analyze.SaveGravityProfileAction;

import glum.gui.action.PopupMenu;

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
	public static PopupMenu<PolyLine> formPopupMenu(LineModel<PolyLine> refItemManager, Component aParent,
			PolyhedralModel aSmallBody)
	{
		PopupMenu<PolyLine> retPM = new PopupMenu<>(refItemManager);

		retPM.installPopAction(new SaveGravityProfileAction(refItemManager, aParent, aSmallBody), "Save Profile...");

		return retPM;
	}

}
