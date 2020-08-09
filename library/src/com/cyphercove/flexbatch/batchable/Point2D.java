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

import com.badlogic.gdx.math.Vector2;
import com.cyphercove.flexbatch.Batchable;
import com.cyphercove.flexbatch.utils.AttributeOffsets;
import com.cyphercove.flexbatch.utils.RenderContextAccumulator;
import org.jetbrains.annotations.NotNull;

/** A {@link Point} {@link Batchable Batchable} that supports a single texture at a time, with
 * two-dimensional position and color. It is designed to be drawn in a 2D plane.
 * <p>
 * A Point is always drawn as a square, centered at its position.
 * <p>
 * It may be subclassed to create a Batchable class that supports zero or multiple textures and additional attributes--see
 * {@link #getNumberOfTextures()} and {@link #addVertexAttributes(com.badlogic.gdx.utils.Array) addVertexAttributes()}. Such a
 * subclass would not be compatible with a FlexBatch that was instantiated for the base Point2D type.
 * 
 * @author cypherdare */
public class Point2D extends Point<Point2D> {
	@Override
	protected final boolean isPosition3D () {
		return false;
	}

	@Override
	protected void prepareSharedContext (RenderContextAccumulator renderContext) {
		super.prepareSharedContext(renderContext);
		renderContext.setDepthMasking(false);
	}

	/** Sets the position of the bottom left of the texture region in world space.
	 * @return This object for chaining. */
	public @NotNull Point2D position (float x, float y) {
		this.x = x;
		this.y = y;
		return this;
	}

	/** Sets the position of the bottom left of the texture region in world space.
	 * @return This object for chaining. */
	public @NotNull Point2D position (Vector2 position) {
		x = position.x;
		y = position.y;
		return this;
	}

	@Override
	protected int apply (float[] vertices, int vertexStartingIndex, AttributeOffsets offsets, int vertexSize) {
		super.apply(vertices, vertexStartingIndex, offsets, vertexSize);
		vertices[vertexStartingIndex] = x;
		vertices[vertexStartingIndex + 1] = y;
		return 1;
	}

}
