package edu.jhuapl.sbmt.dem;

import java.awt.Component;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import vtk.vtkObject;
import vtk.vtkProp;

import edu.jhuapl.saavtk.camera.CameraUtil;
import edu.jhuapl.saavtk.camera.CoordinateSystem;
import edu.jhuapl.saavtk.color.provider.ColorProvider;
import edu.jhuapl.saavtk.color.provider.GroupColorProvider;
import edu.jhuapl.saavtk.color.provider.RandomizeGroupColorProvider;
import edu.jhuapl.saavtk.gui.render.SceneChangeNotifier;
import edu.jhuapl.saavtk.gui.render.VtkPropProvider;
import edu.jhuapl.saavtk.gui.util.IconUtil;
import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.saavtk.pick.HookUtil;
import edu.jhuapl.saavtk.pick.PickListener;
import edu.jhuapl.saavtk.pick.PickMode;
import edu.jhuapl.saavtk.pick.PickTarget;
import edu.jhuapl.saavtk.pick.PickUtil;
import edu.jhuapl.saavtk.status.StatusNotifier;
import edu.jhuapl.saavtk.view.AssocActor;
import edu.jhuapl.saavtk.view.light.LightCfg;
import edu.jhuapl.saavtk.vtk.VtkUtil;
import edu.jhuapl.sbmt.dem.gui.analyze.AnalyzePanel;
import edu.jhuapl.sbmt.dem.gui.analyze.AnalyzeWindowListener;
import edu.jhuapl.sbmt.dem.gui.popup.ActionUtil;
import edu.jhuapl.sbmt.dem.gui.popup.DemGuiUtil;
import edu.jhuapl.sbmt.dem.vtk.DataMode;
import edu.jhuapl.sbmt.dem.vtk.ItemDrawAttr;
import edu.jhuapl.sbmt.dem.vtk.VtkDemPainter;
import edu.jhuapl.sbmt.dem.vtk.VtkDemSurface;

import glum.gui.GuiUtil;
import glum.gui.info.WindowCfg;
import glum.item.BaseItemManager;
import glum.item.ItemEventType;

/**
 * Class that provides management logic for a collection of DEM objects.
 * <p>
 * The following features are supported:
 * <ul>
 * <li>Event handling
 * <li>Management of collection of DEMs
 * <li>Support for DEM selection
 * <li>Configuration of associated rendering properties
 * <li>Support to apply a radial offset
 * </ul>
 * <p>
 * Manager that defines a collection of methods to manage, handle notification,
 * and customize display of a collection of DEM objects.
 * <p>
 * Access to various rendering properties of the DEMs is provided. The rendering
 * properties supported are:
 * <ul>
 * <li>Visibility
 * <li>Coloring
 * </ul>
 *
 * @author lopeznr1
 */
public class DemManager extends BaseItemManager<Dem> implements PickListener, VtkPropProvider
{
	// Constants
	// Minimum Time between which a refresh update (for progress notification)
	private static final long REFRESH_FREQ_MS = 47;

	// Reference vars
	private final SceneChangeNotifier refSceneChangeNotifier;
	private final StatusNotifier refStatusNotifier;
	private final PolyhedralModel refSmallBody;
	private final Component refParent;

	// State vars
	private List<LoadListener<Dem>> loadListenerL;
	private Map<Dem, DemConfigAttr> configM;
	private Map<Dem, DemStruct> structM;
	private Map<Dem, AnalyzePanel> analyzeM;
	private GroupColorProvider exteriorGCP;
	private LightCfg systemLightCfg;
	private int globIdx;
	private boolean isInitDone;

	// Work vars
	private final Executor workExecutor;
	private long workLastUpdateTime;

	// VTK vars
	private Map<Dem, VtkDemPainter> vPainterM;

	/** Standard Constructor */
	public DemManager(SceneChangeNotifier aSceneChangeNotifier, StatusNotifier aStatusNotifier,
			PolyhedralModel aSmallBody, Component aParent)
	{
		refSceneChangeNotifier = aSceneChangeNotifier;
		refStatusNotifier = aStatusNotifier;
		refSmallBody = aSmallBody;
		refParent = aParent;

		loadListenerL = new ArrayList<>();
		configM = new HashMap<>();
		structM = new LinkedHashMap<>();
		analyzeM = new HashMap<>();
		exteriorGCP = new RandomizeGroupColorProvider(0);
		systemLightCfg = LightCfg.Invalid;
		globIdx = 0;
		isInitDone = false;

		// Limit max simultaneous downloads to 4
		int numProcs = Runtime.getRuntime().availableProcessors();
		if (numProcs > 4)
			numProcs = 4;
		workExecutor = Executors.newWorkStealingPool(numProcs);
		workLastUpdateTime = 0L;

		vPainterM = new HashMap<>();
	}

	/**
	 * Adds a {@link LoadListener} to this manager.
	 */
	public void addLoadListener(LoadListener<Dem> aListener)
	{
		loadListenerL.add(aListener);
	}

	/**
	 * Removes a {@link LoadListener} from this manager.
	 */
	public void delLoadListener(LoadListener<Dem> aListener)
	{
		loadListenerL.remove(aListener);
	}

	/**
	 * Clears out all flags that will cause the items to be auto loaded
	 */
	public void clearAutoLoadFlags(Collection<Dem> aItemC)
	{
		for (Dem aItem : aItemC)
		{
			DemConfigAttr tmpDCA = configM.get(aItem);
			WindowCfg tmpWC = tmpDCA.getWindowCfg();
			if (tmpWC != null)
				tmpDCA = tmpDCA.cloneWithWindowCfg(tmpWC.withIsShown(false));

			ItemDrawAttr tmpIDA = tmpDCA.getDrawAttr();
			tmpIDA = tmpIDA.cloneWithExteriorIsShown(false);
			tmpIDA = tmpIDA.cloneWithInteriorIsShown(false);
			tmpDCA = tmpDCA.cloneWithDrawAttr(tmpIDA);

			configM.put(aItem, tmpDCA);
		}

		// Send out the appropriate notifications
		updateVtkVars(aItemC);
		notifyListeners(this, ItemEventType.ItemsMutated);
	}

	/**
	 * Returns a list of all the installed {@link DemStruct}s.
	 * <p>
	 * Note there is a one-to-one correspondence between {@link Dem} and
	 * {@link DemStruct} - thus all of the {@link Dem}s are implicitly returned
	 * as well.
	 */
	public List<DemStruct> getAllStructs()
	{
		return new ArrayList<>(structM.values());
	}

	/**
	 * Returns the ColorProvider used to render the item's exterior.
	 */
	public ColorProvider getColorProviderExterior(Dem aItem)
	{
		// Delegate
		return getDrawAttr(aItem).getExtCP();
	}

	/**
	 * Returns the ColorProvider used to render the item's interior.
	 */
	public ColorProvider getColorProviderInterior(Dem aItem)
	{
		DemConfigAttr tmpDCA = configM.get(aItem);
		return tmpDCA.getDrawAttr().getIntCP();
	}

	/**
	 * Returns the {@link CoordinateSystem} relative to the dem.
	 * <p>
	 * Returns null if the {@link VtkDemPainter} associated with the {@link Dem}
	 * has not been loaded.
	 */
	public CoordinateSystem getCoordinateSystem(Dem aItem)
	{
		// Retrieve the cached CoordinateSystem
		DemStruct tmpStruct = structM.get(aItem);
		if (tmpStruct.coordinateSystem != null)
			return tmpStruct.coordinateSystem;

		// Bail if the painter is not ready
		VtkDemPainter tmpPainter = vPainterM.get(aItem);
		if (tmpPainter.isReady() == false)
			return null;

		// Form a CoordinateSystem relative to the DEM
		VtkDemSurface tmpDem = tmpPainter.getVtkDemSurface();
		Vector3D centerVect = tmpDem.getGeometricCenterPoint();
		Vector3D normalVect = tmpDem.getAverageSurfaceNormal();
		CoordinateSystem tmpCoordinateSystem = CameraUtil.formCoordinateSystem(normalVect, centerVect);

		// Update the cache
		tmpStruct = new DemStruct(tmpStruct.dem, tmpStruct.keyValueM, tmpCoordinateSystem);
		structM.put(aItem, tmpStruct);

		return tmpStruct.coordinateSystem;
	}

	/**
	 * Returns the description associated with the item.
	 */
	public String getDisplayName(Dem aItem)
	{
		DemConfigAttr tmpDCA = configM.get(aItem);
		String description = tmpDCA.getDescription();
		if (description != null)
			return description;

		return aItem.getSource().getName();
	}

	/**
	 * Returns the {@link ItemDrawAttr} that should be utilized to render the
	 * specified {@link Dem}.
	 * <p>
	 * To retrieve the actual {@link ItemDrawAttr} that should be serialized
	 * utilize the value as accessed via {@link #getConfigAttr(Dem)}.
	 */
	public ItemDrawAttr getDrawAttr(Dem aItem)
	{
		DemConfigAttr tmpDCA = configM.get(aItem);
		ItemDrawAttr retIDA = tmpDCA.getDrawAttr();
		if (tmpDCA.getIsSyncCoring() == false)
			retIDA = retIDA.cloneWithInteriorColorProvider(ColorProvider.Invalid);

		if (retIDA.getExtCP() == ColorProvider.Invalid)
		{
			ColorProvider tmpCP = exteriorGCP.getColorProviderFor(aItem, tmpDCA.getIdx(), globIdx);
			retIDA = retIDA.cloneWithExteriorColorProvider(tmpCP);
		}

		return retIDA;
	}

	/**
	 * Returns whether the specified item is currently being analyzed.
	 */
	public boolean getIsDemAnalyzed(Dem aItem)
	{
		DemConfigAttr tmpDCA = configM.get(aItem);
		if (tmpDCA.getWindowCfg() != null && tmpDCA.getWindowCfg().isShown() == true)
			return true;

		return false;
	}

	/**
	 * Returns whether the specified item's exterior is rendered.
	 */
	public boolean getIsVisibleExterior(Dem aItem)
	{
		DemConfigAttr tmpDCA = configM.get(aItem);
		return tmpDCA.getDrawAttr().getIsExtShown();
	}

	/**
	 * Returns whether the specified item's interior is rendered.
	 */
	public boolean getIsVisibleInterior(Dem aItem)
	{
		DemConfigAttr tmpDCA = configM.get(aItem);
		return tmpDCA.getDrawAttr().getIsIntShown();
	}

	/**
	 * Returns whether the specified item's colorization should be synchronized
	 * to the main window.
	 */
	public boolean getIsSyncWithMain(Dem aItem)
	{
		DemConfigAttr tmpDCA = configM.get(aItem);
		return tmpDCA.getIsSyncCoring();
	}

	/**
	 * Returns whether the specified item's lighting should be synchronized from
	 * the main window.
	 */
	public boolean getIsSyncLightingWithMain(Dem aItem)
	{
		DemConfigAttr tmpDCA = configM.get(aItem);
		return tmpDCA.getIsSyncLighting();
	}

	/**
	 * Returns a mapping of key-value nodes associated with the specified item.
	 */
	public Map<String, KeyValueNode> getKeyValuePairMap(Dem aItem)
	{
		return structM.get(aItem).keyValueM;
	}

	/**
	 * Returns the opacity associated with the specified item.
	 */
	public double getOpacity(Dem aItem)
	{
		DemConfigAttr tmpDCA = configM.get(aItem);
		return tmpDCA.getDrawAttr().getOpacity();
	}

	/**
	 * Returns the radial offset associated with the specified item.
	 */
	public double getRadialOffset(Dem aItem)
	{
		DemConfigAttr tmpDCA = configM.get(aItem);
		return tmpDCA.getDrawAttr().getRadialOffset();
	}

	/**
	 * Returns the painter associated with the specified item.
	 * <p>
	 * A valid {@link VtkDemPainter} will always be returned, however it may not
	 * be ready for use and should be checked via
	 * {@link VtkDemPainter#isReady()}.
	 */
	public VtkDemPainter getPainterFor(Dem aItem)
	{
		return vPainterM.get(aItem);
	}

	/**
	 * Returns the {@link DemConfigAttr} associated with the specified DEM.
	 * <p>
	 * Changes to the returned {@link DemConfigAttr} will not have an effect on
	 * this manager's internal copy.
	 */
	public DemConfigAttr getConfigAttr(Dem aItem)
	{
		DemConfigAttr retDCA = configM.get(aItem);

		// Synch the relevant components associated with the "Analyze" window
		JFrame tmpFrame = getAnalyzeWindow(aItem);
		if (tmpFrame != null)
			retDCA = retDCA.cloneWithWindowCfg(new WindowCfg(tmpFrame));

		return retDCA;
	}

	/**
	 * Returns the {@link VtkDemPainter} associated with the specified
	 * {@link PickTarget}. Returns null if the {@link PickTarget} is not
	 * associated with this manager.
	 */
	public VtkDemPainter getPainterFor(PickTarget aPickTarg)
	{
		// Bail if tmpProp is not the right type
		vtkProp tmpProp = aPickTarg.getActor();
		if (tmpProp instanceof AssocActor == false)
			return null;

		VtkDemSurface tmpSurface = ((AssocActor) tmpProp).getAssocModel(VtkDemSurface.class);
		if (tmpSurface == null)
			return null;

		// Retrieve the painter associated with the Dem
		Dem tmpDem = tmpSurface.getDem();
		return vPainterM.get(tmpDem);
	}

	/**
	 * Returns the status of the Dem.
	 */
	public String getStatus(Dem aItem)
	{
		VtkDemPainter tmpPainter = vPainterM.get(aItem);
		return tmpPainter.getStatusMsg(true);
	}

	/**
	 * Returns the {@link DataMode} associated with the {@link Dem}'s data.
	 */
	public DataMode getViewDataMode(Dem aItem)
	{
		DemConfigAttr tmpDCA = configM.get(aItem);
		return tmpDCA.getViewDataMode();
	}

	/**
	 * Returns true if the specified {@link Dem} is associated with a custom
	 * exterior {@link ColorProvider}.
	 */
	public boolean hasCustomExteriorColorProvider(Dem aItem)
	{
		// Determine if a custom ColorProvider is installed
		DemConfigAttr tmpDCA = configM.get(aItem);
		boolean isCustom = tmpDCA.getDrawAttr().getExtCP() != ColorProvider.Invalid;
		return isCustom;
	}

	/**
	 * Notifies the manager to install the specified {@link DemConfigAttr}s
	 * configuration.
	 */
	public void installConfiguration(Map<Dem, DemConfigAttr> aAttrM)
	{
		List<Dem> tmpItemL = ImmutableList.copyOf(aAttrM.keySet());
		if (tmpItemL.size() == 0)
			return;

		// Update the configurations
		for (Dem aItem : tmpItemL)
		{
			// Skip to next if no record of the associated ConfigAttr
			DemConfigAttr origDCA = configM.get(aItem);
			if (origDCA == null)
				continue;

			// Update our internal copy of the ConfigAttr
			DemConfigAttr tmpDCA = aAttrM.get(aItem);
			configM.put(aItem, tmpDCA.cloneWithIdx(origDCA.getIdx()));
		}

		// Send out the appropriate notifications
		updateVtkVars(tmpItemL);
		notifyListeners(this, ItemEventType.ItemsMutated);
	}

	/**
	 * Notifies the manager that the "initialization stage" has been completed.
	 * <p>
	 * Until this method is called loading of VTK state will not proceed.
	 */
	public void markInitDone()
	{
		isInitDone = true;
	}

	/**
	 * Notification method that the (corresponding VtkDemPainter's) load state
	 * has changed.
	 */
	public void notifyLoadUpdate(Dem aItem, boolean aForceUpdate)
	{
		// Bail if enough time has not passed yet
		long currTime = System.currentTimeMillis();
		long diffTime = currTime - workLastUpdateTime;
		if (aForceUpdate == false && diffTime < REFRESH_FREQ_MS)
			return;

		// Ensure we are on the AWT
		if (GuiUtil.redispatchOnAwtIfNeeded(() -> notifyLoadUpdate(aItem, aForceUpdate)) == true)
			return;

		// Update our last refresh time
		workLastUpdateTime = currTime;

		// Send out the appropriate notifications
		if (getPainterFor(aItem).isReady() == true)
		{
			// Update the vtk state before showing the analyze window
			updateVtkVars(ImmutableList.of(aItem));

			// Show the Analyze window if appropriate
			WindowCfg tmpWC = getConfigAttr(aItem).getWindowCfg();
			if (tmpWC != null && tmpWC.isShown() == true)
				showAnalyzePanel(aItem, true);
		}

		// Send out the appropriate notifications
		notifyListeners(this, ItemEventType.ItemsMutated);
		notifyLoadListeners(ImmutableList.of(aItem));
	}

	/**
	 * Sets the (custom) exterior {@link ColorProvider} installed on the
	 * specified list of items.
	 * <p>
	 * Note that if {@link ColorProvider#Invalid} is provided then the exterior
	 * coloring will revert back to the default.
	 *
	 * @param aItemC The list of items of interest.
	 * @param aExteriorCP The {@link ColorProvider} used to color the exterior.
	 */
	public void setColorProviderExterior(Collection<Dem> aItemC, ColorProvider aExteriorCP)
	{
		for (Dem aItem : aItemC)
		{
			DemConfigAttr tmpDCA = configM.get(aItem);
			ItemDrawAttr tmpIDA = tmpDCA.getDrawAttr().cloneWithExteriorColorProvider(aExteriorCP);
			tmpDCA = tmpDCA.cloneWithDrawAttr(tmpIDA);
			configM.put(aItem, tmpDCA);
		}

		// Send out the appropriate notifications
		updateVtkVars(aItemC);
		notifyListeners(this, ItemEventType.ItemsMutated);
	}

	/**
	 * Sets the interior {@link ColorProvider} installed on the specified list of
	 * items.
	 *
	 * @param aItemC The list of items of interest.
	 * @param aInteriorCP The {@link ColorProvider} used to color the interior.
	 */
	public void setColorProviderInterior(Collection<Dem> aItemC, ColorProvider aInteriorCP)
	{
		for (Dem aItem : aItemC)
		{
			DemConfigAttr tmpDCA = configM.get(aItem);
			ItemDrawAttr tmpIDA = tmpDCA.getDrawAttr().cloneWithInteriorColorProvider(aInteriorCP);
			tmpDCA = tmpDCA.cloneWithDrawAttr(tmpIDA);
			configM.put(aItem, tmpDCA);
		}

		// Send out the appropriate notifications
		updateVtkVars(aItemC);
		notifyListeners(this, ItemEventType.ItemsMutated);
	}

	/**
	 * Sets the default {@link LightCfg}. All (relevant) {@link AnalyzePanel}s
	 * will be updated to reflect the specified {@link LightCfg}.
	 */
	public void setSystemLightCfg(LightCfg aLightCfg)
	{
		systemLightCfg = aLightCfg;

		for (Dem aItem : analyzeM.keySet())
		{
			DemConfigAttr tmpDCA = configM.get(aItem);
			updateLightCfg(aItem, tmpDCA);
		}
	}

	/**
	 * Sets the description associated with the list of items.
	 */
	public void setDescription(Collection<Dem> aItemC, String aDescription)
	{
		for (Dem aItem : aItemC)
		{
			DemConfigAttr tmpDCA = configM.get(aItem);
			tmpDCA = tmpDCA.cloneWithDescription(aDescription);
			configM.put(aItem, tmpDCA);

			// Update the "Analyze" window
			JFrame tmpFrame = getAnalyzeWindow(aItem);
			if (tmpFrame != null)
				tmpFrame.setTitle(getAnalyzeTitle(aItem));
		}

		notifyListeners(this, ItemEventType.ItemsMutated);
	}

	/**
	 * Sets the group color provider used to color the exterior of the dem.
	 */
	public void setGroupColorProviderExterior(GroupColorProvider aExteriorGCP)
	{
		exteriorGCP = aExteriorGCP;

		// Determine the list of Dems that need to be updated
		List<Dem> updateL = new ArrayList<Dem>();
		for (Dem aItem : configM.keySet())
		{
			DemConfigAttr tmpDCA = configM.get(aItem);
			if (tmpDCA.getDrawAttr().getIsExtShown() == true)
				updateL.add(aItem);
		}

		// Send out the appropriate notifications
		updateVtkVars(updateL);
		notifyListeners(this, ItemEventType.ItemsMutated);
	}

	/**
	 * Sets whether the specified list of items should be analyzed.
	 */
	public void setIsDemAnalyzed(Collection<Dem> aItemC, boolean aIsShown)
	{
		// Prep for the initial display
		if (aIsShown == true)
			ActionUtil.analyzeDems(this, aItemC, refParent);

		// Delegate
		for (Dem aItem : aItemC)
			showAnalyzePanel(aItem, aIsShown);

		// Send out the appropriate notifications
		updateVtkVars(aItemC);
		notifyListeners(this, ItemEventType.ItemsMutated);
	}

	/**
	 * Sets whether the exterior associated with the specified list of items
	 * should be rendered.
	 *
	 * @param aItemC The list of items of interest.
	 * @param aBool True if the exterior should be visible
	 */
	public void setIsVisibleExterior(Collection<Dem> aItemC, boolean aBool)
	{
		for (Dem aItem : aItemC)
		{
			DemConfigAttr tmpDCA = configM.get(aItem);
			ItemDrawAttr tmpIDA = tmpDCA.getDrawAttr().cloneWithExteriorIsShown(aBool);
			tmpDCA = tmpDCA.cloneWithDrawAttr(tmpIDA);
			configM.put(aItem, tmpDCA);
		}

		// Send out the appropriate notifications
		updateVtkVars(aItemC);
		notifyListeners(this, ItemEventType.ItemsMutated);

		// Force a "load" update whenever the visibility of any component changes.
		notifyLoadListeners(aItemC);
	}

	/**
	 * Sets whether the interior associated with the specified list of items
	 * should be rendered.
	 *
	 * @param aItemC The list of items of interest.
	 * @param aBool True if the interior should be visible
	 */
	public void setIsVisibleInterior(Collection<Dem> aItemC, boolean aBool)
	{
		for (Dem aItem : aItemC)
		{
			DemConfigAttr tmpDCA = configM.get(aItem);
			ItemDrawAttr tmpIDA = tmpDCA.getDrawAttr().cloneWithInteriorIsShown(aBool);
			tmpDCA = tmpDCA.cloneWithDrawAttr(tmpIDA);
			configM.put(aItem, tmpDCA);
		}

		// Send out the appropriate notifications
		updateVtkVars(aItemC);
		notifyListeners(this, ItemEventType.ItemsMutated);

		// Force a "load" update whenever the visibility of any component changes.
		notifyLoadListeners(aItemC);
	}

	/**
	 * Sets whether the specified list of item's colorization should be
	 * synchronized to the main window.
	 *
	 * @param aItemC The list of items of interest.
	 * @param aBool True if the objects colorization should be synchronized.
	 */
	public void setIsSyncColoring(Collection<Dem> aItemC, boolean aBool)
	{
		for (Dem aItem : aItemC)
		{
			DemConfigAttr tmpDCA = configM.get(aItem);
			tmpDCA = tmpDCA.cloneWithSyncColoring(aBool);
			configM.put(aItem, tmpDCA);
		}

		// Send out the appropriate notifications
		updateVtkVars(aItemC);
		notifyListeners(this, ItemEventType.ItemsMutated);
	}

	/**
	 * Sets whether the specified list of item's Lighting should be synchronized
	 * from the main window.
	 *
	 * @param aItemC The list of items of interest.
	 * @param aBool True if the objects colorization should be synchronized.
	 */
	public void setIsSyncLighting(Collection<Dem> aItemC, boolean aBool)
	{
		for (Dem aItem : aItemC)
		{
			DemConfigAttr tmpDCA = configM.get(aItem);
			tmpDCA = tmpDCA.cloneWithSyncLighting(aBool);
			configM.put(aItem, tmpDCA);
		}

		// Send out the appropriate notifications
		updateVtkVars(aItemC);
		notifyListeners(this, ItemEventType.ItemsMutated);
	}

	/**
	 * Sets the key-value pairs (properties) associated with the specified item.
	 */
	public void setKeyValuePairMap(Dem aItem, Map<String, KeyValueNode> aKeyValueM)
	{
		DemStruct tmpStruct = structM.get(aItem);
		tmpStruct = new DemStruct(aItem, aKeyValueM, tmpStruct.coordinateSystem);
		structM.put(aItem, tmpStruct);

		// Send out the appropriate notifications
		notifyListeners(this, ItemEventType.ItemsMutated);
	}

	/**
	 * Sets {@link LightCfg} to be used by the the specified list of items.
	 *
	 * @param aItemC The list of items of interest.
	 * @param aLightCfg The {@link LightCfg} of interest.
	 */
	public void setLightCfg(Collection<Dem> aItemC, LightCfg aLightCfg)
	{
		for (Dem aItem : aItemC)
		{
			DemConfigAttr tmpDCA = configM.get(aItem);
			tmpDCA = tmpDCA.cloneWithLightCfg(aLightCfg);
			configM.put(aItem, tmpDCA);
		}

		// Send out the appropriate notifications
		updateVtkVars(aItemC);
		notifyListeners(this, ItemEventType.ItemsMutated);
	}

	/**
	 * Sets the opacity of the list of items.
	 */
	public void setOpacity(Collection<Dem> aItemC, double aOpacity)
	{
		for (Dem aItem : aItemC)
		{
			DemConfigAttr tmpDCA = configM.get(aItem);
			ItemDrawAttr tmpIDA = tmpDCA.getDrawAttr().cloneWithOpacity(aOpacity);
			tmpDCA = tmpDCA.cloneWithDrawAttr(tmpIDA);
			configM.put(aItem, tmpDCA);
		}

		// Send out the appropriate notifications
		updateVtkVars(aItemC);
		notifyListeners(this, ItemEventType.ItemsMutated);
	}

	/**
	 * Sets the radial offset of the specified list of items.
	 */
	public void setRadialOffset(Collection<Dem> aItemC, double aRadialOffset)
	{
		for (Dem aItem : aItemC)
		{
			DemConfigAttr tmpDCA = configM.get(aItem);
			ItemDrawAttr tmpIDA = tmpDCA.getDrawAttr().cloneWithRadialOffset(aRadialOffset);
			tmpDCA = tmpDCA.cloneWithDrawAttr(tmpIDA);
			configM.put(aItem, tmpDCA);
		}

		// Send out the appropriate notifications
		updateVtkVars(getAllItems());
		notifyListeners(this, ItemEventType.ItemsMutated);
	}

	/**
	 * Sets the {@link DataMode} for the list of items.
	 */
	public void setViewDataMode(Collection<Dem> aItemC, DataMode aViewDataMode)
	{
		for (Dem aItem : aItemC)
		{
			DemConfigAttr tmpDCA = configM.get(aItem);
			tmpDCA = tmpDCA.cloneWithViewDataMode(aViewDataMode);
			configM.put(aItem, tmpDCA);
		}

		// Send out the appropriate notifications
		updateVtkVars(aItemC);
		notifyListeners(this, ItemEventType.ItemsMutated);
	}

	/**
	 * Notification that this manager will be shutdown
	 */
	public void shutdown()
	{
		; // Nothing to do
	}

	/**
	 * Updates the mapping of {@link Dem} to {@link DemConfigAttr}.
	 * <p>
	 * This method will not remove currently installed {@link DemConfigAttr}s if
	 * there is no mapping. In addition a copy of the utilized
	 * {@link DemConfigAttr} will be created rather than the provided one.
	 * Changes to the {@link DemConfigAttr}s after calling this method will have
	 * no effect on this manager.
	 */
	public void updateConfigAttrMap(Map<Dem, DemConfigAttr> aAttrM)
	{
		for (Dem aItem : aAttrM.keySet())
		{
			// Skip to next if there is no current mapping
			DemConfigAttr oldDCA = configM.get(aItem);
			if (oldDCA == null)
				continue;

			DemConfigAttr tmpDCA = aAttrM.get(aItem);
			configM.put(aItem, tmpDCA.cloneWithIdx(oldDCA.getIdx()));
		}

		updateVtkVars(aAttrM.keySet());
		notifyListeners(this, ItemEventType.ItemsMutated);
	}

	@Override
	public List<vtkProp> getProps()
	{
		List<vtkProp> retL = new ArrayList<>();

		for (Dem aItem : getAllItems())
		{
			// Skip to next if both the exterior and interior are not visible
			ItemDrawAttr tmpDA = configM.get(aItem).getDrawAttr();
			if (tmpDA.getIsExtShown() == false && tmpDA.getIsIntShown() == false)
				continue;

			// Skip to next if the painter is not ready
			VtkDemPainter tmpPainter = vPainterM.get(aItem);
			if (tmpPainter.isReady() == false)
				continue;

			retL.addAll(tmpPainter.getProps());
		}

		return retL;
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

		// Bail if no associated painter
		VtkDemPainter tmpPainter = getPainterFor(aPrimaryTarg);
		if (tmpPainter == null)
			return;

		// Update the selection
		Dem tmpItem = tmpPainter.getItem();
		HookUtil.updateSelection(this, aEvent, tmpItem);

		Set<Dem> tmpItemS = getSelectedItems();
		updateVtkVars(tmpItemS);

		Object source = aEvent.getSource();
		notifyListeners(source, ItemEventType.ItemsSelected);
	}

	@Override
	public void removeItems(Collection<Dem> aItemC)
	{
		if (aItemC.isEmpty() == true)
			return;

		List<Dem> fullL = new ArrayList<>(getAllItems());
		fullL.removeAll(aItemC);
		setAllItems(fullL);
	}

	public void setAllStructs(Collection<DemStruct> aItemC)
	{
		// Synthesize the list of dems
		List<Dem> fullDemL = new ArrayList<>();
		for (DemStruct aItem : aItemC)
			fullDemL.add(aItem.dem);

		// Clear out the various maps
		Set<Dem> delS = Sets.difference(new HashSet<>(getAllItems()), new HashSet<>(fullDemL));
		for (Dem aItem : delS)
		{
			configM.remove(aItem);
			structM.remove(aItem);

			AnalyzePanel tmpPanel = analyzeM.remove(aItem);
			if (tmpPanel != null)
			{
				tmpPanel.dispose();

				// Release the window resources
				JFrame tmpFrame = (JFrame) JOptionPane.getFrameForComponent(tmpPanel);
				if (tmpFrame != null)
				{
					tmpFrame.setVisible(false);
					tmpFrame.dispose();
				}
			}
		}

		// Clear out unused VtkDemPainters
		VtkUtil.flushResourceMap(vPainterM, fullDemL);

		// Garbage collect
		System.gc();
		vtkObject.JAVA_OBJECT_MANAGER.gc(true);

		// Update the struct cache
		structM = new LinkedHashMap<>();
		for (DemStruct aItem : aItemC)
			structM.put(aItem.dem, aItem);

		// Set up the initial configurations and painters for all items
		Map<Dem, DemConfigAttr> oldConfigM = configM;
		configM = new HashMap<>();
		for (Dem aItem : fullDemL)
		{
			DemConfigAttr tmpDCA = oldConfigM.get(aItem);
			if (tmpDCA == null)
			{
				tmpDCA = new DemConfigAttr(globIdx);
				globIdx++;
			}
			configM.put(aItem, tmpDCA);

			VtkDemPainter tmpPainter = vPainterM.get(aItem);
			if (tmpPainter == null)
				vPainterM.put(aItem, new VtkDemPainter(this, aItem));
		}

		// Delegate
		super.setAllItems(fullDemL);

		updateVtkVars(fullDemL);
	}

	@Override
	public void setAllItems(Collection<Dem> aItemC)
	{
		// Synthesize a list of DemStructs
		List<DemStruct> tmpStructL = new ArrayList<>();
		for (Dem aItem : aItemC)
		{
			// Utilize the cache or create a new one
			DemStruct tmpStruct = structM.get(aItem);
			if (tmpStruct == null)
				tmpStruct = new DemStruct(aItem);

			tmpStructL.add(tmpStruct);
		}

		// Delegate to setAllStructs
		setAllStructs(tmpStructL);
	}

	@Override
	public void setSelectedItems(Collection<Dem> aItemC)
	{
		super.setSelectedItems(aItemC);

		updateStatus(aItemC);
	}

	/**
	 * Helper method that return the title that should be used for the "Analyze"
	 * window.
	 */
	private String getAnalyzeTitle(Dem aItem)
	{
		return "Analyze: " + getDisplayName(aItem);
	}

	/**
	 * Helper method that returns the "Analyze" window corresponding to the
	 * specified item.
	 */
	private JFrame getAnalyzeWindow(Dem aItem)
	{
		// Bail if there is not even a UI component for the Dem
		AnalyzePanel analyzePanel = analyzeM.get(aItem);
		if (analyzePanel == null)
			return null;

		// Delegate
		JFrame retFrame = (JFrame) JOptionPane.getFrameForComponent(analyzePanel);
		return retFrame;
	}

	/**
	 * Helper method to send out notification when a load has been completed.
	 */
	private void notifyLoadListeners(Collection<Dem> aItemC)
	{
		for (LoadListener<Dem> aListener : loadListenerL)
			aListener.handleLoadEvent(this, aItemC);
	}

	/**
	 * Helper method that will show (or hide) the {@link AnalyzePanel}
	 * corresponding to the specified item.
	 */
	private void showAnalyzePanel(Dem aItem, boolean aIsShown)
	{
		// Toggle the visibility configuration for the Analyze window
		DemConfigAttr tmpDCA = configM.get(aItem);
		WindowCfg tmpWC = tmpDCA.getWindowCfg();
		if (tmpWC != null)
		{
			tmpDCA = tmpDCA.cloneWithWindowCfg(tmpWC.withIsShown(aIsShown));
			configM.put(aItem, tmpDCA);
		}

		// Just bail if the window should not be shown and does not exist
		JFrame tmpFrame = getAnalyzeWindow(aItem);
		if (tmpFrame == null && aIsShown == false)
			return;

		// Bail if the painter is not ready
		if (getPainterFor(aItem).isReady() == false)
			return;

		// Just toggle the visibility of the analyze window if it already exists
		if (tmpFrame != null)
		{
			tmpFrame.setVisible(aIsShown);
			if (aIsShown == true)
				tmpFrame.toFront();
			return;
		}

		// Create and install the AnalyzePanel
		VtkDemSurface tmpSurface = vPainterM.get(aItem).getVtkDemSurface();
		AnalyzePanel analyzePanel = new AnalyzePanel(this, aItem, tmpSurface, refSmallBody, tmpDCA);
		analyzeM.put(aItem, analyzePanel);

		// Manually set the initial light configuration
		updateLightCfg(aItem, tmpDCA);

		// Set up the Analyze window
		tmpFrame = new JFrame();
		tmpFrame.addWindowListener(new AnalyzeWindowListener(this, aItem));
		tmpFrame.setIconImage(IconUtil.getAppMainImage().getImage());
		tmpFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		tmpFrame.setContentPane(analyzePanel);
		tmpFrame.setJMenuBar(DemGuiUtil.formAnalyzeMenuBar(analyzePanel));
		tmpFrame.setTitle(getAnalyzeTitle(aItem));

		// Show the Analyze window
		WindowCfg tmpWindowCfg = tmpDCA.getWindowCfg();
		if (tmpWindowCfg != null)
			tmpWindowCfg.configure(tmpFrame);

		tmpFrame.setVisible(true);
	}

	/**
	 * Helper method that updates the {@link LightCfg} associated the Dem's
	 * {@link AnalyzePanel}.
	 */
	private void updateLightCfg(Dem aItem, DemConfigAttr aItemDCA)
	{
		// Bail if no AnalyzePanel
		AnalyzePanel tmpAnalyzePanel = analyzeM.get(aItem);
		if (tmpAnalyzePanel == null)
			return;

		// Update the AnalyzePanel's with the appropriate LightCfg
		LightCfg tmpLightCfg = aItemDCA.getRenderLC();
		if (aItemDCA.getIsSyncLighting() == true)
			tmpLightCfg = systemLightCfg;

		tmpAnalyzePanel.setLightCfg(tmpLightCfg);
	}

	/**
	 * Helper method that updates the {@link StatusNotifier} with the selected
	 * items.
	 */
	private void updateStatus(Collection<Dem> aItemC)
	{
		// Send out the status
		String briefMsg = null;
		String detailMsg = null;
		if (aItemC.size() == 1)
		{
			Dem tmpItem = aItemC.iterator().next();
			briefMsg = "Regional DTM: " + getDisplayName(tmpItem);
			detailMsg = tmpItem.getSource().getPath();
		}
		else if (aItemC.size() > 1)
		{
			briefMsg = "Multiple regional DTMs selected: " + aItemC.size();

			int currCnt = 0;
			detailMsg = "<html>";
			for (Dem aItem : aItemC)
			{
				currCnt++;
				detailMsg += getDisplayName(aItem);
				if (currCnt == 5)
				{
					int numRemain = aItemC.size() - currCnt;
					if (numRemain > 0)
						detailMsg += "<br>plus " + numRemain + " others.";
					break;
				}
				detailMsg += "<br>";
			}
			detailMsg += "</html>";
		}

		refStatusNotifier.setPriStatus(briefMsg, detailMsg);
	}

	/**
	 * Helper method that will update all relevant VTK vars.
	 */
	private void updateVtkVars(Collection<Dem> aUpdateC)
	{
		// Bail if initialization has not completed
		if (isInitDone == false)
			return;

		for (Dem aItem : aUpdateC)
		{
			VtkDemPainter tmpPainter = vPainterM.get(aItem);
			tmpPainter.vtkUpdateState();

			// Start a load if necessary
			DemConfigAttr tmpDCA = configM.get(aItem);
			boolean tmpBool = tmpPainter.isLoadNeeded() == true;
			tmpBool |= tmpDCA.getWindowCfg() != null && tmpDCA.getWindowCfg().isShown() == true;
			if (tmpBool == true)
				tmpPainter.vtkStateInit(workExecutor);

			// Update the LightCfg for the corresponding AnalyzePanel
			updateLightCfg(aItem, tmpDCA);
		}

		refSceneChangeNotifier.notifySceneChange();
	}

}
