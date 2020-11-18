package edu.jhuapl.sbmt.dem.gui.analyze;

import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import vtk.vtkFloatArray;
import vtk.vtkIdList;
import vtk.vtkPolyData;

import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.sbmt.dem.vtk.VtkDemSurface;

/**
 * Source for this class originated from: edu.jhuapl.sbmt.dtm.model.DEM
 */
public class PlotUtil
{
	/**
	 * Utility method that will generate a profile line plot with value-data
	 * sourced from aValuePerPointFA.
	 *
	 * @param aVDS
	 * @param aXyzPointL
	 * @param aProfileValueL
	 * @param aProfileDistanceL
	 * @param aValuePerPointFA
	 */
	public static void generateProfile(VtkDemSurface aVDS, List<Vector3D> aXyzPointL, List<Double> aProfileValueL,
			List<Double> aProfileDistanceL, vtkFloatArray aValuePerPointFA)
	{
		aProfileValueL.clear();
		aProfileDistanceL.clear();

		// For each point in xyzPointList, find the cell containing that
		// point and then, using barycentric coordinates find the value
		// of the dem at that point
		//
		// To compute the distance, assume we have a straight line connecting the
		// first
		// and last points of xyzPointList. For each point, p, in xyzPointList,
		// find the point
		// on the line closest to p. The distance from p to the start of the line
		// is what
		// is placed in heights. Use SPICE's nplnpt function for this.

		double[] first = aXyzPointL.get(0).toArray();
		double[] last = aXyzPointL.get(aXyzPointL.size() - 1).toArray();
		double[] lindir = new double[3];
		lindir[0] = last[0] - first[0];
		lindir[1] = last[1] - first[1];
		lindir[2] = last[2] - first[2];

		// The following can be true if the user clicks on the same point twice
		boolean zeroLineDir = MathUtil.vzero(lindir);

		double[] pnear = new double[3];
		double[] notused = new double[1];
		vtkIdList idList = new vtkIdList();

		vtkPolyData vInteriorPD = aVDS.getVtkInteriorPD();

		// Form the plot
		for (Vector3D aPt : aXyzPointL)
		{
			double[] xyzArr = aPt.toArray();

			int cellId = aVDS.findClosestCell(xyzArr);

			double val;
			// Compute the radius
			if (aValuePerPointFA == null)
				val = MathUtil.reclat(xyzArr).rad * 1000;
			// Interpolate to get the plate coloring
			else
				val = PolyDataUtil.interpolateWithinCell(vInteriorPD, aValuePerPointFA, cellId, xyzArr, idList);

			aProfileValueL.add(val);

			if (zeroLineDir)
			{
				aProfileDistanceL.add(0.0);
			}
			else
			{
				MathUtil.nplnpt(first, lindir, xyzArr, pnear, notused);
				double dist = 1000.0f * MathUtil.distanceBetween(first, pnear);
				aProfileDistanceL.add(dist);
			}
		}
	}

}
