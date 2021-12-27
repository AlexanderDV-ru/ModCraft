package ru.alexanderdv.modcraft;

public class Time {
	private double ticksPerSecond = 50, physicalScale = 1;

	public boolean updateStopped;

	public double getTicksPerSecond() { return ticksPerSecond; }

	public double getPhysicalScale() { return physicalScale; }

	public void changePhysicalScaleTo(double scale) { this.physicalScale = scale; }

	public double countRendersPerSecond() { return getResolution() / -(lastRender - (lastRender = countNowTime())); }

	public double countFramesPerSecond() { return countRendersPerSecond(); }

	public double fps() { return countFramesPerSecond(); }

	private double nano = 1, resolutionModifier = 1d;

	public double getResolution() { return (nano != 0 ? 1000000000d : 1000d) * resolutionModifier; }

	public double countNowTime() { return (nano != 0 ? System.nanoTime() / 1000000d : System.currentTimeMillis()) / 1000d * getResolution(); }

	public double countWorkTime() { return countNowTime() - start; }

	private double start = countNowTime(), lastRender = countNowTime(), lastUpdate = countNowTime(), remainingUpdateCount = 0;

	public void doRemainingUpdateCount(Runnable updateMethod, Runnable doInLastUpdate) {
		for (remainingUpdateCount += -(lastUpdate - (lastUpdate = countNowTime())) / getResolution() * ticksPerSecond; remainingUpdateCount > 0; remainingUpdateCount--)
			if (!updateStopped) {
				updateMethod.run();
				if (remainingUpdateCount < 1)
					doInLastUpdate.run();
			}
	}
}