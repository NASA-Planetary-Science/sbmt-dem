package edu.jhuapl.sbmt.dem;

/**
 * Exception specific to the dem package of SBMT.
 *
 * @author lopeznr1
 */
public class DemException extends RuntimeException
{
	/** Delegate Constructor **/
	public DemException(String aErrorMsg, Throwable aThrowable)
	{
		super(aErrorMsg, aThrowable);
	}

	/** Delegate Constructor **/
	public DemException(String aErrorMsg)
	{
		super(aErrorMsg);
	}

}
