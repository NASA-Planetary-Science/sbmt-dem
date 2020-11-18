package edu.jhuapl.sbmt.dem.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

import org.apache.commons.io.FilenameUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;

import edu.jhuapl.saavtk.color.gui.ColorMode;
import edu.jhuapl.saavtk.color.gui.ColorProviderCellEditor;
import edu.jhuapl.saavtk.color.gui.ColorProviderCellRenderer;
import edu.jhuapl.saavtk.color.provider.ColorProvider;
import edu.jhuapl.saavtk.color.provider.GroupColorProvider;
import edu.jhuapl.saavtk.gui.dialog.CustomFileChooser;
import edu.jhuapl.saavtk.gui.render.Renderer;
import edu.jhuapl.saavtk.gui.util.IconUtil;
import edu.jhuapl.saavtk.gui.util.ToolTipUtil;
import edu.jhuapl.saavtk.model.PolyhedralModel;
import edu.jhuapl.saavtk.pick.PickListener;
import edu.jhuapl.saavtk.pick.PickManager;
import edu.jhuapl.saavtk.pick.PickMode;
import edu.jhuapl.saavtk.pick.PickTarget;
import edu.jhuapl.saavtk.pick.PickUtil;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemCatalog;
import edu.jhuapl.sbmt.dem.DemConfigAttr;
import edu.jhuapl.sbmt.dem.DemManager;
import edu.jhuapl.sbmt.dem.DemStruct;
import edu.jhuapl.sbmt.dem.KeyValueNode;
import edu.jhuapl.sbmt.dem.gui.color.ColorConfigPanel;
import edu.jhuapl.sbmt.dem.gui.popup.ActionUtil;
import edu.jhuapl.sbmt.dem.gui.popup.DemGuiUtil;
import edu.jhuapl.sbmt.dem.gui.prop.PropsPanel;
import edu.jhuapl.sbmt.dem.io.DemCacheUtil;
import edu.jhuapl.sbmt.dem.io.DemCatalogUtil;

import glum.gui.GuiExeUtil;
import glum.gui.GuiUtil;
import glum.gui.action.PopupMenu;
import glum.gui.component.GComboBox;
import glum.gui.component.GNumberField;
import glum.gui.component.GSlider;
import glum.gui.misc.BooleanCellEditor;
import glum.gui.misc.BooleanCellRenderer;
import glum.gui.panel.itemList.ItemHandler;
import glum.gui.panel.itemList.ItemListPanel;
import glum.gui.panel.itemList.ItemProcessor;
import glum.gui.panel.itemList.query.QueryComposer;
import glum.gui.panel.task.FullTaskPanel;
import glum.gui.table.NumberRenderer;
import glum.gui.table.TablePopupHandler;
import glum.item.ItemEventListener;
import glum.item.ItemEventType;
import glum.item.ItemManagerUtil;
import glum.task.ConsoleTask;
import glum.task.SilentTask;
import glum.task.Task;
import glum.util.ThreadUtil;
import net.miginfocom.swing.MigLayout;

/**
 * Panel used to display a list of DEMs.
 * <p>
 * The following functionality is supported:
 * <ul>
 * <li>Display list of DEMs in a table
 * <li>Allow user to show, hide, configure, or remove DTMs
 * <li>Allow user to analyze DEMs
 * </ul>
 *
 * @author lopeznr1
 */
public class DemListPanel extends JPanel implements ActionListener, ItemEventListener, PickListener
{
	// Constants
	private static final Range<Double> OpacityRange = Range.closed(0.0, 1.0);

	// Ref vars
	private final DemManager refItemManager;
	private final Renderer refRenderer;
	private final ImmutableList<DemCatalog> refCatalogL;

	// State vars
	private DemCatalog workCatalog;
	private double radialOffsetScale;

	// GUI vars
	private final PopupMenu<?> popupMenu;
	private ItemListPanel<Dem> itemILP;
	private ColorConfigPanel colorExtPanel;
	private PropsPanel detailsPanel;
	private StatusPanel statusPanel;
	private DemEditPanel editPanel;
	private FullTaskPanel loadPanel;
	private JLabel titleL, sourceL;
	private JButton actionCenterB;
	private JButton itemAddB, itemDelB, itemEditB;
	private JButton selectAllB, selectInvertB, selectNoneB;
	private JCheckBox anaItemCB, intItemCB, extItemCB;
	private GComboBox<DemCatalog> catalogBox;
	private GNumberField opacityNF, radialNF;
	private GSlider opacityS, radialS;
	private JButton opacityResetB, radialResetB;
	private JLabel opacityL, radialL1, radialL2;

	/** Standard constructor */
	private DemListPanel(DemManager aItemManager, Renderer aRenderer, PickManager aPickManager,
			PolyhedralModel aSmallBody, List<DemCatalog> aCatalogL, JPanel aSpawnPanel)
	{
		refItemManager = aItemManager;
		refRenderer = aRenderer;
		refCatalogL = ImmutableList.copyOf(aCatalogL);

		workCatalog = DemCatalog.Invalid;
		radialOffsetScale = DemGuiUtil.getNominalRadialOffsetScale(aSmallBody);

		editPanel = null;
		loadPanel = null;

		setLayout(new MigLayout("", "", ""));

		// Table header
		actionCenterB = GuiUtil.formButton(this, IconUtil.getActionCenter());
		actionCenterB.setToolTipText("Center DEM in window");

		itemAddB = GuiUtil.formButton(this, IconUtil.getItemAdd());
		itemAddB.setToolTipText(ToolTipUtil.getItemAdd());

		itemDelB = GuiUtil.formButton(this, IconUtil.getItemDel());
		itemDelB.setToolTipText(ToolTipUtil.getItemDel());

		itemEditB = GuiUtil.formButton(this, IconUtil.getItemEdit());
		itemEditB.setToolTipText(ToolTipUtil.getItemEdit());

		selectInvertB = GuiUtil.formButton(this, IconUtil.getSelectInvert());
		selectInvertB.setToolTipText(ToolTipUtil.getSelectInvert());

		selectNoneB = GuiUtil.formButton(this, IconUtil.getSelectNone());
		selectNoneB.setToolTipText(ToolTipUtil.getSelectNone());

		selectAllB = GuiUtil.formButton(this, IconUtil.getSelectAll());
		selectAllB.setToolTipText(ToolTipUtil.getSelectAll());

		titleL = new JLabel("DEMs: ---");
		add(titleL, "growx,span,split");
		add(actionCenterB, "w 24!,h 24!,gapright 24");
		add(itemAddB, "w 24!,h 24!");
		add(itemDelB, "w 24!,h 24!");
		add(itemEditB, "gapright 24,w 24!,h 24!");
		add(selectInvertB, "w 24!,h 24!");
		add(selectNoneB, "w 24!,h 24!");
		add(selectAllB, "w 24!,h 24!,wrap 2");

		// Table Content
		QueryComposer<LookUp> tmpComposer = new QueryComposer<>();
		tmpComposer.addAttribute(LookUp.IsAnalyzePanel, Boolean.class, "Ana.", 40);
		tmpComposer.addAttribute(LookUp.IsShowInterior, Boolean.class, "DEM", 40);
		tmpComposer.addAttribute(LookUp.IsShowExterior, Boolean.class, "Bndr", 40);
//		tmpComposer.addAttribute(LookUp.ColorInterior, ColorProvider.class, "DEM Color", 50);
		tmpComposer.addAttribute(LookUp.ColorExterior, ColorProvider.class, "Bndr", 50);
		tmpComposer.addAttribute(LookUp.Status, String.class, "Status", null);
		tmpComposer.addAttribute(LookUp.Description, String.class, "Description", null);
		tmpComposer.addAttribute(LookUp.GroundSampleDistance, Double.class, "GSD", null);
		tmpComposer.addAttribute(LookUp.NumPixels, Integer.class, "Pix", null);
		tmpComposer.addAttribute(LookUp.Latitude, Double.class, "Lat", null);
		tmpComposer.addAttribute(LookUp.Longitude, Double.class, "Lon", null);

		tmpComposer.setEditor(LookUp.IsAnalyzePanel, new BooleanCellEditor());
		tmpComposer.setRenderer(LookUp.IsAnalyzePanel, new BooleanCellRenderer());
		tmpComposer.setEditor(LookUp.IsShowInterior, new BooleanCellEditor());
		tmpComposer.setRenderer(LookUp.IsShowInterior, new BooleanCellRenderer());
		tmpComposer.setEditor(LookUp.IsShowExterior, new BooleanCellEditor());
		tmpComposer.setRenderer(LookUp.IsShowExterior, new BooleanCellRenderer());
		tmpComposer.setEditor(LookUp.ColorExterior, new ColorProviderCellEditor<>());
		tmpComposer.setRenderer(LookUp.ColorExterior, new ColorProviderCellRenderer(false));
//		tmpComposer.setEditor(LookUp.ColorInterior, new ColorProviderCellEditor<>());
//		tmpComposer.setRenderer(LookUp.ColorInterior, new ColorProviderCellRenderer(false));
		tmpComposer.setRenderer(LookUp.NumPixels, new NumberRenderer("###,##0", "---"));
		tmpComposer.setRenderer(LookUp.GroundSampleDistance, new NumberRenderer("###,##0.00", "---"));
		tmpComposer.setRenderer(LookUp.Latitude, new NumberRenderer("###,##0.00", "---"));
		tmpComposer.setRenderer(LookUp.Longitude, new NumberRenderer("###,##0.00", "---"));

		tmpComposer.getItem(LookUp.Description).defaultSize *= 3;
		tmpComposer.getItem(LookUp.NumPixels).defaultSize *= 1.75;
		tmpComposer.getItem(LookUp.GroundSampleDistance).defaultSize *= 2;
		tmpComposer.getItem(LookUp.Latitude).defaultSize *= 2;
		tmpComposer.getItem(LookUp.Longitude).defaultSize *= 2;
		tmpComposer.getItem(LookUp.Status).defaultSize *= 2;

		ItemHandler<Dem> tmpIH = new DemItemHandler(refItemManager, tmpComposer);
		ItemProcessor<Dem> tmpIP = refItemManager;
		itemILP = new ItemListPanel<>(tmpIH, tmpIP, true);
		itemILP.setSortingEnabled(true);

		JTable itemTable = itemILP.getTable();
		itemTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		add(new JScrollPane(itemTable), "growx,growy,pushy,span,h 70::,wrap");

		// Popup menu
		popupMenu = DemGuiUtil.formPopupMenu(refItemManager, this, refRenderer, aSmallBody);
		itemTable.addMouseListener(new TablePopupHandler(refItemManager, popupMenu));

		// Source area
		JLabel tmpL = new JLabel("Src:");
		sourceL = new JLabel("");
		add(tmpL, "span,split");
		add(sourceL, "growx,w 100:100:,wrap");

		add(GuiUtil.createDivider(), "growx,h 4!,span,wrap");

		// Action buttons
		anaItemCB = GuiUtil.createJCheckBox("Analyze Window", this);
		intItemCB = GuiUtil.createJCheckBox("Show DEM", this);
		extItemCB = GuiUtil.createJCheckBox("Show Boundary", this);

		// Form the left and right sub panels
		DemCatalog tmpCatalog = refCatalogL.get(0);
		boolean isFixedCatalogMode = tmpCatalog.getIsEditable();

		JComponent leftComp = formLeftComp(isFixedCatalogMode);
		JComponent rightComp = formRightComp();
		add(leftComp, "ay top,span,split");
//		add(GuiUtil.createDivider(), "growy,w 4!");
		add(rightComp, "ax right,ay top,growx,pushx");

		updateGui();

		// Register for events of interest
		refItemManager.addListener(this);
		aPickManager.getDefaultPicker().addListener(this);

		GuiExeUtil.executeOnceWhenShowing(this, () -> doActionSwitchToCatalog(tmpCatalog));
		ThreadUtil.addShutdownHook(() -> shutdownPanel());
	}

	/** Constructor for building a panel for showing a custom list of DEMs. */
	public DemListPanel(DemManager aItemManager, Renderer aRenderer, PickManager aPickManager,
			PolyhedralModel aSmallBody, DemCatalog aDemCatalog, JPanel aSpawnPanel)
	{
		// Delegate
		this(aItemManager, aRenderer, aPickManager, aSmallBody, ImmutableList.of(aDemCatalog), aSpawnPanel);
	}

	/** Constructor for building a panel for showing fixed list(s) of DEMs. */
	public DemListPanel(DemManager aItemManager, Renderer aRenderer, PickManager aPickManager,
			PolyhedralModel aSmallBody, List<DemCatalog> aCatalogL)
	{
		// Delegate
		this(aItemManager, aRenderer, aPickManager, aSmallBody, aCatalogL, null);
	}

	@Override
	public void actionPerformed(ActionEvent aEvent)
	{
		Object source = aEvent.getSource();

		List<Dem> tmpL = refItemManager.getSelectedItems().asList();
		if (source == colorExtPanel)
			doActionColorExtPanel();
		else if (source == itemAddB)
			doActionItemLoad();
		else if (source == itemDelB)
			doActionItemDelete();
		else if (source == itemEditB)
			doActionItemEdit();
		else if (source == selectAllB)
			ItemManagerUtil.selectAll(refItemManager);
		else if (source == selectNoneB)
			ItemManagerUtil.selectNone(refItemManager);
		else if (source == selectInvertB)
			ItemManagerUtil.selectInvert(refItemManager);
		else if (source == actionCenterB)
			ActionUtil.centerOnDem(refItemManager, tmpL.get(0), refRenderer);
		else if (source == opacityNF || source == opacityS)
			doActionOpacity(tmpL, source);
		else if (source == opacityResetB)
			doActionOpacityReset(tmpL);
		else if (source == radialNF || source == radialS)
			doActionRadialOffset(tmpL, source);
		else if (source == radialResetB)
			doActionRadialReset(tmpL);
		else if (source == anaItemCB)
			refItemManager.setIsDemAnalyzed(tmpL, anaItemCB.isSelected());
		else if (source == intItemCB)
			refItemManager.setIsVisibleInterior(tmpL, intItemCB.isSelected());
		else if (source == extItemCB)
			refItemManager.setIsVisibleExterior(tmpL, extItemCB.isSelected());
		else if (source == catalogBox)
			doActionSwitchToCatalog(catalogBox.getChosenItem());

		updateGui();
	}

	@Override
	public void handleItemEvent(Object aSource, ItemEventType aEventType)
	{
		List<Dem> pickL = refItemManager.getSelectedItems().asList();
		Dem pickItem = null;
		if (pickL.size() > 0)
			pickItem = pickL.get(pickL.size() - 1);

		if (aEventType == ItemEventType.ItemsSelected)
		{
			if (aSource != refItemManager)
				itemILP.scrollToItem(pickItem);

			// Update detailsPanel
			Map<String, KeyValueNode> keyValueM = ImmutableMap.of();
			if (pickL.size() == 1)
				keyValueM = refItemManager.getKeyValuePairMap(pickItem);

			detailsPanel.setKeyValuePairs(keyValueM);

			// Status area
			statusPanel.setItems(pickL);
		}
		else if (aEventType == ItemEventType.ItemsMutated)
		{
			// Update detailsPanel
			Map<String, KeyValueNode> keyValueM = ImmutableMap.of();
			if (pickL.size() == 1)
				keyValueM = refItemManager.getKeyValuePairMap(pickItem);

			detailsPanel.setKeyValuePairs(keyValueM);

			// Status area
			statusPanel.setItems(pickL);
		}

		updateGui();
	}

	@Override
	public void handlePickAction(InputEvent aEvent, PickMode aMode, PickTarget aPrimaryTarg, PickTarget aSurfaceTarg)
	{
		// Bail if we are are not associated with the PickTarget
		if (refItemManager.getPainterFor(aPrimaryTarg) == null)
			return;

		// Bail if not a valid pick action
		if (PickUtil.isPopupTrigger(aEvent) == false || aMode != PickMode.ActiveSec)
			return;

		// Show the popup
		Component tmpComp = aEvent.getComponent();
		int posX = ((MouseEvent) aEvent).getX();
		int posY = ((MouseEvent) aEvent).getY();
		popupMenu.show(tmpComp, posX, posY);
	}

	/**
	 * Helper method to process the delete action.
	 */
	private void doActionItemDelete()
	{
		// Determine the new list of items
		Set<Dem> fullS = ImmutableSet.copyOf(refItemManager.getAllItems());
		Set<Dem> itemS = refItemManager.getSelectedItems();
		Set<Dem> tmpS = Sets.difference(fullS, itemS);

		// Prompt the user for confirmation
		if (DemGuiUtil.promptDeletionConfirmation(this, refItemManager, itemS) == false)
			return;

		// Update the list of items
		refItemManager.setAllItems(tmpS);

		// Save the changes to the configuration file(s)
		Task tmpTask = new SilentTask();
		saveConfiguration(tmpTask, true, true);

		// Remove the corresponding file from the cache
		for (Dem aItem : itemS)
			DemCacheUtil.removeContentFor(tmpTask, aItem);
	}

	/**
	 * Helper method to process the edit action.
	 */
	private void doActionItemEdit()
	{
		// Lazy init
		if (editPanel == null)
			editPanel = new DemEditPanel(this, refItemManager);

		// Prompt the user for the edits
		editPanel.setItemsToEdit(refItemManager.getSelectedItems());
		editPanel.setVisible(true);
	}

	/**
	 * Helper method to process the load action.
	 */
	private void doActionItemLoad()
	{
		// Prompt the user for the files
		List<String> extL = new ArrayList<>(Arrays.asList("fit", "fits", "obj"));
		File[] fileArr = CustomFileChooser.showOpenDialog(this, "Load DEM(s)", extL, true);
		if (fileArr == null || fileArr.length == 0)
			return;

		// Lazy init
		if (loadPanel == null)
		{
			loadPanel = new FullTaskPanel(this, true, false);
			loadPanel.setSize(800, 300);
			loadPanel.setTabSize(3);
		}
		loadPanel.reset();
		loadPanel.setTitle("DEM files to load: " + fileArr.length);
		loadPanel.setVisible(true);

		// Process all of the files
		int passCnt = 0;
		loadPanel.logRegln("Files to process: " + fileArr.length + "\n");
		List<DemStruct> fullStructL = new ArrayList<>(refItemManager.getAllStructs());
		Map<Dem, DemConfigAttr> attrM = new LinkedHashMap<>();
		for (File aFile : fileArr)
		{
			File tmpCacheDir = workCatalog.getCacheDir();
			DemStruct tmpStruct = DemCacheUtil.copyToCacheAndLoad(loadPanel, tmpCacheDir, aFile);
			if (tmpStruct == null)
				continue;

			// Bail if aborted
			if (loadPanel.isAborted() == true)
				return;

			fullStructL.add(tmpStruct);

			String description = FilenameUtils.removeExtension(aFile.getName());
			DemConfigAttr tmpDCA = DemConfigAttr.Invalid.cloneWithDescription(description);
			attrM.put(tmpStruct.dem, tmpDCA);

			passCnt++;
		}

		loadPanel.setProgress(1.0);
		loadPanel.logRegln("\nFiles successfully added: " + passCnt);
		int failCnt = fileArr.length - passCnt;
		if (failCnt != 0)
			loadPanel.logRegln("\tFailed to add: " + failCnt);
		loadPanel.logRegln("");

		// Bail if nothing was added
		if (passCnt == 0)
			return;

		// Update the list of items (and corresponding ConfigDemAttrs)
		refItemManager.setAllStructs(fullStructL);
		refItemManager.updateConfigAttrMap(attrM);

		// Save the changes to the configuration file(s)
		Task tmpTask = new SilentTask();
		saveConfiguration(tmpTask, true, true);
	}

	/**
	 * Helper method that handles events from the colorExtPanel
	 */
	private void doActionColorExtPanel()
	{
		GroupColorProvider extGCP = colorExtPanel.getGroupColorProviderExterior();
		refItemManager.setGroupColorProviderExterior(extGCP);
	}

	/**
	 * Helper method that handles the radial reset action
	 */
	private void doActionOpacity(List<Dem> aItemL, Object aSource)
	{
		if (aSource == opacityNF && opacityNF.isValidInput() == false)
			return;

		double tmpVal = Double.NaN;
		if (aSource == opacityNF)
			tmpVal = opacityNF.getValue();
		else if (aSource == opacityS)
			tmpVal = opacityS.getModelValue();

		refItemManager.setOpacity(aItemL, tmpVal);
	}

	/**
	 * Helper method that handles the radial reset action
	 */
	private void doActionOpacityReset(List<Dem> aItemL)
	{
		opacityNF.setValue(1.0);
		opacityS.setModelValue(1.0);
		refItemManager.setOpacity(aItemL, 1.0);
	}

	/**
	 * Helper method that handles the radialOffset action
	 */
	private void doActionRadialOffset(List<Dem> aItemL, Object aSource)
	{
		if (aSource == radialNF && radialNF.isValidInput() == false)
			return;

		double tmpVal = Double.NaN;
		if (aSource == radialNF)
			tmpVal = radialNF.getValue();
		else if (aSource == radialS)
			tmpVal = radialS.getModelValue();

		refItemManager.setRadialOffset(aItemL, tmpVal);
	}

	/**
	 * Helper method that handles the radial reset action
	 */
	private void doActionRadialReset(List<Dem> aItemL)
	{
		radialNF.setValue(0.0);
		radialS.setModelValue(0.0);
		refItemManager.setRadialOffset(aItemL, 0.0);
	}

	/**
	 * Helper method to process the change dataset action.
	 */
	private void doActionSwitchToCatalog(DemCatalog aCatalog)
	{
		// Bail if nothing has changed
		if (workCatalog == aCatalog)
			return;
		workCatalog = aCatalog;

		// Retrieve the list of DemStructs
		List<DemStruct> tmpStructL = aCatalog.getStructs();

		// Load the corresponding painter configuration
		Task tmpTask = new ConsoleTask();
		Map<Dem, DemConfigAttr> tmpConfigM = new HashMap<>();

		File catalogFile = aCatalog.getSource().getLocalFile();
		File painterFile = DemCatalogUtil.getConfigFilePainter(catalogFile);
		if (painterFile.isFile() == true)
			tmpConfigM = DemCatalogUtil.loadPainterFromFile(tmpTask, painterFile, tmpStructL);

		// Install the catalog
		refItemManager.setAllStructs(tmpStructL);
		refItemManager.updateConfigAttrMap(tmpConfigM);

		// Prompt the user about (any) previous settings
		prompRestoreSettings();

		// Regardless of the users choice initialization is done
		refItemManager.markInitDone();
	}

	/**
	 * Helper method that forms the configuration options that are placed on the
	 * left side.
	 *
	 * @param aFixedBrowseNameS
	 */
	private JPanel formLeftComp(boolean aIsFixedCatalogMode)
	{
		JPanel retPanel = new JPanel(new MigLayout("", "[]", "0[][]"));

		// FixedCatalogList area
		catalogBox = null;
		if (aIsFixedCatalogMode == false)
		{
			catalogBox = new GComboBox<>(this, refCatalogL);
			catalogBox.setRenderer(new CatalogRenderer());
			retPanel.add(new JLabel("Dataset:"), "span,split");
			retPanel.add(catalogBox, "growx,wrap");

			// Divider
			retPanel.add(GuiUtil.createDivider(), "growx,h 4!,span,wrap");
		}

		// Analyze, Show Dem, Show Boundary
		retPanel.add(anaItemCB, "span,wrap");
		retPanel.add(intItemCB, "span,wrap");
		retPanel.add(extItemCB, "span,growx,wrap");

		// Radial offset area
		double radialMinVal = -150 * radialOffsetScale;
		double radialMaxVal = +150 * radialOffsetScale;
		Range<Double> radialRange = Range.closed(radialMinVal, radialMaxVal);
		radialResetB = GuiUtil.formButton(this, IconUtil.getActionReset());
		radialResetB.setToolTipText(ToolTipUtil.getItemReset());
		radialL1 = new JLabel("Rad.:");
		radialL1.setToolTipText("Radial Offset");
		radialL2 = new JLabel("km");
		radialNF = new GNumberField(this, new DecimalFormat("0.000"), radialRange);
		radialNF.setColumns(6);
		radialS = new GSlider(this, radialRange);
		radialS.setNumSteps(100);
		retPanel.add(GuiUtil.createDivider(), "growx,h 4!,span,wrap");
		retPanel.add(radialResetB, "w 24!,h 24!");
		retPanel.add(radialL1, "span,split");
		retPanel.add(radialNF, "growx,span");
		retPanel.add(radialL2, "wrap");
		retPanel.add(radialS, "growx,span,wrap");

		// Opacity area
		opacityResetB = GuiUtil.formButton(this, IconUtil.getActionReset());
		opacityResetB.setToolTipText(ToolTipUtil.getItemReset());
		opacityL = new JLabel("Opacity:");
		opacityNF = new GNumberField(this, new DecimalFormat("0.00"), OpacityRange);
		opacityNF.setColumns(4);
		opacityS = new GSlider(this, OpacityRange);
		opacityS.setNumSteps(100);
		retPanel.add(GuiUtil.createDivider(), "growx,h 4!,span,wrap");
		retPanel.add(opacityResetB, "w 24!,h 24!");
		retPanel.add(opacityL);
		retPanel.add(opacityNF, "growx,span,wrap");
		retPanel.add(opacityS, "growx,span");

		return retPanel;
	}

	/**
	 * Helper method that forms the configurtion options that are placed on the
	 * left side.
	 */
	private JComponent formRightComp()
	{
		colorExtPanel = new ColorConfigPanel(this);
		colorExtPanel.setActiveMode(ColorMode.Randomize);

		detailsPanel = new PropsPanel();
		statusPanel = new StatusPanel(refItemManager);

		JTabbedPane retPane = new JTabbedPane();
		retPane.add("Bndr Color", colorExtPanel);
		retPane.add("Details", detailsPanel);
		retPane.add("Status", statusPanel);
		retPane.setSelectedIndex(1);
		return retPane;
	}

	/**
	 * Helper method that presents a prompt to determine the settings to be
	 * restored.
	 */
	private void prompRestoreSettings()
	{
		// Determine if there is any state to be "restored"
		Map<Dem, DemConfigAttr> restoreM = new HashMap<>();
		boolean isRestoreAvail = false;
		for (Dem aItem : refItemManager.getAllItems())
		{
			DemConfigAttr tmpDCA = refItemManager.getConfigAttr(aItem);
			restoreM.put(aItem, tmpDCA);

			// Determine if there is state that can be restored
			isRestoreAvail |= tmpDCA.getWindowCfg() != null && tmpDCA.getWindowCfg().getIsShown() == true;
			isRestoreAvail |= tmpDCA.getDrawAttr().getIsExtShown() == true;
			isRestoreAvail |= tmpDCA.getDrawAttr().getIsIntShown() == true;
		}

		// Bail if there is no configuration that can be "restored"
		if (isRestoreAvail == false)
			return;

		// Show and delegate handling to the restore dialag
		RestoreConfigPanel tmpPanel = new RestoreConfigPanel(refItemManager, this);
		tmpPanel.setConfiguration(restoreM);
		tmpPanel.setSize(500, 275);
		tmpPanel.setVisibleAsModal();
	}

	/**
	 * Helper method that will save the dem configuration settings associated
	 * with this panel.
	 */
	private void saveConfiguration(Task aTask, boolean aSaveCatalog, boolean aSavePainter)
	{
		// Do not save the invalid catalog
		if (workCatalog == DemCatalog.Invalid)
			return;

		// Retrieve the list of Dems
		List<DemStruct> fullStructL = refItemManager.getAllStructs();

		// Get the mapping of Dem to DemConfigAttr
		Map<Dem, DemConfigAttr> attrM = new LinkedHashMap<>();
		for (DemStruct aStruct : fullStructL)
		{
			Dem tmpDem = aStruct.dem;
			DemConfigAttr tmpDCA = refItemManager.getConfigAttr(tmpDem);
			attrM.put(tmpDem, tmpDCA);
		}

		// Update the (local) configuration files
		String dispName = workCatalog.getDisplayName();
		File catalogFile = workCatalog.getSource().getLocalFile();
		File painterFile = DemCatalogUtil.getConfigFilePainter(catalogFile);

		if (aSaveCatalog == true)
			DemCatalogUtil.saveCatalogFile(aTask, catalogFile, fullStructL, dispName);

		if (aSavePainter == true)
			DemCatalogUtil.savePainterFile(aTask, painterFile, attrM);
	}

	/**
	 * Helper method that handles the shutdown logic.
	 */
	private void shutdownPanel()
	{
		saveConfiguration(new SilentTask(), false, true);
		refItemManager.shutdown();
	}

	/**
	 * Helper method that updates the various UI elements to keep them
	 * synchronized.
	 */
	private void updateGui()
	{
		// Gather various stats
		int cntFullItems = refItemManager.getAllItems().size();

		Set<Dem> pickS = refItemManager.getSelectedItems();
		Dem pickItem = null;
		if (pickS.size() >= 1)
			pickItem = pickS.iterator().next();
		int cntPickItems = pickS.size();

		int cntItemsReady = 0;
		int cntItemsAna = 0;
		int cntItemsExt = 0;
		int cntItemsInt = 0;
		for (Dem aItem : pickS)
		{
			if (refItemManager.getPainterFor(aItem).isReady() == true)
				cntItemsReady++;
			if (refItemManager.getIsDemAnalyzed(aItem) == true)
				cntItemsAna++;
			if (refItemManager.getIsVisibleExterior(aItem) == true)
				cntItemsExt++;
			if (refItemManager.getIsVisibleInterior(aItem) == true)
				cntItemsInt++;
		}

		double opacity = DemGuiUtil.getUnifiedOpacityFor(refItemManager, pickS);
		opacityNF.setValue(opacity);
		opacityS.setModelValue(opacity);

		double radialOffset = DemGuiUtil.getUnifiedRadialOffsetFor(refItemManager, pickS);
		radialNF.setValue(radialOffset);
		radialS.setModelValue(radialOffset);

		// Update action buttons
		boolean isEnabled, isSelected;

		isEnabled = cntFullItems > 0;
		selectInvertB.setEnabled(isEnabled);

		boolean isEditable = workCatalog.getIsEditable();
		itemAddB.setEnabled(isEditable);

		isEnabled = cntPickItems > 0;
		isEnabled &= isEditable == true;
		itemDelB.setEnabled(isEnabled);

		isEnabled = cntPickItems > 0;
		itemEditB.setEnabled(isEnabled);
		selectNoneB.setEnabled(isEnabled);
		GuiUtil.setEnabled(isEnabled, opacityL, opacityNF, opacityS);
		GuiUtil.setEnabled(isEnabled, radialL1, radialL2, radialNF, radialS);

		isEnabled = cntPickItems > 0;
		isEnabled &= opacity != 1.0;
		opacityResetB.setEnabled(isEnabled);

		isEnabled = cntPickItems > 0;
		isEnabled &= radialOffset != 0.0;
		radialResetB.setEnabled(isEnabled);

		isEnabled = cntFullItems > 0 && cntPickItems < cntFullItems;
		selectAllB.setEnabled(isEnabled);

		isEnabled = cntPickItems > 0;
		anaItemCB.setEnabled(isEnabled);
		intItemCB.setEnabled(isEnabled);
		extItemCB.setEnabled(isEnabled);

		isSelected = cntPickItems == cntItemsAna && cntPickItems > 0;
		anaItemCB.setSelected(isSelected);

		isSelected = cntPickItems == cntItemsExt && cntPickItems > 0;
		extItemCB.setSelected(isSelected);

		isSelected = cntPickItems == cntItemsInt && cntPickItems > 0;
		intItemCB.setSelected(isSelected);

		isEnabled = cntItemsReady == 1;
		actionCenterB.setEnabled(isEnabled);

		// Table title
		DecimalFormat cntFormat = new DecimalFormat("#,###");
		String infoStr = "DEMs: " + cntFormat.format(cntFullItems);
		if (cntPickItems > 0)
			infoStr += "  (Selected: " + cntFormat.format(cntPickItems) + ")";
		titleL.setText(infoStr);

		// Source area
		String srcText = "";
		String srcTips = "";
		if (cntPickItems == 1)
		{
			srcText = pickItem.getSource().getName();
			srcTips = pickItem.getSource().getPath();
		}
		else if (cntPickItems > 1)
		{
			srcText = "Multiple dems selected: " + cntPickItems;
		}
		sourceL.setText(srcText);
		sourceL.setToolTipText(srcTips);
	}

}
