package edu.jhuapl.sbmt.dem.io.legacy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.jhuapl.saavtk.view.light.LightCfg;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemConfigAttr;
import edu.jhuapl.sbmt.dem.DemStruct;
import edu.jhuapl.sbmt.dem.io.DemLoadUtil;
import edu.jhuapl.sbmt.dem.legacy.DEMKey;
import edu.jhuapl.sbmt.dem.vtk.DataMode;
import edu.jhuapl.sbmt.dem.vtk.ItemDrawAttr;

import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Metadata;
import edu.jhuapl.ses.jsqrl.api.MetadataManager;
import edu.jhuapl.ses.jsqrl.api.Version;
import edu.jhuapl.ses.jsqrl.impl.FixedMetadata;
import edu.jhuapl.ses.jsqrl.impl.SettableMetadata;
import edu.jhuapl.ses.jsqrl.impl.gson.Serializers;

/**
 * Logic for this bridge class was taken from:
 * edu.jhuapl.sbmt.dtm.model.creation.DtmCreationModel
 * <p>
 * The sole purpose of this class is to support the transition away from the DEM
 * metadata serialization framework.
 *
 * @author lopeznr1
 */
public class DemMetadata implements MetadataManager
{
	Key<List<DEMKey>> demKeysKey = Key.of("demKeys");
	private List<DEMKey> infoL;

	public DemMetadata()
	{
		infoL = new ArrayList<>();
	}

	public void loadLegacyConfig(File aFile, List<DemStruct> aStoreStructL, Map<Dem, DemConfigAttr> aStoreConfigM)
	{
		// Bail if the file does not exist
		if (aFile.exists() == false)
			return;

		try
		{
			FixedMetadata metadata = Serializers.deserialize(aFile, "CustomDEMs");
			retrieve(metadata);
		}
		catch (IOException aExp)
		{
			aExp.printStackTrace();
		}

		for (DEMKey aKey : infoL)
		{
			// Load the Dem
			Dem tmpDem;
			try
			{
				// Correct defective render prop
				String filePath = aKey.demfilename;
				if (filePath.startsWith("file://") == true)
					filePath = filePath.substring(7);

				File tmpFile = new File(filePath);
				tmpDem = DemLoadUtil.formDemFromFile(tmpFile).dem;

				DemStruct tmpStruct = new DemStruct(tmpDem);
				aStoreStructL.add(tmpStruct);
			}
			catch (Exception aExp)
			{
				aExp.printStackTrace();
				continue;
			}

			// Synthesize the DemConfigAttr
			DataMode tmpDataMode = DataMode.Regular;
			if (aKey.viewBadData == false)
				tmpDataMode = DataMode.Valid;
			DemConfigAttr tmpDCA = new DemConfigAttr(-1, aKey.name, ItemDrawAttr.Default, false, true, LightCfg.Default,
					tmpDataMode, null);
			aStoreConfigM.put(tmpDem, tmpDCA);
		}
	}

	@Override
	public void retrieve(Metadata source)
	{
		try
		{
			infoL = source.get(demKeysKey);
		}
		catch (ClassCastException | IllegalArgumentException ex)
		{
			Key<Metadata[]> oldCustomDEMKey = Key.of("demInfos");
			Metadata[] oldCustomItemArr = source.get(oldCustomDEMKey);
			List<DEMKey> migratedItemL = new ArrayList<DEMKey>();
			for (Metadata meta : oldCustomItemArr)
				migratedItemL.add(DEMKey.retrieveOldFormat(meta));

			infoL = migratedItemL;
		}
	}

	@Override
	public Metadata store()
	{
		SettableMetadata result = SettableMetadata.of(Version.of(1, 0));
		result.put(demKeysKey, infoL);
		return result;
	}

}
