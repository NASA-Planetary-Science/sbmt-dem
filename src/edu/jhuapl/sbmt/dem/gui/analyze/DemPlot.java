package edu.jhuapl.sbmt.dem.gui.analyze;

import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;

import vtk.vtkFloatArray;

import edu.jhuapl.saavtk.feature.FeatureType;
import edu.jhuapl.saavtk.model.structure.LineModel;
import edu.jhuapl.saavtk.structure.PolyLine;
import edu.jhuapl.saavtk.structure.plot.BaseLinePlot;
import edu.jhuapl.sbmt.dem.vtk.VtkDemSurface;

/**
 * Implementation of {@link BaseLinePlot}.
 * <p>
 * Class originated from the source: edu.jhuapl.sbmt.dtm.ui.properties.DEMPlot
 *
 * @author lopeznr1
 */
public class DemPlot extends BaseLinePlot implements ChartMouseListener
{
	// Ref vars
	private final LineModel<PolyLine> refManager;
	private final VtkDemSurface refDemSurface;

	// State vars
	private FeatureType currFeatureType;

	// Chart vars
	private final ChartPanel chartPanel;

	/** Standard Constructor */
	public DemPlot(LineModel<PolyLine> aManager, VtkDemSurface aDemSurface, FeatureType aFeatureType)
	{
		super(aManager);

		refManager = aManager;
		refDemSurface = aDemSurface;

		currFeatureType = aFeatureType;

		JFreeChart tmpChart = ChartFactory.createXYLineChart("", "", "", getXYDataSet(), PlotOrientation.VERTICAL, false,
				true, false);

		// add the jfreechart graph
		chartPanel = new ChartPanel(tmpChart);
		chartPanel.setMouseWheelEnabled(true);
		chartPanel.addChartMouseListener(this);
		updateChartLabels();

		XYPlot plot = (XYPlot) tmpChart.getPlot();
		plot.setDomainPannable(true);
		plot.setRangePannable(true);

		XYItemRenderer r = plot.getRenderer();
		if (r instanceof XYLineAndShapeRenderer)
		{
			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
			renderer.setBaseShapesVisible(false);
			renderer.setBaseShapesFilled(true);
		}
	}

	// TODO: This needs to be flushed out further...
	public String getProfileAsString(PolyLine aItem)
	{
		// Retrieve the plot's label
		String rangeLabel = "Value";
		if (currFeatureType != FeatureType.Invalid)
			rangeLabel = currFeatureType.getName();

		StringBuilder buffer = new StringBuilder();

		XYSeries series = getSeriesFor(aItem);

		String eol = System.getProperty("line.separator");

		int N = series.getItemCount();

		buffer.append("Distance=");
		for (int i = 0; i < N; ++i)
			buffer.append(series.getX(i) + " ");
		buffer.append(eol);

		buffer.append(rangeLabel + "=");
		for (int i = 0; i < N; ++i)
			buffer.append(series.getY(i) + " ");
		buffer.append(eol);

		return buffer.toString();
	}

	/**
	 * Sets in the {@link FeatureType} to be used by this plot.
	 */
	public void setFeatureType(FeatureType aFeatureType)
	{
		currFeatureType = aFeatureType;

		// Update chart labels
		updateChartLabels();

		// Update all plots
		notifyAllStale();
	}

	@Override
	public void chartMouseClicked(ChartMouseEvent arg0)
	{
		ChartEntity entity = arg0.getEntity();
		if (entity instanceof XYItemEntity)
		{
			// int id = ((XYItemEntity)entity).getItem();
		}
	}

	@Override
	public void chartMouseMoved(ChartMouseEvent arg0)
	{
	}

	@Override
	public ChartPanel getChartPanel()
	{
		return chartPanel;
	}

	@Override
	protected void getPlotPoints(PolyLine aItem, List<Double> xValueL, List<Double> yValueL)
	{
		// Bail if the line is not visible or valid
		if (aItem.getVisible() == false || aItem.getControlPoints().size() < 2)
			return;

		// Bail if there are no points for this plot
		List<Vector3D> xyzPointL = refManager.getXyzPointsFor(aItem);
		if (xyzPointL.size() == 0)
			return;

		vtkFloatArray tmpValueFA = refDemSurface.getValuesForPointData(currFeatureType);
		PlotUtil.generateProfile(refDemSurface, xyzPointL, yValueL, xValueL, tmpValueFA);
	}

	/**
	 * Helper method that will configure the chart labels based on the current
	 * {@link FeatureType}.
	 */
	private void updateChartLabels()
	{
		// Figure out labels to utilize
		String title = "Radius vs. Distance";
		String domainLabel = "Distance (m)";
		String rangeLabel = "Radius (m)";
		if (currFeatureType != FeatureType.Invalid)
		{
			String tmpName = currFeatureType.getName();
			title = tmpName + " vs. Distance";
			domainLabel = "Distance (m)";
			rangeLabel = tmpName;

			// Try to add units for range label if possible
			String tmpUnit = currFeatureType.getUnit();
			if (tmpUnit != null)
				rangeLabel += " (" + tmpUnit + ")";
		}

		// Update the chart's labels
		chartPanel.getChart().setTitle(title);
		chartPanel.getChart().getXYPlot().getDomainAxis().setLabel(domainLabel);
		chartPanel.getChart().getXYPlot().getRangeAxis().setLabel(rangeLabel);
	}

}
