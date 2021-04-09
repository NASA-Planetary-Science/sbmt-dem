package edu.jhuapl.sbmt.dem.gui.popup;

import java.awt.Component;
import java.util.Collection;
import java.util.List;

import javax.swing.JMenuItem;

import edu.jhuapl.saavtk.gui.util.MessageUtil;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemManager;
import edu.jhuapl.sbmt.dem.gui.DemEditPanel;

import glum.gui.action.PopAction;

/**
 * Action used to edit the specified {@link Dem}s.
 *
 * @author lopeznr1
 */
public class EditAction extends PopAction<Dem>
{
	// Ref vars
	private final DemManager refManager;
	private final Component refParent;

	// Gui vars
	private DemEditPanel editPanel;

	/** Standard Constructor */
	public EditAction(DemManager aManager, Component aParent)
	{
		refManager = aManager;
		refParent = aParent;

		editPanel = null;
	}

	@Override
	public void executeAction(List<Dem> aItemL)
	{
		// Lazy init
		if (editPanel == null)
			editPanel = new DemEditPanel(refParent, refManager);

		// Prompt the user for the edits
		editPanel.setItemsToEdit(aItemL);
		editPanel.setVisible(true);
	}

	@Override
	public void setChosenItems(Collection<Dem> aItemC, JMenuItem aAssocMI)
	{
		super.setChosenItems(aItemC, aAssocMI);

		boolean isEnabled = aItemC.size() > 0;
		aAssocMI.setEnabled(isEnabled);

		// Determine the display string
		String displayStr = "Edit DTM";
		displayStr = MessageUtil.toPluralForm(displayStr, aItemC);

		// Update the text of the associated MenuItem
		aAssocMI.setText(displayStr);
	}

}
