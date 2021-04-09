package edu.jhuapl.sbmt.dem.gui.analyze.control;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.google.common.collect.ImmutableList;

import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemManager;

import glum.gui.GuiPaneUtil;
import glum.gui.GuiUtil;
import glum.item.ItemEventListener;
import glum.item.ItemEventType;
import net.miginfocom.swing.MigLayout;

/**
 * Simple panel that provides for configuration of a single {@link Dem}'s show
 * and synchronize (to main body) state.
 *
 * @author lopeznr1
 */
public class ShowAndSyncPanel extends JPanel implements ActionListener, ItemEventListener
{
	// Reference vars
	private final DemManager refManager;
	private final Dem refDem;

	// Gui vars
	private final JCheckBox showOnMainWinCB;
	private final JCheckBox syncColoringCB;

	/** Standard Constructor */
	public ShowAndSyncPanel(DemManager aManager, Dem aDem)
	{
		refManager = aManager;
		refDem = aDem;

		setLayout(new MigLayout("", "0[]0", "0[][]0"));

		// Dem main area
		showOnMainWinCB = GuiUtil.createJCheckBox("Show on body", this);
		add(showOnMainWinCB, "wrap");

		syncColoringCB = GuiUtil.createJCheckBox("Sync coloring to...", this);
		syncColoringCB.setToolTipText("Sync coloring to body");
		add(syncColoringCB, "");

		// Register for events of interest
		refManager.addListener(this);

		updateGui();
	}

	/** Manual destruction */
	public void dispose()
	{
		refManager.delListener(this);
	}

	@Override
	public void handleItemEvent(Object aSource, ItemEventType aEventType)
	{
		updateGui();
	}

	@Override
	public void actionPerformed(ActionEvent aEvent)
	{
		Object source = aEvent.getSource();
		if (source == showOnMainWinCB)
			refManager.setIsVisibleInterior(ImmutableList.of(refDem), showOnMainWinCB.isSelected());
		else if (source == syncColoringCB)
			doActionSyncColoring();

		updateGui();
	}

	/**
	 * Helper method to handle the action synchronize coloring.
	 */
	private void doActionSyncColoring()
	{
		try
		{
			refManager.setIsSyncColoring(ImmutableList.of(refDem), syncColoringCB.isSelected());
		}
		catch (Throwable aExp)
		{
			String infoMsg = "An error occurred synchronizing primary dem surface coloring to the source dem surface.\n";
			GuiPaneUtil.showFailMessage(this, "Synchronize Dem Colorization Error", infoMsg, aExp);
		}
	}

	/**
	 * Helper method that updates the various UI elements to keep them
	 * synchronized.
	 */
	private void updateGui()
	{
		boolean isEnabled, isSelected;

		isSelected = refManager.getIsVisibleInterior(refDem);
		showOnMainWinCB.setSelected(isSelected);

		isEnabled = showOnMainWinCB.isSelected() == true;
		syncColoringCB.setEnabled(isEnabled);

		isSelected = refManager.getIsSyncWithMain(refDem);
		syncColoringCB.setSelected(isSelected);
	}

}
