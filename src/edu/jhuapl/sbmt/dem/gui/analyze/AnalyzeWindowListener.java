package edu.jhuapl.sbmt.dem.gui.analyze;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import com.google.common.collect.ImmutableList;

import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemManager;

/**
 * Listener responsible for keeping the DemManager's view state (shown vs not
 * shown) of the window associated with the {@link AnalyzePanel} synchronized.
 *
 * @author lopeznr1
 */
public class AnalyzeWindowListener implements WindowListener
{
	// Ref vars
	private final DemManager refManager;
	private final Dem refDem;

	/** Standard Constructor */
	public AnalyzeWindowListener(DemManager aManager, Dem aDem)
	{
		refManager = aManager;
		refDem = aDem;
	}

	@Override
	public void windowOpened(WindowEvent aEvent)
	{
		; // Nothing to do
	}

	@Override
	public void windowClosing(WindowEvent aEvent)
	{
		refManager.setIsDemAnalyzed(ImmutableList.of(refDem), false);
	}

	@Override
	public void windowClosed(WindowEvent aEvent)
	{
		; // Nothing to do
	}

	@Override
	public void windowIconified(WindowEvent aEvent)
	{
		; // Nothing to do
	}

	@Override
	public void windowDeiconified(WindowEvent aEvent)
	{
		; // Nothing to do
	}

	@Override
	public void windowActivated(WindowEvent aEvent)
	{
		; // Nothing to do
	}

	@Override
	public void windowDeactivated(WindowEvent aEvent)
	{
		; // Nothing to do
	}

}
