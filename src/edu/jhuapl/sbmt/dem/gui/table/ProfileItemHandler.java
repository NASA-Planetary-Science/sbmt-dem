package edu.jhuapl.sbmt.dem.gui.table;

import java.awt.Color;

import com.google.common.collect.ImmutableList;

import edu.jhuapl.saavtk.structure.PolyLine;
import edu.jhuapl.saavtk.structure.StructureManager;
import edu.jhuapl.saavtk.util.LatLon;

import glum.gui.panel.itemList.BasicItemHandler;
import glum.gui.panel.itemList.query.QueryComposer;

/**
 * ItemHandler used to process {@link PolyLine} (profiles).
 *
 * @author lopeznr1
 */
public class ProfileItemHandler<G1 extends PolyLine> extends BasicItemHandler<G1, LookUp>
{
	// Ref vars
	private final StructureManager<G1> refManager;

	/** Standard Constructor */
	public ProfileItemHandler(StructureManager<G1> aManager, QueryComposer<LookUp> aComposer)
	{
		super(aComposer);

		refManager = aManager;
	}

	@Override
	public Object getColumnValue(G1 aItem, LookUp aEnum)
	{
		var controlPointL = aItem.getControlPoints();
		var begLL = new LatLon(Double.NaN, Double.NaN);
		if (controlPointL.size() > 0)
			begLL = controlPointL.get(0);
		var endLL = begLL;
		if (controlPointL.size() > 1)
			endLL = controlPointL.get(1);

		switch (aEnum)
		{
			case Id:
				return aItem.getId();
			case Source:
				return aItem.getSource();
//			case Type:
//				return getTypeString(aItem);
			case IsVisible:
				return aItem.getVisible();
			case Color:
				return aItem.getColor();

			case BegLat:
				return begLL.lat;
			case BegLon:
				return begLL.lon;

			case EndLat:
				return endLL.lat;
			case EndLon:
				return endLL.lon;

//			case Name:
//				return aItem.getName();
//			case Label:
//				return aItem.getLabel();

			case Length:
				return aItem.getRenderState().pathLength();

			default:
				break;
		}

		throw new UnsupportedOperationException("Column is not supported. Enum: " + aEnum);
	}

	@Override
	public void setColumnValue(G1 aItem, LookUp aEnum, Object aValue)
	{
		var itemL = ImmutableList.of(aItem);
		if (aEnum == LookUp.IsVisible)
			refManager.setIsVisible(itemL, (boolean) aValue);
		else if (aEnum == LookUp.Color)
			refManager.setColor(itemL, (Color) aValue);
//		else if (aEnum == LookUp.Name)
//			aItem.setName((String) aValue);
//		else if (aEnum == LookUp.Label)
//			refManager.setLabel(aItem, (String) aValue);
		else
			throw new UnsupportedOperationException("Column is not supported. Enum: " + aEnum);
	}

}
