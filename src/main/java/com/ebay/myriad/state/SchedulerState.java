/**
 * Copyright 2012-2014 eBay Software Foundation, All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.ebay.myriad.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.mesos.Protos.FrameworkID;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class SchedulerState {
	private FrameworkID frameworkId;
	private Map<String, Cluster> clusters;
	private Map<String, NodeTask> tasks;
	private Set<String> pendingTasks;
	private Set<String> stagingTasks;
	private Set<String> activeTasks;
	private Set<String> lostTasks;
	private Set<String> killableTasks;
	private Map<String, Boolean> rebalancerLock;

	public SchedulerState() {
		this.clusters = new ConcurrentHashMap<>();
		this.tasks = new ConcurrentHashMap<>();
		this.pendingTasks = new HashSet<>();
		this.stagingTasks = new HashSet<>();
		this.activeTasks = new HashSet<>();
		this.lostTasks = new HashSet<>();
		this.killableTasks = new HashSet<>();
		this.rebalancerLock = new ConcurrentHashMap<>();
	}

	public void makeTaskPending(String taskId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(taskId),
				"taskId cannot be empty or null");

		pendingTasks.add(taskId);
		stagingTasks.remove(taskId);
		activeTasks.remove(taskId);
		lostTasks.remove(taskId);
		killableTasks.remove(taskId);
	}

	public void makeTaskPending(NodeTask task) {
		Preconditions.checkArgument(task != null,
				"NodeTask object cannot be null");

		makeTaskPending(task.getTaskId());
	}

	public void makeTaskStaging(String taskId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(taskId),
				"taskId cannot be empty or null");

		pendingTasks.remove(taskId);
		stagingTasks.add(taskId);
		activeTasks.remove(taskId);
		lostTasks.remove(taskId);
		killableTasks.remove(taskId);
	}

	public void makeTaskStaging(NodeTask task) {
		Preconditions.checkArgument(task != null,
				"NodeTask object cannot be null");

		makeTaskStaging(task.getTaskId());
	}

	public void makeTaskActive(String taskId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(taskId),
				"taskId cannot be empty or null");

		pendingTasks.remove(taskId);
		stagingTasks.remove(taskId);
		activeTasks.add(taskId);
		lostTasks.remove(taskId);
		killableTasks.remove(taskId);
	}

	public void makeTaskActive(NodeTask task) {
		Preconditions.checkArgument(task != null,
				"NodeTask object cannot be null");

		makeTaskActive(task.getTaskId());
	}

	public void makeTaskLost(String taskId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(taskId),
				"taskId cannot be empty or null");

		pendingTasks.remove(taskId);
		stagingTasks.remove(taskId);
		activeTasks.remove(taskId);
		lostTasks.add(taskId);
		killableTasks.remove(taskId);
	}

	public void makeTaskLost(NodeTask task) {
		Preconditions.checkArgument(task != null,
				"NodeTask object cannot be null");

		makeTaskLost(task.getTaskId());
	}

	public void makeTaskKillable(String taskId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(taskId),
				"taskId cannot be empty or null");

		pendingTasks.remove(taskId);
		stagingTasks.remove(taskId);
		activeTasks.remove(taskId);
		lostTasks.remove(taskId);
		killableTasks.add(taskId);
	}

	public void makeTaskKillable(NodeTask task) {
		Preconditions.checkArgument(task != null,
				"NodeTask object cannot be null");

		makeTaskKillable(task.getTaskId());
	}

	public Cluster getCluster(String clusterId) {
		return clusters.get(clusterId);
	}

	public Map<String, Cluster> getClusters() {
		return clusters;
	}

	public void addCluster(Cluster cluster) {
		Preconditions.checkArgument(cluster != null,
				"Cluster object cannot be null");

		clusters.put(cluster.getClusterId(), cluster);
		this.addClusterNodes(cluster.getClusterId(), cluster.getNodes());
	}

	public void addClusterNodes(String clusterId, Collection<NodeTask> nodes) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(clusterId));

		Cluster cluster = this.clusters.get(clusterId);

		// TODO(mohit): error handling

		nodes.forEach(node -> {
			cluster.addNode(node);
			String taskId = node.getTaskId();
			tasks.put(taskId, node);
			pendingTasks.add(taskId);
		});
	}

	public void deleteCluster(String clusterId) {
		Collection<NodeTask> nodes = clusters.get(clusterId).getNodes();
		nodes.forEach(node -> this.makeTaskKillable(node));
		// TODO(mohit): Make this more correct.
		this.clusters.remove(clusterId);
	}

	public void deleteCluster(Cluster cluster) {
		Preconditions.checkArgument(cluster != null,
				"Cluster object cannot be null");

		deleteCluster(cluster.getClusterId());
	}

	public Set<String> getKillableTasks() {
		return this.killableTasks;
	}

	public NodeTask getTask(String taskId) {
		return this.tasks.get(taskId);
	}

	public void removeTask(String taskId) {
		this.pendingTasks.remove(taskId);
		this.stagingTasks.remove(taskId);
		this.activeTasks.remove(taskId);
		this.lostTasks.remove(taskId);
		this.killableTasks.remove(taskId);
		this.tasks.remove(taskId);
	}

	public Set<String> getPendingTaskIds() {
		return this.pendingTasks;
	}

	public Set<String> getActiveTaskIds() {
		return this.activeTasks;
	}

	public Collection<NodeTask> getActiveTasks() {
		List<NodeTask> activeNodeTasks = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(activeTasks)
				&& CollectionUtils.isNotEmpty(tasks.values())) {
			activeNodeTasks = tasks.values().stream()
					.filter(task -> activeTasks.contains(task.getTaskId()))
					.collect(Collectors.toList());
		}
		return activeNodeTasks;
	}

	public Set<String> getStagingTaskIds() {
		return this.stagingTasks;
	}

	public Set<String> getLostTaskIds() {
		return this.lostTasks;
	}

	public FrameworkID getFrameworkId() {
		return frameworkId;
	}

	public void setFrameworkId(FrameworkID frameworkId) {
		this.frameworkId = frameworkId;
	}

	public boolean acquireLock(String clusterId) {
		Boolean lock = this.rebalancerLock.getOrDefault(clusterId,
				Boolean.FALSE);
		if (Boolean.FALSE == lock) {
			this.rebalancerLock.put(clusterId, Boolean.TRUE);
			return true;
		}
		return false;
	}

	public boolean releaseLock(String clusterId) {
		this.rebalancerLock.put(clusterId, Boolean.FALSE);
		return true;
	}

}