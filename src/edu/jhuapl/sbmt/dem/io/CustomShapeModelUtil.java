package edu.jhuapl.sbmt.dem.io;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vtk.vtkPolyData;
import vtk.vtkPolyDataWriter;

import edu.jhuapl.saavtk.config.ViewConfig;
import edu.jhuapl.saavtk.gui.ShapeModelImporter;
import edu.jhuapl.saavtk.gui.ShapeModelImporter.FormatType;
import edu.jhuapl.saavtk.gui.dialog.ShapeModelImporterDialog;
import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.saavtk.model.ShapeModelType;
import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.sbmt.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.client.SmallBodyViewConfigMetadataIO;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemException;
import edu.jhuapl.sbmt.dem.DemManager;
import edu.jhuapl.sbmt.dem.vtk.VtkDemPainter;

import glum.gui.panel.generic.MessagePanel;
import glum.gui.panel.generic.TextInputPanel;
import glum.util.ThreadUtil;

/**
 * Utility methods to support the exporting of a {@link Dem} to a custom shape
 * model.
 *
 * @author lopeznr1
 */
public class CustomShapeModelUtil
{
	/**
	 * Utility method that takes a regional dem and will export that dem to a
	 * custom shape model accessible via the SBMT client.
	 * <p>
	 * The user will be prompted for the name to give to the exported custom
	 * shape model.
	 */
	public static void exportDemToCustomShapeModel(Component aParent, DemManager aManager, List<Dem> aItemL,
			PolyhedralModel aSmallBodyModel)
	{
		// Bail if the painter is not ready
		Dem tmpDem = aItemL.get(0);
		VtkDemPainter tmpPainter = aManager.getPainterFor(tmpDem);
		if (tmpPainter.isReady() == false)
			return;

		// Prompt the user for the name for the custom shape model
		TextInputPanel namePanel = new TextInputPanel(aParent, "Export to Custom Shape Model");
		namePanel.setMatchRegex("^[^\\s\\\\/]+");
		namePanel.setReservedNames(getReseservedCustomShapeModelNames());
		namePanel.setVisibleAsModal();
		String shapeModelName = namePanel.getInput();
		if (shapeModelName == null)
			return;

		// Determine where to place files in order to maintain backwards
		// compatibility with the brittle export-as-custom-model functionality
		File srcDemFile = tmpDem.getSource().getLocalFile();

		String tmpFilePath = srcDemFile.getPath();
		String tmpVtkFilePath = tmpFilePath + ".vtk";
		String tmpJsonFilePath = tmpFilePath + ".json";
		int tmpIdx = tmpFilePath.lastIndexOf(".");
		if (tmpIdx != -1)
		{
			tmpVtkFilePath = tmpFilePath.substring(0, tmpIdx) + ".vtk";
			tmpJsonFilePath = tmpFilePath.substring(0, tmpIdx) + ".json";
		}
		File dstVtkFile = new File(tmpVtkFilePath);
		File dstJsonFile = new File(tmpJsonFilePath);

		// Write out a VTK file
		vtkPolyData vTmpPD = tmpPainter.getVtkDemSurface().getVtkInteriorPD();
		vtkPolyDataWriter writer = new vtkPolyDataWriter();
		writer.SetFileName(dstVtkFile.getPath());
		writer.SetFileTypeToBinary();
		writer.SetInputData(vTmpPD);
		writer.Write();

		// Determine if we have a fits (or an obj) file
		// Fits files should have a number of key-value pairs
		boolean isFits = aManager.getKeyValuePairMap(tmpDem).size() > 0;

		// Attempt the export
		String failMsg = null;
		try
		{
			// 1st step: Write out metadata
			writeMetaDataForCustomShapeModel(dstJsonFile, shapeModelName, aSmallBodyModel.getConfig());

			// 2nd step: Export as a custom shape model
			String origModelPath = srcDemFile.getPath();
			exportToCustomShapeModel(shapeModelName, isFits, origModelPath);
		}
		catch (DemException aExp)
		{
			failMsg = "Failed while trying to export dem to custom shape model.\n";
			failMsg += "\tSrc File: " + srcDemFile + "\n";
			failMsg += "\tName: " + shapeModelName + "\n\n";
			failMsg += aExp.getMessage();
		}
		catch (Exception aExp)
		{
			failMsg = "Failed while trying to export dem to custom shape model.\n";
			failMsg += "\tSrc File: " + srcDemFile + "\n";
			failMsg += "\tName: " + shapeModelName + "\n\n";
			failMsg += ThreadUtil.getStackTrace(aExp);
		}

		if (failMsg != null)
		{
			MessagePanel failPanel = new MessagePanel(aParent, "Export has failed.");
			failPanel.setInfo(failMsg);
			failPanel.setSize(650, 300);
			failPanel.setVisibleAsModal();
		}

		// Remove intermediate files if canceled
		dstVtkFile.delete();
		dstJsonFile.delete();

		// Bail if we had an issue
		if (failMsg != null)
			return;

		// Send out notification of the added custom shape model
		PropertyChangeEvent tmpEvent = new PropertyChangeEvent(tmpDem, Properties.CUSTOM_MODEL_ADDED, "", shapeModelName);
		ShapeModelImporterDialog.pcl.propertyChange(tmpEvent);
	}

	/**
	 * Utility helper method to perform the actual export of the dem to the
	 * "custom shape model".
	 */
	private static void exportToCustomShapeModel(String aShapeModelName, boolean aIsFits, String aModelPath)
			throws DemException
	{
		// 2nd step: Utilize the ShapeModelImporter to perform the export
		FormatType tmpFormatType = FormatType.FIT;
		if (aIsFits == false)
			tmpFormatType = FormatType.OBJ;

		ShapeModelImporter importer = new ShapeModelImporter();
		importer.setShapeModelType(edu.jhuapl.saavtk.gui.ShapeModelImporter.ShapeModelType.FILE);
		importer.setName(aShapeModelName);
		importer.setFormat(tmpFormatType);
		importer.setModelPath(aModelPath);

		// Throw an exception if we fail to do the export
		String[] errorMsgArr = new String[1];
		boolean isPass = importer.importShapeModel(errorMsgArr, true);
		if (isPass == false)
			throw new DemException(errorMsgArr[0]);
	}

	/**
	 * Utility method that returns all of the reserved custom shape model names.
	 */
	private static Set<String> getReseservedCustomShapeModelNames()
	{
		Set<String> retS = new HashSet<>();

		File modelDir = new File(Configuration.getImportedShapeModelsDir());
		File[] tmpDirArr = modelDir.listFiles();
		if (tmpDirArr == null)
			return retS;

		for (File aDir : tmpDirArr)
			retS.add(aDir.getName());

		return retS;
	}

	/**
	 * Utility helper method to export the necessary metadata needed to record a
	 * custom data model to the specified file.
	 * <p>
	 * Note the {@link ViewConfig} will not be modified.
	 */
	private static void writeMetaDataForCustomShapeModel(File aFile, String aShapeModelName, ViewConfig aConfig)
			throws IOException
	{
		SmallBodyViewConfig tmpConfig = (SmallBodyViewConfig) aConfig.clone();
		tmpConfig.modelLabel = aShapeModelName;
		tmpConfig.customTemporary = false;
		tmpConfig.author = ShapeModelType.CUSTOM;
		SmallBodyViewConfigMetadataIO metadataIO = new SmallBodyViewConfigMetadataIO(tmpConfig);

		metadataIO.write(aFile, aShapeModelName);
	}

}
