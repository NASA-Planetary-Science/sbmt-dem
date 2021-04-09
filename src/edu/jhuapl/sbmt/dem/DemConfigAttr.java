package edu.jhuapl.sbmt.dem;

import edu.jhuapl.saavtk.view.light.LightCfg;
import edu.jhuapl.sbmt.dem.gui.analyze.AnalyzePanel;
import edu.jhuapl.sbmt.dem.vtk.DataMode;
import edu.jhuapl.sbmt.dem.vtk.ItemDrawAttr;

import glum.gui.info.WindowCfg;

/**
 * Immutable object that defines the configuration associated with a
 * {@link Dem}.
 * <p>
 * The supported attributes are:
 * <ul>
 * <li>Textual description of the {@link Dem}.
 * <li>{@link ItemDrawAttr}
 * <li>Flag of whether the coloring should be synchronized to the body
 * <li>Flag of whether the lighting should be synchronized from the body
 * <li>Flag that defines if bad data should be shown
 * <li>{@link WindowCfg} associated with an {@link AnalyzePanel}
 * </ul>
 *
 * @author lopeznr1
 */
public class DemConfigAttr
{
	// Constants
	/** The "invalid" {@link DemConfigAttr}. */
	public static final DemConfigAttr Invalid = new DemConfigAttr(-1, null, ItemDrawAttr.Default, false, true,
			LightCfg.Invalid, DataMode.Regular, null);

	// Associated properties
	/** Defines a "unique" index for the item. */
	private final int uIdx;
	/** Defines the description of the item. May be null. */
	private final String description;
	/** Defines the attributes that control how the dem will be drawn. */
	private final ItemDrawAttr drawAttr;
	/** Defines if the dem's colorization is synchronized to the main body. */
	private final boolean isSyncColoring;
	/** Defines if the dem's lighting is synchronized from the main body. */
	private final boolean isSyncLighting;
	/** Defines a custom {@link LightCfg} to be used to render the dem. */
	private final LightCfg renderLC;
	/** Defines the {@link DataMode} for which the dem will be viewed. **/
	private final DataMode viewDataMode;
	/** Defines the window display component settings. */
	private final WindowCfg analyzeWC;

	/** Standard Constructor */
	public DemConfigAttr(int aIdx, String aDescription, ItemDrawAttr aDrawAttr, boolean aIsSyncColoring,
			boolean aIsSyncLighting, LightCfg aRenderLC, DataMode aViewDataMode, WindowCfg aAnalyzeWC)
	{
		uIdx = aIdx;
		description = aDescription;
		drawAttr = aDrawAttr;
		isSyncColoring = aIsSyncColoring;
		isSyncLighting = aIsSyncLighting;
		renderLC = aRenderLC;
		viewDataMode = aViewDataMode;
		analyzeWC = aAnalyzeWC;
	}

	/** Simplified Constructor */
	public DemConfigAttr(int aIdx)
	{
		this(aIdx, null, ItemDrawAttr.Default, false, true, LightCfg.Default, DataMode.Regular, null);
	}

	/** Clone object with the custom description. */
	public DemConfigAttr cloneWithDescription(String aDescription)
	{
		return new DemConfigAttr(uIdx, aDescription, drawAttr, isSyncColoring, isSyncLighting, renderLC, viewDataMode,
				analyzeWC);
	}

	/** Clone object with the custom {@link ItemDrawAttr}. */
	public DemConfigAttr cloneWithDrawAttr(ItemDrawAttr aDrawAttr)
	{
		return new DemConfigAttr(uIdx, description, aDrawAttr, isSyncColoring, isSyncLighting, renderLC, viewDataMode,
				analyzeWC);
	}

	/** Clone object with the custom unique index. */
	public DemConfigAttr cloneWithIdx(int aIdx)
	{
		return new DemConfigAttr(aIdx, description, drawAttr, isSyncColoring, isSyncLighting, renderLC, viewDataMode,
				analyzeWC);
	}

	/** Clone object with the custom isSyncColoring flag. */
	public DemConfigAttr cloneWithSyncColoring(boolean aIsSyncColoring)
	{
		return new DemConfigAttr(uIdx, description, drawAttr, aIsSyncColoring, isSyncLighting, renderLC, viewDataMode,
				analyzeWC);
	}

	/** Clone object with the custom isSyncLighting flag. */
	public DemConfigAttr cloneWithSyncLighting(boolean aIsSyncLighting)
	{
		return new DemConfigAttr(uIdx, description, drawAttr, isSyncColoring, aIsSyncLighting, renderLC, viewDataMode,
				analyzeWC);
	}

	/** Clone object with the custom render {@link LightCfg}. */
	public DemConfigAttr cloneWithLightCfg(LightCfg aRenderLC)
	{
		return new DemConfigAttr(uIdx, description, drawAttr, isSyncColoring, isSyncLighting, aRenderLC, viewDataMode,
				analyzeWC);
	}

	/** Clone object with the custom view {@link DataMode}. */
	public DemConfigAttr cloneWithViewDataMode(DataMode aViewDataMode)
	{
		return new DemConfigAttr(uIdx, description, drawAttr, isSyncColoring, isSyncLighting, renderLC, aViewDataMode,
				analyzeWC);
	}

	/** Clone object with the custom {@link WindowCfg}. */
	public DemConfigAttr cloneWithWindowCfg(WindowCfg aWindowCfg)
	{
		return new DemConfigAttr(uIdx, description, drawAttr, isSyncColoring, isSyncLighting, renderLC, viewDataMode,
				aWindowCfg);
	}

	public int getIdx()
	{
		return uIdx;
	}

	public String getDescription()
	{
		return description;
	}

	public ItemDrawAttr getDrawAttr()
	{
		return drawAttr;
	}

	public boolean getIsSyncCoring()
	{
		return isSyncColoring;
	}

	public boolean getIsSyncLighting()
	{
		return isSyncLighting;
	}

	/**
	 * Returns the {@link LightCfg} when rendered with custom lighting.
	 */
	public LightCfg getRenderLC()
	{
		return renderLC;
	}

	public DataMode getViewDataMode()
	{
		return viewDataMode;
	}

	public WindowCfg getWindowCfg()
	{
		return analyzeWC;
	}

}
