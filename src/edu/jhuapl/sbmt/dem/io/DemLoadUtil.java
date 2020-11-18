package edu.jhuapl.sbmt.dem.io;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemStruct;
import edu.jhuapl.sbmt.dem.KeyValueNode;

import glum.source.LocalSource;
import glum.source.Source;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.Cursor;

/**
 * Collection of utility methods used to load {@link Dem} data files.
 * <p>
 * The following dem formats are supported with the following features:
 * <ul>
 * <li>FITS: Loads and checks for valid headers
 * <li>OBJ: File is not loaded and assumed to be an obj if ends with a valid
 * file extension (.obj).
 * </ul>
 *
 * @author lopeznr1
 */
public class DemLoadUtil
{
	/**
	 * Utility method that will form a {@link Dem} from the contents of the
	 * specified file.
	 * <p>
	 * An attempt will be made to load the file as a fits file, if that fails
	 * then the file will be assumed to be obj file - if the file name has an
	 * .obj file extension.
	 * <p>
	 * On failure an {@link IOException} or {@link FitsException} will be thrown.
	 */
	public static DemStruct formDemFromFile(File aFile) throws Exception
	{
		Exception failExp;

		// Attempt load as fits file
		try
		{
			return formDemFromFitsFile(aFile);
		}
		catch (Exception aExp)
		{
			failExp = aExp;
		}

		// If the file has an .obj extension then assume the file is an obj file
		String fileName = aFile.getName();
		if (fileName.toLowerCase().endsWith(".obj") == true)
		{
			Source tmpSource = new LocalSource(aFile);
			Dem tmpDem = new Dem(tmpSource, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
			return new DemStruct(tmpDem, ImmutableMap.of(), null);
		}

		// Throw the original exception
		throw failExp;
	}

	/**
	 * Utility helper method that will form a {@link Dem} from the contents of
	 * the specified file. The file is assumed to be a FITS file.
	 * <p>
	 * On failure an {@link IOException} or {@link FitsException} will be thrown.
	 */
	private static DemStruct formDemFromFitsFile(File aFile) throws IOException, FitsException
	{
		double lat = Double.NaN;
		double lon = Double.NaN;
		double gsd = Double.NaN;
		double numPix = Double.NaN;

		Map<String, KeyValueNode> tmpKeyValueM = new LinkedHashMap<>();

		// Retrieve the missing header stuff
		try (Fits tmpFits = new Fits(aFile))
		{
			tmpFits.readHDU();

			Header tmpHeader = tmpFits.getHDU(0).getHeader();
			tmpKeyValueM = loadKeyValueMap(tmpHeader);

			lat = tmpHeader.getDoubleValue("CLAT", Double.NaN);
			if (Double.isNaN(lat) == true)
				lat = tmpHeader.getDoubleValue("LATITUDE", Double.NaN);

			lon = tmpHeader.getDoubleValue("CLON", Double.NaN);
			if (Double.isNaN(lon) == true)
				lon = 360.0 - tmpHeader.getDoubleValue("LONGTUDE", Double.NaN);

			gsd = tmpHeader.getDoubleValue("GSD", Double.NaN);
			if (Double.isNaN(gsd) == true)
				gsd = tmpHeader.getDoubleValue("SCALE", Double.NaN);

			numPix = tmpHeader.getDoubleValue("NAXIS1", Double.NaN);
//			if (Double.isNaN(numPix) == true)
//				numPix = (tmpHeader.getDoubleValue("HALFSIZE", Double.NaN) * 2) + 1;
		}

		Source tmpSource = new LocalSource(aFile);
		Dem tmpDem = new Dem(tmpSource, lat, lon, gsd, numPix);
		return new DemStruct(tmpDem, tmpKeyValueM, null);
	}

	/**
	 * Utility method to extract the key-value pairings out of all
	 * {@link HeaderCard}s found in the {@link Header}.
	 * <p>
	 * This method is specific to fits files.
	 */
	public static Map<String, KeyValueNode> loadKeyValueMap(Header aHeader)
	{
		Map<String, KeyValueNode> retKeyValueM = new LinkedHashMap<>();

		Cursor<String, HeaderCard> cursor = aHeader.iterator();
		while (cursor.hasNext())
		{
			HeaderCard tmpHC = cursor.next();
			if (tmpHC.getValue() == null)
				continue;

			KeyValueNode tmpNode = new KeyValueNode(tmpHC.getKey(), tmpHC.getValue(), tmpHC.getComment());
			retKeyValueM.putIfAbsent(tmpHC.getKey(), tmpNode);
		}

		return retKeyValueM;
	}

}
