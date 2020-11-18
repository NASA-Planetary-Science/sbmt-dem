package edu.jhuapl.sbmt.dem.io.legacy;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemConfigAttr;
import edu.jhuapl.sbmt.dem.DemStruct;
import edu.jhuapl.sbmt.dem.io.DemCatalogUtil;

import glum.task.ConsoleTask;
import glum.task.Task;

/**
 * Collection of utility methods for working with legacy "{@link DemMetadata}"
 * catalogs.
 *
 * @author lopeznr1
 */
public class LegacyUtil
{
	// Constants
	private static final String FileNameCatalogLegacy = "demConfig.txt";

	/**
	 * Utility method to transform a legacy {@link DemMetadata} catalog to a csv
	 * catalog.
	 * <p>
	 * This transformation will only be executed if the following is true:
	 * <ul>
	 * <li>The custom cache directory does NOT exist
	 * <li>The legacy catalog file does exist
	 * </ul>
	 */
	public static void migrateLegacyMetaCatalog(File aLegacyCustomDir, File aCustomCacheDir, String aTagName)
	{
		// Bail if the (modern) cache directory exists
		if (aCustomCacheDir.exists() == true)
			return;

		// Bail if the legacy catalog does not exist
		File legacyCatalogFile = new File(aLegacyCustomDir, FileNameCatalogLegacy);
		if (legacyCatalogFile.isFile() == false)
			return;

		// Load the legacy catalog
		Task tmpTask = new ConsoleTask();
		tmpTask.logRegln("Loading legacy dem configration file: " + legacyCatalogFile + "\n");
		DemMetadata tmpDemMetaData = new DemMetadata();

		List<DemStruct> tmpStoreStructL = new ArrayList<>();
		Map<Dem, DemConfigAttr> tmpStoreConfigM = new LinkedHashMap<>();
		tmpDemMetaData.loadLegacyConfig(legacyCatalogFile, tmpStoreStructL, tmpStoreConfigM);

		// Migrate to the new format
		File tocDir = aCustomCacheDir.getParentFile();
		File catalogFile = DemCatalogUtil.getConfigFileCatalog(tocDir, aTagName);
		File painterFile = DemCatalogUtil.getConfigFilePainter(tocDir, aTagName);

		tmpTask.logRegln("Migrating DEM configuration files...");
		aCustomCacheDir.mkdirs();
		DemCatalogUtil.saveCatalogFile(tmpTask, catalogFile, tmpStoreStructL, null);
		DemCatalogUtil.savePainterFile(tmpTask, painterFile, tmpStoreConfigM);
	}

}
