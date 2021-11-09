package edu.jhuapl.sbmt.dem.gui.popup;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import edu.jhuapl.saavtk.camera.Camera;
import edu.jhuapl.saavtk.camera.CoordinateSystem;
import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemConfigAttr;
import edu.jhuapl.sbmt.dem.DemManager;
import edu.jhuapl.sbmt.dem.vtk.VtkDemPainter;
import edu.jhuapl.sbmt.dem.vtk.VtkDemSurface;

import glum.gui.info.WindowCfg;

/**
 * Collection of utility methods that provide various action related
 * functionality relevant to {@link Dem}s.
 *
 * @author lopeznr1
 */
public class ActionUtil
{
	// Constants
	private static int DefaultAnalyzeWindowWidth = 850;
	private static int DefaultAnalyzeWindowHeight = 400;

	/**
	 * Utility method that will execute the "analyze" action on the list of
	 * {@link Dem}s.
	 */
	public static void analyzeDems(DemManager aManager, Collection<Dem> aItemC, Component aParent)
	{
		// Get stats on various aspects of the screen / window
		Frame rootFrame = JOptionPane.getFrameForComponent(aParent);
		int posX = rootFrame.getLocation().x + 50;
		int posY = rootFrame.getLocation().y + 50;

		int screenW = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		int screenH = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
		int maxPosX = screenW - DefaultAnalyzeWindowWidth;
		int maxPosY = screenH - DefaultAnalyzeWindowHeight;

		// Configure where the windows will be displayed
		List<Dem> readyToShowL = new ArrayList<>();
		Map<Dem, DemConfigAttr> configM = new HashMap<>();
		for (Dem aItem : aItemC)
		{
			// Keep track of items that can be shown immediately
			if (aManager.getPainterFor(aItem).isReady() == true)
				readyToShowL.add(aItem);

			// Prep a request to show the window
			DemConfigAttr tmpDCA = aManager.getConfigAttr(aItem);
			if (tmpDCA.getWindowCfg() == null)
			{
				WindowCfg tmpWC = new WindowCfg(true, posX, posY, DefaultAnalyzeWindowWidth, DefaultAnalyzeWindowHeight);
				tmpDCA = tmpDCA.cloneWithWindowCfg(tmpWC);

				// Stagger placement of new windows
				posX += 50;
				if (posX > maxPosX)
					posX = maxPosX;

				posY += 50;
				if (posY > maxPosY)
					posY = maxPosY;
			}
			else
				tmpDCA = tmpDCA.cloneWithWindowCfg(tmpDCA.getWindowCfg().withIsShown(true));

			configM.put(aItem, tmpDCA);
		}

		aManager.installConfiguration(configM);
	}

	/**
	 * Utility method that will cause the {@link Renderer} to be centered on the
	 * {@link Dem}.
	 */
	public static void centerOnDem(DemManager aManager, Dem aItem, Renderer aRenderer)
	{
		// Bail if the painter is not ready
		VtkDemPainter tmpPainter = aManager.getPainterFor(aItem);
		if (tmpPainter.isReady() == false)
			return;

		// Bail if no CoordinateSystem
		CoordinateSystem tmpCoordinateSystem = aManager.getCoordinateSystem(aItem);
		if (tmpCoordinateSystem == null)
			return;

		// Compute the appropriate view vectors
		Vector3D focalVect = tmpCoordinateSystem.getOrigin();

		VtkDemSurface tmpDem = tmpPainter.getVtkDemSurface();
		double zMag = tmpDem.getBoundingBoxDiagonalLength() * 2.0;
		Vector3D targVect = tmpCoordinateSystem.getAxisZ().scalarMultiply(zMag).add(focalVect);

		Vector3D viewUpVect = tmpCoordinateSystem.getAxisY();

		// Update the camera to reflect the new view
		Camera tmpCamera = aRenderer.getCamera();
		tmpCamera.setView(focalVect, targVect, viewUpVect);
	}

}
