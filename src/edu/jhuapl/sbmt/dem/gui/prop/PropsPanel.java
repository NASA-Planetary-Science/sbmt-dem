package edu.jhuapl.sbmt.dem.gui.prop;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import edu.jhuapl.saavtk.gui.util.IconUtil;
import edu.jhuapl.saavtk.gui.util.ToolTipUtil;
import edu.jhuapl.sbmt.core.util.KeyValueNode;

import glum.gui.GuiUtil;
import glum.gui.panel.itemList.ItemListPanel;
import glum.gui.panel.itemList.query.QueryComposer;
import glum.item.BaseItemManager;
import glum.item.ItemEventListener;
import glum.item.ItemEventType;
import glum.item.ItemManager;
import glum.item.ItemManagerUtil;
import net.miginfocom.swing.MigLayout;

/**
 * Panel used to display the available properties associated with the DEM.
 * <p>
 * The available properties are key-value pairs that originate from the header
 * of source DEM (or an auxiliary file).
 *
 * @author lopeznr1
 */
public class PropsPanel extends JPanel implements ActionListener, ItemEventListener
{
	// Attributes
	private final ItemManager<String> workIM;
	private final KeyValueItemHandler workIH;

	// Gui vars
	private final ItemListPanel<String> itemILP;
	private final JLabel titleL;
	private final JButton selectAllB, selectInvertB, selectNoneB;

	/** Standard Constructor */
	public PropsPanel()
	{
		setLayout(new MigLayout("", "0[][][][]0", "0[][]0"));

		// Table header
		selectInvertB = GuiUtil.formButton(this, IconUtil.getSelectInvert());
		selectInvertB.setToolTipText(ToolTipUtil.getSelectInvert());

		selectNoneB = GuiUtil.formButton(this, IconUtil.getSelectNone());
		selectNoneB.setToolTipText(ToolTipUtil.getSelectNone());

		selectAllB = GuiUtil.formButton(this, IconUtil.getSelectAll());
		selectAllB.setToolTipText(ToolTipUtil.getSelectAll());

		titleL = new JLabel("Items: ---");
		add(titleL, "growx,pushx,span,split");
		add(selectInvertB, "w 24!,h 24!");
		add(selectNoneB, "w 24!,h 24!");
		add(selectAllB, "w 24!,h 24!,wrap 2");

		// Table Content
		QueryComposer<LookUp> tmpComposer = new QueryComposer<>();
		tmpComposer.addAttribute(LookUp.Key, String.class, "Property", null);
		tmpComposer.addAttribute(LookUp.Value, String.class, "Value", null);
		tmpComposer.addAttribute(LookUp.Comment, String.class, "Comment", null);
		tmpComposer.getItem(LookUp.Key).defaultSize *= 1.3;
		tmpComposer.getItem(LookUp.Value).defaultSize *= 4;
		tmpComposer.getItem(LookUp.Comment).defaultSize *= 2;

		workIM = new BaseItemManager<>();
		workIH = new KeyValueItemHandler(tmpComposer);
		itemILP = new ItemListPanel<>(workIH, workIM, true);
		itemILP.setSortingEnabled(true);

		JTable itemTable = itemILP.getTable();
		itemTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		add(new JScrollPane(itemTable), "growx,growy,pushy,span,h 70::");

		updateGui();

		// Register for events of interest
		workIM.addListener(this);
	}

	/**
	 * Sets in the key-value pairs to display.
	 */
	public void setKeyValuePairs(Map<String, KeyValueNode> aKeyValueNodeM)
	{
		// Bail if nothing has changed
		if (aKeyValueNodeM == workIH.getKeyValuePairMap())
			return;

		workIH.setKeyValuePairMap(aKeyValueNodeM);
		workIM.setAllItems(aKeyValueNodeM.keySet());

		updateGui();
	}

	@Override
	public void actionPerformed(ActionEvent aEvent)
	{
		Object source = aEvent.getSource();
		if (source == selectAllB)
			ItemManagerUtil.selectAll(workIM);
		else if (source == selectNoneB)
			ItemManagerUtil.selectNone(workIM);
		else if (source == selectInvertB)
			ItemManagerUtil.selectInvert(workIM);

		updateGui();
	}

	@Override
	public void handleItemEvent(Object aSource, ItemEventType aEventType)
	{
		updateGui();
	}

	/**
	 * Helper method that updates the various UI elements to keep them
	 * synchronized.
	 */
	private void updateGui()
	{
		// Gather various stats
		int cntFullItems = workIM.getAllItems().size();

		Set<String> pickS = workIM.getSelectedItems();
		int cntPickItems = pickS.size();

		// Update action buttons
		boolean isEnabled;

		isEnabled = cntFullItems > 0;
		selectInvertB.setEnabled(isEnabled);

		isEnabled = cntPickItems > 0;
		selectNoneB.setEnabled(isEnabled);

		isEnabled = cntFullItems > 0 && cntPickItems < cntFullItems;
		selectAllB.setEnabled(isEnabled);

		// Table title
		DecimalFormat cntFormat = new DecimalFormat("#,###");
		String infoStr = "Items: " + cntFormat.format(cntFullItems);
		if (cntPickItems > 0)
			infoStr += "  (Selected: " + cntFormat.format(cntPickItems) + ")";
		titleL.setText(infoStr);
	}
}
