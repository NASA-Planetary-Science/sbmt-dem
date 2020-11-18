package edu.jhuapl.sbmt.dem.gui.analyze;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import edu.jhuapl.saavtk.camera.gui.CameraQuaternionPanel;
import edu.jhuapl.saavtk.camera.gui.CameraRegularPanel;
import edu.jhuapl.saavtk.color.provider.ColorProvider;
import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.saavtk.model.structure.LineModel;
import edu.jhuapl.saavtk.pick.ControlPointsPicker;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.saavtk.scalebar.ScaleBarPainter;
import edu.jhuapl.saavtk.scalebar.gui.ScaleBarPanel;
import edu.jhuapl.saavtk.status.LocationStatusHandler;
import edu.jhuapl.saavtk.status.gui.StatusBarPanel;
import edu.jhuapl.saavtk.structure.PolyLine;
import edu.jhuapl.saavtk.structure.PolyLineMode;
import edu.jhuapl.saavtk.view.lod.LodStatusPainter;
import edu.jhuapl.saavtk.view.lod.gui.LodPanel;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemConfigAttr;
import edu.jhuapl.sbmt.dem.DemManager;
import edu.jhuapl.sbmt.dem.KeyValueNode;
import edu.jhuapl.sbmt.dem.gui.analyze.control.ControlPanel;
import edu.jhuapl.sbmt.dem.gui.prop.PropsPanel;
import edu.jhuapl.sbmt.dem.gui.table.ProfileTablePanel;
import edu.jhuapl.sbmt.dem.vtk.ItemDrawAttr;
import edu.jhuapl.sbmt.dem.vtk.VtkDemSurface;

import glum.gui.GuiUtil;
import net.miginfocom.swing.MigLayout;

/**
 * UI component that provides an "Analyze" regional dem panel.
 * <p>
 * This panel is the UI component that provides functionality for analyzing
 * regional {@link Dem}s.
 * <p>
 * This panel provides the following features:
 * <ul>
 * <li>Coloring configuration / control area
 * <li>Render area for the local dem
 * <li>Collection of auxiliary tabs (camera, chart, table, ...)
 * <li>Status bar
 * </ul>
 *
 * @author lopeznr1
 */
public class AnalyzePanel extends JPanel
{
	// State vars
	private final LineModel<PolyLine> profileManager;
	private final DemPlot plot;

	// Gui vars
	private final ControlPanel controlPanel;
	private final Renderer renderer;

	// VTK vars
	private final VtkDemSurface vPriSurface;

	/**
	 * Standard Constructor
	 *
	 * @param aDemManager The reference {@link DemManager}.
	 * @param aItem The {@link Dem} to be analyzed.
	 * @param aSrcSurface The source {@link VtkDemSurface} corresponding to the
	 * {@link Dem}. A copy of this VTK resource will be made. This is faster than
	 * instantiating a {@link VtkDemSurface} from scratch.
	 * @param aRootSmallBody The parent small body associated with this
	 * {@link Dem}.
	 * @param aConfigAttr The configuration associated with this panel.
	 */
	public AnalyzePanel(DemManager aDemManager, Dem aItem, VtkDemSurface aSrcSurface, PolyhedralModel aRootSmallBody,
			DemConfigAttr aConfigAttr)
	{
		// Create a new VtkDemSurface specifically for this panel. It is a
		// requirement that each Renderer has their own copy of the dem surface.
		vPriSurface = VtkDemSurface.formClone(aSrcSurface);

		// Set our primary surface to match the source surface
		ColorProvider srcIntCP = aSrcSurface.getDrawAttr().getIntCP();
		ItemDrawAttr tmpAttr = new ItemDrawAttr(ColorProvider.Invalid, false, srcIntCP, true, 1.0, 0.0);
		vPriSurface.setDrawAttr(tmpAttr);

		// Set up the Renderer
		renderer = new Renderer(vPriSurface);
		renderer.addVtkPropProvider(vPriSurface);
		renderer.setMinimumSize(new Dimension(0, 0));

		// Set up the structure manager for profiles
		StatusBarPanel tmpStatusBarPanel = new StatusBarPanel();
		profileManager = new LineModel<>(renderer, tmpStatusBarPanel, vPriSurface, PolyLineMode.PROFILE);
		profileManager.setMaximumVerticesPerLine(2);
		renderer.addVtkPropProvider(profileManager);

		// Set up the PickManager / Pickers
		PickManager pickManager = new PickManager(renderer, vPriSurface);
		pickManager.getDefaultPicker().addListener(renderer);
		pickManager.getDefaultPicker().addListener(profileManager);
		pickManager.getDefaultPicker().addPropProvider(profileManager);
		ControlPointsPicker<PolyLine> priPicker = new ControlPointsPicker<>(renderer, pickManager, vPriSurface,
				profileManager);

		// Setup the plot
		plot = new DemPlot(profileManager, vPriSurface, srcIntCP.getFeatureType());

		// Add the components in
		JTabbedPane tmpTabbedPane = new JTabbedPane();
		tmpTabbedPane.add("Chart", plot.getChartPanel());
		tmpTabbedPane.add("Table", new ProfileTablePanel(profileManager, pickManager, priPicker, aRootSmallBody));
		tmpTabbedPane.add("Config", formConfigPanel(renderer));
		tmpTabbedPane.add("Camera: Reg", new CameraRegularPanel(renderer, vPriSurface));
		tmpTabbedPane.add("Camera: Quat", formCameraQuaternionPanel());
		tmpTabbedPane.add("Details", formDetailsPanel(aDemManager, aItem));

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, renderer, tmpTabbedPane);
		splitPane.setDividerLocation(300);
		splitPane.setResizeWeight(0.5);
		splitPane.setOneTouchExpandable(true);

		controlPanel = new ControlPanel(aDemManager, profileManager, pickManager, priPicker, aItem, renderer, vPriSurface,
				plot);

		// Form the GUI
		setLayout(new MigLayout("", "0[]0", "0[]0"));
		add(controlPanel, "ay top,h 0::");
		add(splitPane, "growx,growy,h 0::,pushx,pushy,wrap");
		add(tmpStatusBarPanel, "growx,span");

		// Force the renderer's camera to the "reset" default view
		renderer.getCamera().reset();

		// Register for events of interest
		LocationStatusHandler tmpHandler = new LocationStatusHandler(tmpStatusBarPanel, renderer);
		pickManager.getDefaultPicker().addListener(tmpHandler);
	}

	/** Manual destruction */
	public void dispose()
	{
		controlPanel.dispose();
	}

	/**
	 * Returns the {@link Renderer} associated with this AnalyzePanel.
	 */
	public Renderer getRenderer()
	{
		return renderer;
	}

	/**
	 * Returns the {@link VtkDemSurface} associated with this AnalyzePanel.
	 */
	public VtkDemSurface getDemSurface()
	{
		return vPriSurface;
	}

	/**
	 * Helper method that constructs a {@link CameraQuaternionPanel} suitable for
	 * being embedded in another UI component.
	 */
	private Component formCameraQuaternionPanel()
	{
		// Specialized quaternion panel
		CameraQuaternionPanel retQuatPanel = new CameraQuaternionPanel(renderer);

		boolean isFirst = true;
		for (Component aComp : retQuatPanel.getActionButtons())
		{
			if (isFirst == true)
				retQuatPanel.add(aComp, "ax right,span,split");
			else
				retQuatPanel.add(aComp, "");

			isFirst = false;
		}

		return retQuatPanel;
	}

	/**
	 * Utility helper method to construct a "configuration" panel.
	 * <p>
	 * The returned configuration panel is composed of the following child
	 * panels:
	 * <ul>
	 * <li>{@link LodPanel}
	 * <li>{@link ScaleBarPanel}
	 * </ul>
	 */
	private static JPanel formConfigPanel(Renderer aRenderer)
	{
		JPanel retPanel = new JPanel(new MigLayout("", "", ""));

		ScaleBarPainter scaleBarPainter = new ScaleBarPainter(aRenderer);
		aRenderer.addVtkPropProvider(scaleBarPainter);
		retPanel.add(new ScaleBarPanel(aRenderer, scaleBarPainter), "span,growx,wrap");

		retPanel.add(GuiUtil.createDivider(), "growx,h 4!,span,wrap");

		LodStatusPainter lodPainter = new LodStatusPainter(aRenderer);
		aRenderer.addVtkPropProvider(lodPainter);
		retPanel.add(new LodPanel(aRenderer, lodPainter), "span,growx,wrap");

		return retPanel;
	}

	/**
	 * Helper method that constructs a {@link PropsPanel} used to display the
	 * {@link KeyValueNode}s associated with the {@link Dem}.
	 */
	private Component formDetailsPanel(DemManager aDemManager, Dem aItem)
	{
		PropsPanel retPanel = new PropsPanel();

		Map<String, KeyValueNode> tmpKeyValueM = aDemManager.getKeyValuePairMap(aItem);
		retPanel.setKeyValuePairs(tmpKeyValueM);

		return retPanel;
	}

}
