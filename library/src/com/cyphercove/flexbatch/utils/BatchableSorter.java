/* ******************************************************************************
 * Copyright 2017 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.cyphercove.flexbatch.utils;

import java.util.Comparator;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Pool;
import com.cyphercove.flexbatch.Batchable;
import com.cyphercove.flexbatch.FlexBatch;
import org.jetbrains.annotations.NotNull;

/** Sorts 3D {@link Batchable Batchables} to ensure proper render order before passing them to a {@link FlexBatch}.
 * <p>
 * Opaque Batchables are sorted by texture configuration to minimize flushes and drawn first. Blended Batchables are sorted by
 * distance from camera and drawn far to near.
 * 
 * @author cypherdare */
public class BatchableSorter<T extends Batchable & SortableBatchable> {

	protected final int opaqueInitialCapacityPerTexture;
	private final ObjectMap<T, ObjectSet<T>> opaqueBatchables; // first entered to material group
	public final Array<T> blendedBatchables;
	private final BatchableComparator comparator = new BatchableComparator();
	private Camera camera;
	private boolean needSort;

	private final Pool<ObjectSet<T>> objectSetPool = new Pool<ObjectSet<T>>() {
		protected void reset (ObjectSet<T> object) {
			object.clear();
		}

		protected ObjectSet<T> newObject () {
			return new ObjectSet<>(opaqueInitialCapacityPerTexture);
		}

	};

	/**
	 * Construct a BatchableSorter suitable for rendering both opaque and blended batchables.
	 *
	 * @param camera Initial camera to use for sorting distance.
	 */
	public BatchableSorter (Camera camera) {
		this(camera, 2, 1000, 1000);
	}

	/**
	 * Construct a BatchableSorter using specific values for initial backing array sizes.
	 *
	 * @param camera Initial camera to use for sorting distance.
	 * @param opaqueInitialTextureCapacity The initial number of backing arrays created for opaque Batchables. One is
	 *                                        used for each unique texture that is simultaneously queued. This should be
	 *                                        as high as the expected number of unique textures for opaque Batchables to
	 *                                        prevent potential stutters from adding new backing arrays during queuing.
	 * @param opaqueInitialCapacityPerTexture The initial capacity of each backing array of opaque Batchables. One is
	 *                                        created for each unique texture that is simultaneously queued. A larger
	 *                                        initial capacity may prevent backing array resizes during queuing, and so
	 *                                        may reduce the potential for stutters.
	 * @param blendedInitialCapacity The initial capacity of the backing array that stores blended Batchables before they
	 *                               are flushed or drawn. A larger initial capacity may prevent backing array resizes
	 *                               during queuing, and so may reduce the potential for stutters.
	 */
	public BatchableSorter (Camera camera,
							int opaqueInitialTextureCapacity,
							int opaqueInitialCapacityPerTexture,
							int blendedInitialCapacity) {
		setCamera(camera);
		this.opaqueInitialCapacityPerTexture = Math.max(10, opaqueInitialCapacityPerTexture);
		opaqueBatchables = new ObjectMap<>();
		for (int i = 0; i < opaqueInitialTextureCapacity; i++) { // seed the pool to avoid delay on first use
			objectSetPool.free(objectSetPool.obtain());
		}
		blendedBatchables = new Array<>(Math.max(32, blendedInitialCapacity));
	}

	/** Clear the queue without drawing anything. */
	public void clear () {
		for (ObjectSet<T> set : opaqueBatchables.values())
			objectSetPool.free(set);
		opaqueBatchables.clear();
		blendedBatchables.clear();
	}

	/** Sort (if necessary) and draw the queued Batchables without clearing them. Must be called in between
	 * {@link FlexBatch#begin()} and {@link FlexBatch#end()}.
	 *
	 * @param flexBatch The batch to draw the batchables with.
	 */
	public void draw (FlexBatch<T> flexBatch) {
		if (needSort) {
			blendedBatchables.sort(comparator);
			needSort = false;
		}
		for (ObjectSet<T> set : opaqueBatchables.values())
			for (T batchable : set)
				flexBatch.draw(batchable);
		for (T batchable : blendedBatchables)
			flexBatch.draw(batchable);
	}

	/**
	 * Sort (if necessary), draw, and clear references to the queued Batchables. Must be called in between
	 * {@link FlexBatch#begin()} and {@link FlexBatch#end()}.
	 *
	 * @param flexBatch The batch to draw the batchables with.
	 * */
	public void flush (FlexBatch<T> flexBatch) {
		draw(flexBatch);
		clear();
	}

	/**
	 * Add a Batchable to the queue.
	 *
	 * @param batchable The Batchable to add.
	 * */
	public void add (T batchable) {
		if (batchable.isOpaque()) {
			for (ObjectMap.Entry<T, ObjectSet<T>> entry : opaqueBatchables) {
				if (batchable.hasEquivalentTextures(entry.key)) {
					entry.value.add(batchable);
					return;
				}
			}
			ObjectSet<T> set = objectSetPool.obtain();
			set.add(batchable);
			opaqueBatchables.put(batchable, set);
		} else {
			blendedBatchables.add(batchable);
		}
		needSort = true;
	}

	/** Get the camera currently used for distance comparisons.
	 *
	 * @return The current camera.
	 */
	public @NotNull Camera getCamera() {
		return camera;
	}

	/** Sets the camera that is used for distance comparisons to sort the blended Batchables.
	 *
	 * @param camera The camera to use for distance comparisons.
	 * */
	public void setCamera (@NotNull Camera camera) {
		this.camera = camera;
		comparator.cameraPosition = camera.position;
	}

	private static class BatchableComparator implements Comparator<SortableBatchable> {
		Vector3 cameraPosition = null;
		@Override
		public int compare(SortableBatchable o1, SortableBatchable o2) {
			return Float.compare(o1.calculateDistanceSquared(cameraPosition), o2.calculateDistanceSquared(cameraPosition));
		}
	}
}
