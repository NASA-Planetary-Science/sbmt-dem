package edu.jhuapl.sbmt.dem.gui;

import java.awt.Component;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.google.common.collect.ImmutableList;

import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.saavtk.gui.render.VtkPropProvider;
import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.saavtk.status.StatusNotifier;
import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.view.ViewChangeReason;
import edu.jhuapl.saavtk.view.light.LightCfg;
import edu.jhuapl.sbmt.common.client.SmallBodyViewConfig;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemCatalog;
import edu.jhuapl.sbmt.dem.DemManager;
import edu.jhuapl.sbmt.dem.io.DemCatalogUtil;
import edu.jhuapl.sbmt.dem.io.legacy.LegacyUtil;
import edu.jhuapl.sbmt.dem.vtk.VtkDemPainter;

import glum.net.UrlUtil;
import glum.source.PlainSource;
import glum.source.Source;
import glum.task.ConsoleTask;
import glum.task.SilentTask;
import glum.task.Task;

/**
 * Top level panel used to display the custom and browse panels.
 *
 * @author lopeznr1
 */
public class DemMainPanel extends JTabbedPane
{
	// Constants
	/** Defines the tag (base filename for custom catalog files ) **/
	private static final String CustomTagName = "custom";

	/** Standard Constructor */
	public DemMainPanel(Renderer aRenderer, PolyhedralModel aSmallBody, StatusNotifier aStatusNotifier,
			PickManager aPickManager, SmallBodyViewConfig aBodyViewConfig)
	{
		// Retrieve the relative root model directory
		// (Ex: [1] GASKELL/EROS or [2] bennu/altwg-spc-v20191027
		String relModelRootPathStr = aBodyViewConfig.getRootDirOnServer();
		if (relModelRootPathStr != null && relModelRootPathStr.startsWith("/") == true)
			relModelRootPathStr = relModelRootPathStr.substring(1);

		// Define the custom path
		boolean isCustomModel = relModelRootPathStr == null;
		if (isCustomModel == true)
			relModelRootPathStr = "custom/" + aBodyViewConfig.modelLabel;

		// Retrieve the (top level) application path
		// Ex: ~/.sbmt
		String appRootPathStr = Configuration.getApplicationDataDir();

		// Retrieve the (top level) application url
		// Ex: http://sbmt.jhuapl.edu/internal/multi-mission/test/data
		String appRootUrlStr = Configuration.getDataRootURL().toString();

		// Synthesize the tocSource
		File tocDir = Paths.get(appRootPathStr, "dem", relModelRootPathStr).toFile();
		URL tocUrl = UrlUtil.resolve(appRootUrlStr, relModelRootPathStr, "dtm");
		PlainSource tocSource = new PlainSource(tocDir, tocUrl);

		// Custom panel
		JPanel customPanel = formCustomPanel(aRenderer, aStatusNotifier, aSmallBody, aPickManager, tocDir, this);
		addTab("Custom", customPanel);

		// Bail if there are no remote DTMs (via meta data)
		if (aBodyViewConfig.hasRemoteDtmData() == false)
			return;

		// Browse panel
		JPanel browsePanel = null;
		if (isCustomModel == false)
			browsePanel = formBrowsePanel(aRenderer, aStatusNotifier, aSmallBody, aPickManager, tocSource, this);
		if (browsePanel != null)
		{
			addTab("Browse", browsePanel);
			setSelectedComponent(browsePanel);
		}
	}

	/**
	 * Utility helper method that forms the 'browse' tabbed panel.
	 */
	private static JPanel formBrowsePanel(Renderer aRenderer, StatusNotifier aStatusNotifier, PolyhedralModel aSmallBody,
			PickManager aPickManager, Source aTocSource, Component aParent)
	{
		// Load the default catalog file
		Task tmpTask = new ConsoleTask();
		tmpTask.setTabSize(3);
		DemCatalog defCatalog = DemCatalogUtil.loadDefaultCatalog(tmpTask, aTocSource);
		if (defCatalog == null)
			return null;
		List<DemCatalog> catalogL = ImmutableList.of(defCatalog);

		// Form the DemManager
		DemManager tmpDemManager = new DemManager(aRenderer, aStatusNotifier, aSmallBody, aParent);
		aRenderer.addVtkPropProvider(tmpDemManager);

		// Manually register for events of interest
		aRenderer.addViewChangeListener((aSource, aReason) -> handleViewAction(tmpDemManager, aRenderer, aReason));
		aPickManager.getDefaultPicker().addListener(tmpDemManager);
		aPickManager.getDefaultPicker().addPropProvider(tmpDemManager);
		tmpDemManager.addLoadListener((aSource, aItemC) -> handleLoadChange(tmpDemManager, aItemC, aPickManager));

		// Initial update
		handleViewAction(tmpDemManager, aRenderer, ViewChangeReason.Light);

		// Form the 'list' panel
		DemListPanel retPanel = new DemListPanel(tmpDemManager, aRenderer, aPickManager, aSmallBody, catalogL);
		return retPanel;
	}

	/**
	 * Utility helper method that forms the 'custom' tabbed panel.
	 */
	private static JPanel formCustomPanel(Renderer aRenderer, StatusNotifier aStatusNotifier, PolyhedralModel aSmallBody,
			PickManager aPickManager, File aTocDir, Component aParent)
	{
		// Perform a migration away from the legacy custom folder
		File legacyCustomDir = new File(aSmallBody.getCustomDataFolder());
		File customCacheDir = new File(aTocDir, "custom");
		LegacyUtil.migrateLegacyMetaCatalog(legacyCustomDir, customCacheDir, CustomTagName);
		customCacheDir.mkdirs();

		// Load the catalog (for custom dems)
		File localFile = DemCatalogUtil.getConfigFileCatalog(aTocDir, CustomTagName);
		Source tmpSource = new PlainSource(localFile, null);
		DemCatalog tmpCatalog = DemCatalogUtil.loadCatalog(new SilentTask(), tmpSource, true, customCacheDir);

		// Form the DemManager
		DemManager tmpDemManager = new DemManager(aRenderer, aStatusNotifier, aSmallBody, aParent);
		aRenderer.addVtkPropProvider(tmpDemManager);

		// Manually register for events of interest
		aRenderer.addViewChangeListener((aSource, aReason) -> handleViewAction(tmpDemManager, aRenderer, aReason));
		aPickManager.getDefaultPicker().addListener(tmpDemManager);
		aPickManager.getDefaultPicker().addPropProvider(tmpDemManager);
		tmpDemManager.addLoadListener((aSource, aItemC) -> handleLoadChange(tmpDemManager, aItemC, aPickManager));

		// Initial update
		handleViewAction(tmpDemManager, aRenderer, ViewChangeReason.Light);

		// Form the 'list' panel
		DemListPanel retPanel = new DemListPanel(tmpDemManager, aRenderer, aPickManager, aSmallBody, tmpCatalog, null);
		return retPanel;
	}

	/**
	 * Utility helper method that notifies the {@link PickManager}'s default
	 * picker that a {@link VtkPropProvider}'s state has changed.
	 * <p>
	 * This notification will be sent only if {@link VtkDemPainter} corresponding
	 * to the specified {@link Dem} has reached a "ready" state.
	 */
	private static void handleLoadChange(DemManager aDemManager, Collection<Dem> aItemC, PickManager aPickManager)
	{
		// Bail if none of the items are ready
		boolean isReady = false;
		for (Dem aItem : aItemC)
			isReady |= aDemManager.getPainterFor(aItem).isReady() == true;
		if (isReady == false)
			return;

		aPickManager.getDefaultPicker().notifyPropProviderChanged();
	}

	/**
	 * Utility helper method that notifies the {@link DemManager} of the system
	 * {@link LightCfg}.
	 */
	private static void handleViewAction(DemManager aManager, Renderer aRenderer, ViewChangeReason aReason)
	{
		// Bail if the event is not associated with a change in lighting
		if (aReason != ViewChangeReason.Light)
			return;

		// Update the "system" LightCfg
		LightCfg tmpLightCfg = aRenderer.getLightCfg();
		aManager.setSystemLightCfg(tmpLightCfg);
	}

}
