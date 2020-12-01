package edu.jhuapl.sbmt.dem.vtk;

/**
 * Enum which defines the mode flags associated with the data.
 *
 * @author lopeznr1
 */
public enum DataMode
{
	/** Defines the mode where there is no concept of invalid / valid data. */
	Plain("Pla.", "All data is considered valid."),

	/** Defines the mode where both valid and invalid data will be utilized. */
	Regular("Reg.", "View valid and invalid data."),

	/** Defines the mode where only valid data will be utilized. */
	Valid("Val.", "View only valid data.");

	// Attributes
	private final String descrBrief;
	private final String descrFull;

	/** Private Constructor. */
	private DataMode(String aDescrBrief, String aDescrFull)
	{
		descrBrief = aDescrBrief;
		descrFull = aDescrFull;
	}

	public String getDescrBrief()
	{
		return descrBrief;
	}

	public String getDescrFull()
	{
		return descrFull;
	}

}
