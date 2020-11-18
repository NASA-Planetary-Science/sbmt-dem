package edu.jhuapl.sbmt.dem;

import java.util.Collection;

/**
 * Interface that defines the callback mechanism for notification of items that
 * have been loaded.
 *
 * @author lopeznr1
 */
public interface LoadListener<G1>
{
	/**
	 * Notification method that the load state of the collection of items has
	 * changed.
	 *
	 * @param aSource The object that generated this event.
	 * @param aItemC The list of items that have been loaded.
	 */
	public void handleLoadEvent(Object aSource, Collection<G1> aItemC);

}
