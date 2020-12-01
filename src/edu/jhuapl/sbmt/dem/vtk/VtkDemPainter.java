package edu.jhuapl.sbmt.dem.vtk;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import com.google.common.collect.ImmutableList;

import vtk.vtkProp;

import edu.jhuapl.saavtk.vtk.VtkResource;
import edu.jhuapl.sbmt.dem.Dem;
import edu.jhuapl.sbmt.dem.DemException;
import edu.jhuapl.sbmt.dem.DemManager;
import edu.jhuapl.sbmt.dem.KeyValueNode;

import glum.source.Source;
import glum.source.SourceState;
import glum.source.SourceUtil;
import glum.task.BufferTask;
import glum.task.NotifyTask;
import glum.task.Task;
import glum.task.TaskListener;
import glum.util.ThreadUtil;

/**
 * Class used to render a {@link Dem} via the VTK framework.
 * <p>
 * This class supports the following features:
 * <ul>
 * <li>Asynchronous loading of the @link Dem}
 * <li>Tracking of the dem's source and load state
 * <li>Configuration of view bad data flag
 * </ul>
 *
 * @author lopeznr1
 */
public class VtkDemPainter implements TaskListener, VtkResource
{
	// Reference vars
	private final DemManager refManager;
	private final Dem refItem;

	// Cache vars
	private ItemDrawAttr cDemDA;
	private DataMode cViewDataMode;

	// State vars
	private NotifyTask workTask;
	private Exception workExp;
	private SourceState workSS;

	// VTK vars
	private Map<DataMode, VtkDemSurface> vDemSurfaceM;

	/**
	 * Standard Constructor
	 *
	 * @param aManager The {@link DemManager} responsible for the specified item.
	 * @param aItem The DEM of interest.
	 */
	public VtkDemPainter(DemManager aManager, Dem aItem)
	{
		refManager = aManager;
		refItem = aItem;

		cDemDA = ItemDrawAttr.Default;
		cViewDataMode = DataMode.Regular;

		workTask = new NotifyTask(Task.Invalid, this);
		workExp = null;
		workSS = null;

		vDemSurfaceM = new HashMap<>();
	}

	/**
	 * Returns any Error associated with this DEM.
	 */
	public Exception getError()
	{
		return workExp;
	}

	/**
	 * Returns the reference DEM.
	 */
	public Dem getItem()
	{
		return refItem;
	}

	/**
	 * Returns the associated DemMa
	 */
	public DemManager getManager()
	{
		return refManager;
	}

	/**
	 * Returns the list of VtkProps used to render this painter.
	 */
	public List<vtkProp> getProps()
	{
		// Delegate
		VtkDemSurface tmpSurface = vDemSurfaceM.get(cViewDataMode);
		if (tmpSurface != null)
			return tmpSurface.getProps();

		return ImmutableList.of();
	}

	/**
	 * Returns the status of this {@link VtkDemPainter}.
	 *
	 * @param aIsBrief If true then the returned status message will be very
	 * brief.
	 */
	public String getStatusMsg(boolean aIsBrief)
	{
		if (aIsBrief == true)
			return getStatusMsgBrief();

		return getStatusMsgDetailed();
	}

	/**
	 * Returns the {@link VtkDemSurface} associated with this painter.
	 * <p>
	 * This will return null if this has painter has not been fully loaded via
	 * {@link #vtkStateInit()}.
	 */
	public VtkDemSurface getVtkDemSurface()
	{
		return vDemSurfaceM.get(cViewDataMode);
	}

	/**
	 * Returns true if this painter is currently being loaded up.
	 */
	public boolean isLoadActive()
	{
		return workTask.isDone() == false;
	}

	/**
	 * Helper method that determines if the associated {@link VtkDemPainter}
	 * needs to be loaded.
	 */
	public boolean isLoadNeeded()
	{
		// Load is not needed if already loaded (or in process of being loaded)
		if (isReady() == true || isLoadActive() == true)
			return false;

		// Determine if the there is a need to load the DEM
		boolean isInitNeeded = false;
		isInitNeeded |= cDemDA.getIsExtShown() == true;
		isInitNeeded |= cDemDA.getIsIntShown() == true;
		return isInitNeeded;
	}

	/**
	 * Returns true if this painter is ready for painting.
	 */
	public boolean isReady()
	{
		boolean retBool = false;
		VtkDemSurface tmpSurface = vDemSurfaceM.get(cViewDataMode);
		if (tmpSurface != null)
			retBool = true;

		return retBool;
	}

	/**
	 * Aborts the active asynchronous load of VTK state.
	 * <p>
	 * If there is no active load then no action is performed.
	 */
	public void vtkStateHalt()
	{
		// Bail if there is no active load
		if (isLoadActive() == false)
			return;

		// Switch off flags that will cause a load (if no available VtkDemSurface)
		ImmutableList<Dem> tmpL = ImmutableList.of(refItem);
		if (vDemSurfaceM.size() == 0)
			refManager.clearAutoLoadFlags(tmpL);
		// Switch to the available VtkDemSurface
		else
		{
			DataMode tmpDataMode = vDemSurfaceM.keySet().iterator().next();
			refManager.setViewDataMode(tmpL, tmpDataMode);
		}

		// Abort the load
		workTask.abort();
	}

	/**
	 * Notifies this Painter to perform an asynchronous load of VTK state.
	 * <p>
	 * On completion of a successful load the method {@link #isReady()} will
	 * return true.
	 */
	public void vtkStateInit(Executor aExecutor)
	{
		// Bail if there is a load in progress
		if (workTask.isDone() == false)
			return;

		// Bail if we have been loaded
		VtkDemSurface tmpSurface = vDemSurfaceM.get(cViewDataMode);
		if (tmpSurface != null)
			return;

		// Start a new load
		workTask = new NotifyTask(new BufferTask(), this);
		aExecutor.execute(() -> VtkDemLoadUtil.loadVtkDemPainter(workTask, refManager, this));
	}

	@Override
	public void taskUpdate(Task aTask)
	{
		refManager.notifyLoadUpdate(refItem, false);
	}

	@Override
	public void vtkDispose()
	{
		// Delegate
		for (VtkDemSurface aSurface : vDemSurfaceM.values())
			aSurface.vtkDispose();

		cDemDA = ItemDrawAttr.Default;
		vDemSurfaceM.clear();
	}

	@Override
	public void vtkUpdateState()
	{
		// Assume nothing has changed
		boolean isChanged = false;

		// Ensure our cache vars reflect the latest configuration
		DataMode oViewDataMode = cViewDataMode;
		cViewDataMode = refManager.getViewDataMode(refItem);
		isChanged |= cViewDataMode != oViewDataMode;

		ItemDrawAttr oDemDA = cDemDA;
		cDemDA = refManager.getDrawAttr(refItem);
		isChanged |= cDemDA.equals(oDemDA) == false;

		// Bail if nothing has changed
		if (isChanged == false)
			return;

		// Bail if we are not ready
		if (isReady() == false)
			return;

		// Update relevant VTK state
		VtkDemSurface tmpSurface = vDemSurfaceM.get(cViewDataMode);
		if (tmpSurface != null)
			tmpSurface.setDrawAttr(cDemDA);
	}

	/**
	 * Notifies this painter that it's (loaded) associated VTK state.
	 * <p>
	 * This method will be called after a successful asynchronous load.
	 */
	protected void markComplete(VtkDemStruct aVDS)
	{
		// Update the key-value-pair mapping
		Map<String, KeyValueNode> tmpKeyValueM = refManager.getKeyValuePairMap(refItem);
		if (tmpKeyValueM.isEmpty() == true)
			tmpKeyValueM = aVDS.keyValueM;
		else
		{
			tmpKeyValueM = new LinkedHashMap<>(tmpKeyValueM);
			tmpKeyValueM.putAll(aVDS.keyValueM);
		}
		refManager.setKeyValuePairMap(refItem, tmpKeyValueM);

		// Set up the dem surface
		VtkDemSurface tmpSurface = new VtkDemSurface(refItem, aVDS);
		tmpSurface.setDrawAttr(cDemDA);
		vDemSurfaceM.put(aVDS.viewDataMode, tmpSurface);
		workTask.setProgress(1.0);

		// Switch to DataMode.Plain if that is what we are populated with
		if (aVDS.viewDataMode == DataMode.Plain)
			refManager.setViewDataMode(ImmutableList.of(refItem), DataMode.Plain);

		refManager.notifyLoadUpdate(refItem, true);
	}

	/**
	 * Notifies this painter that the attempted (loaded) of associated VTK state
	 * resulted in failure.
	 * <p>
	 * This method will be called after a failed asynchronous load.
	 */
	protected void markFailure(Exception aExp)
	{
		workExp = aExp;
		refManager.clearAutoLoadFlags(ImmutableList.of(refItem));
		refManager.notifyLoadUpdate(refItem, true);
	}

	/**
	 * Notifies this painter that there is a significant update.
	 * <p>
	 * Currently this is just notification that the download is complete.
	 */
	protected void markUpdate()
	{
		workSS = SourceUtil.getState(refItem.getSource());
	}

	/**
	 * Helper method that returns a "brief" status message.
	 */
	private String getStatusMsgBrief()
	{
		VtkDemSurface tmpSurface = vDemSurfaceM.get(cViewDataMode);
		if (tmpSurface != null)
			return "Loaded: " + cViewDataMode.getDescrBrief();

		if (workExp != null)
			return "Failure";

		// Lazy init FileState
		if (workSS == null)
			workSS = SourceUtil.getState(refItem.getSource());

		if (workTask.getRefTask() != Task.Invalid)
		{
			DecimalFormat tmpDF = new DecimalFormat("0.00%");
			double tmpProgress = workTask.getProgress();

			if (workTask.isDone() == false)
			{
				if (workSS == SourceState.Local)
					return "LD: " + tmpDF.format(tmpProgress);
				else
					return "DL: " + tmpDF.format(tmpProgress);
			}
			else
				return "Halt: " + tmpDF.format(tmpProgress);
		}

		switch (workSS)
		{
			case Local:
				return "UL: Local";
			case Partial:
				return "UL: Partial";
			case Remote:
				return "UL: Remote";
			case Unavailable:
				return "Unavailable";
			default:
				throw new RuntimeException("Unsupported enum: " + workSS);
		}
	}

	/**
	 * Helper method that returns a "detailed" status message.
	 */
	private String getStatusMsgDetailed()
	{
		Source tmpSource = refItem.getSource();

		// Details on load status
		String retMsg = "";
		if (workTask.getRefTask() != Task.Invalid)
			retMsg = ((BufferTask) workTask.getRefTask()).getBuffer();
		else
			retMsg += SourceUtil.getStatusMsg(tmpSource);

		if (workExp != null)
		{
			retMsg += "\nAn error occurred while loading the DEM.\n";
			if (workExp instanceof DemException)
				retMsg += "\t" + workExp.getMessage();
			else
				retMsg += ThreadUtil.getStackTraceClassic(workExp);
			retMsg += "\n";
		}

		if (isReady() == true)
		{
			retMsg += "\n";
			retMsg += "Mode: " + cViewDataMode + " - " + cViewDataMode.getDescrFull();
		}

		return retMsg;
	}

}
