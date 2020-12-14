package edu.jhuapl.sbmt.dem.gui.analyze.control;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;

import edu.jhuapl.saavtk.color.gui.ColorBarPanel;
import edu.jhuapl.saavtk.color.painter.ColorBarPainter;
import edu.jhuapl.saavtk.color.provider.ColorBarColorProvider;
import edu.jhuapl.saavtk.color.provider.ColorProvider;
import edu.jhuapl.saavtk.color.table.ColorMapAttr;
import edu.jhuapl.saavtk.feature.FeatureType;
import edu.jhuapl.saavtk.gui.dialog.CustomFileChooser;
import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.saavtk.gui.util.IconUtil;
import edu.jhuapl.saavtk.gui.util.ToolTipUtil;
import edu.jhuapl.saavtk.model.structure.LineModel;
import edu.jhuapl.saavtk.pick.ControlPointsPicker;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.saavtk.pick.PickManagerListener;
import edu.jhuapl.saavtk.pick.Picker;
import edu.jhuapl.saavtk.status.StatusNotifier;
import edu.jhuapl.saavtk.structure.PolyLine;
import edu.jhuapl.saavtk.structure.io.StructureMiscUtil;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemManager;
import edu.jhuapl.sbmt.dem.gui.analyze.DemPlot;
import edu.jhuapl.sbmt.dem.gui.analyze.PlateDataPickListener;
import edu.jhuapl.sbmt.dem.gui.popup.DemGuiUtil;
import edu.jhuapl.sbmt.dem.gui.table.ProfileGuiUtil;
import edu.jhuapl.sbmt.dem.io.ProfileIoUtil;
import edu.jhuapl.sbmt.dem.vtk.ItemDrawAttr;
import edu.jhuapl.sbmt.dem.vtk.VtkDemSurface;

import glum.gui.GuiPaneUtil;
import glum.gui.GuiUtil;
import glum.item.ItemEventListener;
import glum.item.ItemEventType;
import net.miginfocom.swing.MigLayout;

/**
 * UI panel that provides the functionality for the control area of the
 * {@link AnalyzePanel1}.
 *
 * @author lopeznr1
 */
public class ControlPanel extends JPanel implements ActionListener, ItemEventListener, PickManagerListener
{
	// Reference vars
	private final DemManager refDemManager;
	private final LineModel<PolyLine> refItemManager;
	private final PickManager refPickManager;
	private final Picker refPicker;
	private final Dem refDem;

	private final Renderer refRenderer;
	private final VtkDemSurface refPriSurface;
	private final DemPlot refPlot;

	// State vars
	private final ColorBarPainter workCBP;
	private final PlateDataPickListener workPDPL;

	// Gui vars
	private final ShowAndSyncPanel showAndSyncPanel;
	private final ColorBarPanel featurePanel;

	private final JLabel profileL;
	private final JButton itemAddB, itemDelB;
	private final JToggleButton itemEditTB;
	private final JButton loadB, saveB;
	private final JLabel fileL;

	/** Standard Constructor */
	public ControlPanel(DemManager aDemManager, LineModel<PolyLine> aItemManager, PickManager aPickManager,
			Picker aPicker, Dem aDem, Renderer aRenderer, VtkDemSurface aPriSurface, DemPlot aPlot,
			StatusNotifier aStatusNotifier)
	{
		refDemManager = aDemManager;
		refItemManager = aItemManager;
		refPickManager = aPickManager;
		refPicker = aPicker;
		refDem = aDem;

		refRenderer = aRenderer;
		refPriSurface = aPriSurface;
		refPlot = aPlot;

		workCBP = new ColorBarPainter(refRenderer);

		setLayout(new MigLayout("", "[]", "[]"));

		// Show and sync area
		showAndSyncPanel = new ShowAndSyncPanel(aDemManager, aDem);
		add(showAndSyncPanel, "growx,wrap");

		// Feature ColorBar area
		featurePanel = new ColorBarPanel(workCBP, false);
		featurePanel.addActionListener(this);
		for (FeatureType aItem : refPriSurface.getFeatureTypeList())
		{
			featurePanel.addFeatureType(aItem, aItem.getName());

			Range<Double> tmpRange = refPriSurface.getValueRangeFor(aItem);
			featurePanel.setResetRange(aItem, tmpRange);
		}
		featurePanel.addFeatureType(FeatureType.Invalid, "No Coloring");
		featurePanel.setFeatureType(FeatureType.Invalid);
		add(featurePanel, "growx,span,w 0::,wrap 0");

		add(GuiUtil.createDivider(), "growx,h 4!,span,wrap");

		// Profile edit area
		profileL = new JLabel("Profiles: 0");
		add(profileL, "ax center,span,wrap");

		itemAddB = GuiUtil.formButton(this, IconUtil.getItemAdd());
		itemAddB.setToolTipText(ToolTipUtil.getProfileAdd());

		itemDelB = GuiUtil.formButton(this, IconUtil.getItemDel());
		itemDelB.setToolTipText(ToolTipUtil.getItemDel());
		itemDelB.setToolTipText(ToolTipUtil.getProfileDel());

		itemEditTB = GuiUtil.formToggleButton(this, IconUtil.getItemEditFalse(), IconUtil.getItemEditTrue());
		itemEditTB.setToolTipText(ToolTipUtil.getProfileEdit());

		add(itemAddB, "w 24!,h 24!,span,split");
		add(itemDelB, "w 24!,h 24!");
		add(itemEditTB, "w 24!,h 24!,wrap");

		// File area
		loadB = new JButton("Load...");
		loadB.addActionListener(this);
		loadB.setToolTipText("Load Profile Data");
		saveB = new JButton("Save...");
		saveB.addActionListener(this);
		saveB.setToolTipText("Save Profile Data");
		add(loadB, "sg 3,span,split");
		add(saveB, "sg 3,wrap");

		fileL = new JLabel("");
		add(fileL, "growx,span,w 0:0:");

		// Add support for readout of the plate data
		workPDPL = new PlateDataPickListener(aStatusNotifier);
		aPickManager.getDefaultPicker().addListener(workPDPL);

		// Register for events of interest
		refItemManager.addListener(this);
		refPickManager.addListener(this);

		updateGui();
	}

	/** Manual destruction */
	public void dispose()
	{
		refItemManager.delListener(this);
		refPickManager.delListener(this);
		showAndSyncPanel.dispose();
	}

	@Override
	public void actionPerformed(ActionEvent aEvent)
	{
		Object source = aEvent.getSource();
		if (source == featurePanel)
			doActionFeaturePanel();
		else if (source == loadB)
			doActionLoad();
		else if (source == saveB)
			doActionSave();
		else if (source == itemAddB)
			doActionItemAdd();
		else if (source == itemDelB)
			doActionItemDel();
		else if (source == itemEditTB)
			doActionItemEdit();
	}

	@Override
	public void handleItemEvent(Object aSource, ItemEventType aEventType)
	{
		updateGui();
	}

	@Override
	public void pickerChanged()
	{
		boolean tmpBool = refPicker == refPickManager.getActivePicker();
		itemEditTB.setSelected(tmpBool);

		updateGui();
	}

	/**
	 * Helper method that handles the item add action.
	 */
	private void doActionItemAdd()
	{
		// Clear out the selection
		refItemManager.setSelectedItems(ImmutableList.of());

		// Set in the next spawn color
		int nextId = StructureMiscUtil.calcNextId(refItemManager);
		Color tmpColor = DemGuiUtil.getAutoGenerateColor(nextId);
		refItemManager.setSpawnColor(tmpColor);

		ControlPointsPicker<?> workPicker = (ControlPointsPicker<?>) refPicker;
		workPicker.startNewItem();

		refPickManager.setActivePicker(refPicker);
	}

	/**
	 * Helper method to process the item delete action.
	 */
	private void doActionItemDel()
	{
		Set<PolyLine> pickS = refItemManager.getSelectedItems();

		// Prompt the user for confirmation
		if (ProfileGuiUtil.promptDeletionConfirmation(this, refItemManager, pickS) == false)
			return;

		// Update internal state vars
		refPickManager.setActivePicker(null);

		// Remove the list items
		refItemManager.removeItems(pickS);
		refItemManager.setSelectedItems(ImmutableList.of());

		updateGui();
	}

	/**
	 * Helper method that handles the edit action.
	 */
	private void doActionItemEdit()
	{
		boolean isEditMode = itemEditTB.isSelected();

		// Switch to the proper picker
		Picker tmpPicker = null;
		if (isEditMode == true)
			tmpPicker = refPicker;
		refPickManager.setActivePicker(tmpPicker);
	}

	/**
	 * Helper method to handle the load action.
	 */
	private void doActionLoad()
	{
		// Bail if no file selected
		File file = CustomFileChooser.showOpenDialog(loadB, "Load Profiles");
		if (file == null)
			return;

		try
		{
			refPickManager.setActivePicker(null);

			// Load and install the items
			List<PolyLine> tmpItemL = ProfileIoUtil.loadView(file);
			refItemManager.setAllItems(tmpItemL);

			// Force the activation painter to be properly updated
			PolyLine activeItem = null;
			if (tmpItemL.size() > 0)
				activeItem = tmpItemL.get(0);
			refItemManager.setActivatedItem(activeItem);

			// Update the file info area
			fileL.setText("File: " + file.getName());
			fileL.setToolTipText(file.getPath());
		}
		catch (Exception aExp)
		{
			String infoMsg = "Loading file: " + file + "\n";
			GuiPaneUtil.showFailMessage(this, "Error Loading Profiles", infoMsg, aExp);
		}
	}

	/**
	 * Helper method to handle the save action.
	 */
	private void doActionSave()
	{
		// Bail if no file selected
		File file = CustomFileChooser.showSaveDialog(saveB, "Save Profiles", "profiles.txt");
		if (file == null)
			return;

		try
		{
			ProfileIoUtil.saveView(file, refItemManager.getAllItems(), refPlot);

			fileL.setText("File: " + file.getName());
			fileL.setToolTipText(file.getPath());
		}
		catch (Exception aExp)
		{
			String infoMsg = "Saving file: " + file + "\n";
			GuiPaneUtil.showFailMessage(this, "Error Saving Profiles", infoMsg, aExp);
		}
	}

	/**
	 * Helper method that will synchronize the DEM(s) to reflect the coloring as
	 * specified via the featurePanel.
	 */
	private void doActionFeaturePanel()
	{
		// Synthesize the ColorProvider corresponding to the featurePanel
		FeatureType tmpFeatureType = featurePanel.getFeatureType();
		ColorMapAttr tmpColorMapAttr = featurePanel.getColorMapAttr();
		ColorProvider tmpColorProvider = new ColorBarColorProvider(tmpColorMapAttr, tmpFeatureType);

		// Install the ColorProvider
		try
		{
			// Notify the DemManager of the ColorProvider to associate with refDem
			refDemManager.setColorProviderInterior(ImmutableList.of(refDem), tmpColorProvider);

			// Update the primary VtkDemSurface and refPlot
			ItemDrawAttr tmpDA = new ItemDrawAttr(ColorProvider.Invalid, false, tmpColorProvider, true, 1.0, 0.0);
			refPriSurface.setDrawAttr(tmpDA);
			refPlot.setFeatureType(tmpFeatureType);
		}
		catch (Throwable aExp)
		{
			String infoMsg = "An error occurred synchronizing primary dem surface coloring to the source dem surface.\n";
			GuiPaneUtil.showFailMessage(this, "Synchronize Dem Colorization Error", infoMsg, aExp);
		}

		// Delegate updating the renderer's colorBar
		updateColorBar(tmpFeatureType, tmpColorMapAttr);
	}

	/**
	 * Helper method that updates the ColorB
	 */
	private void updateColorBar(FeatureType aFeatureType, ColorMapAttr aColorMapAttr)
	{
		// Bail if the specified feature is Invalid
		boolean isShown = aFeatureType != FeatureType.Invalid;
		if (isShown == false)
		{
			refRenderer.delVtkPropProvider(workCBP);
			return;
		}

		// Update the color bar
		workCBP.setColorMapAttr(aColorMapAttr);
		String nameStr = aFeatureType.getName();
		String unitStr = aFeatureType.getUnit();
		if (unitStr != null)
			nameStr += " (" + unitStr + ")";
		workCBP.setTitle(nameStr);

		// Ensure the colorbar is installed
		refRenderer.addVtkPropProvider(workCBP);
		refRenderer.notifySceneChange();

		// Force an update to the plate data readout
		workPDPL.updateDisplay();
	}

	/**
	 * Helper method that updates the various UI elements to keep them
	 * synchronized.
	 */
	private void updateGui()
	{
		int cntFullItems = refItemManager.getAllItems().size();
		int cntPickItems = refItemManager.getSelectedItems().size();
		boolean isEnabled;

		profileL.setText("Profiles: " + cntFullItems);

		isEnabled = cntFullItems > 0 || refPickManager.getActivePicker() == refPicker;
		itemEditTB.setEnabled(isEnabled);

		isEnabled = cntPickItems > 0;
		itemDelB.setEnabled(isEnabled);
	}

}
