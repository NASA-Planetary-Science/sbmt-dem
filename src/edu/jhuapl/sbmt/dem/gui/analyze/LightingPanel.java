package edu.jhuapl.sbmt.dem.gui.analyze;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import com.google.common.collect.ImmutableList;

import edu.jhuapl.saavtk.gui.util.Colors;
import edu.jhuapl.saavtk.view.light.LightCfg;
import edu.jhuapl.saavtk.view.light.gui.LightCfgPanel;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemManager;

import glum.gui.GuiUtil;
import glum.gui.panel.GPanel;
import glum.item.ItemEventListener;
import glum.item.ItemEventType;
import net.miginfocom.swing.MigLayout;

/**
 * UI panel that provides the functionality for lighting (configuration) of the
 * {@link AnalyzePanel}.
 *
 * @author lopeznr1
 */
public class LightingPanel extends GPanel implements ActionListener, ItemEventListener
{
	// Ref vars
	private final DemManager refManager;
	private final Dem refDem;

	// Gui vars
	private LightCfgPanel lightCfgPanel;
	private JCheckBox useSystemSettingCB;

	private final JLabel statusL;

	/** Standard Constructor */
	public LightingPanel(DemManager aManager, Dem aDem)
	{
		refManager = aManager;
		refDem = aDem;

		// Form the GUI
		setLayout(new MigLayout("", "[]", "[]"));

		useSystemSettingCB = GuiUtil.createJCheckBox("Use body lighting configuration", this);
		add(useSystemSettingCB, "sgy G1,span,wrap");

		lightCfgPanel = new LightCfgPanel();
		lightCfgPanel.setLightCfg(refManager.getConfigAttr(refDem).getRenderLC());
		add(lightCfgPanel, "growx,pushx,wrap 0");

		statusL = new JLabel("");
		add(statusL, "growx,sgy G1,span,w 0::,wrap 0");

		// Register for events of interest
		lightCfgPanel.addActionListener(this);
		refManager.addListener(this);
	}

	/** Manual destruction */
	public void dispose()
	{
		lightCfgPanel.delActionListener(this);
		refManager.delListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent aEvent)
	{
		Object source = aEvent.getSource();
		if (source == lightCfgPanel)
			doActionLightPanel();
		else if (source == useSystemSettingCB)
			doActionSystemSettings();

		updateGui();
	}

	@Override
	public void handleItemEvent(Object aSource, ItemEventType aEventType)
	{
		boolean isSelected = refManager.getIsSyncLightingWithMain(refDem);
		useSystemSettingCB.setSelected(isSelected);

		updateGui();
	}

	/**
	 * Helper method to handle the LightPanel action.
	 */
	private void doActionLightPanel()
	{
		LightCfg tmpLightCfg = lightCfgPanel.getLightCfg();
		refManager.setLightCfg(ImmutableList.of(refDem), tmpLightCfg);
	}

	/**
	 * Helper method to handle the system settings action.
	 */
	private void doActionSystemSettings()
	{
		boolean isSelected = useSystemSettingCB.isSelected();
		refManager.setIsSyncLighting(ImmutableList.of(refDem), isSelected);

		boolean isEnabled = isSelected == false;
		lightCfgPanel.setEnabled(isEnabled);
	}

	/**
	 * Helper method that updates the various UI elements to keep them
	 * synchronized.
	 */
	private void updateGui()
	{
		// Update enable state of lightCfgPanel
		boolean isSelected = refManager.getIsSyncLightingWithMain(refDem);
		boolean isEnabled = isSelected == false;
		lightCfgPanel.setEnabled(isEnabled);

		// Update the status area
		List<String> errMsgL = lightCfgPanel.getMsgFailList();
		String failMsg = null;
		if (errMsgL.size() > 0)
			failMsg = errMsgL.get(0);
		statusL.setText(failMsg);

		Color fgColor = Colors.getPassFG();
		if (failMsg != null)
			fgColor = Colors.getFailFG();
		statusL.setForeground(fgColor);
	}

}
