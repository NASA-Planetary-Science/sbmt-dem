package edu.jhuapl.sbmt.dem;

import java.io.File;
import java.util.List;

import com.google.common.collect.ImmutableList;

import glum.source.Source;

/**
 * Immutable object that defines a "catalog" of {@link Dem}s.
 *
 * @author lopeznr1
 */
public class DemCatalog
{
	// Constants
	/** Catalog that defines the "invalid" catalog. */
	public static final DemCatalog Invalid = new DemCatalog(null, ImmutableList.of(), null, false, null);

	// Attributes
	private final Source source;

	private final ImmutableList<DemStruct> structL;
	private final String displayName;
	private final boolean isEditable;

	private final File cacheDir;

	/** Standard Constructor */
	public DemCatalog(Source aSource, List<DemStruct> aStructL, String aDisplayName, boolean aIsEditable, File aCacheDir)
	{
		source = aSource;

		structL = ImmutableList.copyOf(aStructL);
		displayName = aDisplayName;
		isEditable = aIsEditable;

		cacheDir = aCacheDir;
	}

	/**
	 * Returns the folder used to cache items associated with this
	 * {@link #DemCatalog}.
	 * <p>
	 * The returned location should be viewed as a preferred location rather than
	 * a hard requirement - since any of the associated {@link Dem} are not
	 * required to use this cache location.
	 */
	public File getCacheDir()
	{
		return cacheDir;
	}

	/**
	 * Returns the user friendly name of this catalog.
	 *
	 * @return
	 */
	public String getDisplayName()
	{
		return displayName;
	}

	/**
	 * Returns true if this catalog is editable.
	 * <p>
	 * A catalog is considered editable if the (backing) list of DEMs can be
	 * changed (added or removed).
	 */
	public boolean getIsEditable()
	{
		return isEditable;
	}

	/**
	 * Returns the {@link Source} of this {@link DemCatalog}.
	 */
	public Source getSource()
	{
		return source;
	}

	/**
	 * Returns the list of {@link DemStruct}s associated with this
	 * {@link DemCatalog}.
	 */
	public ImmutableList<DemStruct> getStructs()
	{
		return structL;
	}

}
