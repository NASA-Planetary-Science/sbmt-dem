package edu.jhuapl.sbmt.dem.io;

import java.awt.Component;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.google.common.io.Files;

import vtk.vtkFloatArray;
import vtk.vtkIdList;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkTriangle;

import edu.jhuapl.saavtk.feature.FeatureType;
import edu.jhuapl.saavtk.gui.dialog.CustomFileChooser;
import edu.jhuapl.saavtk.gui.dialog.DirectoryChooser;
import edu.jhuapl.saavtk.util.LatLon;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemManager;
import edu.jhuapl.sbmt.dem.vtk.VtkDemSurface;

import glum.gui.GuiPaneUtil;
import glum.gui.panel.task.FullTaskPanel;
import glum.source.Source;
import glum.source.SourceState;
import glum.source.SourceUtil;
import glum.util.ThreadUtil;

/**
 * Collection of utility method to aide in the exporting dem data to a variety
 * of file formats.
 * <p>
 * The following functionality is supported:
 * <ul>
 * <li>Saving dems to the local file system
 * <li>Exporting a {@link VtkDemSurface} to the following formats: OBJ, PTL, STL
 * <li>Exporting the {@link VtkDemSurface}'s plate data to a CSV file.
 * </ul>
 *
 * @author lopeznr1
 */
public class DemExportUtil
{
	/**
	 * Utility method that will prompt the user for a folder where to save dem
	 * files.
	 * <p>
	 * The specified files will be saved to the user specified folder.
	 */
	public static void saveDemsToFolder(DemManager aItemManager, List<Dem> aItemL, Component aParent)
	{
		// Prompt the user for the folder to save to
		File dstDir = DirectoryChooser.showOpenDialog(aParent);
		if (dstDir == null)
			return;

		// Info panel
		FullTaskPanel loadPanel = new FullTaskPanel(aParent, true, false);
		loadPanel.setSize(850, 300);
		loadPanel.setTabSize(3);

		loadPanel.reset();
		loadPanel.setTitle("DEM files to save " + aItemL.size());
		loadPanel.setVisible(true);

		// Process all of the dems
		int passCnt = 0;
		loadPanel.logRegln("DEMs to process: " + aItemL.size());
		loadPanel.logRegln("\tFolder: " + dstDir + "\n");
		for (Dem aItem : aItemL)
		{
			// Bail if aborted
			if (loadPanel.isAborted() == true)
				return;

			// Retrieve the source and destination
			Source tmpSource = aItem.getSource();
			File srcFile = tmpSource.getLocalFile();

			String tmpName = srcFile.getName();
			File dstFile = new File(dstDir, tmpName);

			String descrStr = aItemManager.getDisplayName(aItem);

			// Skip if the file is not local
			if (SourceUtil.getState(tmpSource) != SourceState.Local)
			{
				loadPanel.logRegln("[Fail] Skipping dem: " + descrStr);
				loadPanel.logRegln("\tThe source file is not on the local file system.");
				if (tmpSource.getRemoteUrl() != null)
					loadPanel.logRegln("\tSource needs to be downloaded...");
				else
					loadPanel.logRegln("\tSrc File: " + srcFile);
				continue;
			}

			// Copy the file
			try
			{
				// Copy the file
				Files.copy(srcFile, dstFile);

				loadPanel.logRegln("[Pass] Saved dem: " + descrStr);
				loadPanel.logRegln("\tDst File: " + dstFile);
				passCnt++;
			}
			catch (Exception aExp)
			{
				loadPanel.logRegln("[Fail] Skipping dem: " + descrStr);
				loadPanel.logRegln("\tSrc File: " + srcFile);
				loadPanel.logRegln("\tDst File: " + dstFile);
				loadPanel.logRegln(ThreadUtil.getStackTrace(aExp) + "\n");
			}
		}

		loadPanel.setProgress(1.0);
		loadPanel.logRegln("\nDEMs successfully saved: " + passCnt);
		int failCnt = aItemL.size() - passCnt;
		if (failCnt != 0)
			loadPanel.logRegln("\tFailed to save: " + failCnt);
		loadPanel.logRegln("");
	}

	/**
	 * Utility method that takes aDem and exports it to a user provided file.
	 * <p>
	 * The exported file will be in the OBJ format.
	 *
	 * See also {@link PolyDataUtil#saveShapeModelAsOBJ}
	 *
	 * @param aSurface
	 * @param aParent
	 */
	public static void saveToObj(VtkDemSurface aSurface, Component aParent)
	{
		// Bail if no file specified
		File file = CustomFileChooser.showSaveDialog(aParent, "Export Shape Model to OBJ", "model.obj");
		if (file == null)
			return;

		// Delegate
		try
		{
			PolyDataUtil.saveShapeModelAsOBJ(aSurface.getVtkInteriorPD(), file);
		}
		catch (Exception aExp)
		{
			String infoMsg = "An error occurred exporting the shape model.\n";
			GuiPaneUtil.showFailMessage(aParent, "Export Dem Error: OBJ", infoMsg, aExp);
		}
	}

	/**
	 * Utility method that takes aDem and exports it to a user provided file.
	 * <p>
	 * See also {@link PolyDataUtil#saveShapeModelAsPLT}
	 *
	 * @param aSurface
	 * @param aParent
	 */
	public static void saveToPtl(VtkDemSurface aSurface, Component aParent)
	{
		// Bail if no file specified
		File file = CustomFileChooser.showSaveDialog(aParent, "Export Shape Model to PLT", "model.plt");
		if (file == null)
			return;

		// Delegate
		try
		{
			PolyDataUtil.saveShapeModelAsPLT(aSurface.getVtkInteriorPD(), file);
		}
		catch (Exception aExp)
		{
			String infoMsg = "An error occurred exporting the shape model.\n";
			GuiPaneUtil.showFailMessage(aParent, "Export Dem Error: PLT", infoMsg, aExp);
		}
	}

	/**
	 * Utility method that takes aDem and exports it to a user provided file.
	 * <p>
	 * The exported file will be in the STL format.
	 *
	 * See also {@link PolyDataUtil#saveShapeModelAsSTL}
	 *
	 * @param aSurface
	 * @param aParent
	 */
	public static void saveToStl(VtkDemSurface aSurface, Component aParent)
	{
		// Bail if no file specified
		File file = CustomFileChooser.showSaveDialog(aParent, "Export Shape Model to STL", "model.stl");
		if (file == null)
			return;

		// Delegate
		try
		{
			PolyDataUtil.saveShapeModelAsSTL(aSurface.getVtkInteriorPD(), file);
		}
		catch (Exception aExp)
		{
			String infoMsg = "An error occurred exporting the shape model.\n";
			GuiPaneUtil.showFailMessage(aParent, "Export Dem Error: STL", infoMsg, aExp);
		}
	}

	/**
	 * Utility method that takes aDem and exports it to a user provided file.
	 *
	 * See also {@link VtkDemSurface#savePlateData(File)}
	 *
	 * @param aSurface
	 * @param aParent
	 */
	public static void saveToPlateData(VtkDemSurface aSurface, Component aParent)
	{
		// Bail if no file specified
		File file = CustomFileChooser.showSaveDialog(aParent, "Export Plate Data", "platedata.csv");
		if (file == null)
			return;

		// Delegate
		try
		{
			saveToPlateData(aSurface, file);
		}
		catch (Exception aExp)
		{
			String infoMsg = "An error occurred exporting the plate data.\n";
			GuiPaneUtil.showFailMessage(aParent, "Export Dem Error: Plate Data", infoMsg, aExp);
		}
	}

	/**
	 * Utility helper method to serialize the plate data associated with the
	 * specified {@link VtkDemSurface}.
	 * <p>
	 * File content will be output as a CSV.
	 */
	private static void saveToPlateData(VtkDemSurface aSurface, File aFile) throws IOException
	{
		try (BufferedWriter out = new BufferedWriter(new FileWriter(aFile)))
		{
			out.write("Plate Id");
			out.write(",Area (km^2)");
			out.write(",Center X (km)");
			out.write(",Center Y (km)");
			out.write(",Center Z (km)");
			out.write(",Center Latitude (deg)");
			out.write(",Center Longitude (deg)");
			out.write(",Center Radius (km)");

			List<FeatureType> tmpTypeL = aSurface.getFeatureTypeList();
			for (FeatureType aFT : tmpTypeL)
			{
				out.write("," + aFT.getName());
				if (aFT.getUnit() != null)
					out.write(" (" + aFT.getUnit() + ")");
			}
			out.write("\n");

			vtkTriangle triangle = new vtkTriangle();

			vtkPolyData vTmpIntPD = aSurface.getVtkInteriorPD();
			vtkPoints points = vTmpIntPD.GetPoints();
			vTmpIntPD.BuildCells();
			vtkIdList idList = new vtkIdList();

			int numCells = vTmpIntPD.GetNumberOfCells();
			for (int aCellIdx = 0; aCellIdx < numCells; ++aCellIdx)
			{
				double[] pt0 = new double[3];
				double[] pt1 = new double[3];
				double[] pt2 = new double[3];
				double[] center = new double[3];

				vTmpIntPD.GetCellPoints(aCellIdx, idList);
				int id0 = idList.GetId(0);
				int id1 = idList.GetId(1);
				int id2 = idList.GetId(2);
				points.GetPoint(id0, pt0);
				points.GetPoint(id1, pt1);
				points.GetPoint(id2, pt2);

				double area = triangle.TriangleArea(pt0, pt1, pt2);
				triangle.TriangleCenter(pt0, pt1, pt2, center);
				LatLon llr = MathUtil.reclat(center);

				out.write(aCellIdx + ",");
				out.write(area + ",");
				out.write(center[0] + ",");
				out.write(center[1] + ",");
				out.write(center[2] + ",");
				out.write((llr.lat * 180.0 / Math.PI) + ",");
				out.write((llr.lon * 180.0 / Math.PI) + ",");
				out.write("" + llr.rad);

				for (FeatureType aFT : tmpTypeL)
				{
					vtkFloatArray vTmpFA = aSurface.getValuesForCellData(aFT);

					double[] valArr;
					int numComp = vTmpFA.GetNumberOfComponents();
					switch (numComp)
					{
						case 1:
							valArr = new double[] { vTmpFA.GetTuple1(aCellIdx) };
							break;
						case 2:
							valArr = vTmpFA.GetTuple2(aCellIdx);
							break;
						case 3:
							valArr = vTmpFA.GetTuple3(aCellIdx);
							break;
						case 4:
							valArr = vTmpFA.GetTuple4(aCellIdx);
							break;
						case 6:
							valArr = vTmpFA.GetTuple6(aCellIdx);
							break;
						case 9:
							valArr = vTmpFA.GetTuple9(aCellIdx);
							break;
						default:
							throw new IOException("Unsupported number of components: " + numComp);
					}

					for (double aVal : valArr)
						out.write("," + aVal);
				}

				out.write("\n");
			}

			triangle.Delete();
			idList.Delete();
		}
	}

}
