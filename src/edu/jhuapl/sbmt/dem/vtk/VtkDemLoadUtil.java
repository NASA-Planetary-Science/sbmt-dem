package edu.jhuapl.sbmt.dem.vtk;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.google.common.collect.ImmutableMap;

import vtk.vtkCellArray;
import vtk.vtkDataArray;
import vtk.vtkFloatArray;
import vtk.vtkIdList;
import vtk.vtkObject;
import vtk.vtkPointDataToCellData;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataNormals;

import edu.jhuapl.saavtk.feature.FeatureType;
import edu.jhuapl.saavtk.model.PolyhedralModel.ColoringValueType;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.saavtk.vtk.VtkUtil;
import edu.jhuapl.sbmt.core.body.SmallBodyModel;
import edu.jhuapl.sbmt.core.util.KeyValueNode;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemException;
import edu.jhuapl.sbmt.dem.DemManager;
import edu.jhuapl.sbmt.dem.io.DemCatalogUtil;
import edu.jhuapl.sbmt.dem.io.DemLoadUtil;

import glum.net.Credential;
import glum.source.Source;
import glum.source.SourceState;
import glum.source.SourceUtil;
import glum.task.Task;
import glum.unit.NumberUnit;
import glum.util.ThreadUtil;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;

/**
 * Class that provides a collection of utility methods for loading the contents
 * of a {@link Dem}.
 * <p>
 * The FITS related loading logic originates from the the source:
 * edu.jhuapl.sbmt.dtm.model.DEM
 *
 * @author lopeznr1
 */
public class VtkDemLoadUtil
{
	// Constants
	private static final float INVALID_VALUE = -1.0e38f;

	/**
	 * Utility method that given a file will return the corresponding
	 * {@link VtkDemStruct}.
	 * <p>
	 * Below are the supported formats and corresponding capability:
	 * <ul>
	 * <li>FITS: Supports: Progress update + handling of invalid data.
	 * <li>OBJ: Support: No progress update or handling of invalid data.
	 * <ul>
	 *
	 * @param aTask The {@link Task} used to monitor the load process.
	 * @param aFile The file to be loaded. Supported formats are FITS and OBJ.
	 * @param aDataMode The {@link DataMode} for which the data should be loaded.
	 */
	private static VtkDemStruct loadFile(Task aTask, File aFile, DataMode aDataMode) throws Exception
	{
		Exception failExp = null;
		String fileNameLC = aFile.getName().toLowerCase();

		// Try loading via the fits load routine
		try
		{
			return loadFitsFile(aTask, aFile, aDataMode);
		}
		catch (FitsException | IOException aExp)
		{
			// Capture the (fits) Exception if matching file extension
			if (fileNameLC.endsWith(".fit") == true || fileNameLC.endsWith(".fits") == true)
				failExp = aExp;
		}

		// Try loading via the obj load routine
		try
		{
			return loadObjFile(aTask, aFile);
		}
		catch (IOException aExp)
		{
			; // Nothing to do
		}

		// Throw the original Exception (if it was captured)
		if (failExp != null)
			throw failExp;

		// Throw an exception if failed to load as either a fits or obj file
		throw new DemException("The provided file is neither a valid FITS or OBJ file.", failExp);
	}

	/**
	 * Utility method that will perform the actual load of the VTK state needed
	 * for a {@link VtkDemPainter}.
	 * <p>
	 * Live updates and the ability to abort the process is provided via the
	 * {@link Task} mechanism.
	 */
	public static void loadVtkDemPainter(Task aTask, DemManager aManager, VtkDemPainter aPainter)
	{
		// Force the initial formal update
		Dem tmpDem = aPainter.getItem();
		aManager.notifyLoadUpdate(tmpDem, true);

		// Log some stats
		Source tmpSource = tmpDem.getSource();
		String statusMsg = SourceUtil.getStatusMsg(tmpSource);
		aTask.logRegln(statusMsg);

		// Ensure the Source has been downloaded
		SourceState tmpSS = SourceUtil.getState(tmpSource);
		if (tmpSS == SourceState.Partial || tmpSS == SourceState.Remote)
		{
			try
			{
				Credential tmpCredential = DemCatalogUtil.getCredential();
				SourceUtil.download(aTask, tmpSource, tmpCredential);
				aPainter.markUpdate();
				if (aTask.isAborted() == true)
					ThreadUtil.invokeAndWaitOnAwt(() -> aPainter.markFailure(null));
				tmpCredential.dispose();
			}
			catch (Exception aExp)
			{
				aTask.abort();
				ThreadUtil.invokeAndWaitOnAwt(() -> aPainter.markFailure(aExp));
			}
		}

		// Bail if the task was aborted
		if (aTask.isAborted() == true)
			return;

		// Log some stats
		aTask.logRegln("Loading file...");
		aTask.setProgress(0.0);

		// Load the VTK state
		File tmpFile = tmpDem.getSource().getLocalFile();
		DataMode tmpDataMode = aManager.getViewDataMode(tmpDem);

		try
		{
			// Bail if the file does not exist
			if (tmpFile != null && tmpFile.exists() == false)
				throw new DemException("File does not exist: " + tmpFile);

			// Load the DEM's VTK state
			VtkDemStruct tmpVDS = VtkDemLoadUtil.loadFile(aTask, tmpFile, tmpDataMode);
			if (aTask.isAborted() == true)
				aTask.logRegln("\tThe load has been aborted!");
			else
				aTask.logRegln("\tThe load has been completed.");

			// Bail if the Task was aborted
			if (aTask.isAborted() == true)
				ThreadUtil.invokeAndWaitOnAwt(() -> aPainter.markFailure(null));
			else
				ThreadUtil.invokeAndWaitOnAwt(() -> aPainter.markComplete(tmpVDS));
		}
		catch (Exception aExp)
		{
			aTask.abort();
			ThreadUtil.invokeAndWaitOnAwt(() -> aPainter.markFailure(aExp));
		}
	}

	// TODO: Add javadoc
	public static void colorDEM(String filename, SmallBodyModel smallBodyModel) throws IOException, FitsException
	{
		Fits tmpFits = new Fits(filename);
		HeaderStruct tmpHS = parseHeader(tmpFits);

		// Define arrays now that we know the number of backplanes
		int numBackPlanes = tmpHS.backPlaneIndexL.size();
		List<FeatureType> featureTypeL = new ArrayList<>(numBackPlanes);
		vtkFloatArray[] vValuesPerCellArr = new vtkFloatArray[numBackPlanes];
		vtkFloatArray[] vValuesPerPointArr = new vtkFloatArray[numBackPlanes];
		int[] backPlaneIdxArr = new int[numBackPlanes];

		// Go through each backplane
		for (int i = 0; i < numBackPlanes; i++)
		{
			// Get the FeatureType (name, unit, and scale factor) to use
			FeatureType tmpFeatureType = formNormalizedFeatureType(tmpHS.unprocessedBackPlaneNameL.get(i),
					tmpHS.unprocessedBackPlaneUnitL.get(i));
			featureTypeL.add(tmpFeatureType);

			// Set number of components for each vtkFloatArray to 1
			vValuesPerCellArr[i] = new vtkFloatArray();
			vValuesPerCellArr[i].SetNumberOfComponents(1);
			vValuesPerPointArr[i] = new vtkFloatArray();
			vValuesPerPointArr[i].SetNumberOfComponents(1);

			// Copy List element to array for faster lookup later
			backPlaneIdxArr[i] = tmpHS.backPlaneIndexL.get(i);
		}

		// Check dimensions of actual data
		final int NUM_PLANES = numBackPlanes + 3;
		int[] axes = tmpHS.hdu.getAxes();
//		if (axes.length != 3 || axes[0] != NUM_PLANES || axes[1] != axes[2])
//		{
//			throw new IOException("FITS file has incorrect dimensions");
//		}

		int liveSize = axes[1];
		int liveSize2 = axes[2];

		float[][][] data = (float[][][]) tmpHS.hdu.getData().getData();
		tmpFits.getStream().close();

		int[][] indices = new int[liveSize][liveSize2];
		int c = 0;
		float x, y, z;
		float d;

		// First add points to the vtkPoints array
		for (int m = 0; m < liveSize; ++m)
			for (int n = 0; n < liveSize2; ++n)
			{
				indices[m][n] = -1;

				// A pixel value of -1.0e38 means that pixel is invalid and should
				// be skipped
				x = data[tmpHS.xIdx][m][n];
				y = data[tmpHS.yIdx][m][n];
				z = data[tmpHS.zIdx][m][n];

				// Check to see if x,y,z values are all valid
				boolean valid = true;
				valid &= VtkDemLoadUtil.isValidValue(x);
				valid &= VtkDemLoadUtil.isValidValue(y);
				valid &= VtkDemLoadUtil.isValidValue(z);

				// Check to see if data for all backplanes are also valid
				for (int i = 0; i < numBackPlanes; i++)
				{
					d = data[backPlaneIdxArr[i]][m][n];
					valid &= VtkDemLoadUtil.isValidValue(d);
				}

				// Only add point if everything is valid
				if (valid)
				{
					for (int i = 0; i < numBackPlanes; i++)
					{
						d = data[backPlaneIdxArr[i]][m][n] * (float) featureTypeL.get(i).getScale();
						vValuesPerCellArr[i].InsertNextTuple1(d);
					}

					indices[m][n] = c;
					++c;
				}
			}

		Map<FeatureType, vtkFloatArray> featureTypeValueM = new HashMap<>();
		for (int c1 = 0; c1 < featureTypeL.size(); c1++)
			featureTypeValueM.put(featureTypeL.get(c1), vValuesPerCellArr[c1]);
		convertPointDataToCellData(smallBodyModel.getSmallBodyPolyData(), featureTypeValueM);
		for (int c1 = 0; c1 < featureTypeL.size(); c1++)
			vValuesPerCellArr[c1] = featureTypeValueM.get(featureTypeL.get(c1));

		// Apply colors to the small body model
		String[] nameArr = new String[featureTypeL.size()];
		String[] unitArr = new String[featureTypeL.size()];
		for (int c1 = 0; c1 < featureTypeL.size(); c1++)
		{
			FeatureType tmpItem = featureTypeL.get(c1);
			nameArr[c1] = tmpItem.getName();
			unitArr[c1] = tmpItem.getUnit();
		}
		smallBodyModel.setSmallBodyPolyData(null, vValuesPerCellArr, nameArr, unitArr, ColoringValueType.CELLDATA);
	}

	/**
	 * Utility helper method to convert the point data (which is how they are
	 * stored in the Gaskell's cube file) to cell data.
	 * <p>
	 * Note the values will be replaced with completely different values in the
	 * {@link FeatureType} to {@link vtkFloatArray} mapping.
	 */
	private static void convertPointDataToCellData(vtkPolyData aPolyData,
			Map<FeatureType, vtkFloatArray> aFeatureTypeValueM)
	{
		vtkPointDataToCellData vConvertPDTCD = new vtkPointDataToCellData();
		vConvertPDTCD.SetInputData(aPolyData);

		for (FeatureType aKey : aFeatureTypeValueM.keySet())
		{
			vtkFloatArray vOldFA = aFeatureTypeValueM.get(aKey);
			aPolyData.GetPointData().SetScalars(vOldFA);
			vConvertPDTCD.Update();
			vtkFloatArray vNewFA = new vtkFloatArray();
			vtkDataArray vOScalarDA = ((vtkPolyData) vConvertPDTCD.GetOutput()).GetCellData().GetScalars();
			vNewFA.DeepCopy(vOScalarDA);

			aFeatureTypeValueM.put(aKey, vNewFA);
		}

		aPolyData.GetPointData().SetScalars(null);

		vConvertPDTCD.Delete();
	}

	/**
	 * Forms a normalized {@link FeatureType}.
	 * <p>
	 * Names and units (with the corresponding scale factor) will be updated to
	 * reflect the following:
	 * <ul>
	 * <li>Elevation Relative to Gravity (km) -> Geopotential Height (m)
	 * <li>Elevation Relative to Normal Plane (km) -> Height Relative to Normal
	 * Plane (m)
	 * <li>Slope (rad) -> Slope (deg)
	 * </ul>
	 *
	 * @param aUnprocessedName
	 * @param aUnprocessedUnits
	 * @return
	 */
	private static FeatureType formNormalizedFeatureType(String aUnprocessedName, String aUnprocessedUnits)
	{
		// Keep as is by default
		String processedName = aUnprocessedName.trim();

		String processedUnits = aUnprocessedUnits;
		if (processedUnits != null)
			processedUnits = processedUnits.trim();
		if (processedUnits.isEmpty() == true)
			processedUnits = null;

		float processedScale = 1.0f;

		// Process here
		if (processedUnits == null)
		{
			; // Nothing to do
		}
		if (processedName.equals("Elevation Relative to Gravity") == true && processedUnits.equals("kilometers") == true)
		{
			// From Mapmaker output
			processedName = "Geopotential Height";
			processedUnits = "m";
			processedScale = 1000.0f; // km -> m
		}
		else if (processedName.equals("Elevation Relative to Normal Plane") == true
				&& processedUnits.equals("kilometers") == true)
		{
			// From Mapmaker output
			processedName = "Height Relative to Normal Plane";
			processedUnits = "m";
			processedScale = 1000.0f; // km -> m
		}
		else if (processedName.equals("Slope") == true && processedUnits.equals("radians") == true)
		{
			// From Mapmaker output
			processedUnits = "deg";
			processedScale = (float) (180.0 / Math.PI); // rad -> deg
		}

		return new FeatureType(processedName, processedUnits, processedScale);
	}

	/**
	 * Utility helper method that returns true if the provided value is valid.
	 * <p>
	 * The value is valid as long as they do not equal {@literal INVALID_VALUE}
	 */
	private static boolean isValidValue(double aVal)
	{
		boolean valid = aVal != INVALID_VALUE;
		return valid;
	}

	/**
	 * Utility helper method that given a (FITS) file will return the
	 * corresponding {@link VtkDemStruct}.
	 * <p>
	 * The load process can be canceled via the {@link Task#abort()}.
	 * <p>
	 * Returns the {@link VtkDemStruct} or null if aborted.
	 *
	 * @param aTask The corresponding task for progress updates / aborting.
	 * @param aFile The file to be loaded.
	 * @param aDataMode The {@link DataMode} for which the data should be loaded.
	 */
	private static VtkDemStruct loadFitsFile(Task aTask, File aFile, DataMode aDataMode)
			throws IOException, FitsException
	{
		List<vtkObject> abortL = new ArrayList<>();
		vtkPoints points = new vtkPoints();

		Fits tmpFits = new Fits(aFile);
		HeaderStruct tmpHS = parseHeader(tmpFits);

		// Check to see if x,y,z planes were all defined
		if (tmpHS.xIdx < 0)
			throw new IOException("FITS file does not contain plane for X coordinate");
		else if (tmpHS.yIdx < 0)
			throw new IOException("FITS file does not contain plane for Y coordinate");
		else if (tmpHS.zIdx < 0)
			throw new IOException("FITS file does not contain plane for Z coordinate");

		// Define arrays now that we know the number of backplanes
		int numBackPlanes = tmpHS.backPlaneIndexL.size();
		int[] backPlaneIdx = new int[numBackPlanes];

		// Containers to define our coloring options
		List<FeatureType> featureTypeL = new ArrayList<>();
		Map<FeatureType, vtkFloatArray> vValuesPerCellM = new HashMap<>();
		Map<FeatureType, vtkFloatArray> vValuesPerPointM = new HashMap<>();

		// Go through each backplane
		for (int i = 0; i < numBackPlanes; i++)
		{
			// Get the name, unit, and scale factor to use
			FeatureType tmpFeatureType = formNormalizedFeatureType(tmpHS.unprocessedBackPlaneNameL.get(i),
					tmpHS.unprocessedBackPlaneUnitL.get(i));
			featureTypeL.add(tmpFeatureType);

			// Set number of components for each vtkFloatArray to 1
			vtkFloatArray tmpColoringValuesPerCell = new vtkFloatArray();
			tmpColoringValuesPerCell.SetNumberOfComponents(1);
			vValuesPerCellM.put(tmpFeatureType, tmpColoringValuesPerCell);
			abortL.add(tmpColoringValuesPerCell);

			vtkFloatArray tmpColoringValuesPerPoint = new vtkFloatArray();
			tmpColoringValuesPerPoint.SetNumberOfComponents(1);
			vValuesPerPointM.put(tmpFeatureType, tmpColoringValuesPerPoint);
			abortL.add(tmpColoringValuesPerPoint);

			// Copy List element to array for faster lookup later
			backPlaneIdx[i] = tmpHS.backPlaneIndexL.get(i);
		}

		// Check dimensions of actual data
		final int NUM_PLANES = numBackPlanes + 3;
		int[] axes = tmpHS.hdu.getAxes();
//       if (axes.length != 3 || axes[0] != NUM_PLANES || axes[1] != axes[2])
//       {
//           throw new IOException("FITS file has incorrect dimensions");
//       }

		int liveSize = axes[1];
		int liveSize2 = axes[2];

		float[][][] data = (float[][][]) tmpHS.hdu.getData().getData();
		tmpFits.getStream().close();

		int[][] indices = new int[liveSize][liveSize2];
		int c = 0;
		float x, y, z;
		float d;

		// First add points to the vtkPoints array
		NumberUnit perNU = new NumberUnit("", "", 1.0, "0.00 %");
		for (int m = 0; m < liveSize; ++m)
		{
			// Bail if the task has been aborted
			if (aTask.isAborted() == true)
				break;

			// Update progress
			double tmpProgress = (m + 0.0) / liveSize;
			tmpProgress = tmpProgress * 0.50;
			aTask.logRegUpdate("\tProgress: " + perNU.getString(tmpProgress) + "\n");
			aTask.setProgress(tmpProgress);

			for (int n = 0; n < liveSize2; ++n)
			{
				indices[m][n] = -1;

				// A pixel value of -1.0e38 means that pixel is invalid and should
				// be skipped
				x = data[tmpHS.xIdx][m][n];
				y = data[tmpHS.yIdx][m][n];
				z = data[tmpHS.zIdx][m][n];
				// Check to see if x,y,z values are all valid
				boolean valid = x != INVALID_VALUE && y != INVALID_VALUE && z != INVALID_VALUE;
				if (data.length > 6)
					valid = aDataMode == DataMode.Regular || (valid && (data[7][m][n] != 0));

				// Check to see if data for all backplanes are also valid
				for (int i = 0; i < numBackPlanes; i++)
				{
					d = data[backPlaneIdx[i]][m][n];
					valid = (valid && d != INVALID_VALUE);
				}

				// Only add point if everything is valid
				if (valid)
				{
					points.InsertNextPoint(x, y, z);
					for (int i = 0; i < numBackPlanes; i++)
					{
						FeatureType tmpFeatureType = featureTypeL.get(i);

						d = data[backPlaneIdx[i]][m][n] * (float) featureTypeL.get(i).getScale();
						vValuesPerCellM.get(tmpFeatureType).InsertNextTuple1(d);
					}

					indices[m][n] = c;
					++c;
				}
			}
		}

		vtkPolyData tmpExteriorPD = new vtkPolyData();
		vtkPolyData tmpInteriorPD = new vtkPolyData();
		vtkCellArray polys = new vtkCellArray();
		vtkIdList idList = new vtkIdList();
		idList.SetNumberOfIds(3);

		abortL.add(tmpExteriorPD);
		abortL.add(tmpInteriorPD);
		abortL.add(points);
		abortL.add(polys);
		abortL.add(idList);

		tmpInteriorPD.SetPoints(points);
		tmpInteriorPD.SetPolys(polys);

		// Now add connectivity information
		int i0, i1, i2, i3;
		for (int m = 1; m < liveSize; ++m)
		{
			// Bail if the task has been aborted
			if (aTask.isAborted() == true)
				break;

			// Update progress
			double tmpProgress = (m + 0.0) / liveSize;
			tmpProgress = (tmpProgress * 0.50) + 0.50;
			aTask.logRegUpdate("\tProgress: " + perNU.getString(tmpProgress) + "\n");
			aTask.setProgress(tmpProgress);

			for (int n = 1; n < liveSize2; ++n)
			{
				// Get the indices of the 4 corners of the rectangle to the upper
				// left
				i0 = indices[m - 1][n - 1];
				i1 = indices[m][n - 1];
				i2 = indices[m - 1][n];
				i3 = indices[m][n];

				// Add upper left triangle
				if (i0 >= 0 && i1 >= 0 && i2 >= 0)
				{
					idList.SetId(0, i0);
					idList.SetId(1, i2);
					idList.SetId(2, i1);
					polys.InsertNextCell(idList);
				}
				// Add bottom right triangle
				if (i2 >= 0 && i1 >= 0 && i3 >= 0)
				{
					idList.SetId(0, i2);
					idList.SetId(1, i3);
					idList.SetId(2, i1);
					polys.InsertNextCell(idList);
				}
			}
		}

		// Bail if the task has been aborted
		if (aTask.isAborted() == true)
		{
			VtkUtil.deleteAll(abortL);
			return null;
		}

		vtkPolyDataNormals normalsFilter = new vtkPolyDataNormals();
		normalsFilter.SetInputData(tmpInteriorPD);
		normalsFilter.SetComputeCellNormals(0);
		normalsFilter.SetComputePointNormals(1);
		normalsFilter.SplittingOff();
		normalsFilter.FlipNormalsOn();
		normalsFilter.Update();

		vtkPolyData normalsFilterOutput = normalsFilter.GetOutput();
		tmpInteriorPD.DeepCopy(normalsFilterOutput);

		// Form the exterior
		PolyDataUtil.getBoundary(tmpInteriorPD, tmpExteriorPD);
		// Remove scalar data since it interferes with setting the boundary color
		tmpExteriorPD.GetCellData().SetScalars(null);

		// Make a copy of per point data structures since we need that later for
		// drawing profile plots.
		for (int aIdx = 0; aIdx < numBackPlanes; aIdx++)
		{
			FeatureType tmpKey = featureTypeL.get(aIdx);
			vValuesPerPointM.get(tmpKey).DeepCopy(vValuesPerCellM.get(tmpKey));
		}
		convertPointDataToCellData(tmpInteriorPD, vValuesPerCellM);

		int centerIndex = liveSize / 2;
		float cX = data[tmpHS.xIdx][centerIndex][centerIndex];
		float cY = data[tmpHS.yIdx][centerIndex][centerIndex];
		float cZ = data[tmpHS.zIdx][centerIndex][centerIndex];
		Vector3D centerPos = new Vector3D(cX, cY, cZ);

		// Delete data structures
		idList.Delete();
		double tmpProgress = 1.0;
		aTask.logRegUpdate("\tProgress: " + perNU.getString(tmpProgress) + "\n");
		aTask.setProgress(tmpProgress);

		return new VtkDemStruct(centerPos, tmpHS.keyValueM, featureTypeL, vValuesPerCellM, vValuesPerPointM,
				tmpInteriorPD, tmpExteriorPD, aDataMode);
	}

	/**
	 * Utility method that given a (OBJ) file will return the corresponding
	 * {@link VtkDemStruct}.
	 * <p>
	 * TODO: Due to the lack of streaming support there is no capability to
	 * provide the user with live updates or allow the load process to be
	 * aborted. This needs to be corrected!
	 */
	private static VtkDemStruct loadObjFile(Task aTask, File aFile) throws IOException
	{
		// Progress is indeterminate
		aTask.logRegln("\tProgress is not available.");
		aTask.logRegln("\tAbort support is limited.");
		aTask.logRegln("\tPlease wait...");

		// Load the file
		vtkPolyData vInteriorPD;
		try
		{
			vInteriorPD = PolyDataUtil.loadOBJShapeModel(aFile.getPath());
		}
		catch (Exception aExp)
		{
			throw new IOException("Failed to load obj file: " + aFile, aExp);
		}
		if (vInteriorPD == null)
			throw new IOException("Failed to load obj file: " + aFile);

		// Ensure we have a populated vInteriorPD
		if (vInteriorPD.GetPoints().GetNumberOfPoints() == 0)
			throw new IOException("Failed to load obj file: " + aFile + "\n\tData is empty!");

		// Bail if the task was aborted
		if (aTask.isAborted() == true)
		{
			vInteriorPD.Delete();
			return null;
		}

		Map<String, KeyValueNode> tmpKeyValueM = ImmutableMap.of();
		Vector3D tmpCenterPos = new Vector3D(vInteriorPD.GetCenter());

		// Form the exterior
		vtkPolyData vExteriorPD = new vtkPolyData();
		PolyDataUtil.getBoundary(vInteriorPD, vExteriorPD);
		// Remove scalar data since it interferes with setting the boundary color
		vExteriorPD.GetCellData().SetScalars(null);

		// Create the available FeatureTypes.
		// Currently there is no support for FeatureTypes from OBJ files.
		Map<FeatureType, vtkFloatArray> vValuesPerCellM = ImmutableMap.of();
		Map<FeatureType, vtkFloatArray> vValuesPerPointM = ImmutableMap.of();
		List<FeatureType> featureTypeL = new ArrayList<>();

		return new VtkDemStruct(tmpCenterPos, tmpKeyValueM, featureTypeL, vValuesPerCellM, vValuesPerPointM, vInteriorPD,
				vExteriorPD, DataMode.Plain);
	}

	// TODO: Add javadocs
	private static HeaderStruct parseHeader(Fits aFits) throws IOException, FitsException
	{
		BasicHDU<?> hdu = aFits.getHDU(0);
		Header header = hdu.getHeader();

		// Retrieve the key-value map
		Map<String, KeyValueNode> keyValueM = DemLoadUtil.loadKeyValueMap(header);

		// First pass, figure out number of planes and grab size and scale
		// information
		List<Integer> backPlaneIndexL = new ArrayList<>();
		List<String> unprocessedBackPlaneNameL = new ArrayList<>();
		List<String> unprocessedBackPlaneUnitL = new ArrayList<>();

		int xIdx = -1;
		int yIdx = -1;
		int zIdx = -1;
		int planeCount = 0;
		HeaderCard headerCard;
		while ((headerCard = header.nextCard()) != null)
		{
			String headerKey = headerCard.getKey();
			String headerValue = headerCard.getValue();
			String headerComment = headerCard.getComment();

			if (headerKey.startsWith("PLANE"))
			{
				// Determine if we are looking at a coordinate or a backplane
				if (headerValue.startsWith("X"))
				{
					// This plane is the X coordinate, save the index
					xIdx = planeCount;
				}
				else if (headerValue.startsWith("Y"))
				{
					// This plane is the Y coordinate, save the index
					yIdx = planeCount;
				}
				else if (headerValue.startsWith("Z"))
				{
					// This plane is the Z coordinate, save the index
					zIdx = planeCount;
				}
				else
				{
					// We are looking at a backplane, save the index in order of
					// appearance
					backPlaneIndexL.add(planeCount);

					// Try to break the value into name and unit components
					String[] valueSplitResults = headerValue.split("[\\(\\[\\)\\]]");
					String planeName = valueSplitResults[0];
					String planeUnits = "";
					if (valueSplitResults.length > 1)
					{
						planeUnits = valueSplitResults[1];
					}
					else if (headerComment != null)
					{
						// Couldn't find units in the value, try looking in comments
						// instead
						String[] commentSplitResults = headerComment.split("[\\(\\[\\)\\]]");
						if (commentSplitResults.length > 1)
						{
							planeUnits = commentSplitResults[1];
						}
					}
					unprocessedBackPlaneNameL.add(planeName);
					unprocessedBackPlaneUnitL.add(planeUnits);
				}

				// Increment plane count
				planeCount++;
			}
		}

		return new HeaderStruct(hdu, keyValueM, backPlaneIndexL, unprocessedBackPlaneNameL, unprocessedBackPlaneUnitL,
				xIdx, yIdx, zIdx);
	}

}
