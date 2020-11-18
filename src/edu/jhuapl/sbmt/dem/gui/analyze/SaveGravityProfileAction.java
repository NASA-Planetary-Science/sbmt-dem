package edu.jhuapl.sbmt.dem.gui.analyze;

import java.awt.Component;
import java.io.File;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import edu.jhuapl.saavtk.gui.dialog.CustomFileChooser;
import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.saavtk.model.structure.LineModel;
import edu.jhuapl.saavtk.structure.PolyLine;
import edu.jhuapl.sbmt.util.gravity.Gravity;

import glum.gui.GuiPaneUtil;
import glum.gui.action.PopAction;

/**
 * {@link PopAction} that defines the action: "Save Profile" action.
 *
 * @author lopeznr1
 */
public class SaveGravityProfileAction extends PopAction<PolyLine>
{
	// Ref vars
	private final LineModel<PolyLine> refManager;
	private final Component refParent;
	private final PolyhedralModel refSmallBody;

	/** Standard Constructor */
	public SaveGravityProfileAction(LineModel<PolyLine> aManager, Component aParent, PolyhedralModel aSmallBody)
	{
		refManager = aManager;
		refParent = aParent;
		refSmallBody = aSmallBody;
	}

	@Override
	public void executeAction(List<PolyLine> aItemL)
	{
		// Bail if no items specified
		if (aItemL.size() == 0)
			return;
		PolyLine tmpLine = aItemL.get(0);

		// Bail if no file selected
		File file = CustomFileChooser.showSaveDialog(refParent, "Save Profile", "profile.csv");
		if (file == null)
			return;

		try
		{
			// Require at least two control points
			if (tmpLine.getControlPoints().size() != 2)
				throw new Exception("Line must contain exactly 2 control points.");

			// Delegate
			List<Vector3D> xyzPointL = refManager.getXyzPointsFor(tmpLine);
			Gravity.saveProfileUsingGravityProgram(xyzPointL, file, refSmallBody);
		}
		catch (Exception aExp)
		{
			String infoMsg = "Saving file: " + file + "\n";
			GuiPaneUtil.showFailMessage(refParent, "Error Saving Profile", infoMsg, aExp);
		}
	}

}
