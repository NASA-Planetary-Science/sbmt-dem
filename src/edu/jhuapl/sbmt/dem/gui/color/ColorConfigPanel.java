package edu.jhuapl.sbmt.dem.gui.color;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.jhuapl.saavtk.color.gui.AutoColorPanel;
import edu.jhuapl.saavtk.color.gui.ColorMode;
import edu.jhuapl.saavtk.color.gui.EditGroupColorPanel;
import edu.jhuapl.saavtk.color.gui.RandomizePanel;
import edu.jhuapl.saavtk.color.gui.SimplePanel;
import edu.jhuapl.saavtk.color.provider.GroupColorProvider;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemManager;

import glum.gui.GuiExeUtil;
import glum.gui.component.GComboBox;
import glum.gui.panel.CardPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Panel used to allow a user to configure the {@link DemManager}'s exterior
 * {@link GroupColorProvider}.
 *
 * @author lopeznr1
 */
public class ColorConfigPanel extends JPanel implements ActionListener
{
	// Ref vars
	private final ActionListener refListener;

	// GUI vars
	private CardPanel<EditGroupColorPanel> colorPanel;
	private GComboBox<ColorMode> colorModeBox;

	/** Standard Constructor */
	public ColorConfigPanel(ActionListener aListener)
	{
		refListener = aListener;

		setLayout(new MigLayout("", "0[][]0", "0[][]0"));

		JLabel tmpL = new JLabel("Colorize:");
		ColorMode[] keyArr = { ColorMode.AutoHue, ColorMode.Randomize, ColorMode.Simple };
		colorModeBox = new GComboBox<>(this, keyArr);
		add(tmpL);
		add(colorModeBox, "growx,wrap 2");

		colorPanel = new CardPanel<>();
		colorPanel.addCard(ColorMode.AutoHue, new AutoColorPanel(this));
		colorPanel.addCard(ColorMode.Randomize, new RandomizePanel(this, 0));
		colorPanel.addCard(ColorMode.Simple, new SimplePanel(this, "Boundary:", Color.GREEN));

		add(colorPanel, "growx,growy,span");

		// Custom initialization code
		Runnable tmpRunnable = () -> colorPanel.getActiveCard().activate(true);
		GuiExeUtil.executeOnceWhenShowing(this, tmpRunnable);
	}

	/**
	 * Returns the {@link GroupColorProvider} that should be used to color the
	 * {@link Dem}'s exterior.
	 */
	public GroupColorProvider getGroupColorProviderExterior()
	{
		EditGroupColorPanel tmpPanel = colorPanel.getActiveCard();
		return tmpPanel.getGroupColorProvider();
	}

	/**
	 * Sets the ColorProviderMode which will be active
	 */
	public void setActiveMode(ColorMode aMode)
	{
		colorPanel.getActiveCard().activate(false);

		colorModeBox.setChosenItem(aMode);
		colorPanel.switchToCard(aMode);
		colorPanel.getActiveCard().activate(true);
	}

	@Override
	public void actionPerformed(ActionEvent aEvent)
	{
		Object source = aEvent.getSource();
		if (source == colorModeBox)
			doUpdateColorPanel();

		refListener.actionPerformed(new ActionEvent(this, 0, ""));
	}

	/**
	 * Helper method to properly update the colorPanel.
	 */
	private void doUpdateColorPanel()
	{
		colorPanel.getActiveCard().activate(false);

		ColorMode tmpCM = colorModeBox.getChosenItem();
		colorPanel.switchToCard(tmpCM);
		colorPanel.getActiveCard().activate(true);
	}

}
