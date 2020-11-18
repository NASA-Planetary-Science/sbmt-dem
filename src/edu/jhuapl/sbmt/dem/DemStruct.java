package edu.jhuapl.sbmt.dem;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.jhuapl.saavtk.camera.CoordinateSystem;

/**
 * Immutable intermediate object used to associate a {@link Dem} with a variety
 * of immutable associated structures.
 * <p>
 * This class is intended to be used as intermediate staging step while
 * instantiating DEM data.
 *
 * @author lopeznr1
 */
public class DemStruct
{
	public final Dem dem;
	public final ImmutableMap<String, KeyValueNode> keyValueM;
	public final CoordinateSystem coordinateSystem;

	/** Standard Constructor */
	public DemStruct(Dem aDem, Map<String, KeyValueNode> aKeyValueM, CoordinateSystem aCoordinateSystem)
	{
		dem = aDem;
		keyValueM = ImmutableMap.copyOf(aKeyValueM);
		coordinateSystem = aCoordinateSystem;
	}

	/** Simplified Constructor */
	public DemStruct(Dem aDem)
	{
		this(aDem, ImmutableMap.of(), null);
	}
}
