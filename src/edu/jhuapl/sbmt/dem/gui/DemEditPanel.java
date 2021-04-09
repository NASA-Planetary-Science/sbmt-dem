package edu.jhuapl.sbmt.dem.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import com.google.common.collect.ImmutableList;

import edu.jhuapl.saavtk.gui.util.Colors;
import edu.jhuapl.saavtk.gui.util.IconUtil;
import edu.jhuapl.saavtk.gui.util.ToolTipUtil;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemManager;
import edu.jhuapl.sbmt.dem.gui.popup.DemGuiUtil;
import edu.jhuapl.sbmt.dem.vtk.DataMode;

import glum.gui.FocusUtil;
import glum.gui.GuiUtil;
import glum.gui.action.ClickAction;
import glum.gui.component.GTextField;
import glum.gui.panel.GlassPanel;
import net.miginfocom.swing.MigLayout;

/**
 * UI component that allows a collection of {@link Dem}s to be edited.
 *
 * @author lopeznr1
 */
public class DemEditPanel extends GlassPanel implements ActionListener
{
	// Constants
	private static final String ERR_MSG_NO_ITEMS_PROVIDED = "No DTMs have been selected.";

	// Ref vars
	private final DemManager refItemManager;

	// Gui vars
	private JLabel titleL, descriptionL;
	private GTextField descriptionTF;
	private JCheckBox viewBadDataCB;
	private JButton applyB, closeB, descrResetB, mainResetB;
	private JLabel statusL;

	// State vars
	private ImmutableList<Dem> itemL;
	private String oldDescrip;
	private DataMode oldViewDataMode;

	/** Standard Constructor */
	public DemEditPanel(Component aParent, DemManager aItemManager)
	{
		super(aParent);

		refItemManager = aItemManager;

		itemL = ImmutableList.of();
		oldDescrip = "";
		oldViewDataMode = null;

		setLayout(new MigLayout("", "[]", ""));

		// Title Area
		titleL = new JLabel("Edit DTMs", JLabel.CENTER);
		add(titleL, "growx,span,wrap 2");
		add(GuiUtil.createDivider(), "growx,h 4!,span,wrap 12");

		// Action area
		descrResetB = GuiUtil.formButton(this, IconUtil.getActionReset());
		descrResetB.setToolTipText(ToolTipUtil.getItemResetMsg("---"));
		descriptionL = new JLabel("Description:");
		descriptionTF = new GTextField(this);
		add(descrResetB, "w 24!,h 24!");
		add(descriptionL, "");
		add(descriptionTF, "w 100::,growx,pushx,wrap");

		viewBadDataCB = GuiUtil.createJCheckBox("View Invalid Data", this);
		add(viewBadDataCB, "growx,span,wrap");

		statusL = new JLabel(ERR_MSG_NO_ITEMS_PROVIDED);
		add(statusL, "w 350:,growx,span,wrap");

		// Control area
		applyB = GuiUtil.createJButton("Apply", this);
		closeB = GuiUtil.createJButton("Close", this);
		mainResetB = GuiUtil.createJButton("Reset", this);
		add(applyB, "ax right,split,span");
		add(mainResetB);
		add(closeB);

		// Set up keyboard short cuts
		FocusUtil.addAncestorKeyBinding(this, "ESCAPE", new ClickAction(closeB));
		FocusUtil.addAncestorKeyBinding(this, "ENTER", new ClickAction(applyB));
	}

	/**
	 * Sets in the items to be edited.
	 */
	public void setItemsToEdit(Collection<Dem> aItemC)
	{
		itemL = ImmutableList.copyOf(aItemC);

		boolean isEnabled = itemL.size() == 1;
		descriptionL.setEnabled(isEnabled);
		descriptionTF.setEnabled(isEnabled);
		descrResetB.setEnabled(isEnabled);

		// Description area
		String descrResetTip = null;
		oldDescrip = null;
		int tmpAlignment = JTextField.LEFT;
		if (itemL.size() >= 1)
		{
			Dem tmpItem = itemL.get(0);
			oldDescrip = refItemManager.getDisplayName(tmpItem);
			descrResetTip = ToolTipUtil.getItemResetMsg(tmpItem.getSource().getName());
			if (itemL.size() > 1)
			{
				oldDescrip = "\u2192" + " multiple items specified " + "\u2190";
				descrResetTip = null;
				tmpAlignment = JTextField.CENTER;
			}
		}
		descriptionTF.setHorizontalAlignment(tmpAlignment);
		descriptionTF.setValue(oldDescrip);
		descrResetB.setToolTipText(descrResetTip);

		// Dem view DataMode area
		boolean viewBadDataEnabled = true;
		boolean viewBadDataFlag = false;
		String viewBadDataTip = null;
		oldViewDataMode = DemGuiUtil.getUnifiedViewDataMode(refItemManager, itemL);
		if (oldViewDataMode == DataMode.Plain)
		{
			viewBadDataEnabled = false;
			viewBadDataFlag = false;
			viewBadDataTip = "All data is considered valid.";
		}
		else if (DemGuiUtil.isAnyDemViewDataModeNonCofigurable(refItemManager, itemL) == true)
		{
			viewBadDataTip = "Some DTM's view mode can not be configured. These will be ignored.";
		}
		else if (oldViewDataMode == DataMode.Regular)
		{
			viewBadDataFlag = true;
		}
		else if (oldViewDataMode == DataMode.Valid)
		{
			viewBadDataFlag = false;
		}
		viewBadDataCB.setEnabled(viewBadDataEnabled);
		viewBadDataCB.setSelected(viewBadDataFlag);
		viewBadDataCB.setToolTipText(viewBadDataTip);

		updateGui();
	}

	@Override
	public void actionPerformed(ActionEvent aEvent)
	{
		Object source = aEvent.getSource();
		if (source == applyB)
			doActionApply();
		if (source == descrResetB)
			descriptionTF.setValue(itemL.get(0).getSource().getName());
		if (source == mainResetB)
			doActionMainReset();
		else if (source == closeB)
			setVisible(false);

		updateGui();
	}

	/**
	 * Helper method to handle the apply action.
	 */
	private void doActionApply()
	{
		String newDescrip = descriptionTF.getValue();
		if (itemL.size() == 1)
			refItemManager.setDescription(itemL, newDescrip);

		DataMode newViewDataMode = getViewDataModeFromUI();
		if (viewBadDataCB.isEnabled() == true)
			refItemManager.setViewDataMode(itemL, newViewDataMode);
	}

	/**
	 * Helper method to handle the main reset action.
	 */
	private void doActionMainReset()
	{
		descriptionTF.setValue(oldDescrip);

		boolean viewBadDataFlag = false;
		if (oldViewDataMode == DataMode.Regular)
			viewBadDataFlag = true;
		else if (oldViewDataMode == DataMode.Valid)
			viewBadDataFlag = false;
		if (viewBadDataCB.isEnabled() == true)
			viewBadDataCB.setSelected(viewBadDataFlag);

		doActionApply();
	}

	/**
	 * Helper method that returns the {@link DataMode} as specified in the UI.
	 */
	private DataMode getViewDataModeFromUI()
	{
		if (viewBadDataCB.isEnabled() == false)
			return DataMode.Plain;
		if (viewBadDataCB.isSelected() == true)
			return DataMode.Regular;

		return DataMode.Valid;
	}

	/**
	 * Helper method that keeps the GUI synchronized with user input.
	 */
	private void updateGui()
	{
		// Determine if there are errors
		String errMsg = null;
		if (itemL.size() == 0)
			errMsg = ERR_MSG_NO_ITEMS_PROVIDED;
		else if (descriptionTF.isValidInput() == false)
			errMsg = "Please specify a valid description.";
		else if (descriptionTF.isEnabled() == false && viewBadDataCB.isEnabled() == false)
			errMsg = "The selected items can not be configured as a group.";

		// Title area
		titleL.setText("Edit DTMs: " + itemL.size());

		// Description area
		String newDescr = descriptionTF.getValue();
		String defDescr = "";
		if (itemL.size() == 1)
			defDescr = itemL.get(0).getSource().getName();
		boolean isEnabled = defDescr.equals(newDescr) == false && itemL.size() == 1;
		descrResetB.setEnabled(isEnabled);

		// Main reset area
		DataMode newViewDataMode = getViewDataModeFromUI();
		isEnabled = Objects.equals(newDescr, oldDescrip) == false && itemL.size() == 1;
		isEnabled |= Objects.equals(newViewDataMode, oldViewDataMode) == false;
		mainResetB.setEnabled(isEnabled);

		// Apply area
		isEnabled = false;
		if (itemL.size() == 1)
			isEnabled |= Objects.equals(refItemManager.getDisplayName(itemL.get(0)), newDescr) == false;
		isEnabled |= Objects.equals(DemGuiUtil.getUnifiedViewDataMode(refItemManager, itemL), newViewDataMode) == false;
		isEnabled &= errMsg == null;
		applyB.setEnabled(isEnabled);

		// Status area
		String regMsg = "No changes have been made.";
		if (isEnabled == true)
			regMsg = "Changes have not been applied.";

		String tmpMsg = regMsg;
		if (errMsg != null)
			tmpMsg = errMsg;
		statusL.setText(tmpMsg);

		Color fgColor = Colors.getPassFG();
		if (errMsg != null)
			fgColor = Colors.getFailFG();
		statusL.setForeground(fgColor);
	}

}
