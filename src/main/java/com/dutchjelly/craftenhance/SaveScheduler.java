package com.dutchjelly.craftenhance;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class SaveScheduler implements Runnable {
	private static final long SAVE_TASK_INTERVAL = 600 * 4;
	private static final long SAVE_INTERVAL = 60000 * 8;
	private int taskId = -1;
	private long lastSaveTime = 0;
	private long lastSaveTask = 0;
	private volatile boolean isRunningTask;
	private BukkitTask task;
	private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();

	public void start() {
		if (task != null && !task.isCancelled()) {
			return;
		}

		this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(self(), this, 20 * 30, 20);
		taskId = this.task.getTaskId();
	}

	public void stop() {
		if (taskId == -1) return;

		if (Bukkit.getScheduler().isCurrentlyRunning(taskId) && Bukkit.getScheduler().isQueued(taskId)) {
			Bukkit.getScheduler().cancelTask(taskId);
		}
	}

	@Override
	public void run() {
		long now = System.currentTimeMillis();

		if (now - lastSaveTask >= SAVE_TASK_INTERVAL && !isRunningTask) {
			executeTask();
			lastSaveTask = now;
		}

		if (now - lastSaveTime >= SAVE_INTERVAL) {
			System.out.println("saving");
			self().getCacheRecipes().save();
			lastSaveTime = now;
		}

	}

	private void executeTask() {
		if (taskQueue.isEmpty() || isRunningTask) {
			return;
		}
		System.out.println("can runn " );
		isRunningTask = true;
		new BukkitRunnable() {
			@Override
			public void run() {
				int batchSize = 4;
				for (int i = 0; i < batchSize && !taskQueue.isEmpty(); i++) {
					Runnable nextTask = taskQueue.poll();
					if (nextTask != null) {
						nextTask.run();
					}
				}
				if (taskQueue.isEmpty()) {
					isRunningTask = false;
					cancel();
				}
			}
		}.runTaskTimerAsynchronously(self(), 1, 5);
	}

	public void addTask(Runnable task) {
		taskQueue.add(task);
	}

}
