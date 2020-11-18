package edu.jhuapl.sbmt.dem.gui.prop;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.jhuapl.sbmt.dem.KeyValueNode;

import glum.gui.panel.itemList.BasicItemHandler;
import glum.gui.panel.itemList.query.QueryComposer;

/**
 * ItemHandler used to process key-value pairs.
 *
 * @author lopeznr1
 */
public class KeyValueItemHandler extends BasicItemHandler<String, LookUp>
{
	// State vars
	private ImmutableMap<String, KeyValueNode> workKeyValueM;

	/** Standard Constructor */
	public KeyValueItemHandler(QueryComposer<LookUp> aComposer)
	{
		super(aComposer);

		workKeyValueM = ImmutableMap.of();
	}

	/**
	 * Returns the currently installed key-value map
	 */
	public ImmutableMap<String, KeyValueNode> getKeyValuePairMap()
	{
		return workKeyValueM;
	}

	/**
	 * Installs in the new key-value map.
	 */
	public void setKeyValuePairMap(Map<String, KeyValueNode> aKeyValueM)
	{
		workKeyValueM = ImmutableMap.copyOf(aKeyValueM);
	}

	@Override
	public Object getColumnValue(String aItem, LookUp aEnum)
	{
		String tmpKey = aItem;

		switch (aEnum)
		{
			case Key:
				return tmpKey;
			case Value:
				return workKeyValueM.get(tmpKey).getValue();
			case Comment:
				return workKeyValueM.get(tmpKey).getComment();
			default:
				break;
		}

		throw new UnsupportedOperationException("Column is not supported. Enum: " + aEnum);
	}

	@Override
	public void setColumnValue(String aItem, LookUp aEnum, Object aValue)
	{
		throw new UnsupportedOperationException("Column is not supported. Enum: " + aEnum);
	}

}
