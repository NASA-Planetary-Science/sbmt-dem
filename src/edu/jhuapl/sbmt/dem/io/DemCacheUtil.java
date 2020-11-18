package edu.jhuapl.sbmt.dem.io;

import java.io.File;
import java.io.IOException;

import com.google.common.io.Files;

import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemStruct;

import glum.io.IoUtil;
import glum.source.LocalSource;
import glum.source.Source;
import glum.task.Task;
import glum.unit.TimeCountUnit;
import glum.util.WallTimer;

/**
 * Collection of utility methods that provide access to the cache corresponding
 * to {@link Dem}s.
 *
 * @author lopeznr1
 */
public class DemCacheUtil
{
	/**
	 * Utility method that copies the specified file to the cache folder and
	 * returns the corresponding {@link Dem}.
	 * <p>
	 * Returns null on failure.
	 * <p>
	 * Logging is provided via the {@link Task}.
	 */
	public static DemStruct copyToCacheAndLoad(Task aTask, File aCacheDir, File aSrcFile)
	{
		TimeCountUnit timeU = new TimeCountUnit(2);
		WallTimer tmpWT = new WallTimer(true);

		try
		{
			// Ensure we have a valid file
			if (aSrcFile.exists() == false)
				throw new IOException("File does not exist.");
			else if (aSrcFile.isFile() == false)
				throw new IOException("Path is not a valid file.");

			// Instantiate the Dem from the provided (FITS) file
			DemStruct retStruct = DemLoadUtil.formDemFromFile(aSrcFile);

			// Locate an available location in the cache directory
			// and copy the file to the cache
			File destFile = new File(aCacheDir, aSrcFile.getName());
			destFile = IoUtil.locateNextAvailableFile(destFile);
			Files.copy(aSrcFile, destFile);

			// Synthesize a Dem to reflect the cached source
			Source tmpSource = new LocalSource(destFile);
			Dem tmpDem = retStruct.dem;
			tmpDem = new Dem(tmpSource, tmpDem.getLat(), tmpDem.getLon(), tmpDem.getGsd(), tmpDem.getNumPixels());

			// Determine if this is a educated get rather than a validated dem
			// file. Currently parsing of obj files is simple due to the lack of a
			// proper parse logic but rather utilization of VTK's obj parse logic.
			boolean isLoadGuess = Double.isNaN(tmpDem.getLat()) == true;

			aTask.logRegln("[Pass] Added file: " + aSrcFile + "   (" + timeU.getString(tmpWT) + ")");
			if (isLoadGuess == true)
				aTask.logRegln("\tNote that the file content has not been validated. Assuming obj file.");
			retStruct = new DemStruct(tmpDem, retStruct.keyValueM, retStruct.coordinateSystem);
			return retStruct;
		}
		catch (Exception aExp)
		{
			aTask.logRegln("[Fail] Skipped file: " + aSrcFile);
			aTask.logRegln("\tReason: " + aExp.getMessage());
			return null;
		}
	}

	/**
	 * Utility method that will remove the file corresponding to the specified
	 * {@link Dem} from the cache folder.
	 * <p>
	 * This method should be called when the {@link Dem} will no longer be
	 * needed. The backing DEM file will be removed.
	 */
	public static void removeContentFor(Task aTask, Dem aItem)
	{
		// Bail if the backing cache file does not exist
		File tmpFile = aItem.getSource().getLocalFile();
		if (tmpFile == null || tmpFile.exists() == false)
			return;

		// Remove the file
		aTask.logRegln("Removing cache file: " + tmpFile);
		tmpFile.delete();
	}

}
