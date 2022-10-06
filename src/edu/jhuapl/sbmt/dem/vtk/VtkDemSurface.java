package edu.jhuapl.sbmt.dem.vtk;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;

import vtk.vtkFloatArray;
import vtk.vtkLookupTable;
import vtk.vtkMapper;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProp;
import vtk.vtkUnsignedCharArray;

import edu.jhuapl.saavtk.color.provider.ColorBarColorProvider;
import edu.jhuapl.saavtk.color.provider.ColorProvider;
import edu.jhuapl.saavtk.color.table.ColorMapAttr;
import edu.jhuapl.saavtk.color.table.ColorTableUtil;
import edu.jhuapl.saavtk.feature.FeatureType;
import edu.jhuapl.saavtk.gui.render.VtkPropProvider;
import edu.jhuapl.saavtk.model.GenericPolyhedralModel;
import edu.jhuapl.saavtk.model.PolyModelUtil;
import edu.jhuapl.saavtk.view.lod.LodMode;
import edu.jhuapl.saavtk.view.lod.LodUtil;
import edu.jhuapl.saavtk.view.lod.VtkLodActor;
import edu.jhuapl.saavtk.vtk.VtkResource;
import edu.jhuapl.saavtk.vtk.VtkUtil;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemException;

/**
 * Class used to render a single dem surface via the VTK framework.
 * <p>
 * This class supports the following features:
 * <ul>
 * <li>Configuration of exterior via {@link ItemDrawAttr}
 * <li>Configuration of interior via {@link ItemDrawAttr}
 * <li>Configuration of opacity and radial offset via {@link ItemDrawAttr}
 * <li>Support of interior {@link FeatureType} configuration
 * </ul>
 *
 * @author lopeznr1
 */
public class VtkDemSurface extends GenericPolyhedralModel implements VtkResource, VtkPropProvider
{
	// Ref vars
	private final Dem refDem;

	// Attributes
	private final ImmutableList<FeatureType> featureTypeL;
	private final DataMode viewDataMode;

	// State vars
	private ItemDrawAttr currDA;
	private ItemDrawAttr prevDA;

	// Cache vars
	private Vector3D cAverageSurfaceNormal;
	private Vector3D cGeometricCenterPoint;
	private ColorMapAttr cColorMapAttr;
	private vtkFloatArray cValueFA;

	// VTK vars
	private final ImmutableMap<FeatureType, vtkFloatArray> vValuesPerCellM;
	private final ImmutableMap<FeatureType, vtkFloatArray> vValuesPerPointM;

	private final VtkLodActor vExteriorA;
	private final vtkPolyData vExteriorPD;
	private final VtkLodActor vInteriorA;
	private final vtkPolyData vInteriorPD;
	private final vtkLookupTable vColorLT;

	/** Standard Constructor */
	public VtkDemSurface(Dem aDem, VtkDemStruct aStruct)
	{
		super(aDem.getSource().getPath());

		refDem = aDem;

		featureTypeL = ImmutableList.copyOf(aStruct.featureTypeL);
		viewDataMode = aStruct.viewDataMode;

		currDA = ItemDrawAttr.Default;
		prevDA = ItemDrawAttr.Default;

		cAverageSurfaceNormal = null;
		cGeometricCenterPoint = aStruct.centerOfDEM;
		cColorMapAttr = null;
		cValueFA = null;

		vValuesPerCellM = aStruct.vValuesPerCellM;
		vValuesPerPointM = aStruct.vValuesPerPointM;

		vExteriorA = new VtkLodActor(this);
		vExteriorPD = aStruct.vExteriorPD;
		vtkPolyDataMapper vExteriorPDM = new vtkPolyDataMapper();
		vExteriorPDM.SetInputData(vExteriorPD);
		vExteriorA.setDefaultMapper(vExteriorPDM);

		setSmallBodyPolyData(aStruct.vInteriorPD, new vtkFloatArray[0], new String[0], new String[0],
				ColoringValueType.CELLDATA);

		// Force actors to be initialized
		initializeActorsAndMappers();

		vInteriorA = (VtkLodActor) getSmallBodyActor();
		vInteriorPD = getSmallBodyPolyData();

		vColorLT = new vtkLookupTable();
	}

	/**
	 * Returns the reference {@link Dem}.
	 */
	public Dem getDem()
	{
		return refDem;
	}

	/**
	 * Returns the current {@link ItemDrawAttr}.
	 */
	public ItemDrawAttr getDrawAttr()
	{
		return currDA;
	}

	/**
	 * Returns the list of available {@link FeatureType}s.
	 */
	public ImmutableList<FeatureType> getFeatureTypeList()
	{
		return featureTypeL;
	}

	/**
	 * Returns the range of values that are associated with the specified
	 * {@link FeatureType}.
	 * <p>
	 * If this surface does not support the specified FeatureType then null will
	 * be returned.
	 */
	public Range<Double> getValueRangeFor(FeatureType aFeatureType)
	{
		vtkFloatArray vTmpFA = vValuesPerCellM.get(aFeatureType);
		if (vTmpFA == null)
			return null;

		float[] tmpRangeArr = vTmpFA.GetValueRange();
		if (tmpRangeArr[0] > tmpRangeArr[1])
			return Range.singleton(Double.NaN);

		return Range.closed((double)tmpRangeArr[0], (double)tmpRangeArr[1]);
	}

	/**
	 * Returns the (cell) values ({@link vtkFloatArray}) associated with the
	 * specified {@link FeatureType}.
	 * <p>
	 * Note any changes to the returned structure will have an effect on this
	 * surface. The returned value should be handled in a read only manner.
	 */
	public vtkFloatArray getValuesForCellData(FeatureType aFeatureType)
	{
		return vValuesPerCellM.get(aFeatureType);
	}

	/**
	 * Returns the (point) values ({@link vtkFloatArray}) associated with the
	 * specified {@link FeatureType}.
	 * <p>
	 * Note any changes to the returned structure will have an effect on this
	 * surface. The returned value should be handled in a read only manner.
	 */
	public vtkFloatArray getValuesForPointData(FeatureType aFeatureType)
	{
		return vValuesPerPointM.get(aFeatureType);
	}

	/**
	 * Returns the {@link vtkPolyData} associated with the exterior of this
	 * {@link VtkDemSurface}. This is effectively the border.
	 */
	public vtkPolyData getVtkExteriorPD()
	{
		return vExteriorPD;
	}

	/**
	 * Returns the {@link vtkPolyData} associated with the interior of this
	 * {@link VtkDemSurface}.
	 */
	public vtkPolyData getVtkInteriorPD()
	{
		return vInteriorPD;
	}

	/**
	 * Updates the {@link VtkDemSurface} with the specified {@link ItemDrawAttr}.
	 */
	public void setDrawAttr(ItemDrawAttr aTmpDA)
	{
		currDA = aTmpDA;
		vtkUpdateState();
	}

	@Override
	public Vector3D getAverageSurfaceNormal()
	{
		// Return the cached value
		if (cAverageSurfaceNormal != null)
			return cAverageSurfaceNormal;

		cAverageSurfaceNormal = PolyModelUtil.calcSurfaceNormal(this);
		return cAverageSurfaceNormal;
	}

	@Override
	public Vector3D getGeometricCenterPoint()
	{
		// Return the cached value
		if (cGeometricCenterPoint != null)
			return cGeometricCenterPoint;

		cGeometricCenterPoint = PolyModelUtil.calcCenterPoint(this);
		return cGeometricCenterPoint;
	}

	@Override
	public List<vtkProp> getProps()
	{
		List<vtkProp> retItemL = new ArrayList<>();

		boolean isUsed = currDA.getIsIntShown() == true;
		if (isUsed == true)
			retItemL.add(vInteriorA);

		isUsed = currDA.getIsExtShown() == true;
		isUsed &= currDA.getExtCP() != ColorProvider.Invalid;
		if (isUsed == true)
			retItemL.add(vExteriorA);

		return retItemL;
	}

	@Override
	public void vtkDispose()
	{
		VtkUtil.deleteAll(vValuesPerCellM.values());
		VtkUtil.deleteAll(vValuesPerPointM.values());

		vExteriorA.Delete();
		vExteriorPD.Delete();
		vInteriorA.Delete();
		vInteriorPD.Delete();
		vColorLT.Delete();
	}

	@Override
	public void vtkUpdateState()
	{
		boolean isStale;

		// Update color state
		ColorProvider tmpIntCP = currDA.getIntCP();
		FeatureType tmpFeatureType = tmpIntCP.getFeatureType();
		vtkFloatArray vTmpValueFA = vValuesPerCellM.get(tmpFeatureType);

		ColorMapAttr tmpColorMapAttr = null;
		if (tmpIntCP instanceof ColorBarColorProvider)
			tmpColorMapAttr = ((ColorBarColorProvider) tmpIntCP).getColorMapAttr();
		isStale = Objects.equals(cColorMapAttr, tmpColorMapAttr) == false;
		isStale |= cValueFA != vTmpValueFA;
		if (isStale == true)
			doUpdateColorState(tmpFeatureType, vTmpValueFA, tmpColorMapAttr);

		// Update various other state
		double tmpRadialOffset = currDA.getRadialOffset();
		isStale = prevDA.getRadialOffset() != tmpRadialOffset;
		if (isStale == true)
			doUpdateRadialoffset(tmpRadialOffset);

		double tmpOpacity = currDA.getOpacity();
		isStale = prevDA.getOpacity() != tmpOpacity;
		if (isStale == true)
			doUpdateOpacity(tmpOpacity);

		ColorProvider tmpExtCP = currDA.getExtCP();
		isStale = Objects.equals(prevDA.getExtCP(), tmpExtCP) == false;
		if (isStale == true)
			doUpdateExterior(tmpExtCP);

		prevDA = currDA;
	}

	@Override
	public void delete()
	{
		throw new DemException("LogicError: Method should never be called...");
	}

	@Override
	public void setOpacity(double aOpacity)
	{
		throw new DemException("LogicError: Method should never be called...");
	}

	@Override
	public void setVisible(boolean b)
	{
		throw new DemException("LogicError: Method should never be called...");
	}

	/**
	 * Helper method to update the VTK state associated with the color of the
	 * dem's interior.
	 */
	private void doUpdateColorState(FeatureType aFeatureType, vtkFloatArray aValueFA, ColorMapAttr aColorMapAttr)
	{
		// Update the cache
		cValueFA = aValueFA;
		cColorMapAttr = aColorMapAttr;

		// Clear out the colorization and bail
		vtkMapper vInteriorM = vInteriorA.GetMapper();
		if (aFeatureType == FeatureType.Invalid || aValueFA == null || aColorMapAttr == null)
		{
			vtkPolyDataMapper tmpDecimatedPDM = LodUtil.createQuadricDecimatedMapper(vInteriorPD);
			vInteriorA.setLodMapper(LodMode.MaxSpeed, tmpDecimatedPDM);
			tmpDecimatedPDM.ScalarVisibilityOff();
			vInteriorM.ScalarVisibilityOff();

			vInteriorPD.Modified();
			return;
		}

		// Update the lookup table to reflect the color map
		ColorTableUtil.updateLookUpTable(vColorLT, aColorMapAttr);
		vInteriorM.SetLookupTable(vColorLT);
		vInteriorM.ScalarVisibilityOn();

		// Update the surface color
		Color nanColor = aColorMapAttr.getColorTable().getNanColor();
		vtkUnsignedCharArray vTmpColorUCA = new vtkUnsignedCharArray();
		vTmpColorUCA.SetNumberOfComponents(3);
		for (int aIdx = 0; aIdx < aValueFA.GetNumberOfTuples(); ++aIdx)
		{
			double value = aValueFA.GetValue(aIdx);

			Color tmpColor = nanColor;
			if (Double.isNaN(value) == false && Double.isFinite(value) == true)
			{
				double[] colorArr = vColorLT.GetColor(value);
				tmpColor = new Color((float) colorArr[0], (float) colorArr[1], (float) colorArr[2]);
			}

			vTmpColorUCA.InsertNextTuple3(tmpColor.getRed(), tmpColor.getGreen(), tmpColor.getBlue());
		}

		vtkPolyDataMapper tmpDecimatedPDM = LodUtil.createQuadricDecimatedMapper(vInteriorPD);
		vInteriorA.setLodMapper(LodMode.MaxSpeed, tmpDecimatedPDM);
		tmpDecimatedPDM.SetLookupTable(vColorLT);
		tmpDecimatedPDM.UseLookupTableScalarRangeOn();

		vInteriorPD.GetCellData().SetScalars(vTmpColorUCA);
		vInteriorPD.Modified();
	}

	/**
	 * Helper method to update the VTK state associated with the interior of the
	 * dem data.
	 */
	private void doUpdateExterior(ColorProvider aExteriorCP)
	{
		// Bail if no valid color
		Color tmpColor = aExteriorCP.getColor(0.0, 1.0, 0.5);
		if (tmpColor == null)
			return;

		vExteriorA.GetProperty().SetColor(tmpColor.getRed() / 255.0, tmpColor.getGreen() / 255.0,
				tmpColor.getBlue() / 255.0);
		vExteriorA.GetProperty().SetLineWidth(1.0f);
	}

	/**
	 * Helper method to update the VTK state associated with the opacity.
	 */
	public void doUpdateOpacity(double aOpacity)
	{
		vInteriorA.GetProperty().SetOpacity(aOpacity);
	}

	/**
	 * Helper method to update the VTK state associated with the radial offset.
	 */
	private void doUpdateRadialoffset(double aRadialOffset)
	{
		Vector3D tmpNorm = getAverageSurfaceNormal();
		Vector3D tmpPos = tmpNorm.scalarMultiply(aRadialOffset);

		vInteriorA.SetPosition(tmpPos.toArray());
		vExteriorA.SetPosition(tmpPos.toArray());
	}

	/**
	 * Utility method to form a clone of this {@link VtkDemSurface}.
	 * <p>
	 * The returned clone will have independent VTK state.
	 */
	public static VtkDemSurface formClone(VtkDemSurface aVDS)
	{
		vtkPolyData tmpInteriorPD = new vtkPolyData();
		tmpInteriorPD.DeepCopy(aVDS.vInteriorPD);

		vtkPolyData tmpExteriorPD = null;
		if (aVDS.vExteriorPD != null)
		{
			tmpExteriorPD = new vtkPolyData();
			tmpExteriorPD.DeepCopy(aVDS.vExteriorPD);
		}

		Map<FeatureType, vtkFloatArray> tmpColoringValuesPerCellM = new HashMap<>();
		for (FeatureType aKey : aVDS.vValuesPerCellM.keySet())
		{
			vtkFloatArray origFA = aVDS.vValuesPerCellM.get(aKey);
			if (origFA == null)
				continue;

			vtkFloatArray tmpFA = new vtkFloatArray();
			tmpFA.DeepCopy(origFA);
			tmpColoringValuesPerCellM.put(aKey, tmpFA);
		}

		Map<FeatureType, vtkFloatArray> tmpColoringValuesPerPointM = new HashMap<>();
		for (FeatureType aKey : aVDS.vValuesPerPointM.keySet())
		{
			vtkFloatArray origFA = aVDS.vValuesPerPointM.get(aKey);
			if (origFA == null)
				continue;

			vtkFloatArray tmpFA = new vtkFloatArray();
			tmpFA.DeepCopy(origFA);
			tmpColoringValuesPerPointM.put(aKey, tmpFA);
		}

		VtkDemStruct tmpStruct = new VtkDemStruct(aVDS.cGeometricCenterPoint, ImmutableMap.of(), aVDS.featureTypeL,
				tmpColoringValuesPerCellM, tmpColoringValuesPerPointM, tmpInteriorPD, tmpExteriorPD, aVDS.viewDataMode);

		VtkDemSurface retVDS = new VtkDemSurface(aVDS.refDem, tmpStruct);
		return retVDS;
	}

}
