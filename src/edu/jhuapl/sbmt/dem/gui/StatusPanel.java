package edu.jhuapl.sbmt.dem.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.google.common.collect.ImmutableList;

import edu.jhuapl.saavtk.gui.util.ToolTipUtil;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemManager;
import edu.jhuapl.sbmt.dem.vtk.VtkDemPainter;

import glum.gui.GuiUtil;
import glum.gui.panel.generic.MessagePanel;
import glum.source.Source;
import glum.source.SourceState;
import glum.source.SourceUtil;
import glum.unit.ByteUnit;
import net.miginfocom.swing.MigLayout;

/**
 * Panel used to display the "cache" status associated with a {@link Dem}.
 *
 * @author lopeznr1
 */
public class StatusPanel extends JPanel implements ActionListener
{
	// Ref vars
	private final DemManager refManager;

	// Gui vars
	private JTextArea infoTA;
	private JButton abortB, clearB;

	// State vars
	private List<Dem> workL;

	/** Standard Constructor */
	public StatusPanel(DemManager aManager)
	{
		refManager = aManager;

		workL = ImmutableList.of();

		setLayout(new MigLayout("", "", ""));

		infoTA = GuiUtil.createUneditableTextArea(3, 0);
		infoTA.setTabSize(2);
		infoTA.setLineWrap(false);
		JScrollPane tmpScrollPane = new JScrollPane(infoTA);
		add(tmpScrollPane, "growx,growy,pushx,pushy,wrap");

		abortB = GuiUtil.createJButton("Abort", this);
		clearB = GuiUtil.createJButton("Clear", this);
//		add(clearB, "ax right,span,split");
//		add(abortB);
		add(abortB, "ax right,span,split");
	}

	/**
	 * Set the list of {@link Dem}s for this panel.
	 */
	public void setItems(Collection<Dem> aDemC)
	{
		workL = ImmutableList.copyOf(aDemC);

		updateGui();
	}

	@Override
	public void actionPerformed(ActionEvent aEvent)
	{
		Object source = aEvent.getSource();
		if (source == abortB)
			doActionAbort();
		else if (source == clearB)
			doActionClear();

		updateGui();
	}

	/**
	 * Helper method that handles the abort action
	 */
	private void doActionAbort()
	{
		for (Dem aItem : workL)
			refManager.getPainterFor(aItem).vtkStateHalt();
	}

	/**
	 * Helper method that handles the clear action
	 */
	private void doActionClear()
	{
//		// Abort any items that are being loaded
//		for (Dem aItem : workL)
//			refManager.getPainterFor(aItem).vtkStateHalt();

		String infoMsg = "Clearing of the cache is not ready.\n";
		infoMsg += ToolTipUtil.getFutureFunctionality();

		MessagePanel tmpPanel = new MessagePanel(this, "Incomplete Logic", 500, 180);
		tmpPanel.setInfo(infoMsg);
		tmpPanel.setVisibleAsModal();
	}

	/**
	 * Helper method that updates the various UI elements to keep them
	 * synchronized.
	 */
	private void updateGui()
	{
		// Gather various stats
		long diskFetched = 0L, diskLocal = 0L;
		int cntFetched = 0, cntLocal = 0;
		int cntAbortable = 0, cntInMemory = 0;
		for (Dem aItem : workL)
		{
			Source tmpSouce = aItem.getSource();
			SourceState tmpSS = SourceUtil.getState(tmpSouce);
			if (tmpSouce.getRemoteUrl() == null)
			{
				cntLocal++;
				diskLocal += tmpSouce.getLocalFile().length();
			}
			else
			{
				if (tmpSS == SourceState.Local)
				{
					cntFetched++;
					diskFetched += tmpSouce.getLocalFile().length();
				}
				else if (tmpSS == SourceState.Partial)
				{
					cntFetched++;
					diskFetched += SourceUtil.getTempFile(tmpSouce).length();
				}
			}

			// Determine the number of abortable items
			VtkDemPainter tmpPainter = refManager.getPainterFor(aItem);
			if (tmpPainter.isLoadActive() == true)
				cntAbortable++;

			// Determine the number of items that are loaded into memory
			if (tmpPainter.getVtkDemSurface() != null)
				cntInMemory++;
		}

		// Abort button
		boolean isEnabled = cntAbortable > 0;
		abortB.setEnabled(isEnabled);

		// Clear button
		isEnabled = cntFetched > 0 || cntInMemory > 0;
		clearB.setEnabled(isEnabled);

		// Info area
		String infoMsg = null;
		if (workL.size() == 1)
		{
			Dem tmpItem = workL.get(0);
			infoMsg = refManager.getPainterFor(tmpItem).getStatusMsg(false);
		}
		else if (workL.size() == 0)
			infoMsg = "No items are selected.";
		else // if (workL.size() > 1)
		{
			infoMsg = "Multiple items are selected.\n\n";

			ByteUnit tmpBU = new ByteUnit(2);
			if (cntLocal > 0)
			{
				infoMsg += "Items stored on local disk:\n";
				infoMsg += "\tFiles: " + cntLocal + "  Disk space: " + tmpBU.getString(diskLocal) + "\n\n";
			}

			if (cntFetched > 0)
			{
				infoMsg += "Items cached on local disk:\n";
				infoMsg += "\tFiles: " + cntFetched + "  Disk space: " + tmpBU.getString(diskFetched) + "\n\n";
			}

			infoMsg += "Items cached in system memory (ram):\n";
			infoMsg += "\tDTMs: " + cntInMemory;
			// + " Disk space: " + tmpBU.getString(totDiskSize) + "\n";
		}

		infoTA.setText(infoMsg);
	}

}
