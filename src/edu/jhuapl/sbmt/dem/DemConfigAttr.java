package edu.jhuapl.sbmt.dem;

import edu.jhuapl.sbmt.dem.gui.analyze.AnalyzePanel;
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
 * <li>Flag of whether the interior should be colorized
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
	public static final DemConfigAttr Invalid = new DemConfigAttr(-1, null, ItemDrawAttr.Default, false, true, null);

	// Associated properties
	/** Defines a "unique" index for the item. */
	private final int uIdx;
	/** Defines the description of the item. May be null. */
	private final String description;
	/** Defines the attributes that control how the dem will be drawn. */
	private final ItemDrawAttr drawAttr;
	/** Defines if the dem's interior should be colorized. */
	private final boolean isColorizeInterior;
	/** Defines whether bad data should be displayed. **/
	private final boolean viewBadData;
	/** Defines the window display component settings. */
	private final WindowCfg analyzeWC;

	/** Standard Constructor */
	public DemConfigAttr(int aIdx, String aDescription, ItemDrawAttr aDrawAttr, boolean aIsColorizedInterior,
			boolean aViewBadData, WindowCfg aAnalyzeWC)
	{
		uIdx = aIdx;
		description = aDescription;
		drawAttr = aDrawAttr;
		isColorizeInterior = aIsColorizedInterior;
		viewBadData = aViewBadData;
		analyzeWC = aAnalyzeWC;
	}

	/** Simplified Constructor */
	public DemConfigAttr(int aIdx)
	{
		uIdx = aIdx;
		description = null;
		drawAttr = ItemDrawAttr.Default;
		isColorizeInterior = false;
		viewBadData = true;
		analyzeWC = null;
	}

	/** Clone object with the custom description. */
	public DemConfigAttr cloneWithDescription(String aDescription)
	{
		return new DemConfigAttr(uIdx, aDescription, drawAttr, isColorizeInterior, viewBadData, analyzeWC);
	}

	/** Clone object with the custom {@link ItemDrawAttr}. */
	public DemConfigAttr cloneWithDrawAttr(ItemDrawAttr aDrawAttr)
	{
		return new DemConfigAttr(uIdx, description, aDrawAttr, isColorizeInterior, viewBadData, analyzeWC);
	}

	/** Clone object with the custom unique index. */
	public DemConfigAttr cloneWithIdx(int aIdx)
	{
		return new DemConfigAttr(aIdx, description, drawAttr, isColorizeInterior, viewBadData, analyzeWC);
	}

	/** Clone object with the custom isColorizeInterior flag. */
	public DemConfigAttr cloneWithIsColorizeInterior(boolean aIsColorizeInterior)
	{
		return new DemConfigAttr(uIdx, description, drawAttr, aIsColorizeInterior, viewBadData, analyzeWC);
	}

	/** Clone object with the custom viewBadData mode. */
	public DemConfigAttr cloneWithViewBadData(boolean aViewBadData)
	{
		return new DemConfigAttr(uIdx, description, drawAttr, isColorizeInterior, aViewBadData, analyzeWC);
	}

	/** Clone object with the custom {@link WindowCfg}. */
	public DemConfigAttr cloneWithWindowCfg(WindowCfg aWindowCfg)
	{
		return new DemConfigAttr(uIdx, description, drawAttr, isColorizeInterior, viewBadData, aWindowCfg);
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

	public boolean getIsColorizeInterior()
	{
		return isColorizeInterior;
	}

	public boolean getViewBadData()
	{
		return viewBadData;
	}

	public WindowCfg getWindowCfg()
	{
		return analyzeWC;
	}

}
