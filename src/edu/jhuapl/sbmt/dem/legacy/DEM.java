package edu.jhuapl.sbmt.dem.legacy;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import vtk.vtkCellArray;
import vtk.vtkDataArray;
import vtk.vtkFloatArray;
import vtk.vtkGenericCell;
import vtk.vtkIdList;
import vtk.vtkPointDataToCellData;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataNormals;
import vtk.vtkProp;
import vtk.vtksbCellLocator;

import edu.jhuapl.saavtk.model.PolyModelUtil;
import edu.jhuapl.saavtk.util.MathUtil;
import edu.jhuapl.saavtk.util.PolyDataUtil;
import edu.jhuapl.saavtk.util.ProgressListener;
import edu.jhuapl.saavtk.util.Properties;
import edu.jhuapl.saavtk2.geom.Geometry;
import edu.jhuapl.saavtk2.io.ObjGeometryReader;
import edu.jhuapl.sbmt.core.body.SmallBodyModel;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;

public class DEM extends SmallBodyModel implements PropertyChangeListener
{
    public static final String DEM_NAMES = "DemNames"; // What name to give this DEM for display
    public static final String DEM_FILENAMES = "DemFilenames"; // Filename of DEM on disk
    public static final String DEM_MAP_PATHS = "DemMapPaths"; // For backwards compatibility, still read this in
    public static final String HALFSIZE = "HalfSize";
    private static final float INVALID_VALUE = -1.0e38f;
    private vtkPolyData dem;
    private vtkPolyData boundary;
    private vtkFloatArray[] coloringValuesPerCell;
    private vtkFloatArray[] coloringValuesPerPoint;
    private float[] coloringValuesScale;
    private String[] coloringNames;
    private String[] coloringUnits;
    private double[] centerOfDEM = null;
    private double[] normalOfDEM = null;
    private vtksbCellLocator boundaryLocator;
    private vtkGenericCell genericCell;
//    private DEMView demView;

    private DEMLoadingTask task;
    private ProgressMonitor demLoadingProgressMonitor;

    /** Class DEM **/
    // Attributes
    protected final DEMKey key;
    private double latitude;
    private double longitude;
    private double halfSize;
    private double scale;

    // Cache vars
    private Vector3D cAverageSurfaceNormal;
    private Vector3D cGeometricCenterPoint;

    // Old constructor based on filename only, called all over SBMT
    public DEM(String filename) //throws IOException, FitsException
    {
        this(new DEMKey(filename,filename, false));
    }

    // Copy constructor
    public DEM(DEM copyDEM)
    {
        super(copyDEM.getKey().demfilename);
        if(copyDEM.dem != null)
        {
            dem = new vtkPolyData();
            dem.DeepCopy(copyDEM.dem);
        }
        if(copyDEM.boundary != null)
        {
            boundary = new vtkPolyData();
            boundary.DeepCopy(copyDEM.boundary);
        }
        if(copyDEM.coloringValuesPerCell != null)
        {
            coloringValuesPerCell = new vtkFloatArray[copyDEM.coloringValuesPerCell.length];
            for(int i=0; i<coloringValuesPerCell.length; i++)
            {
                if(copyDEM.coloringValuesPerCell[i] != null)
                {
                    coloringValuesPerCell[i] = new vtkFloatArray();
                    coloringValuesPerCell[i].DeepCopy(copyDEM.coloringValuesPerCell[i]);
                }
            }
        }
        if(copyDEM.coloringValuesPerPoint != null)
        {
            coloringValuesPerPoint = new vtkFloatArray[copyDEM.coloringValuesPerPoint.length];
            for(int i=0; i<coloringValuesPerPoint.length; i++)
            {
                if(copyDEM.coloringValuesPerPoint[i] != null)
                {
                    coloringValuesPerPoint[i] = new vtkFloatArray();
                    coloringValuesPerPoint[i].DeepCopy(copyDEM.coloringValuesPerPoint[i]);
                }
            }
        }
        coloringValuesScale = (copyDEM.coloringValuesScale == null) ? null : copyDEM.coloringValuesScale.clone();
        coloringNames = (copyDEM.coloringNames == null) ? null : copyDEM.coloringNames.clone();
        coloringUnits = (copyDEM.coloringUnits == null) ? null : copyDEM.coloringUnits.clone();
        centerOfDEM = (copyDEM.centerOfDEM == null) ? null : copyDEM.centerOfDEM.clone();
        normalOfDEM = (copyDEM.normalOfDEM == null) ? null : copyDEM.normalOfDEM.clone();
        boundaryLocator = null; // Ok to leave this null, it gets constructed when used
        if(copyDEM.genericCell != null)
        {
            genericCell = new vtkGenericCell();
            genericCell.DeepCopy(copyDEM.genericCell);
        }
        key = new DEMKey(copyDEM.key);

        setSmallBodyPolyData(dem, coloringValuesPerCell, coloringNames, coloringUnits, ColoringValueType.CELLDATA);

        cAverageSurfaceNormal = null;
        cGeometricCenterPoint = null;
    }


    // New constructor making use of key
    public DEM(DEMKey key) //throws IOException
    {
        super(key.demfilename);
        // Store the key for future use
        this.key = key;

        // Initialize data structures
        dem = new vtkPolyData();
        boundary = new vtkPolyData();

        cAverageSurfaceNormal = null;
        cGeometricCenterPoint = null;

        demLoadingProgressMonitor = new ProgressMonitor(null, "Loading DTM...", "", 0, 100);
        demLoadingProgressMonitor.setProgress(0);

        task = new DEMLoadingTask(key);
        task.addPropertyChangeListener(this);
        task.execute();
    }

    class DEMLoadingTask extends SwingWorker<Void, Void>
    {
    	DEMKey key;

    	public DEMLoadingTask(DEMKey key)
    	{
    		this.key = key;
    	}

    	@Override
    	protected Void doInBackground() throws Exception
    	{
    		if (FilenameUtils.isExtension(key.demfilename.toLowerCase(), new String[]{"fit","fits"}))
    		{
                fromFits(key.demfilename, new ProgressListener()
        		{
					@Override
					public void setProgress(int progress)
					{
						task.setProgress(progress);
					}

        		});	//set a listener here to fire back progress events to the progress monitor
    		}
            else if (FilenameUtils.isExtension(key.demfilename.toLowerCase(), "obj"))
            {
                fromObj(key.demfilename);
            }
    		return null;
    	}

    	@Override
    	protected void done()
    	{
    		// TODO Auto-generated method stub
    		super.done();
    		try
    		{
    			get();
                setSmallBodyPolyData(dem, coloringValuesPerCell, coloringNames, coloringUnits, ColoringValueType.CELLDATA);
    		}
    		catch (ExecutionException e)
    		{
    			e.getCause().printStackTrace();
    			//TODO I'd really like to get this out of the class and have it passed in, since it is UI related.  Runnable completion block is a possibility, but need a good place to put it
                String msg = String.format("Problem Loading DEM: %s",
                               e.getCause().toString());
                JOptionPane.showMessageDialog(null,
                    msg, "Error", JOptionPane.ERROR_MESSAGE);
    		}
    		catch (InterruptedException e)
    		{
    			//process a thread interruption here
    		}

    	}

    }


    // Get method for key
    public DEMKey getKey()
    {
        return key;
    }

    public static void colorDEM(String filename, SmallBodyModel smallBodyModel) throws IOException, FitsException
    {
        Fits f = new Fits(filename);
        BasicHDU hdu = f.getHDU(0);

        // First pass, figure out number of planes and grab size and scale information
        Header header = hdu.getHeader();
        HeaderCard headerCard;
        List<Integer> backPlaneIndices = new ArrayList<Integer>();
        List<String> unprocessedBackPlaneNames = new ArrayList<String>();
        List<String> unprocessedBackPlaneUnits = new ArrayList<String>();
        int xIdx = -1;
        int yIdx = -1;
        int zIdx = -1;
        int planeCount = 0;
        while((headerCard = header.nextCard()) != null)
        {
            String headerKey = headerCard.getKey();
            String headerValue = headerCard.getValue();
            String headerComment = headerCard.getComment();

            if(headerKey.startsWith("PLANE"))
            {
                // Determine if we are looking at a coordinate or a backplane
                if(headerValue.startsWith("X"))
                {
                    // This plane is the X coordinate, save the index
                    xIdx = planeCount;
                }
                else if(headerValue.startsWith("Y"))
                {
                    // This plane is the Y coordinate, save the index
                    yIdx = planeCount;
                }
                else if(headerValue.startsWith("Z"))
                {
                    // This plane is the Z coordinate, save the index
                    zIdx = planeCount;
                }
                else
                {
                    // We are looking at a backplane, save the index in order of appearance
                    backPlaneIndices.add(planeCount);

                    // Try to break the value into name and unit components
                    String[] valueSplitResults = headerValue.split("[\\(\\[\\)\\]]");
                    String planeName = valueSplitResults[0];
                    String planeUnits = "";
                    if(valueSplitResults.length > 1)
                    {
                        planeUnits = valueSplitResults[1];
                    }
                    else if(headerComment != null)
                    {
                        // Couldn't find units in the value, try looking in comments instead
                        String[] commentSplitResults = headerComment.split("[\\(\\[\\)\\]]");
                        if(commentSplitResults.length > 1)
                        {
                            planeUnits = commentSplitResults[1];
                        }
                    }
                    unprocessedBackPlaneNames.add(planeName);
                    unprocessedBackPlaneUnits.add(planeUnits);
                }

                // Increment plane count
                planeCount++;
            }
        }

        // Define arrays now that we know the number of backplanes
        int numBackPlanes = backPlaneIndices.size();
        vtkFloatArray[] coloringValuesPerCell = new vtkFloatArray[numBackPlanes];
        vtkFloatArray[] coloringValuesPerPoint = new vtkFloatArray[numBackPlanes];
        String[] coloringNames = new String[numBackPlanes];
        String[] coloringUnits = new String[numBackPlanes];
        float[] coloringValuesScale = new float[numBackPlanes];
        int[] backPlaneIdx = new int[numBackPlanes];

        // Go through each backplane
        for(int i=0; i<numBackPlanes; i++)
        {
            // Get the name, unit, and scale factor to use
            setBackplaneInfo(i, unprocessedBackPlaneNames.get(i), unprocessedBackPlaneUnits.get(i),
                    coloringNames, coloringUnits, coloringValuesScale);

            // Set number of components for each vtkFloatArray to 1
            coloringValuesPerCell[i] = new vtkFloatArray();
            coloringValuesPerCell[i].SetNumberOfComponents(1);
            coloringValuesPerPoint[i] = new vtkFloatArray();
            coloringValuesPerPoint[i].SetNumberOfComponents(1);

            // Copy List element to array for faster lookup later
            backPlaneIdx[i] = backPlaneIndices.get(i);
        }

        // Check dimensions of actual data
        final int NUM_PLANES = numBackPlanes + 3;
        int[] axes = hdu.getAxes();
//        if (axes.length != 3 || axes[0] != NUM_PLANES || axes[1] != axes[2])
//        {
//            throw new IOException("FITS file has incorrect dimensions");
//        }

        int liveSize = axes[1];
        int liveSize2 = axes[2];

        float[][][] data = (float[][][])hdu.getData().getData();
        f.getStream().close();

        int[][] indices = new int[liveSize][liveSize2];
        int c = 0;
        float x, y, z;
        float d;

        // First add points to the vtkPoints array
        for (int m=0; m<liveSize; ++m)
            for (int n=0; n<liveSize2; ++n)
            {
                indices[m][n] = -1;

                // A pixel value of -1.0e38 means that pixel is invalid and should be skipped
                x = data[xIdx][m][n];
                y = data[yIdx][m][n];
                z = data[zIdx][m][n];

                // Check to see if x,y,z values are all valid
                boolean valid = x != INVALID_VALUE && y != INVALID_VALUE && z != INVALID_VALUE;

                // Check to see if data for all backplanes are also valid
                for(int i=0; i<numBackPlanes; i++)
                {
                    d = data[backPlaneIdx[i]][m][n];
                    valid = (valid && d != INVALID_VALUE);
                }

                // Only add point if everything is valid
                if (valid)
                {
                    for(int i=0; i<numBackPlanes; i++)
                    {
                        d = data[backPlaneIdx[i]][m][n] * coloringValuesScale[i];
                        coloringValuesPerCell[i].InsertNextTuple1(d);
                    }

                    indices[m][n] = c;
                    ++c;
                }
            }

        convertPointDataToCellData(smallBodyModel.getSmallBodyPolyData(), coloringValuesPerCell);

        // Apply colors to the small body model
        smallBodyModel.setSmallBodyPolyData(null,
                coloringValuesPerCell, coloringNames,
                coloringUnits, ColoringValueType.CELLDATA);
    }

    protected vtkPolyData fromObj(String filename)
    {
        ObjGeometryReader reader=new ObjGeometryReader(Paths.get(filename));
        Geometry geom=reader.get();
        dem=geom.getPolyData();
        centerOfDEM=dem.GetCenter();
        int numBackPlanes = 0;
        coloringValuesPerCell = new vtkFloatArray[numBackPlanes];
        coloringValuesPerPoint = new vtkFloatArray[numBackPlanes];
        coloringNames = new String[numBackPlanes];
        coloringUnits = new String[numBackPlanes];
        coloringValuesScale = new float[numBackPlanes];
        return dem;
    }

    protected vtkPolyData fromFits(String filename, ProgressListener listener) throws IOException, FitsException
    {
        vtkPoints points = new vtkPoints();
        vtkCellArray polys = new vtkCellArray();
        vtkIdList idList = new vtkIdList();
        dem.SetPoints(points);
        dem.SetPolys(polys);

        Fits f = new Fits(filename);
        BasicHDU hdu = f.getHDU(0);

        // First pass, figure out number of planes and grab size and scale information
        Header header = hdu.getHeader();
        HeaderCard headerCard;
        List<Integer> backPlaneIndices = new ArrayList<Integer>();
        List<String> unprocessedBackPlaneNames = new ArrayList<String>();
        List<String> unprocessedBackPlaneUnits = new ArrayList<String>();
        int xIdx = -1;
        int yIdx = -1;
        int zIdx = -1;
        int planeCount = 0;

        while((headerCard = header.nextCard()) != null)
        {
            String headerKey = headerCard.getKey();
            String headerValue = headerCard.getValue();
            String headerComment = headerCard.getComment();

            if(headerKey.startsWith("PLANE"))
            {
                // Determine if we are looking at a coordinate or a backplane
                if(headerValue.startsWith("X"))
                {
                    // This plane is the X coordinate, save the index
                    xIdx = planeCount;
                }
                else if(headerValue.startsWith("Y"))
                {
                    // This plane is the Y coordinate, save the index
                    yIdx = planeCount;
                }
                else if(headerValue.startsWith("Z"))
                {
                    // This plane is the Z coordinate, save the index
                    zIdx = planeCount;
                }
                else
                {
                    // We are looking at a backplane, save the index in order of appearance
                    backPlaneIndices.add(planeCount);

                    // Try to break the value into name and unit components
                    String[] valueSplitResults = headerValue.split("[\\(\\[\\)\\]]");
                    String planeName = valueSplitResults[0];
                    String planeUnits = "";
                    if(valueSplitResults.length > 1)
                    {
                        planeUnits = valueSplitResults[1];
                    }
                    else if(headerComment != null)
                    {
                        // Couldn't find units in the value, try looking in comments instead
                        String[] commentSplitResults = headerComment.split("[\\(\\[\\)\\]]");
                        if(commentSplitResults.length > 1)
                        {
                            planeUnits = commentSplitResults[1];
                        }
                    }
                    unprocessedBackPlaneNames.add(planeName);
                    unprocessedBackPlaneUnits.add(planeUnits);
                }

                // Increment plane count
                planeCount++;
            }
        }

        this.latitude = header.getDoubleValue("LATITUDE");
        this.longitude = header.getDoubleValue("LONGTUDE");
        this.halfSize = header.getDoubleValue("HALFSIZE");
        this.scale = header.getDoubleValue("SCALE");

        // Check to see if x,y,z planes were all defined
        if(xIdx < 0)
        {
            throw new IOException("FITS file does not contain plane for X coordinate");
        }
        else if(yIdx < 0)
        {
            throw new IOException("FITS file does not contain plane for Y coordinate");
        }
        else if(zIdx < 0)
        {
            throw new IOException("FITS file does not contain plane for Z coordinate");
        }

        // Define arrays now that we know the number of backplanes
        int numBackPlanes = backPlaneIndices.size();
        coloringValuesPerCell = new vtkFloatArray[numBackPlanes];
        coloringValuesPerPoint = new vtkFloatArray[numBackPlanes];
        coloringNames = new String[numBackPlanes];
        coloringUnits = new String[numBackPlanes];
        coloringValuesScale = new float[numBackPlanes];
        int[] backPlaneIdx = new int[numBackPlanes];

        // Go through each backplane
        for(int i=0; i<numBackPlanes; i++)
        {
            // Get the name, unit, and scale factor to use
            setBackplaneInfo(i, unprocessedBackPlaneNames.get(i), unprocessedBackPlaneUnits.get(i),
                    coloringNames, coloringUnits, coloringValuesScale);

            // Set number of components for each vtkFloatArray to 1
            coloringValuesPerCell[i] = new vtkFloatArray();
            coloringValuesPerCell[i].SetNumberOfComponents(1);
            coloringValuesPerPoint[i] = new vtkFloatArray();
            coloringValuesPerPoint[i].SetNumberOfComponents(1);

            // Copy List element to array for faster lookup later
            backPlaneIdx[i] = backPlaneIndices.get(i);
        }

        // Check dimensions of actual data
        final int NUM_PLANES = numBackPlanes + 3;
        int[] axes = hdu.getAxes();
//        if (axes.length != 3 || axes[0] != NUM_PLANES || axes[1] != axes[2])
//        {
//            throw new IOException("FITS file has incorrect dimensions");
//        }

        int liveSize = axes[1];
        int liveSize2 = axes[2];

        float[][][] data = (float[][][])hdu.getData().getData();
        f.getStream().close();

        int[][] indices = new int[liveSize][liveSize2];
        int c = 0;
        float x, y, z;
        float d;

        // First add points to the vtkPoints array
        for (int m=0; m<liveSize; ++m)
        {
        	listener.setProgress(m*50/liveSize);
            for (int n=0; n<liveSize2; ++n)
            {
                indices[m][n] = -1;

                // A pixel value of -1.0e38 means that pixel is invalid and should be skipped
                x = data[xIdx][m][n];
                y = data[yIdx][m][n];
                z = data[zIdx][m][n];
                // Check to see if x,y,z values are all valid
                boolean valid = x != INVALID_VALUE && y != INVALID_VALUE && z != INVALID_VALUE;
                if (data.length > 6)
                	valid = key.viewBadData || (valid && (data[7][m][n] != 0));

                // Check to see if data for all backplanes are also valid
                for(int i=0; i<numBackPlanes; i++)
                {
                    d = data[backPlaneIdx[i]][m][n];
                    valid = (valid && d != INVALID_VALUE);
                }

                // Only add point if everything is valid
                if (valid)
                {
                    points.InsertNextPoint(x, y, z);
                    for(int i=0; i<numBackPlanes; i++)
                    {
                        d = data[backPlaneIdx[i]][m][n] * coloringValuesScale[i];
                        coloringValuesPerCell[i].InsertNextTuple1(d);
                    }

                    indices[m][n] = c;
                    ++c;
                }
            }
        }

        idList.SetNumberOfIds(3);

        // Now add connectivity information
        int i0, i1, i2, i3;
        for (int m=1; m<liveSize; ++m)
        {
        	listener.setProgress(50 + m*50/liveSize);
            for (int n=1; n<liveSize2; ++n)
            {
                // Get the indices of the 4 corners of the rectangle to the upper left
                i0 = indices[m-1][n-1];
                i1 = indices[m][n-1];
                i2 = indices[m-1][n];
                i3 = indices[m][n];

                // Add upper left triangle
                if (i0>=0 && i1>=0 && i2>=0)
                {
                    idList.SetId(0, i0);
                    idList.SetId(1, i2);
                    idList.SetId(2, i1);
                    polys.InsertNextCell(idList);
                }
                // Add bottom right triangle
                if (i2>=0 && i1>=0 && i3>=0)
                {
                    idList.SetId(0, i2);
                    idList.SetId(1, i3);
                    idList.SetId(2, i1);
                    polys.InsertNextCell(idList);
                }
            }
        }

        vtkPolyDataNormals normalsFilter = new vtkPolyDataNormals();
        normalsFilter.SetInputData(dem);
        normalsFilter.SetComputeCellNormals(0);
        normalsFilter.SetComputePointNormals(1);
        normalsFilter.SplittingOff();
        normalsFilter.FlipNormalsOn();
        normalsFilter.Update();

        vtkPolyData normalsFilterOutput = normalsFilter.GetOutput();
        dem.DeepCopy(normalsFilterOutput);

        PolyDataUtil.getBoundary(dem, boundary);
        // Remove scalar data since it interferes with setting the boundary color
        boundary.GetCellData().SetScalars(null);

        // Make a copy of per point data structures since we need that later for
        // drawing profile plots.
        for(int i=0; i<numBackPlanes; i++)
        {
            coloringValuesPerPoint[i].DeepCopy(coloringValuesPerCell[i]);
        }
        convertPointDataToCellData(dem, coloringValuesPerCell);

        int centerIndex = liveSize / 2;
        centerOfDEM = new double[3];
        centerOfDEM[0] = data[xIdx][centerIndex][centerIndex];
        centerOfDEM[1] = data[yIdx][centerIndex][centerIndex];
        centerOfDEM[2] = data[zIdx][centerIndex][centerIndex];

        // Delete data structures
        idList.Delete();
        listener.setProgress(100);
        return dem;
    }

    /**
     * Convert the point data (which is how they are stored in the Gaskell's cube file)
     * to cell data.
     */
    private static void convertPointDataToCellData(vtkPolyData polyData, vtkFloatArray[] colorValuesPerCell)
    {
        int numBackPlanes = colorValuesPerCell.length;
        vtkFloatArray[] cellDataArrays = new vtkFloatArray[numBackPlanes];

        vtkPointDataToCellData pointToCell = new vtkPointDataToCellData();
        pointToCell.SetInputData(polyData);

        for (int i=0; i<numBackPlanes; ++i)
        {
            vtkFloatArray array = colorValuesPerCell[i];
            polyData.GetPointData().SetScalars(array);
            pointToCell.Update();
            vtkFloatArray arrayCell = new vtkFloatArray();
            vtkDataArray outputScalars = ((vtkPolyData)pointToCell.GetOutput()).GetCellData().GetScalars();
            arrayCell.DeepCopy(outputScalars);
            cellDataArrays[i] = arrayCell;
        }

        polyData.GetPointData().SetScalars(null);

        for(int i=0; i<numBackPlanes; i++)
        {
            colorValuesPerCell[i] = cellDataArrays[i];
        }

        pointToCell.Delete();
    }

    public vtkPolyData getBoundary()
    {
        return boundary;
    }

    public void generateProfile(List<Vector3D> xyzPointList,
            List<Double> profileValues, List<Double> profileDistances, int coloringIndex)
    {
        profileValues.clear();
        profileDistances.clear();

        // For each point in xyzPointList, find the cell containing that
        // point and then, using barycentric coordinates find the value
        // of the dem at that point
        //
        // To compute the distance, assume we have a straight line connecting the first
        // and last points of xyzPointList. For each point, p, in xyzPointList, find the point
        // on the line closest to p. The distance from p to the start of the line is what
        // is placed in heights. Use SPICE's nplnpt function for this.

        double[] first = xyzPointList.get(0).toArray();
        double[] last = xyzPointList.get(xyzPointList.size()-1).toArray();
        double[] lindir = new double[3];
        lindir[0] = last[0] - first[0];
        lindir[1] = last[1] - first[1];
        lindir[2] = last[2] - first[2];

        // The following can be true if the user clicks on the same point twice
        boolean zeroLineDir = MathUtil.vzero(lindir);

        double[] pnear = new double[3];
        double[] notused = new double[1];
        vtkIdList idList = new vtkIdList();

        // Figure out which data set to sample
        vtkFloatArray valuePerPoint = null;
        boolean useDefaultProfile = false;
        if(coloringIndex >= 0 && coloringIndex < coloringValuesPerCell.length)
        {
            valuePerPoint = coloringValuesPerPoint[coloringIndex];
        }
        else
        {
            // Show default profile
            useDefaultProfile = true;
        }

        // Sample
        if(valuePerPoint != null || useDefaultProfile)
        {
            for (Vector3D aPt : xyzPointList)
            {
                double[] xyzArr = aPt.toArray();

                int cellId = findClosestCell(xyzArr);

                double val;
                if(useDefaultProfile)
                {
                    // Compute the radius
                    val = MathUtil.reclat(xyzArr).rad * 1000;
                }
                else
                {
                    // Interpolate to get the plate coloring
                    val = PolyDataUtil.interpolateWithinCell(dem, valuePerPoint, cellId, xyzArr, idList);
                }

                profileValues.add(val);

                if (zeroLineDir)
                {
                    profileDistances.add(0.0);
                }
                else
                {
                    MathUtil.nplnpt(first, lindir, xyzArr, pnear, notused);
                    double dist = 1000.0f * MathUtil.distanceBetween(first, pnear);
                    profileDistances.add(dist);
                }
            }
        }
    }

    private double getDistanceToBoundary(double[] point)
    {
        if (boundaryLocator == null)
        {
            boundaryLocator = new vtksbCellLocator();
            boundaryLocator.FreeSearchStructure();
            boundaryLocator.SetDataSet(boundary);
            boundaryLocator.CacheCellBoundsOn();
            boundaryLocator.AutomaticOn();
            //boundaryLocator.SetMaxLevel(10);
            //boundaryLocator.SetNumberOfCellsPerNode(5);
            boundaryLocator.BuildLocator();

            genericCell = new vtkGenericCell();
        }

        double[] closestPoint = new double[3];
        long[] cellId = new long[1];
        int[] subId = new int[1];
        double[] dist2 = new double[1];

        boundaryLocator.FindClosestPoint(point, closestPoint, genericCell, cellId, subId, dist2);

        return MathUtil.distanceBetween(point, closestPoint);
    }

    /**
     * Return whether or not point is inside the DEM. By "inside the DEM" we
     * mean that the point is displaced parallel to the mean normal vector of the
     * DEM, it will intersect the DEM.
     *
     * @param point
     * @param minDistanceToBoundary only consider point inside if it is minDistanceToBoundary
     *        or greater away from the boundary
     * @return
     */
    public boolean isPointWithinDEM(double[] point, double minDistanceToBoundary)
    {
        // Take the point and using the normal vector, form a line parallel
        // to the normal vector which passes through the given point.
        // If this line intersects the DEM, return true. Otherwise return
        // false.
        double[] normal = getNormal();

        double size = getBoundingBoxDiagonalLength();
        double[] origin = {
                point[0] + size*normal[0],
                point[1] + size*normal[1],
                point[2] + size*normal[2]
        };

        double[] direction = {
                -normal[0],
                -normal[1],
                -normal[2]
        };

        double[] intersectPoint = new double[3];
        int cellId = computeRayIntersection(origin, direction, intersectPoint );

        if (cellId >= 0 && getDistanceToBoundary(intersectPoint) >= minDistanceToBoundary)
            return true;
        else
            return false;
    }

    /**
     * Return the center point of the DEM.
     */
    public double[] getCenter()
    {
        return centerOfDEM;
    }

    /**
     * return the mean normal vector to the DEM. This is computed by averaging
     * the normal vectors of all plates in the DEM.
     */
    public double[] getNormal()
    {
        if (normalOfDEM == null)
            normalOfDEM = PolyDataUtil.computePolyDataNormal(dem);

        return centerOfDEM;
    }

    @Override
	public void delete()
    {
        dem.Delete();
        boundary.Delete();
        for(int i=0; i<coloringValuesPerCell.length; i++)
        {
            coloringValuesPerCell[i].Delete();
        }
        for(int i=0; i<coloringValuesPerPoint.length; i++)
        {
            coloringValuesPerPoint[i].Delete();
        }
        super.delete();
    }

    public void demAboutToBeRemoved()
    {
//        // If we have a dem view, close it
//        if(demView != null)
//        {
//            demView.dispose();
//            removeView();
//        }

        // Reset colorings and scales
        try
        {
            setColoringIndex(-1);
            for(int i=0; i<this.getNumberOfColors(); i++)
            {
                double[] defaultRange = getDefaultColoringRange(i);
                if (defaultRange[1] > defaultRange[0])
                {
                    setCurrentColoringRange(i, defaultRange);
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        // TODO Do something here.
    }

    @Override
	public void setVisible(boolean b)
    {
        List<vtkProp> props = super.getProps();
        for (vtkProp p : props)
        {
            p.SetVisibility(b ? 1 : 0);
        }

        super.setVisible(b);
    }

    public String[] getColoringNames()
    {
        return coloringNames;
    }

    public String[] getColoringUnits()
    {
        return coloringUnits;
    }

    private static void setBackplaneInfo(int colorIdx, String unprocessedName, String unprocessedUnits,
            String[] coloringNames, String[] coloringUnits, float[] coloringValuesScale)
    {
        // Keep as is by default
        String processedName = unprocessedName.trim();
        String processedUnits = (unprocessedUnits == null) ? "" : unprocessedUnits.trim();
        float processedScale = 1.0f;

        // Process here
        if(processedName.equals("Elevation Relative to Gravity") &&
                processedUnits.equals("kilometers"))
        {
            // From Mapmaker output
            processedName = "Geopotential Height";
            processedUnits = "m";
            processedScale = 1000.0f; // km -> m
        }
        else if(processedName.equals("Elevation Relative to Normal Plane") &&
                processedUnits.equals("kilometers"))
        {
            // From Mapmaker output
            processedName = "Height Relative to Normal Plane";
            processedUnits = "m";
            processedScale = 1000.0f; // km -> m
        }
        else if(processedName.equals("Slope") &&
                processedUnits.equals("radians"))
        {
            // From Mapmaker output
            processedUnits = "deg";
            processedScale = (float)(180.0/Math.PI); // rad -> deg
        }

        // Save backplane label information
        coloringNames[colorIdx] = processedName;
        coloringUnits[colorIdx] = processedUnits;
        coloringValuesScale[colorIdx] = processedScale;
    }

//    public void setView(DEMView demView)
//    {
//        this.demView = demView;
//    }
//
//    public boolean hasView()
//    {
//        return (demView != null);
//    }
//
//    public DEMView getView()
//    {
//        return demView;
//    }
//
//    public void removeView()
//    {
//        this.demView = null;
//    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (Properties.MODEL_CHANGED.equals(evt.getPropertyName()))
            this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);

        if ("progress" == evt.getPropertyName() ) {
            int progress = (Integer) evt.getNewValue();
            demLoadingProgressMonitor.setProgress(progress);
            String message =
                String.format("Completed %d%%.\n", progress);
            demLoadingProgressMonitor.setNote(message);
//            taskOutput.append(message);
            if (demLoadingProgressMonitor.isCanceled() || task.isDone()) {
                if (demLoadingProgressMonitor.isCanceled()) {
                    task.cancel(true);
//                    taskOutput.append("Task canceled.\n");
                } else {
//                    taskOutput.append("Task completed.\n");
                	this.pcs.firePropertyChange(Properties.MODEL_CHANGED, null, null);
                }
            }

        }
    }

    public double getLatitude()
    {
        return latitude;
    }

    public double getLongitude()
    {
        return longitude;
    }

    public double getHalfSize()
    {
        return halfSize;
    }

    public double getScale()
    {
        return scale;
    }

    public vtkPolyData getDem()
    {
        return dem;
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
	public boolean isPolyhedron()
	{
		// A digital elevation maps is a polygonal surface. They typically cover
		// just a small portion of a polyhedral model.
		return false;
	}

}
