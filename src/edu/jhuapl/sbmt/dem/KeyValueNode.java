package edu.jhuapl.sbmt.dem;

/**
 * Immutable object that tracks an individual key-value (with comments) node.
 *
 * @author lopeznr1
 */
public class KeyValueNode
{
	// Attributes
	private final String key;
	private final String value;
	private final String comment;

	/** Standard Constructor */
	public KeyValueNode(String aKey, String aValue, String aComment)
	{
		key = aKey;
		value = aValue;
		comment = aComment;
	}

	public String getKey()
	{
		return key;
	}

	public String getValue()
	{
		return value;
	}

	public String getComment()
	{
		return comment;
	}

}
