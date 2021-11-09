package edu.jhuapl.sbmt.dem.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import edu.jhuapl.saavtk.color.provider.ColorProvider;
import edu.jhuapl.saavtk.util.Authorizor;
import edu.jhuapl.saavtk.util.Configuration;
import edu.jhuapl.saavtk.view.light.LightCfg;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemCatalog;
import edu.jhuapl.sbmt.dem.DemConfigAttr;
import edu.jhuapl.sbmt.dem.DemStruct;
import edu.jhuapl.sbmt.dem.vtk.DataMode;
import edu.jhuapl.sbmt.dem.vtk.ItemDrawAttr;

import glum.gui.info.WindowCfg;
import glum.io.ParseUtil;
import glum.net.Credential;
import glum.net.UrlUtil;
import glum.source.PlainSource;
import glum.source.Source;
import glum.source.SourceState;
import glum.source.SourceUtil;
import glum.task.SilentTask;
import glum.task.Task;
import glum.util.ThreadUtil;
import glum.util.TimeConst;
import glum.version.PlainVersion;
import glum.version.Version;
import glum.version.VersionUtils;

/**
 * Collection of utility methods used to deserialize/serialize dem catalogs.
 *
 * @author lopeznr1
 */
public class DemCatalogUtil
{
	// Constants
	private static final Version RefVersion = new PlainVersion(2020, 11, 0);

	/**
	 * Returns the file that should be used as the catalog configuration file.
	 */
	public static File getConfigFileCatalog(File aCacheDir, String aTagName)
	{
		String tmpName = aTagName + ".cat.csv";
		return new File(aCacheDir, tmpName);
	}

	/**
	 * Returns the file that should be used as the painter configuration file.
	 */
	public static File getConfigFilePainter(File aCacheDir, String aTagName)
	{
		String tmpName = aTagName + ".pcf.csv";
		return new File(aCacheDir, tmpName);
	}

	/**
	 * Returns the file that should be used as associated painter configuration
	 * file to the provided catalog file.
	 * <p>
	 * The returned file will have the same name as the provided catalog file but
	 * with the extension ".pcf.csv" rather than ".cat.csv".
	 */
	public static File getConfigFilePainter(File aCatalogFile)
	{
		String path = aCatalogFile.getParent();
		String name = aCatalogFile.getName();

		int length = name.length();
		if (name.endsWith(".cat.csv") == true)
			name = name.substring(0, length - 8);
		name += ".pcf.csv";

		return new File(path, name);
	}

	/**
	 * Returns the credentials that should be used to access the sbmt resources.
	 */
	public static Credential getCredential()
	{
		Authorizor tmpAuthorizor = Configuration.getAuthorizor();
		String username = tmpAuthorizor.getUserName();
		char[] password = tmpAuthorizor.getPassword();

		if (username == null || password == null)
			return Credential.NONE;

		Credential retCredential = new Credential(username, password);
		return retCredential;
	}

	/**
	 * Utility method to load the {@link DemCatalog} from the specified
	 * {@link Source}.
	 * <p>
	 * If the {@link Source} is not local and can not be downloaded then an empty
	 * catalog will be returned.
	 * <p>
	 * Returns the {@link DemCatalog} that was loaded.
	 */
	public static DemCatalog loadCatalog(Task aTask, Source aSource, boolean aIsEditable, File aCacheDir)
	{
		// Ensure the catalog is local (and up to date)
		SourceState tmpSS = SourceUtil.getState(aSource);
		if (tmpSS != SourceState.Local)
		{
			// If no remote source - just return an empty catalog
			if (aSource.getRemoteUrl() == null)
				return new DemCatalog(aSource, ImmutableList.of(), null, aIsEditable, aCacheDir);

			// Retrieve the catalog
			try
			{
				Credential tmpCredential = getCredential();
				SourceUtil.download(aTask, aSource, tmpCredential);
				tmpCredential.dispose();
			}
			catch (Exception aExp)
			{
				aExp.printStackTrace();
				aTask.abort();
			}
		}

		// Bail if aborted
		if (aTask.isAborted() == true)
			return null;

		// Delegate
		DemCatalog retCatalog = loadCatalogFromSource(aTask, aSource, aIsEditable, aCacheDir, null);
		return retCatalog;
	}

	/**
	 * Utility method to load the default catalog.
	 */
	public static DemCatalog loadDefaultCatalog(Task aTask, Source aTocSource)
	{
		File tocDir = aTocSource.getLocalFile();
		File browseCacheDir = new File(tocDir, "browse");

		// Define the location to the catalog
		URL defCatUrl = UrlUtil.resolve(aTocSource.getRemoteUrl().toString(), "browse.cat.csv");
		File defCatFile = new File(tocDir, "browse.cat.csv");
		Source tmpSource = new PlainSource(defCatFile, defCatUrl);

		// Retrieve the file if necessary
		Credential tmpCredential = null;
		try
		{
			// Determine if a refresh is needed
			long fileAge = System.currentTimeMillis() - defCatFile.lastModified();
			boolean isRefreshNeeded = false;
			isRefreshNeeded |= defCatFile.exists() == false;
			isRefreshNeeded |= defCatFile.length() == 0;
			isRefreshNeeded |= fileAge > TimeConst.MS_IN_DAY * 3;
			if (isRefreshNeeded == true)
			{
				aTask.logRegln("Fetching catalog: " + defCatUrl);
				tmpCredential = DemCatalogUtil.getCredential();
				Task tmpTask = new SilentTask();
				SourceUtil.download(tmpTask, tmpSource, tmpCredential);
			}
		}
		catch (Exception aExp)
		{
			// Create an empty file
			try
			{
				defCatFile.createNewFile();
			}
			catch (IOException aExp2)
			{
				aExp2.printStackTrace();
			}

			aTask.logRegln("Failed to download catalog. URL: " + defCatUrl);
		}
		finally
		{
			if (tmpCredential != null)
				tmpCredential.dispose();
		}

		// Bail if the file is empty
		if (defCatFile.length() == 0)
			return null;

		// Load the catalogs
		String defaultBasePath = UrlUtil.resolve(aTocSource.getRemoteUrl().toString(), "browse/").toString();
		boolean isEditable = false;

		DemCatalog retCatalog = loadCatalogFromSource(aTask, tmpSource, isEditable, browseCacheDir, defaultBasePath);
		return retCatalog;
	}

	/**
	 * Utility method to save a catalog (configuration) of available
	 * {@link DemStruct}s.
	 */
	public static void saveCatalogFile(Task aTask, File aFile, Collection<DemStruct> aItemC, String aDisplayName)
	{
		try (BufferedWriter tmpBW = new BufferedWriter(new FileWriter(aFile)))
		{
			// Header
			writeHeader(tmpBW, true, false);

			if (aDisplayName != null)
				tmpBW.write("name," + aDisplayName + "\n\n");

			// Content
			String currBasePath = null;
			for (DemStruct aItem : aItemC)
			{
				Dem tmpDem = aItem.dem;
				Source tmpSource = tmpDem.getSource();

				// Record the basePath if not compatible with currBasePath
				String tmpBasePath = SourceUtil.getBasePath(tmpSource);
				if (tmpBasePath.equals(currBasePath) == false)
				{
					currBasePath = tmpBasePath;
					tmpBW.write("base," + currBasePath + "\n\n");
				}

				// Record the dem
				String relPath = tmpSource.getName();
				tmpBW.write("dem," + relPath + "\n");

				double lat = tmpDem.getLat();
				double lon = tmpDem.getLon();
				double halfSize = tmpDem.getNumPixels();
				double scale = tmpDem.getGsd();
				tmpBW.write("geom," + lat + "," + lon + "," + halfSize + "," + scale + "\n\n");
			}
		}
		catch (IOException aExp)
		{
			aTask.logRegln("Failed to save file: " + aFile);
			aTask.logRegln(ThreadUtil.getStackTraceClassic(aExp));
			return;
		}

		aTask.logRegln("DemCatalog file saved: " + aFile);
		aTask.logRegln("\tItems: " + aItemC.size() + "\n");
	}

	/**
	 * Utility method to save a painter (configuration) file.
	 * <p>
	 * Note configurations that are equal to the default will not be saved.
	 */
	public static void savePainterFile(Task aTask, File aFile, Map<Dem, DemConfigAttr> aConfigM)
	{
		try (BufferedWriter tmpBW = new BufferedWriter(new FileWriter(aFile)))
		{
			// Header
			writeHeader(tmpBW, false, true);

			// Content
			String currBasePath = null;
			for (Dem aItem : aConfigM.keySet())
			{
				Source tmpSource = aItem.getSource();

				// Record the basePath if not compatible with currBasePath
				String tmpBasePath = SourceUtil.getBasePath(tmpSource);
				if (tmpBasePath.equals(currBasePath) == false)
				{
					currBasePath = tmpBasePath;
					tmpBW.write("base," + currBasePath + "\n\n");
				}

				// Record the dem
				String relPath = tmpSource.getName();
				tmpBW.write("dem," + relPath + "\n");

				DemConfigAttr tmpProp = aConfigM.get(aItem);
				String description = tmpProp.getDescription();
				if (description != null)
					tmpBW.write("descr," + description + "\n");

				ItemDrawAttr tmpIDA = tmpProp.getDrawAttr();
				double radialoffset = tmpIDA.getRadialOffset();
				if (radialoffset != 0.0)
					tmpBW.write("offs," + radialoffset + "\n");

				boolean isItemVisible = tmpIDA.getIsIntShown();
				boolean isBndrVisible = tmpIDA.getIsExtShown();
				double opacity = tmpIDA.getOpacity();
				String viewDataModeStr = "";
				DataMode viewDataMode = tmpProp.getViewDataMode();
				if (viewDataMode != null && viewDataMode != DataMode.Plain)
					viewDataModeStr = "," + viewDataMode.getDescrBrief().toLowerCase().substring(0, 3);
				tmpBW.write("rndr," + isItemVisible + "," + isBndrVisible + "," + opacity + viewDataModeStr + "\n");

				WindowCfg tmpWindowCfg = tmpProp.getWindowCfg();
				if (tmpWindowCfg != null)
				{
					boolean isShown = tmpWindowCfg.isShown();
					int posX = tmpWindowCfg.posX();
					int posY = tmpWindowCfg.posY();
					int dimX = tmpWindowCfg.dimX();
					int dimY = tmpWindowCfg.dimY();
					tmpBW.write("win," + isShown + "," + posX + "," + posY + "," + dimX + "," + dimY + "\n");
				}

				tmpBW.write("\n");
			}
		}
		catch (IOException aExp)
		{
			aTask.logRegln("Failed to save file: " + aFile);
			aTask.logRegln(ThreadUtil.getStackTraceClassic(aExp));
			return;
		}

		aTask.logRegln("DemPainter file saved: " + aFile);
		aTask.logRegln("\tItems: " + aConfigM.size() + "\n");
	}

	/**
	 * Utility helper method to load the {@link DemCatalog} from the specified
	 * {@link Source}.
	 */
	private static DemCatalog loadCatalogFromSource(Task aTask, Source aSource, boolean aIsEditable, File aCacheDir,
			String aDefaultBasePath)
	{
		String basePath = aDefaultBasePath;
		String dispName = null;

		// Valid instructions to ignore
		ImmutableSet<String> skipInstrS = ImmutableSet.of("descr", "offs", "rndr");

		// Vars for created dem
		Set<String> pathS = new HashSet<>();
		List<DemStruct> structL = new ArrayList<>();

		// Vars for current dem being constructed
		String targPath = null;

		long diskSize = -1L;

		double lat = Double.NaN;
		double lon = Double.NaN;
		double halfSize = Double.NaN;
		double scale = Double.NaN;

		// Process the file
		File tmpFile = aSource.getLocalFile();
		aTask.logRegln("Loading catalog: " + aSource.getPath());
		try (BufferedReader tmpBR = new BufferedReader(new FileReader(tmpFile)))
		{
			int lineCnt = 0;
			while (true)
			{
				lineCnt++;

				// Bail at EOF
				String tmpLine = tmpBR.readLine();
				if (tmpLine == null)
					break;

				// Skip empty comments / empty lines
				String tmpStr = tmpLine.trim();
				if (tmpStr.isEmpty() == true || tmpStr.startsWith("#") == true)
					continue;

				// Tokenize
				String[] strArr = tmpStr.split(",");
				String tagStr = strArr[0];

				// Read the version
				if (tagStr.equals("ver") == true && strArr.length >= 2)
				{
					Version tmpVersion = PlainVersion.parse(strArr[1]);
					if (tmpVersion == null)
					{
						aTask.logRegln("Unrecognized version. Input: " + strArr[1]);
						aTask.logRegln("Aborting...\n");
						break;
					}
					if (VersionUtils.isAfter(tmpVersion, RefVersion) == true)
						aTask.logRegln("Future version encountered. Some instructions may not be supported. Ver: " + strArr[1]);
					continue;
				}

				// Read the basePath
				if (tagStr.equals("base") == true && strArr.length >= 2)
				{
					basePath = strArr[1];
					continue;
				}

				// Read the dispName
				if (tagStr.equals("name") == true && strArr.length >= 2)
				{
					dispName = strArr[1];
					continue;
				}

				// Read the start of a new dem
				if (tagStr.equals("dem") == true && strArr.length >= 2)
				{
					// Save off the prior dem
					if (targPath != null)
					{
						Source tmpSource = SourceUtil.formSource(aCacheDir, basePath, targPath, diskSize);
						if (pathS.contains(tmpSource.getPath()) == true)
						{
							aTask.logRegln(
									"\t[L:" + lineCnt + "] Skipping previous dem since path has already been specified. Path: "
											+ tmpSource.getPath());
							aTask.logRegln("");
						}
						else
						{
							Dem tmpDem = new Dem(tmpSource, lat, lon, scale, halfSize);
							structL.add(new DemStruct(tmpDem));
						}
					}

					// Start the new dem
					targPath = strArr[1];

					// Clear out (previous) dem vars
					diskSize = -1;

					lat = Double.NaN;
					lon = Double.NaN;
					halfSize = Double.NaN;
					scale = Double.NaN;
					continue;
				}

				// Read the disk stats
				if (tagStr.equals("disk") == true && strArr.length >= 3)
				{
					diskSize = ParseUtil.readLong(strArr[2], -1);
					continue;
				}

				// Read the attributes associated with the Dem
				if (tagStr.equals("geom") == true && strArr.length >= 5)
				{
					lat = ParseUtil.readDouble(strArr[1], Double.NaN);
					lon = ParseUtil.readDouble(strArr[2], Double.NaN);
					halfSize = ParseUtil.readDouble(strArr[3], Double.NaN);
					scale = ParseUtil.readDouble(strArr[4], Double.NaN);
					continue;
				}

				// Skip the relevant instructions
				if (skipInstrS.contains(tagStr) == true)
					continue;

				// Log the unrecognized line
				aTask.logRegln("\t[L:" + lineCnt + "] Skipping unrecognized line: ");
				aTask.logRegln("\t" + tmpLine);
			}

			// Add the last read dem
			if (targPath != null)
			{
				// Save off the prior dem
				Source tmpSource = SourceUtil.formSource(aCacheDir, basePath, targPath, diskSize);
				if (pathS.contains(tmpSource.getPath()) == true)
				{
					aTask.logRegln("\t[L:" + lineCnt
							+ "] Skipping previous dem since path has already been specified. Path: " + tmpSource.getPath());
					aTask.logRegln("");
				}
				else
				{
					Dem tmpDem = new Dem(tmpSource, lat, lon, scale, halfSize);
					structL.add(new DemStruct(tmpDem));
				}
			}
		}
		catch (IOException aExp)
		{
			aExp.printStackTrace();
		}

		DemCatalog retCatalog = new DemCatalog(aSource, structL, dispName, aIsEditable, aCacheDir);
		return retCatalog;
	}

	/**
	 * Utility method to load a dem painter configuration file.
	 * <p>
	 * Returns a mapping of {@link Dem}s to {@link DemConfigAttr}s.
	 * <p>
	 * The provided list of {@link DemStruct}s should be populated with the list
	 * of valid {@link Dem}s. If a {@link DemConfigAttr} is located but does not
	 * correspond to a {@link Dem} from aStructL then that {@link DemConfigAttr}
	 * will be ignored.
	 * <p>
	 * Note if aStoreStructL is populated then it will be used to form a reverse
	 * lookup map of dem.filePath to corresponding Dem. Any Dem that is located
	 * will not be added if there is an entry in this reverse lookup map.
	 */
	public static Map<Dem, DemConfigAttr> loadPainterFromFile(Task aTask, File aFile, List<DemStruct> aStructL)
	{
		// Valid instructions to ignore
		ImmutableSet<String> skipInstrS = ImmutableSet.of("disk", "geom", "name");

		// Vars for created ConfigAttr
		Map<Dem, DemConfigAttr> retConfigM = new HashMap<>();

		// Populate our lookup map of Dem.sourcePath to Dem
		Map<String, Dem> revLookUpM = new HashMap<>();
		for (DemStruct aItem : aStructL)
		{
			Dem tmpDem = aItem.dem;
			String tmpPath = tmpDem.getSource().getPath();
			revLookUpM.put(tmpPath, tmpDem);
		}

		String basePath = null;

		// Vars for current dem being constructed
		String targPath = null;

		String description = null;
		ColorProvider extCP = ColorProvider.Invalid;
		ColorProvider intCP = ColorProvider.Invalid;
		boolean extIsShown = false;
		boolean intIsShown = false;
		double opacity = 1.0;
		double radialOffset = 0.0;
		DataMode viewDataMode = DataMode.Valid;
		WindowCfg analyzeWC = null;

		// Synthesize the map of Dem.soure.path to Dem
		Map<String, Dem> pathToDemM1 = new HashMap<>();
		Map<String, Dem> pathToDemM2 = new HashMap<>();
		for (DemStruct aItem : aStructL)
		{
			pathToDemM1.put(aItem.dem.getSource().getPath(), aItem.dem);
			pathToDemM2.put(aItem.dem.getSource().getName(), aItem.dem);
		}

		// Process the file
		aTask.logRegln("Loading dem config file: " + aFile);
		try (BufferedReader tmpBR = new BufferedReader(new FileReader(aFile)))
		{
			int lineCnt = 0;
			while (true)
			{
				lineCnt++;

				// Bail at EOF
				String tmpLine = tmpBR.readLine();
				if (tmpLine == null)
					break;

				// Skip empty comments / empty lines
				String tmpStr = tmpLine.trim();
				if (tmpStr.isEmpty() == true || tmpStr.startsWith("#") == true)
					continue;

				// Tokenize
				String[] strArr = tmpStr.split(",");
				String tagStr = strArr[0];

				// Read the version
				if (tagStr.equals("ver") == true && strArr.length >= 2)
				{
					Version tmpVersion = PlainVersion.parse(strArr[1]);
					if (tmpVersion == null)
					{
						aTask.logRegln("Unrecognized version. Input: " + strArr[1]);
						aTask.logRegln("Aborting...\n");
						break;
					}
					if (VersionUtils.isAfter(tmpVersion, RefVersion) == true)
						aTask.logRegln("Future version encountered. Some instructions may not be supported. Ver: " + strArr[1]);
					continue;
				}

				// Read the basePath
				if (tagStr.equals("base") == true && strArr.length >= 2)
				{
					basePath = strArr[1];
					continue;
				}

				// Read the start of a new dem
				if (tagStr.equals("dem") == true && strArr.length >= 2)
				{
					// Save off the prior DemConfigAttr
					if (targPath != null)
					{
						boolean isSyncColoring = false;
						boolean isSyncLighting = true;
						LightCfg renderLC = LightCfg.Default;
						ItemDrawAttr tmpIDA = new ItemDrawAttr(extCP, extIsShown, intCP, intIsShown, opacity, radialOffset);
						DemConfigAttr tmpDCA = new DemConfigAttr(-1, description, tmpIDA, isSyncColoring, isSyncLighting,
								renderLC, viewDataMode, analyzeWC);

						storeConfig(aTask, lineCnt, basePath, targPath, pathToDemM1, pathToDemM2, retConfigM, tmpDCA);
					}

					// Start the new dem
					targPath = strArr[1];

					// Clear out (previous) dem vars
					description = null;
					extCP = ColorProvider.Invalid;
					intCP = ColorProvider.Invalid;
					extIsShown = false;
					intIsShown = false;
					opacity = 1.0;
					radialOffset = 0.0;
					viewDataMode = DataMode.Valid;
					analyzeWC = null;

					continue;
				}

				// Read the description
				if (tagStr.equals("descr") == true && strArr.length >= 2)
				{
					description = tmpStr.substring(6);
					continue;
				}

				// Read the description
				if (tagStr.equals("offs") == true && strArr.length >= 2)
				{
					radialOffset = ParseUtil.readDouble(strArr[1], 0.0);
					continue;
				}

				// Read the render props associated with the Dem
				if (tagStr.equals("rndr") == true && strArr.length >= 4)
				{
					intIsShown = ParseUtil.readBoolean(strArr[1], false);
					extIsShown = ParseUtil.readBoolean(strArr[2], false);
					opacity = ParseUtil.readDouble(strArr[3], 1.0);

					if (strArr.length >= 5)
					{
						String viewModeStr = strArr[4].toLowerCase();
						if (viewModeStr.equals("reg") == true || viewModeStr.equals("true") == true)
							viewDataMode = DataMode.Regular;
						else if (viewModeStr.equals("val") == true || viewModeStr.equals("false") == true)
							viewDataMode = DataMode.Valid;
					}
					continue;
				}

				// Read the window config
				if (tagStr.equals("win") == true && strArr.length >= 6)
				{
					boolean isShown = ParseUtil.readBoolean(strArr[1], false);
					int posX = ParseUtil.readInt(strArr[2], 0);
					int posY = ParseUtil.readInt(strArr[3], 0);
					int dimX = ParseUtil.readInt(strArr[4], 800);
					int dimY = ParseUtil.readInt(strArr[5], 450);
					analyzeWC = new WindowCfg(isShown, posX, posY, dimX, dimY);
					continue;
				}

				// Skip the relevant instructions
				if (skipInstrS.contains(tagStr) == true)
					continue;

				// Log the unrecognized line
				aTask.logRegln("\t[L:" + lineCnt + "] Skipping unrecognized line: ");
				aTask.logRegln("\t" + tmpLine);
			}

			// Add the last DemConfigAttr
			if (targPath != null)
			{
				boolean isSyncColoring = false;
				boolean isSyncLighting = true;
				LightCfg renderLC = LightCfg.Default;
				ItemDrawAttr tmpIDA = new ItemDrawAttr(extCP, extIsShown, intCP, intIsShown, opacity, radialOffset);
				DemConfigAttr tmpDCA = new DemConfigAttr(-1, description, tmpIDA, isSyncColoring, isSyncLighting, renderLC,
						viewDataMode, analyzeWC);

				storeConfig(aTask, lineCnt, basePath, targPath, pathToDemM1, pathToDemM2, retConfigM, tmpDCA);
			}
		}
		catch (IOException aExp)
		{
			aExp.printStackTrace();
		}

		return retConfigM;
	}

	/**
	 * Utility helper method to associate the provided {@link DemConfigAttr} with
	 * the appropriate {@link Dem}.
	 * <p>
	 * On failure to locate an association a message will be logged to the
	 * provided {@link Task}.
	 */
	private static void storeConfig(Task aTask, int aLineCnt, String aBasePath, String aTargPath,
			Map<String, Dem> aPathToDemM1, Map<String, Dem> aPathToDemM2, Map<Dem, DemConfigAttr> aRetConfigM,
			DemConfigAttr aTmpDCA) throws MalformedURLException
	{
		Source tmpSource = SourceUtil.formSource(null, aBasePath, aTargPath, -1);

		String tmpPath = tmpSource.getPath();
		Dem tmpDem = aPathToDemM1.get(tmpPath);
		String tmpName = tmpSource.getName();
		if (tmpDem == null)
			tmpDem = aPathToDemM2.get(tmpName);

		// Bail if we failed to locate the proper DEM
		if (tmpDem == null)
		{
			String tmpStr = "[L:" + aLineCnt + "]: Skipping prior configuration! Path does not map to a dem.";
			tmpStr += "Path: " + tmpPath;
			aTask.logRegln(tmpStr);
			return;
		}

		// Store the configuration
		aRetConfigM.put(tmpDem, aTmpDCA);
	}

	/**
	 * Utility helper method that will write out the (appropriate) header for DEM
	 * configuration files.
	 */
	private static void writeHeader(BufferedWriter aBW, boolean aIsCatalog, boolean aIsPainter) throws IOException
	{
		boolean alwaysTrue = true;

		// If neither catalog or painter then assume both
		if (aIsCatalog == false && aIsPainter == false)
			aIsCatalog = aIsPainter = true;

		// Form the string to use to define the configuration type
		String cfgType;
		if (aIsCatalog == true && aIsPainter == true)
			cfgType = "Catalog / Painter";
		else if (aIsCatalog == true)
			cfgType = "Catalog";
		else // if (isPainter == true)
			cfgType = "Painter";

		writeLine(aBW, alwaysTrue, "# DEM " + cfgType + " File");
		writeLine(aBW, alwaysTrue, "# ------------------------------------------------------------------------------");
		writeLine(aBW, alwaysTrue, "# File consists of a list of instructions.");
		writeLine(aBW, alwaysTrue, "# <aInstr>,<...>*");
		writeLine(aBW, alwaysTrue, "# where <aInstr> can be one of the following:");
		writeLine(aBW, alwaysTrue, "#   ver:       Specifies the version of this DEM configuration file.");
		writeLine(aBW, alwaysTrue, "#");
		writeLine(aBW, alwaysTrue, "#   base:      Specifies the base path for any DEMs after this instruction.");
		writeLine(aBW, alwaysTrue, "#");
		writeLine(aBW, aIsCatalog, "#   name:      Defines a name used to reference this catalog. This name will");
		writeLine(aBW, aIsCatalog, "#              typically be referenced in various user interface (UI) elements.");
		writeLine(aBW, aIsCatalog, "#");
		writeLine(aBW, alwaysTrue, "#   dem:       Specifies the start of a new DEM. The DEM's definition follows");
		writeLine(aBW, alwaysTrue, "#              until the next (dem) instruction.");
		writeLine(aBW, alwaysTrue, "#");
		writeLine(aBW, aIsPainter, "#   descr:     Defines a description of the DEM. The description should be an");
		writeLine(aBW, aIsPainter, "#              informative string about the DEM. If the description will just be");
		writeLine(aBW, aIsPainter, "#              the file name, then there is no need to specify this instruction.");
		writeLine(aBW, aIsPainter, "#");
		writeLine(aBW, aIsCatalog, "#   disk:      Defines attributes of the dem (file) resource.");
		writeLine(aBW, aIsCatalog, "#");
		writeLine(aBW, aIsCatalog, "#   geom:      Defines the geometry of the DEM. If this is specified multiple");
		writeLine(aBW, aIsCatalog, "#              times then only the last (geom) instruction will have effect.");
		writeLine(aBW, aIsCatalog, "#");
		writeLine(aBW, aIsPainter, "#   offs:      Defines an offset applied to the DEM.");
		writeLine(aBW, aIsPainter, "#");
		writeLine(aBW, aIsPainter, "#   rndr:      Configures how the DEM will be rendered.");
		writeLine(aBW, aIsPainter, "#");
		writeLine(aBW, alwaysTrue, "#");
		writeLine(aBW, alwaysTrue, "# Listed below are the available instructions and details on the associated");
		writeLine(aBW, alwaysTrue, "# parameters:");
		writeLine(aBW, alwaysTrue, "#   ver,<aVerStr>");
		writeLine(aBW, alwaysTrue, "#      aVerStr:   The version of the file. The expected format is: yyyy.mm");
		writeLine(aBW, alwaysTrue, "#                 The current supported version is: " + RefVersion);
		writeLine(aBW, alwaysTrue, "#");
		writeLine(aBW, alwaysTrue, "#   base,<aPath>");
		writeLine(aBW, alwaysTrue, "#      aPath:     Defines the base path for any future DEMs. Future paths will");
		writeLine(aBW, alwaysTrue, "#                 will be interpreted as relative to this base path.");
		writeLine(aBW, alwaysTrue, "#");
		writeLine(aBW, alwaysTrue, "#   dem,<aPath>");
		writeLine(aBW, alwaysTrue, "#      aPath:     The path to the DEM resource. If the base path was specified");
		writeLine(aBW, alwaysTrue, "#                 then this path will be treated as relative rather than");
		writeLine(aBW, alwaysTrue, "#                 fully-qualified.");
		writeLine(aBW, alwaysTrue, "#");
		writeLine(aBW, aIsPainter, "#   descr,<aDescr>");
		writeLine(aBW, aIsPainter, "#      aDescr:    A description (or display name) associated with the DEM.");
		writeLine(aBW, aIsPainter, "#");
		writeLine(aBW, aIsCatalog, "#   disk,<unused>,<aSize>");
		writeLine(aBW, aIsCatalog, "#      unused:    Value is unused.");
		writeLine(aBW, aIsCatalog, "#      aSize:     The size of the resource in bytes.");
		writeLine(aBW, aIsCatalog, "#");
		writeLine(aBW, aIsCatalog, "#   geom,<aLat>,<aLon>,<aHalfSize>,<aScale>");
		writeLine(aBW, aIsCatalog, "#      aLat:      Center latitude in degrees.");
		writeLine(aBW, aIsCatalog, "#      aLon:      Center longitude in degrees.");
		writeLine(aBW, aIsCatalog, "#      aHalfSize: HalfSize in pixels.");
		writeLine(aBW, aIsCatalog, "#      aScale:    Scale in meters per pixel.");
		writeLine(aBW, aIsCatalog, "#");
		writeLine(aBW, aIsPainter, "#   offs,<aOffset>");
		writeLine(aBW, aIsPainter, "#      aOffset:   A value to translate the dem along it's normal. Values");
		writeLine(aBW, aIsPainter, "#                 should be integral and in the range: [-100, 100]");
		writeLine(aBW, aIsPainter, "#");
		writeLine(aBW, aIsPainter, "#   rndr,<aShowItem>,<aShowBndr>,<aOpacity>,<aViewDataMode>");
		writeLine(aBW, aIsPainter, "#      aShowItem: Boolean that defines if the DEM should be shown.");
		writeLine(aBW, aIsPainter, "#      aShowBndr: Boolean that defines if the DEM's boundary should be shown.");
		writeLine(aBW, aIsPainter, "#      aOpacity:  Value that defines the opacity of the DEM. Range: [0.0 - 1.0]");
		writeLine(aBW, aIsPainter, "#      aViewDataMode: Value that defines the view mode of the DEM. Options are:");
		writeLine(aBW, aIsPainter, "#                     reg: View valid and invalid data.");
		writeLine(aBW, aIsPainter, "#                     val: View only valid data.");
		writeLine(aBW, aIsPainter, "#");
		writeLine(aBW, alwaysTrue, "#");
		aBW.write("ver," + RefVersion + "\n\n");
	}

	/**
	 * Helper method that optionally writes out the specified line.
	 *
	 * @param aBool If true then aMsg will be output to the
	 * {@link BufferedWriter}.
	 */
	private static void writeLine(BufferedWriter aBW, boolean aBool, String aMsg) throws IOException
	{
		if (aBool == false)
			return;

		aBW.write(aMsg + "\n");
	}

}
