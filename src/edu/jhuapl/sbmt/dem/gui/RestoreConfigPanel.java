package edu.jhuapl.sbmt.dem.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.google.common.collect.ImmutableMap;

import edu.jhuapl.saavtk.gui.util.ToolTipUtil;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemConfigAttr;
import edu.jhuapl.sbmt.dem.DemManager;
import edu.jhuapl.sbmt.dem.vtk.ItemDrawAttr;

import glum.gui.FocusUtil;
import glum.gui.GuiUtil;
import glum.gui.action.ClickAction;
import glum.gui.info.WindowCfg;
import glum.gui.panel.GlassPanel;
import glum.source.SourceState;
import glum.source.SourceUtil;
import net.miginfocom.swing.MigLayout;

/**
 * {@link GlassPanel} that allows the user to configure the settings they would
 * like to restore. These settings are specific to the "Regional DEMs" (via the
 * {@link DemConfigAttr} struct).
 *
 * @author lopeznr1
 */
public class RestoreConfigPanel extends GlassPanel implements ActionListener
{
	// Ref vars
	private final DemManager refManager;

	// GUI vars
	private JLabel titleL;
	private JTextArea infoTA;
	private JCheckBox custColorizeCB;
	private JCheckBox showAnaCB, showIntCB, showExtCB;
	private JButton proceedB;

	// State vars
	private Map<Dem, DemConfigAttr> workConfigM;
	private int cntPartFiles;

	/** Standard Constructor */
	public RestoreConfigPanel(DemManager aManager, Component aParent)
	{
		super(aParent);

		refManager = aManager;

		workConfigM = ImmutableMap.of();

		buildGuiArea();

		// Set up keyboard short cuts
		FocusUtil.addAncestorKeyBinding(this, "ENTER", new ClickAction(proceedB));
	}

	/**
	 * Sets the {@link DemConfigAttr} configuration that will be considered.
	 */
	public void setConfiguration(Map<Dem, DemConfigAttr> aConfigM)
	{
		workConfigM = new LinkedHashMap<>(aConfigM);

		// Gather the various stats
		int cntCustColorize = 0;
		int cntShowAna = 0;
		int cntShowExt = 0;
		int cntShowInt = 0;
		cntPartFiles = 0;
		for (Dem aItem : workConfigM.keySet())
		{
			DemConfigAttr tmpDCA = workConfigM.get(aItem);

			// Skip over all partial loads
			if (SourceUtil.getState(aItem.getSource()) == SourceState.Partial)
			{
				// Keep track of (skipped) partial loads only if they were active
				boolean isActiveLoad = tmpDCA.getWindowCfg() != null && tmpDCA.getWindowCfg().getIsShown() == true;
				isActiveLoad |= tmpDCA.getDrawAttr().getIsExtShown() == true;
				isActiveLoad |= tmpDCA.getDrawAttr().getIsIntShown() == true;
				if (isActiveLoad == true)
					cntPartFiles++;

				continue;
			}

			if (tmpDCA.getWindowCfg() != null && tmpDCA.getWindowCfg().getIsShown() == true)
				cntShowAna++;
			if (tmpDCA.getDrawAttr().getIsExtShown() == true)
				cntShowExt++;
			if (tmpDCA.getDrawAttr().getIsIntShown() == true)
				cntShowInt++;
		}

		// Update the UI components to the initial state
		boolean isEnabled;
		isEnabled = cntShowAna > 0;
		showAnaCB.setEnabled(isEnabled);
		showAnaCB.setSelected(isEnabled);
		showAnaCB.setText("Analyze windows: " + cntShowAna);

		isEnabled = cntCustColorize > 0;
		custColorizeCB.setEnabled(isEnabled);
		custColorizeCB.setSelected(isEnabled);
		custColorizeCB.setText("Custom colorizations: " + cntCustColorize);
		custColorizeCB.setToolTipText(ToolTipUtil.getFutureFunctionality());

		isEnabled = cntShowExt > 0;
		showExtCB.setEnabled(isEnabled);
		showExtCB.setSelected(isEnabled);
		showExtCB.setText("Display boundaries: " + cntShowExt);

		isEnabled = cntShowInt > 0;
		showIntCB.setEnabled(isEnabled);
		showIntCB.setSelected(isEnabled);
		showIntCB.setText("Display dems: " + cntShowInt);

		updateGui();
	}

	@Override
	public void actionPerformed(ActionEvent aEvent)
	{
		Object source = aEvent.getSource();
		if (source == proceedB)
			doActionProceed();
		else
			updateGui();
	}

	/**
	 * Forms the actual GUI
	 */
	protected void buildGuiArea()
	{
		setLayout(new MigLayout("", "[]30[]", "[]"));

		// Title Area
		titleL = new JLabel("Restore Configuration", JLabel.CENTER);
		add(titleL, "growx,span,wrap");

		// Info area
		infoTA = new JTextArea("No status", 3, 0);
		infoTA.setEditable(false);
		infoTA.setLineWrap(true);
		infoTA.setTabSize(3);
		infoTA.setWrapStyleWord(true);

		JScrollPane tmpScrollPane = new JScrollPane(infoTA);
		add(tmpScrollPane, "growx,growy,pushx,pushy,span,wrap");

		// Action area
		showAnaCB = GuiUtil.createJCheckBox("Analyze Windows: ???", this);
		custColorizeCB = GuiUtil.createJCheckBox("Custom colorizations: ???", this);
		custColorizeCB.setEnabled(false);
		showExtCB = GuiUtil.createJCheckBox("Display boundaries: ???", this);
		showIntCB = GuiUtil.createJCheckBox("Display dems: ???", this);

		add(showIntCB, "");
		add(showExtCB, "wrap");
		add(showAnaCB, "");
		add(custColorizeCB, "wrap");

		// Control area
		proceedB = GuiUtil.createJButton("Proceed", this);
		add(proceedB, "ax right,span,split");
	}

	/**
	 * Helper method that handles the "proceed" action.
	 */
	private void doActionProceed()
	{
		boolean isShowAna = showAnaCB.isSelected();
		boolean isShowExt = showExtCB.isSelected();
		boolean isShowInt = showIntCB.isSelected();

		// Update configuration to take into account user's selection
		for (Dem aItem : workConfigM.keySet())
		{
			DemConfigAttr tmpDCA = workConfigM.get(aItem);

			WindowCfg tmpWC = tmpDCA.getWindowCfg();
			if (isShowAna == false && tmpWC != null)
			{
				tmpWC = new WindowCfg(false, tmpWC.getPosX(), tmpWC.getPosY(), tmpWC.getDimX(), tmpWC.getDimY());
				tmpDCA = tmpDCA.cloneWithWindowCfg(tmpWC);
			}

			ItemDrawAttr tmpIDA = tmpDCA.getDrawAttr();
			if (isShowExt == false)
				tmpDCA = tmpDCA.cloneWithDrawAttr(tmpIDA.cloneWithExteriorIsShown(false));

			tmpIDA = tmpDCA.getDrawAttr();
			if (isShowInt == false)
				tmpDCA = tmpDCA.cloneWithDrawAttr(tmpIDA.cloneWithInteriorIsShown(false));

			// Clear the necessary (load) attributes for any partial file
			if (SourceUtil.getState(aItem.getSource()) == SourceState.Partial)
			{
				tmpIDA = tmpIDA.cloneWithExteriorIsShown(false);
				tmpIDA = tmpIDA.cloneWithInteriorIsShown(false);
				tmpDCA = tmpDCA.cloneWithDrawAttr(tmpIDA);
				tmpDCA = tmpDCA.cloneWithWindowCfg(null);
			}

			workConfigM.put(aItem, tmpDCA);
		}

		refManager.markInitDone();
		refManager.installConfiguration(workConfigM);

		setVisible(false);
	}

	/**
	 * Helper method that updates the various UI elements to keep them
	 * synchronized.
	 */
	private void updateGui()
	{
		// Update infoTA
		String infoMsg = "Please select the configuration settings you would like to restore...\n\n";

		boolean isShowIntItem = showIntCB.isSelected();
		if (showIntCB.isEnabled() == false)
			infoMsg += "Diplayed dems: No settings to restore.\n";
		else if (isShowIntItem == true)
			infoMsg += "Displayed dems will be shown on main body.\n";
		else
			infoMsg += "Displayed dems will NOT be shown on main body.\n";

		boolean isShowExtItem = showExtCB.isSelected();
		if (showExtCB.isEnabled() == false)
			infoMsg += "Diplayed boundaries: No settings to restore.\n";
		else if (isShowExtItem == true)
			infoMsg += "Displayed boundaries will be shown on main body.\n";
		else
			infoMsg += "Displayed boundaries will NOT be shown on main body.\n";

		boolean isShowAnaItem = showAnaCB.isSelected();
		if (showAnaCB.isEnabled() == false)
			infoMsg += "Analyze windows: No settings to restore.\n";
		else if (isShowAnaItem == true)
			infoMsg += "Analyze windows will be restored.\n";
		else
			infoMsg += "Analyze windows will NOT be restored.\n";

		boolean isColorize = custColorizeCB.isSelected();
		if (custColorizeCB.isEnabled() == false)
			infoMsg += "Custom colorizations: " + ToolTipUtil.getFutureFunctionality() + "\n";
		else if (isShowIntItem == true && isColorize == true)
			infoMsg += "Custom colorizations will be restored.\n";
		else
			infoMsg += "Custom colorizations will NOT be restored.\n";

		if (isShowAnaItem == false && isColorize == false && isShowExtItem == false && isShowIntItem == false)
			infoMsg += "\nNo configuration will be restored.\n";

		if (cntPartFiles > 0)
			infoMsg += "\nThere are files (" + cntPartFiles + ") that have not been fully downloaded."
					+ " Settings associated with these partial files will NOT be restored.";

		if (infoMsg.endsWith("\n") == true)
			infoMsg = infoMsg.substring(0, infoMsg.length() - 1);
		infoTA.setText(infoMsg);

		// Update linked items
		boolean isEnabled = isShowIntItem == true && custColorizeCB.getText().endsWith("0") == false;
		custColorizeCB.setEnabled(isEnabled);
	}

}
