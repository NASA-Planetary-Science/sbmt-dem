package edu.jhuapl.sbmt.dem.vtk;

import edu.jhuapl.saavtk.color.provider.ColorProvider;

/**
 * Immutable object that defines the render attributes associated with an item.
 * <p>
 * The supported attributes are:
 * <ul>
 * <li>Exterior {@link ColorProvider}
 * <li>Exterior visibility
 * <li>Interior {@link ColorProvider}
 * <li>Interior visibility
 * <li>Opacity
 * <li>Radial offset
 * </ul>
 *
 * @author lopeznr1
 */
public class ItemDrawAttr
{
	// Constants
	/** The "default" {@link ItemDrawAttr}. Results in nothing being drawn. */
	public static final ItemDrawAttr Default = new ItemDrawAttr(ColorProvider.Invalid, false, ColorProvider.Invalid,
			false, 1.0, 0.0);

	// Attributes (exterior)
	/** Defines the exterior ColorProvider associated with the item. */
	private final ColorProvider extCP;
	/** Defines if the exterior should be rendered. */
	private final boolean extIsShown;

	// Attributes (interior)
	/** Defines the interior ColorProvider associated with the item. */
	private final ColorProvider intCP;
	/** Defines if the interior should be rendered. */
	private final boolean intIsShown;

	// Attributes (misc)
	/** Defines the item's opacity: [0.0: Transparent, 1.0: Opaque] **/
	private final double opacity;
	/** Defines the item's radial offset. **/
	private final double radialOffset;

	/** Standard Constructor */
	public ItemDrawAttr(ColorProvider aExtCP, boolean aExtIsVisible, ColorProvider aIntCP, boolean aIntIsVisible,
			double aOpacity, double aRadialOffset)
	{
		extCP = aExtCP;
		extIsShown = aExtIsVisible;

		intCP = aIntCP;
		intIsShown = aIntIsVisible;

		opacity = aOpacity;
		radialOffset = aRadialOffset;
	}

	/** Clone object with the custom exterior {@link ColorProvider}. */
	public ItemDrawAttr cloneWithExteriorColorProvider(ColorProvider aTmpCP)
	{
		return new ItemDrawAttr(aTmpCP, extIsShown, intCP, intIsShown, opacity, radialOffset);
	}

	/** Clone object with the custom exterior visible flag. */
	public ItemDrawAttr cloneWithExteriorIsShown(boolean aIsShown)
	{
		return new ItemDrawAttr(extCP, aIsShown, intCP, intIsShown, opacity, radialOffset);
	}

	/** Clone object with the custom interior {@link ColorProvider}. */
	public ItemDrawAttr cloneWithInteriorColorProvider(ColorProvider aTmpCP)
	{
		return new ItemDrawAttr(extCP, extIsShown, aTmpCP, intIsShown, opacity, radialOffset);
	}

	/** Clone object with the custom interior visible flag. */
	public ItemDrawAttr cloneWithInteriorIsShown(boolean aIsShown)
	{
		return new ItemDrawAttr(extCP, extIsShown, intCP, aIsShown, opacity, radialOffset);
	}

	/** Clone object with the custom opacity. */
	public ItemDrawAttr cloneWithOpacity(double aOpacity)
	{
		return new ItemDrawAttr(extCP, extIsShown, intCP, intIsShown, aOpacity, radialOffset);
	}

	/** Clone object with the custom radial offset. */
	public ItemDrawAttr cloneWithRadialOffset(double aRadialOffset)
	{
		return new ItemDrawAttr(extCP, extIsShown, intCP, intIsShown, opacity, aRadialOffset);
	}

	public ColorProvider getExtCP()
	{
		return extCP;
	}

	public boolean getIsExtShown()
	{
		return extIsShown;
	}

	public ColorProvider getIntCP()
	{
		return intCP;
	}

	public boolean getIsIntShown()
	{
		return intIsShown;
	}

	public double getOpacity()
	{
		return opacity;
	}

	public double getRadialOffset()
	{
		return radialOffset;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ItemDrawAttr other = (ItemDrawAttr) obj;
		if (extCP == null)
		{
			if (other.extCP != null)
				return false;
		}
		else if (!extCP.equals(other.extCP))
			return false;
		if (extIsShown != other.extIsShown)
			return false;
		if (intCP == null)
		{
			if (other.intCP != null)
				return false;
		}
		else if (!intCP.equals(other.intCP))
			return false;
		if (intIsShown != other.intIsShown)
			return false;
		if (Double.doubleToLongBits(opacity) != Double.doubleToLongBits(other.opacity))
			return false;
		if (Double.doubleToLongBits(radialOffset) != Double.doubleToLongBits(other.radialOffset))
			return false;
		return true;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((extCP == null) ? 0 : extCP.hashCode());
		result = prime * result + (extIsShown ? 1231 : 1237);
		result = prime * result + ((intCP == null) ? 0 : intCP.hashCode());
		result = prime * result + (intIsShown ? 1231 : 1237);
		long temp;
		temp = Double.doubleToLongBits(opacity);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(radialOffset);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

}
