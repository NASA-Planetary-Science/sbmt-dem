package edu.jhuapl.sbmt.dem.legacy;


import crucible.crust.metadata.api.Key;
import crucible.crust.metadata.api.Metadata;
import crucible.crust.metadata.api.StorableAsMetadata;
import crucible.crust.metadata.api.Version;
import crucible.crust.metadata.impl.InstanceGetter;
import crucible.crust.metadata.impl.SettableMetadata;

public class DEMKey implements StorableAsMetadata<DEMKey>
{
    public String name = ""; // name to call this image for display purposes
    public String demfilename = ""; // filename of image on disk
    public boolean viewBadData = false;

    public DEMKey(String fileName, String displayName, boolean viewBadData)
    {
        this.demfilename = fileName;
        this.name = displayName;
        this.viewBadData = viewBadData;
    }

    // Copy constructor
    public DEMKey(DEMKey copyKey)
    {
        demfilename = copyKey.demfilename;
        this.name = copyKey.name;
        this.viewBadData = viewBadData;
    }

    @Override
    public String toString()
    {
        return name + " (" + demfilename + ")";
    }

    @Override
    public boolean equals(Object obj)
    {
        return demfilename.equals(((DEMKey)obj).demfilename);
    }

    @Override
    public int hashCode()
    {
        return demfilename.hashCode();
    }

    private static final Key<String> nameKey = Key.of("name");
    private static final Key<String> demfilenameKey = Key.of("demfilename");
    private static final Key<Boolean> viewBadDataKey = Key.of("viewBadData");
    private static final Key<DEMKey> DEM_KEY = Key.of("demKey");

	@Override
	public Metadata store()
	{
		SettableMetadata configMetadata = SettableMetadata.of(Version.of(1, 0));
		write(nameKey, name, configMetadata);
		write(demfilenameKey, demfilename, configMetadata);
		write(viewBadDataKey, viewBadData, configMetadata);
		return configMetadata;
	}

	public static void initializeSerializationProxy()
	{
		InstanceGetter.defaultInstanceGetter().register(DEM_KEY, (metadata) -> {

	        String name = metadata.get(nameKey);
	        String demfilename = metadata.get(demfilenameKey);
	        Boolean viewBadData = false;
	        if (metadata.hasKey(viewBadDataKey))
	        	viewBadData = metadata.get(viewBadDataKey);
			return new DEMKey(demfilename, name, viewBadData);
		});
	}

	public static DEMKey retrieveOldFormat(Metadata metadata)
	{
		 Key<String> nameKey = Key.of("name");
		 Key<String> demFileNameKey = Key.of("demfilename");
		 DEMKey key = new DEMKey(metadata.get(demFileNameKey), metadata.get(nameKey), false);
		 return key;
	}

	@Override
	public Key<DEMKey> getKey()
	{
		return DEM_KEY;
	}

	protected <T> void write(Key<T> key, T value, SettableMetadata configMetadata)
    {
        if (value != null)
        {
            configMetadata.put(key, value);
        }
    }

    protected <T> T read(Key<T> key, Metadata configMetadata)
    {
        T value = configMetadata.get(key);
        if (value != null)
            return value;
        return null;
    }
}
