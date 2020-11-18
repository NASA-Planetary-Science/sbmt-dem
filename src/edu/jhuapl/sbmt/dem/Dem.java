package edu.jhuapl.sbmt.dem;

import glum.source.Source;

/**
 * Immutable object that defines a single digital elevation model (DEM).
 *
 * @author lopeznr1
 */
public class Dem
{
	// Attributes
	private final Source source;
	private final double lat;
	private final double lon;
	private final double gsd;
	private final double numPix;

	/** Standard Constructor */
	public Dem(Source aSource, double aLat, double aLon, double aGsd, double aNumPix)
	{
		source = aSource;
		lat = aLat;
		lon = aLon;
		gsd = aGsd;
		numPix = aNumPix;
	}

	/**
	 * Returns the center latitude.
	 */
	public double getLat()
	{
		return lat;
	}

	/**
	 * Returns the center longitude.
	 */
	public double getLon()
	{
		return lon;
	}

	/**
	 * Returns the ground sample distance (per pixel).
	 */
	public double getGsd()
	{
		return gsd;
	}

	/**
	 * Returns the dimension of the dem in pixels.
	 * <p>
	 * Note this assumes the dem is a square.
	 */
	public double getNumPixels()
	{
		return numPix;
	}

	/**
	 * Returns the source of the dem.
	 */
	public Source getSource()
	{
		return source;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Dem other = (Dem) obj;
		if (Double.doubleToLongBits(numPix) != Double.doubleToLongBits(other.numPix))
			return false;
		if (Double.doubleToLongBits(lat) != Double.doubleToLongBits(other.lat))
			return false;
		if (Double.doubleToLongBits(lon) != Double.doubleToLongBits(other.lon))
			return false;
		if (Double.doubleToLongBits(gsd) != Double.doubleToLongBits(other.gsd))
			return false;
		if (source == null)
		{
			if (other.source != null)
				return false;
		}
		else if (!source.equals(other.source))
			return false;
		return true;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(numPix);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(lat);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(lon);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(gsd);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		return result;
	}

}
