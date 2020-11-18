package edu.jhuapl.sbmt.dem.gui;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import edu.jhuapl.sbmt.dem.DemCatalog;

/**
 * {@link ListCellRenderer} used to render the appropriate label for
 * {@link DemCatalog}s.
 *
 * @author lopeznr1
 */
public class CatalogRenderer extends DefaultListCellRenderer
{
	@Override
	public Component getListCellRendererComponent(JList<?> list, Object aObj, int index, boolean isSelected,
			boolean hasFocus)
	{
		JLabel retL = (JLabel) super.getListCellRendererComponent(list, aObj, index, isSelected, hasFocus);

		String tmpLabel = "INVALID";
		if (aObj instanceof DemCatalog)
			tmpLabel = "" + ((DemCatalog) aObj).getDisplayName();

		retL.setText(tmpLabel);
		return retL;
	}

}
