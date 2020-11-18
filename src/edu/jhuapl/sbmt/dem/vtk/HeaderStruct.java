package edu.jhuapl.sbmt.dem.vtk;

import java.util.List;
import java.util.Map;

import edu.jhuapl.sbmt.dem.KeyValueNode;

import nom.tam.fits.BasicHDU;

/**
 * Intermediate object used to hold FITS header data.
 * <p>
 * While this class is immutable - the field members are not! This class is
 * intended to be used as intermediate staging step while instantiating dem
 * data.
 * <p>
 * The object that creates this intermediate struct is responsible for
 * management of the life cycle of the underlying objects.
 *
 * @author lopeznr1
 */
public class HeaderStruct
{
	public final BasicHDU<?> hdu;

	public final Map<String, KeyValueNode> keyValueM;
	public final List<Integer> backPlaneIndexL;
	public final List<String> unprocessedBackPlaneNameL;
	public final List<String> unprocessedBackPlaneUnitL;

	public final int xIdx;
	public final int yIdx;
	public final int zIdx;

	/** Standard Constructor */
	public HeaderStruct(BasicHDU<?> aHdu, Map<String, KeyValueNode> aKeyValueM, List<Integer> aBackPlaneIndexL,
			List<String> aUnprocessedBackPlaneNameL, List<String> aUnprocessedBackPlaneUnitL, int aXIdx, int aYIdx,
			int aZIdx)
	{
		hdu = aHdu;
		keyValueM = aKeyValueM;
		backPlaneIndexL = aBackPlaneIndexL;
		unprocessedBackPlaneNameL = aUnprocessedBackPlaneNameL;
		unprocessedBackPlaneUnitL = aUnprocessedBackPlaneUnitL;
		xIdx = aXIdx;
		yIdx = aYIdx;
		zIdx = aZIdx;
	}

}
