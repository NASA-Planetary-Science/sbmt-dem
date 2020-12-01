package edu.jhuapl.sbmt.dem.gui.popup;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.jfree.chart.plot.DefaultDrawingSupplier;

import edu.jhuapl.saavtk.gui.render.RenderIoUtil;
import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.saavtk.model.PolyModel;
import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemManager;
import edu.jhuapl.sbmt.dem.gui.action.DemExteriorColorAction;
import edu.jhuapl.sbmt.dem.gui.analyze.AnalyzePanel;
import edu.jhuapl.sbmt.dem.io.DemExportUtil;
import edu.jhuapl.sbmt.dem.vtk.DataMode;
import edu.jhuapl.sbmt.dem.vtk.VtkDemPainter;
import edu.jhuapl.sbmt.dem.vtk.VtkDemSurface;

import glum.gui.action.PopupMenu;
import glum.gui.panel.generic.PromptPanel;

/**
 * Utility class that provides a collection of methods useful for working with
 * the dem related GUIs.
 *
 * @author lopeznr1
 */
public class DemGuiUtil
{
	/**
	 * Utility method that creates the {@link JMenuBar} associated with the
	 * "Analyze: *" window.
	 */
	public static JMenuBar formAnalyzeMenuBar(AnalyzePanel aAnalyzePanel)
	{
		Renderer tmpRenderer = aAnalyzePanel.getRenderer();
		VtkDemSurface tmpSurface = aAnalyzePanel.getDemSurface();

		// File menu
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');

		JMenuItem tmpMI = new JMenuItem("Export to Image...");
		tmpMI.addActionListener((aEvent) -> RenderIoUtil.saveToFile(tmpRenderer));
		fileMenu.add(tmpMI);

		JMenu saveShapeModelMenu = new JMenu("Export Shape Model to");
		fileMenu.add(saveShapeModelMenu);

		tmpMI = new JMenuItem("PLT (Gaskell Format)...");
		tmpMI.addActionListener((aEvent) -> DemExportUtil.saveToPtl(tmpSurface, aAnalyzePanel));
		saveShapeModelMenu.add(tmpMI);

		tmpMI = new JMenuItem("OBJ...");
		tmpMI.addActionListener((aEvent) -> DemExportUtil.saveToObj(tmpSurface, aAnalyzePanel));
		saveShapeModelMenu.add(tmpMI);

		tmpMI = new JMenuItem("STL...");
		tmpMI.addActionListener((aEvent) -> DemExportUtil.saveToStl(tmpSurface, aAnalyzePanel));
		saveShapeModelMenu.add(tmpMI);

		tmpMI = new JMenuItem("Export Plate Data...");
		tmpMI.addActionListener((aEvent) -> DemExportUtil.saveToPlateData(tmpSurface, aAnalyzePanel));
		fileMenu.add(tmpMI);

		JMenuBar retMenuBar = new JMenuBar();
		retMenuBar.add(fileMenu);
		return retMenuBar;
	}

	/**
	 * Forms the popup menu associated with dem items.
	 */
	public static PopupMenu<Dem> formPopupMenu(DemManager aManager, Component aParent, Renderer aRenderer,
			PolyhedralModel aSmallBody)
	{
		PopupMenu<Dem> retPM = new PopupMenu<>(aManager);

		retPM.installPopAction(new HideShowInteriorAction(aManager, "DEM"), "Show DEM");
		retPM.installPopAction(new HideShowExteriorAction(aManager, "Boundary"), "Show Boundary");
		retPM.addSeparator();

		retPM.installPopAction(new AnalyzeAction(aManager), "Analyze");
		retPM.installPopAction(new CenterAction(aManager, aRenderer), "Center DEM in Window");
		retPM.installPopAction(new EditAction(aManager, aParent), "Edit DEM");

		JMenu colorMenu = new JMenu("Boundary Color");
		retPM.installPopAction(new DemExteriorColorAction(aManager, aParent, colorMenu), colorMenu);
		retPM.addSeparator();

		retPM.installPopAction(new SaveDemFileAction(aManager, aParent), "Save DEM file");
		retPM.installPopAction(new ExportAsCustomModelAction(aManager, aSmallBody, aParent), "Export as Custom Model");

		return retPM;
	}

	/**
	 * Utility method that returns the auto generated color to be utilized with
	 * the specified id.
	 */
	public static Color getAutoGenerateColor(int aId)
	{
		int numColors = DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE.length;

		int tmpIdx = aId % numColors;

		Color retColor = (Color) DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE[tmpIdx];
		return retColor;
	}

	/**
	 * Utility method that returns a nominal value to be used as the basis for
	 * setting the offset (from the surface) of objects to be rendered.
	 */
	public static double getNominalRadialOffsetScale(PolyModel aSmallBody)
	{
		double radialOffsetScale = aSmallBody.getBoundingBoxDiagonalLength() / 1500.0;
		return radialOffsetScale;
	}

	/**
	 * Utility method that returns the number of DEMs that are visible.
	 */
	public static int getShownCount(DemManager aManager, Collection<Dem> aItemC)
	{
		int retCnt = 0;
		for (Dem aItem : aItemC)
		{
			if (aManager.getIsVisibleInterior(aItem) == true)
				retCnt++;
		}

		return retCnt;
	}

	/**
	 * Utility method that returns the "unified" opacity value for the provided
	 * list of {@link Dem}s.
	 * <p>
	 * If all items do not have the same value then NaN will be returned.
	 */
	public static double getUnifiedOpacityFor(DemManager aItemManager, Collection<Dem> aItemC)
	{
		if (aItemC.size() == 0)
			return Double.NaN;

		// Retrieve the opacity of first item
		Dem tmpItem = aItemC.iterator().next();
		double retOpacity = aItemManager.getOpacity(tmpItem);

		// Ensure all items have the same value
		boolean isSameValue = true;
		for (Dem aItem : aItemC)
		{
			double tmpOpacity = aItemManager.getOpacity(aItem);
			isSameValue &= Objects.equals(tmpOpacity, retOpacity);
		}

		if (isSameValue == false)
			return Double.NaN;

		return retOpacity;
	}

	/**
	 * Utility method that returns the "unified" radial offset value for the
	 * provided list of {@link Dem}s.
	 * <p>
	 * If all items do not have the same value then NaN will be returned.
	 */
	public static double getUnifiedRadialOffsetFor(DemManager aItemManager, Collection<Dem> aItemC)
	{
		if (aItemC.size() == 0)
			return Double.NaN;

		// Retrieve the opacity of first item
		Dem tmpItem = aItemC.iterator().next();
		double retRadialOffset = aItemManager.getRadialOffset(tmpItem);

		// Ensure all items have the same value
		boolean isSameValue = true;
		for (Dem aItem : aItemC)
		{
			double tmpRadialOffset = aItemManager.getRadialOffset(aItem);
			isSameValue &= Objects.equals(tmpRadialOffset, retRadialOffset);
		}

		if (isSameValue == false)
			return Double.NaN;

		return retRadialOffset;
	}

	/**
	 * Utility method that returns the "unified" viewDataMode for the provided
	 * list of {@link Dem}s.
	 * <p>
	 * If all items do not have the same value then null will be returned.
	 */
	public static DataMode getUnifiedViewDataMode(DemManager aItemManager, Collection<Dem> aItemC)
	{
		if (aItemC.size() == 0)
			return null;

		// Retrieve the opacity of first item
		Dem tmpItem = aItemC.iterator().next();
		DataMode retViewDataMode = aItemManager.getViewDataMode(tmpItem);

		// Ensure all items have the same value
		boolean isSameValue = true;
		for (Dem aItem : aItemC)
		{
			DataMode tmpViewDataMode = aItemManager.getViewDataMode(aItem);
			isSameValue &= tmpViewDataMode == retViewDataMode;
		}

		if (isSameValue == false)
			return null;

		return retViewDataMode;
	}

	/**
	 * Utility method that returns true if any of the {@link Dem}'s view
	 * {@link DataMode} can not be configured.
	 */
	public static boolean isAnyDemViewDataModeNonCofigurable(DemManager aItemManager, Collection<Dem> aItemC)
	{
		for (Dem aItem : aItemC)
		{
			DataMode tmpDataMode = aItemManager.getViewDataMode(aItem);
			if (tmpDataMode == DataMode.Plain)
				return true;
		}

		return false;
	}

	/**
	 * Utility method that returns true if there is one {@link Dem} specified and
	 * it is visible.
	 */
	public static boolean isOneAndShown(DemManager aManager, Collection<Dem> aItemC)
	{
		boolean retBool = aItemC.size() == 1;
		if (retBool == true)
			retBool &= aManager.getIsVisibleInterior(aItemC.iterator().next()) == true;

		return retBool;
	}

	/**
	 * Utility method that prompts the user for confirmation of deleting the
	 * specified items.
	 */
	public static boolean promptDeletionConfirmation(Component aParent, DemManager aManager, Collection<Dem> aItemC)
	{
		String infoMsg = "Are you sure you want to delete " + aItemC.size() + " dems?\n";

		// Determine if any Dems are currently being loaded or analyzed
		List<Dem> activeLoadL = new ArrayList<>();
		List<Dem> analyzeWinL = new ArrayList<>();
		for (Dem aItem : aItemC)
		{
			VtkDemPainter tmpPainter = aManager.getPainterFor(aItem);
			if (tmpPainter != null && tmpPainter.isLoadActive() == true)
			{
				activeLoadL.add(aItem);
				continue;
			}

			if (aManager.getIsDemAnalyzed(aItem) == true)
				analyzeWinL.add(aItem);
		}

		// Alert the user to any active loads that will be aborted
		int loadCnt = activeLoadL.size();
		if (loadCnt > 0)
		{
			infoMsg += "\nThere are (" + loadCnt + ") dems currently being loaded.\n";
			infoMsg += "The corresponding loads will be aborted:\n";
			for (Dem aItem : activeLoadL)
				infoMsg += "   " + aManager.getDisplayName(aItem) + "\n";
		}

		// Alert the user to any windows that will be closed
		int showCnt = analyzeWinL.size();
		if (showCnt > 0)
		{
			infoMsg += "\nThere are (" + showCnt + ") dems currently being analyzed.\n";
			infoMsg += "The corresponding windows will be closed:\n";
			for (Dem aItem : analyzeWinL)
				infoMsg += "   Analyze: " + aManager.getDisplayName(aItem) + "\n";
		}

		PromptPanel tmpPanel = new PromptPanel(aParent, "Confirm Deletion", 400, 160);
		tmpPanel.setInfo(infoMsg);
		tmpPanel.setSize(500, 255);
		tmpPanel.setVisibleAsModal();
		return tmpPanel.isAccepted();
	}

}
