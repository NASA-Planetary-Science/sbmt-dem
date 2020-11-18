package edu.jhuapl.sbmt.dem.vtk;

import edu.jhuapl.sbmt.dem.Dem;

/**
 * Package private enum which defines the source type for this {@link Dem}.
 *
 * @author lopeznr1
 */
enum DataType
{
	/** An unknown type of stream was encountered. */
	Invalid,

	/** {@link Dem} was sourced from a FITS stream. */
	Fits,

	/** {@link Dem} was sourced from a OBJ stream. */
	Obj;

}
