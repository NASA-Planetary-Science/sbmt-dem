package edu.jhuapl.sbmt.dem.gui.action;

import java.awt.Color;
import java.awt.Component;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import edu.jhuapl.saavtk.util.ColorUtil;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemManager;

import glum.gui.action.PopAction;

/**
 * {@link PopAction} corresponding to the dem (boundary) color menu item. This
 * action does not provide any color changing function but rather delegates to
 * sub actions.
 *
 * @author lopeznr1
 */
public class DemExteriorColorAction extends PopAction<Dem>
{
	// Ref vars
	private final DemManager refManager;

	// State vars
	private Map<JMenuItem, PopAction<Dem>> actionM;

	/** Standard Constructor */
	public DemExteriorColorAction(DemManager aManager, Component aParent, JMenu aMenu)
	{
		refManager = aManager;

		actionM = new HashMap<>();

		// Form the static color menu items
		for (ColorUtil.DefaultColor color : ColorUtil.DefaultColor.values())
		{
			PopAction<Dem> tmpLPA = new FixedDemColorAction(aManager, color.color());
			JCheckBoxMenuItem tmpColorMI = new JCheckBoxMenuItem(tmpLPA);
			tmpColorMI.setText(color.toString().toLowerCase().replace('_', ' '));
			actionM.put(tmpColorMI, tmpLPA);

			aMenu.add(tmpColorMI);
		}
		aMenu.addSeparator();

		JMenuItem customColorMI = formMenuItem(new CustomDemExteriorColorAction(aManager, aParent), "Custom...");
		aMenu.add(customColorMI);
		aMenu.addSeparator();

		JMenuItem resetColorMI = formMenuItem(new ResetExteriorColorAction(aManager), "Reset");
		aMenu.add(resetColorMI);
	}

	@Override
	public void executeAction(List<Dem> aItemL)
	{
		; // Nothing to do
	}

	@Override
	public void setChosenItems(Collection<Dem> aItemC, JMenuItem aAssocMI)
	{
		super.setChosenItems(aItemC, aAssocMI);

		// Determine if all selected items have the same (custom) color
		Color initColor = refManager.getColorProviderExterior(aItemC.iterator().next()).getBaseColor();
		boolean isSameCustomColor = true;
		for (Dem aItem : aItemC)
		{
			Color evalColor = refManager.getColorProviderExterior(aItem).getBaseColor();
			isSameCustomColor &= Objects.equals(initColor, evalColor) == true;
			isSameCustomColor &= refManager.hasCustomExteriorColorProvider(aItem) == true;
		}

		// Update our child LidarPopActions
		for (JMenuItem aMI : actionM.keySet())
		{
			PopAction<Dem> tmpLPA = actionM.get(aMI);
			tmpLPA.setChosenItems(aItemC, aMI);

			// If all items have the same custom color and match one of the
			// predefined colors then update the corresponding menu item.
			if (tmpLPA instanceof FixedDemColorAction)
			{
				boolean isSelected = isSameCustomColor == true;
				isSelected &= ((FixedDemColorAction) tmpLPA).getColor().equals(initColor) == true;
				aMI.setSelected(isSelected);
			}
		}
	}

	/**
	 * Helper method to form and return the specified menu item.
	 * <p>
	 * The menu item will be registered into the action map.
	 *
	 * @param aAction Action corresponding to the menu item.
	 * @param aTitle The title of the menu item.
	 */
	private JMenuItem formMenuItem(PopAction<Dem> aAction, String aTitle)
	{
		JMenuItem retMI = new JMenuItem(aAction);
		retMI.setText(aTitle);

		actionM.put(retMI, aAction);

		return retMI;
	}

}
