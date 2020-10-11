/* ******************************************************************************
 * Copyright 2020 See AUTHORS file.
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
package com.cyphercove.flexbatch.batchable;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector3;
import com.cyphercove.flexbatch.Batchable;
import com.cyphercove.flexbatch.utils.AttributeOffsets;
import com.cyphercove.flexbatch.utils.Blending;
import com.cyphercove.flexbatch.utils.RenderContextAccumulator;
import com.cyphercove.flexbatch.utils.SortableBatchable;

/** A {@link Point} {@link Batchable Batchable} that supports a single texture at a time, with
 * three-dimensional position and color.
 * <p>
 * A Point is always drawn as a square, centered at its position.
 * <p>
 * Point3D manages its own blending and ignores the FlexBatch's blend settings.
 * <p>
 * It may be subclassed to create a Batchable class that supports zero or multiple textures and additional attributes--see
 * {@link #getNumberOfTextures()} and {@link #addVertexAttributes(com.badlogic.gdx.utils.Array) addVertexAttributes()}. Such a
 * subclass would not be compatible with a FlexBatch that was instantiated for the base Point2D type.
 * 
 * @author cypherdare */
public class Point3D extends Point<Point3D> implements SortableBatchable {

	public float z;
	public boolean opaque = true;
	public int srcBlendFactor = GL20.GL_SRC_ALPHA;
	public int dstBlendFactor = GL20.GL_ONE_MINUS_SRC_ALPHA;

	@Override
	protected final boolean isPosition3D () {
		return true;
	}

	@Override
	public boolean isOpaque () {
		return opaque;
	}

	@Override
	public float calculateDistanceSquared (Vector3 camPosition) {
		return camPosition.dst2(x, y, z);
	}

	@Override
	protected void prepareSharedContext (RenderContextAccumulator renderContext) {
		super.prepareSharedContext(renderContext);
		renderContext.setDepthMasking(true);
		renderContext.setDepthTesting(true);
	}

	@Override
	protected boolean prepareContext (RenderContextAccumulator renderContext, int remainingVertices, int remainingTriangles) {
		boolean needsFlush = super.prepareContext(renderContext, remainingVertices, remainingTriangles);
		needsFlush |= renderContext.setBlending(!opaque);
		if (!opaque) {
			needsFlush |= renderContext.setBlendFunction(srcBlendFactor, dstBlendFactor);
		}
		return needsFlush;
	}

	@Override
	public void refresh () {
		super.refresh();
		z = 0f;
		opaque = true; // refresh most commonly used for on-the-fly batch.draw(), so default to mode that doesn't need sorting
		srcBlendFactor = GL20.GL_SRC_ALPHA;
		dstBlendFactor = GL20.GL_ONE_MINUS_SRC_ALPHA;
	}

	/** Disables blending. Blending is disabled by default.
	 * @return This object for chaining. */
	public Point3D opaque () {
		opaque = true;
		return this;
	}

	/** Enables blending. Blending is disabled by default.
	 * @return This object for chaining. */
	public Point3D blend () {
		opaque = false;
		return this;
	}

	/** Enables blending and sets the blend function parameters. Blending is disabled by default.
	 * @return This object for chaining. */
	public Point3D blend (int srcBlendFactor, int dstBlendFactor) {
		opaque = false;
		this.srcBlendFactor = srcBlendFactor;
		this.dstBlendFactor = dstBlendFactor;
		return this;
	}

	/** Enables blending and sets the blend function parameters to a commonly used pair. Blending is disabled by default.
	 * @return This object for chaining. */
	public Point3D blend (Blending blending) {
		opaque = false;
		srcBlendFactor = blending.srcBlendFactor;
		dstBlendFactor = blending.dstBlendFactor;
		return this;
	}

	/** Sets the position of the bottom left of the texture region in world space.
	 * @return This object for chaining. */
	public Point3D position (float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	/** Sets the position of the bottom left of the texture region in world space.
	 * @return This object for chaining. */
	public Point3D position (Vector3 position) {
		x = position.x;
		y = position.y;
		z = position.z;
		return this;
	}

	@Override
	protected int apply (float[] vertices, int vertexStartingIndex, AttributeOffsets offsets, int vertexSize) {
		super.apply(vertices, vertexStartingIndex, offsets, vertexSize);
		vertices[vertexStartingIndex] = x;
		vertices[vertexStartingIndex + 1] = y;
		vertices[vertexStartingIndex + 2] = z;
		return 1;
	}

}
