package edu.jhuapl.sbmt.dem.vtk;

import java.util.List;
import java.util.Map;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import vtk.vtkFloatArray;
import vtk.vtkPolyData;

import edu.jhuapl.saavtk.feature.FeatureType;
import edu.jhuapl.sbmt.core.util.KeyValueNode;

/**
 * Intermediate object used to hold VTK state associated with dem data.
 * <p>
 * While this class is immutable - the field members are not! This class is
 * intended to be used as intermediate staging step while instantiating dem
 * data.
 * <p>
 * The object that creates this intermediate struct is responsible for
 * management of the life cycle of the underlying objects.
 *
 * @author lopeznr1
 */
public class VtkDemStruct
{
	public final Vector3D centerOfDEM;
	public final Map<String, KeyValueNode> keyValueM;

	public final List<FeatureType> featureTypeL;

	public final ImmutableMap<FeatureType, vtkFloatArray> vValuesPerCellM;
	public final ImmutableMap<FeatureType, vtkFloatArray> vValuesPerPointM;
	public final vtkPolyData vInteriorPD;
	public final vtkPolyData vExteriorPD;

	public final DataMode viewDataMode;

	/** Standard Constructor */
	public VtkDemStruct(Vector3D aCenterOfDEM, Map<String, KeyValueNode> aKeyValueM, List<FeatureType> aFeatureTypeL,
			Map<FeatureType, vtkFloatArray> aValuesPerCellM, Map<FeatureType, vtkFloatArray> aValuesPerPointM,
			vtkPolyData aInteriorPD, vtkPolyData aExteriorPD, DataMode aViewDataMode)
	{
		centerOfDEM = aCenterOfDEM;
		keyValueM = aKeyValueM;

		featureTypeL = ImmutableList.copyOf(aFeatureTypeL);

		vValuesPerCellM = ImmutableMap.copyOf(aValuesPerCellM);
		vValuesPerPointM = ImmutableMap.copyOf(aValuesPerPointM);
		vInteriorPD = aInteriorPD;
		vExteriorPD = aExteriorPD;

		viewDataMode = aViewDataMode;
	}

}
