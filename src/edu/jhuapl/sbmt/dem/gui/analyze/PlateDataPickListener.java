package edu.jhuapl.sbmt.dem.gui.analyze;

import java.awt.event.InputEvent;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import vtk.vtkActor;
import vtk.vtkFloatArray;
import vtk.vtkIdList;
import vtk.vtkPolyData;

import edu.jhuapl.saavtk.feature.FeatureType;
import edu.jhuapl.saavtk.pick.PickListener;
import edu.jhuapl.saavtk.pick.PickMode;
import edu.jhuapl.saavtk.pick.PickTarget;
import edu.jhuapl.saavtk.pick.PickUtil;
import edu.jhuapl.saavtk.status.StatusNotifier;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.saavtk.view.AssocActor;
import edu.jhuapl.sbmt.dem.vtk.VtkDemSurface;

import glum.text.SigFigNumberFormat;

/**
 * Object that is responsible for providing a read out of the (plate) value
 * associated with a {@link VtkDemSurface}.
 *
 * @author lopeznr1
 */
public class PlateDataPickListener implements PickListener
{
	// Ref vars
	private final StatusNotifier refStatusNotifier;

	// Cache vars
	private VtkDemSurface cSurface;
	private Vector3D cPosition;
	private int cCellId;

	// State vars
	private vtkIdList workIL;

	/** Standard Constructor **/
	public PlateDataPickListener(StatusNotifier aStatusNotifier)
	{
		refStatusNotifier = aStatusNotifier;

		cSurface = null;
		cPosition = null;
		cCellId = -1;

		workIL = new vtkIdList();
	}

	/**
	 * Updates the UI to reflect the plate value.
	 */
	public void updateDisplay()
	{
		// Bail if no selected surface / point
		if (cSurface == null || cPosition == null)
			return;

		// Bail if no active FeatureType
		FeatureType tmpFT = cSurface.getDrawAttr().getIntCP().getFeatureType();
		if (tmpFT == null || tmpFT == FeatureType.Invalid)
			return;

		// Retrieve the value
		double[] tmpPtArr = cPosition.toArray();
		vtkPolyData vTmpPD = cSurface.getVtkInteriorPD();
		vtkFloatArray vTmpFA = cSurface.getValuesForPointData(tmpFT);
		double tmpVal = PolyDataUtil.interpolateWithinCell(vTmpPD, vTmpFA, cCellId, tmpPtArr, workIL);

		// Send out the status update
		SigFigNumberFormat tmpFmt = new SigFigNumberFormat(5);
		String unitStr = "";
		if (tmpFT.getUnit() != null)
			unitStr = tmpFT.getUnit();
		refStatusNotifier.setPriStatus(tmpFT.getName() + ": " + tmpFmt.format(tmpVal) + " " + unitStr, null);
	}

	@Override
	public void handlePickAction(InputEvent aEvent, PickMode aMode, PickTarget aPrimaryTarg, PickTarget aSurfaceTarg)
	{
		// Respond only to active events
		if (aMode != PickMode.ActivePri)
			return;

		// Bail if popup trigger
		if (PickUtil.isPopupTrigger(aEvent) == true)
			return;

		// Clear cache values
		cSurface = null;
		cPosition = null;
		cCellId = -1;

		// Bail if a VtkDemSurface was not clicked
		vtkActor tmpActor = aSurfaceTarg.getActor();
		if (tmpActor instanceof AssocActor == false)
			return;

		cSurface = ((AssocActor) tmpActor).getAssocModel(VtkDemSurface.class);
		if (cSurface == null)
			return;
		cPosition = aSurfaceTarg.getPosition();
		cCellId = cSurface.findClosestCell(cPosition.toArray());

		updateDisplay();
	}

}
