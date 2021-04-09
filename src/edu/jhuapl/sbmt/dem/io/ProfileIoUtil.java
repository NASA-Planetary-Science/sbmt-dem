package edu.jhuapl.sbmt.dem.io;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

import edu.jhuapl.saavtk.structure.PolyLine;
import edu.jhuapl.saavtk.util.LatLon;
import edu.jhuapl.sbmt.dem.gui.analyze.DemPlot;

/**
 * Class that provides a collection of utility methods needed to serialize /
 * deserialize DEM related profiles.
 * <p>
 * These utility methods originated from the source:
 * edu.jhuapl.sbmt.dtm.ui.properties.DEMView
 *
 * @author lopeznr1
 */
public class ProfileIoUtil
{
	// Constants
	private static final String Profile = "Profile";
	private static final String StartLatitude = "StartLatitude";
	private static final String StartLongitude = "StartLongitude";
	private static final String StartRadius = "StartRadius";
	private static final String EndLatitude = "EndLatitude";
	private static final String EndLongitude = "EndLongitude";
	private static final String EndRadius = "EndRadius";
	private static final String Color = "Color";

	// TODO: Add javadoc
	public static List<PolyLine> loadView(File aFile) throws IOException
	{
		InputStream fs = new FileInputStream(aFile);
		InputStreamReader isr = new InputStreamReader(fs);
		BufferedReader in = new BufferedReader(isr);

		String line;

		double[] llBegArr = new double[] { 0.0, 0.0, 1.0 };
		double[] llEndArr = new double[] { 0.0, 0.0, 1.0 };
		int lineId = 0;

		List<PolyLine> retItemL = new ArrayList<>();
		while ((line = in.readLine()) != null)
		{
			line = line.trim();
			if (line.isEmpty())
				continue;
			String[] tokens = line.trim().split("=");
			if (tokens.length != 2)
			{
				in.close();
				throw new IOException("Error parsing file");
			}
			// System.out.println(tokens[0]);

			String key = tokens[0].trim();
			String value = tokens[1].trim();

			if (StartLatitude.equals(key))
				llBegArr[0] = Double.parseDouble(value);
			else if (StartLongitude.equals(key))
				llBegArr[1] = Double.parseDouble(value);
			else if (StartRadius.equals(key))
				llBegArr[2] = Double.parseDouble(value);
			else if (EndLatitude.equals(key))
				llEndArr[0] = Double.parseDouble(value);
			else if (EndLongitude.equals(key))
				llEndArr[1] = Double.parseDouble(value);
			else if (EndRadius.equals(key))
				llEndArr[2] = Double.parseDouble(value);
			else if (Color.equals(key))
			{
				String[] c = value.split("\\s+");
				int rVal = Integer.parseInt(c[0]);
				int gVal = Integer.parseInt(c[1]);
				int bVal = Integer.parseInt(c[2]);
				int aVal = Integer.parseInt(c[3]);
				Color color = new Color(rVal, gVal, bVal, aVal);

				LatLon begLL = new LatLon(llBegArr[0], llBegArr[1], llBegArr[2]);
				LatLon endLL = new LatLon(llEndArr[0], llEndArr[1], llEndArr[2]);
				List<LatLon> controlPointL = ImmutableList.of(begLL, endLL);
				PolyLine tmpItem = new PolyLine(lineId, null, controlPointL);
				tmpItem.setColor(color);
				retItemL.add(tmpItem);

				lineId++;
			}
		}

		in.close();

		return retItemL;
	}

	// TODO: Add javadoc
	public static void saveView(File aFile, List<PolyLine> aItemL, DemPlot aPlot) throws IOException
	{
		FileWriter fstream = new FileWriter(aFile);
		BufferedWriter out = new BufferedWriter(fstream);

		String eol = System.getProperty("line.separator");

		int tmpCnt = -1;
		for (PolyLine aLine : aItemL)
		{
			tmpCnt++;

			// Ignore invalid profiles
			if (aLine.getControlPoints().size() != 2)
				continue;

			LatLon ll0 = aLine.getControlPoints().get(0);
			LatLon ll1 = aLine.getControlPoints().get(1);
			Color color = aLine.getColor();
			out.write(eol + Profile + "=" + tmpCnt + eol);
			out.write(StartLatitude + "=" + ll0.lat + eol);
			out.write(StartLongitude + "=" + ll0.lon + eol);
			out.write(StartRadius + "=" + ll0.rad + eol);
			out.write(EndLatitude + "=" + ll1.lat + eol);
			out.write(EndLongitude + "=" + ll1.lon + eol);
			out.write(EndRadius + "=" + ll1.rad + eol);
			out.write(Color + "=" + color.getRed() + " " + color.getGreen() + " " + color.getBlue() + " "
					+ color.getAlpha() + eol);
			out.write(aPlot.getProfileAsString(aLine));
		}

		out.close();
	}

}
