package edu.jhuapl.sbmt.dem.gui.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import edu.jhuapl.saavtk.color.provider.ColorProvider;
import edu.jhuapl.saavtk.gui.table.ColorCellEditor;
import edu.jhuapl.saavtk.gui.table.ColorCellRenderer;
import edu.jhuapl.saavtk.gui.util.IconUtil;
import edu.jhuapl.saavtk.gui.util.ToolTipUtil;
import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.saavtk.model.structure.LineModel;
import edu.jhuapl.saavtk.pick.ControlPointsPicker;
import edu.jhuapl.saavtk.pick.PickListener;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.saavtk.pick.PickManagerListener;
import edu.jhuapl.saavtk.pick.PickMode;
import edu.jhuapl.saavtk.pick.PickTarget;
import edu.jhuapl.saavtk.pick.PickUtil;
import edu.jhuapl.saavtk.pick.Picker;
import edu.jhuapl.saavtk.structure.PolyLine;
import edu.jhuapl.saavtk.structure.StructureManager;
import edu.jhuapl.saavtk.structure.gui.StructureGuiUtil;
import edu.jhuapl.saavtk.structure.io.StructureMiscUtil;
import edu.jhuapl.sbmt.dem.gui.popup.DemGuiUtil;

import glum.gui.GuiUtil;
import glum.gui.action.PopupMenu;
import glum.gui.misc.BooleanCellEditor;
import glum.gui.misc.BooleanCellRenderer;
import glum.gui.panel.generic.PromptPanel;
import glum.gui.panel.itemList.ItemHandler;
import glum.gui.panel.itemList.ItemListPanel;
import glum.gui.panel.itemList.ItemProcessor;
import glum.gui.panel.itemList.query.QueryComposer;
import glum.gui.table.NumberRenderer;
import glum.gui.table.TablePopupHandler;
import glum.item.ItemEventListener;
import glum.item.ItemEventType;
import glum.item.ItemManagerUtil;
import net.miginfocom.swing.MigLayout;

/**
 * UI panel that provides the following functionality:
 * <ul>
 * <li>Display of list of profiles
 * <li>Adding, editing, or deletion of profiles
 * <li>Changing visibility of profiles
 * </ul>
 * @author lopeznr1
 */
public class ProfileTablePanel extends JPanel
		implements ActionListener, ItemEventListener, PickListener, PickManagerListener
{
	// Ref vars
	private final LineModel<PolyLine> refItemManager;
	private final PickManager refPickManager;
	private final ControlPointsPicker<?> refPicker;

	// State vars
	private ImmutableSet<PolyLine> prevPickS;

	// GUI vars
	private final PopupMenu<PolyLine> popupMenu;
	private final ItemListPanel<PolyLine> itemILP;
	private final JLabel titleL;
	private JButton itemAddB, itemDelB;
	private JToggleButton itemEditTB;
	private JButton selectAllB, selectInvertB, selectNoneB;
	private JCheckBox showItemCB;

	/** Standard Constructor */
	public ProfileTablePanel(LineModel<PolyLine> aItemManager, PickManager aPickManager, ControlPointsPicker<?> aPicker,
			PolyhedralModel aSmallBody)
	{
		refItemManager = aItemManager;
		refPickManager = aPickManager;
		refPicker = aPicker;

		prevPickS = ImmutableSet.of();

		setLayout(new MigLayout("", "", ""));

		// Popup menu
		popupMenu = ProfileGuiUtil.formPopupMenu(refItemManager, this, aSmallBody);

		// Table header
		itemAddB = GuiUtil.formButton(this, IconUtil.getItemAdd());
		itemAddB.setToolTipText(ToolTipUtil.getProfileAdd());

		itemDelB = GuiUtil.formButton(this, IconUtil.getItemDel());
		itemDelB.setToolTipText(ToolTipUtil.getProfileDel());

		itemEditTB = GuiUtil.formToggleButton(this, IconUtil.getItemEditFalse(), IconUtil.getItemEditTrue());
		itemEditTB.setToolTipText(ToolTipUtil.getProfileEdit());

		selectInvertB = GuiUtil.formButton(this, IconUtil.getSelectInvert());
		selectInvertB.setToolTipText(ToolTipUtil.getSelectInvert());

		selectNoneB = GuiUtil.formButton(this, IconUtil.getSelectNone());
		selectNoneB.setToolTipText(ToolTipUtil.getSelectNone());

		selectAllB = GuiUtil.formButton(this, IconUtil.getSelectAll());
		selectAllB.setToolTipText(ToolTipUtil.getSelectAll());

		titleL = new JLabel("Profiles: ---");
		add(titleL, "growx,span,split");
		add(itemAddB, "w 24!,h 24!");
		add(itemDelB, "w 24!,h 24!");
		add(itemEditTB, "gapright 32,w 24!,h 24!");
		add(selectInvertB, "w 24!,h 24!");
		add(selectNoneB, "w 24!,h 24!");
		add(selectAllB, "w 24!,h 24!,wrap 2");

		// Table Content
		QueryComposer<LookUp> tmpComposer = new QueryComposer<>();
		tmpComposer.addAttribute(LookUp.IsVisible, Boolean.class, "", 40);
		tmpComposer.addAttribute(LookUp.Color, ColorProvider.class, "Color", 50);
		tmpComposer.addAttribute(LookUp.Id, Integer.class, "Id", "98");
		tmpComposer.addAttribute(LookUp.BegLat, Double.class, "Beg Lat", null);
		tmpComposer.addAttribute(LookUp.BegLon, Double.class, "Beg Lon", null);
		tmpComposer.addAttribute(LookUp.EndLat, Double.class, "End Lat", null);
		tmpComposer.addAttribute(LookUp.EndLon, Double.class, "End Lon", null);
		tmpComposer.addAttribute(LookUp.Length, Double.class, "Length: km", "Len.: km");

		tmpComposer.setEditor(LookUp.IsVisible, new BooleanCellEditor());
		tmpComposer.setRenderer(LookUp.IsVisible, new BooleanCellRenderer());
		tmpComposer.setEditor(LookUp.Color, new ColorCellEditor());
		tmpComposer.setRenderer(LookUp.Color, new ColorCellRenderer(false));
		tmpComposer.setRenderer(LookUp.BegLat, new NumberRenderer("###,##0.00", "---"));
		tmpComposer.setRenderer(LookUp.BegLon, new NumberRenderer("###,##0.00", "---"));
		tmpComposer.setRenderer(LookUp.EndLat, new NumberRenderer("###,##0.00", "---"));
		tmpComposer.setRenderer(LookUp.EndLon, new NumberRenderer("###,##0.00", "---"));

		tmpComposer.setRenderer(LookUp.Length, new NumberRenderer("#.###", "---"));
		tmpComposer.getItem(LookUp.Length).maxSize *= 2;

		ItemHandler<PolyLine> tmpIH = new ProfileItemHandler<>(refItemManager, tmpComposer);
		ItemProcessor<PolyLine> tmpIP = refItemManager;
		itemILP = new ItemListPanel<>(tmpIH, tmpIP, true);
		itemILP.setSortingEnabled(true);

		JTable itemTable = itemILP.getTable();
		itemTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		itemTable.addMouseListener(new TablePopupHandler(refItemManager, popupMenu));
		add(new JScrollPane(itemTable), "growx,growy,pushy,span,h 70::,wrap");

		// Action buttons: Show item
		showItemCB = GuiUtil.createJCheckBox("Show Profile", this);
		add(showItemCB);

		updateGui();

		// Register for events of interest
		refItemManager.addListener(this);
		refPickManager.addListener(this);
		refPickManager.getDefaultPicker().addListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent aEvent)
	{
		Object source = aEvent.getSource();

		List<PolyLine> pickL = refItemManager.getSelectedItems().asList();
		if (source == itemAddB)
			doActionItemAdd();
		else if (source == itemDelB)
			doActionItemDelete();
		else if (source == itemEditTB)
			doActionItemEdit();
		else if (source == selectAllB)
			ItemManagerUtil.selectAll(refItemManager);
		else if (source == selectNoneB)
			ItemManagerUtil.selectNone(refItemManager);
		else if (source == selectInvertB)
			ItemManagerUtil.selectInvert(refItemManager);
		else if (source == showItemCB)
			refItemManager.setIsVisible(pickL, showItemCB.isSelected());

		updateGui();
	}

	@Override
	public void handleItemEvent(Object aSource, ItemEventType aEventType)
	{
		if (aEventType == ItemEventType.ItemsSelected)
		{
			ImmutableSet<PolyLine> currPickS = refItemManager.getSelectedItems();

			// Scroll only if the selection source was not from our
			// refStructureManager
			if (aSource != refItemManager)
			{
				PolyLine tmpItem = null;
				if (currPickS.size() > 0)
					tmpItem = currPickS.asList().get(currPickS.size() - 1);

				// Scroll only if the previous pickL does not contains all of
				// selected items. This means that an item was deselected and
				// we should not scroll on deselections.
				if (prevPickS.containsAll(currPickS) == false)
					itemILP.scrollToItem(tmpItem);

				prevPickS = currPickS;
			}

			// Switch to the appropriate activation structure (if necessary)
			boolean isEditMode = refPicker == refPickManager.getActivePicker();
			if (isEditMode == true)
				updateActivatedItem();
		}

		updateGui();
	}

	@Override
	public void handlePickAction(InputEvent aEvent, PickMode aMode, PickTarget aPrimaryTarg, PickTarget aSurfaceTarg)
	{
		// Bail if we are are not associated with the primary PickTarget
		if (StructureGuiUtil.isAssociatedPickTarget(aPrimaryTarg, refItemManager) == false)
			return;

		// Bail if not a valid popup action
		if (PickUtil.isPopupTrigger(aEvent) == false || aMode != PickMode.ActiveSec)
			return;

		// Show the popup (if appropriate)
		Component tmpComp = aEvent.getComponent();
		int posX = ((MouseEvent) aEvent).getX();
		int posY = ((MouseEvent) aEvent).getY();
		popupMenu.show(tmpComp, posX, posY);
	}

	@Override
	public void pickerChanged()
	{
		boolean tmpBool = refPicker == refPickManager.getActivePicker();
		itemEditTB.setSelected(tmpBool);

		// Update the activated structure
		updateActivatedItem();
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

		refPicker.startNewItem();
		refPickManager.setActivePicker(refPicker);
	}

	/**
	 * Helper method to process the item delete action.
	 */
	private void doActionItemDelete()
	{
		Set<PolyLine> pickS = refItemManager.getSelectedItems();
		String infoMsg = "Are you sure you want to delete " + pickS.size() + " profiles?\n";

		PromptPanel tmpPanel = new PromptPanel(this, "Confirm Deletion", 400, 160);
		tmpPanel.setInfo(infoMsg);
		tmpPanel.setSize(400, 150);
		tmpPanel.setVisibleAsModal();
		if (tmpPanel.isAccepted() == false)
			return;

		// Update internal state vars
		refPickManager.setActivePicker(null);

		// Remove the list items
		refItemManager.removeItems(pickS);

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
	 * Helper method that updates the manager to reflect the activated item.
	 * <p>
	 * Note: Currently not all {@link StructureManager}s support activation.
	 */
	private void updateActivatedItem()
	{
		// Activate a structure if the following is true:
		// - We are in edit mode
		// - There is only 1 selected structure
		boolean isEditMode = refPicker == refPickManager.getActivePicker();

		PolyLine tmpItem = null;
		ImmutableSet<PolyLine> currPickS = refItemManager.getSelectedItems();
		if (currPickS.size() >= 1 && isEditMode == true)
			tmpItem = currPickS.asList().get(0);

		refItemManager.setActivatedItem(tmpItem);
	}

	/**
	 * Helper method that updates the various UI elements to keep them
	 * synchronized.
	 */
	private void updateGui()
	{
		// Gather various stats
		int cntFullItems = refItemManager.getAllItems().size();

		Set<PolyLine> pickS = refItemManager.getSelectedItems();
		int cntPickItems = pickS.size();

		int cntShowItems = 0;
		for (PolyLine aItem : pickS)
		{
			if (refItemManager.getIsVisible(aItem) == true)
				cntShowItems++;
		}

		// Update action buttons
		boolean isEnabled, isSelected;

		isEnabled = cntFullItems > 0 || refPickManager.getActivePicker() == refPicker;
		itemEditTB.setEnabled(isEnabled);

		isEnabled = cntPickItems > 0;
		itemDelB.setEnabled(isEnabled);

		isEnabled = cntFullItems > 0;
		selectInvertB.setEnabled(isEnabled);

		isEnabled = cntPickItems > 0;
		selectNoneB.setEnabled(isEnabled);

		isEnabled = cntFullItems > 0 && cntPickItems < cntFullItems;
		selectAllB.setEnabled(isEnabled);

		isEnabled = cntPickItems > 0;
		isSelected = cntPickItems == cntShowItems && cntPickItems > 0;
		showItemCB.setEnabled(isEnabled);
		showItemCB.setSelected(isSelected);

		// Table title
		DecimalFormat cntFormat = new DecimalFormat("#,###");
		String infoStr = "Profiles: " + cntFormat.format(cntFullItems);
//		if (cntPickItems > 0)
//			infoStr += "  (Selected: " + cntFormat.format(cntPickItems) + ")";
		titleL.setText(infoStr);

	}

}
