package edu.jhuapl.sbmt.dem.gui;

import java.util.List;

import com.google.common.collect.ImmutableList;

import edu.jhuapl.saavtk.color.provider.ColorProvider;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemManager;

import glum.gui.panel.itemList.BasicItemHandler;
import glum.gui.panel.itemList.query.QueryComposer;

/**
 * ItemHandler used to process DEMs.
 *
 * @author lopeznr1
 */
public class DemItemHandler extends BasicItemHandler<Dem, LookUp>
{
	// Ref vars
	private final DemManager refManager;

	/** Standard Constructor */
	public DemItemHandler(DemManager aManager, QueryComposer<LookUp> aComposer)
	{
		super(aComposer);

		refManager = aManager;
	}

	@Override
	public Object getColumnValue(Dem aItem, LookUp aEnum)
	{
		switch (aEnum)
		{
			case IsAnalyzePanel:
				return refManager.getIsDemAnalyzed(aItem);
			case IsShowExterior:
				return refManager.getIsVisibleExterior(aItem);
			case IsShowInterior:
				return refManager.getIsVisibleInterior(aItem);
			case ColorExterior:
				return refManager.getColorProviderExterior(aItem);
			case ColorInterior:
				return refManager.getColorProviderInterior(aItem);
			case Description:
				return refManager.getDisplayName(aItem);
			case Status:
				return refManager.getStatus(aItem);
			case Latitude:
				return aItem.getLat();
			case Longitude:
				return aItem.getLon();
			case GroundSampleDistance:
				return aItem.getGsd();
			case NumPixels:
				return aItem.getNumPixels();
			default:
				break;
		}

		throw new UnsupportedOperationException("Column is not supported. Enum: " + aEnum);
	}

	@Override
	public void setColumnValue(Dem aItem, LookUp aEnum, Object aValue)
	{
		List<Dem> tmpL = ImmutableList.of(aItem);

		if (aEnum == LookUp.IsAnalyzePanel)
			refManager.setIsDemAnalyzed(tmpL, (boolean) aValue);
		else if (aEnum == LookUp.IsShowExterior)
			refManager.setIsVisibleExterior(tmpL, (boolean) aValue);
		else if (aEnum == LookUp.IsShowInterior)
			refManager.setIsVisibleInterior(tmpL, (boolean) aValue);
		else if (aEnum == LookUp.ColorExterior)
			refManager.setColorProviderExterior(tmpL, (ColorProvider) aValue);
		else if (aEnum == LookUp.ColorInterior)
			refManager.setColorProviderInterior(tmpL, (ColorProvider) aValue);
		else
			throw new UnsupportedOperationException("Column is not supported. Enum: " + aEnum);
	}

}
