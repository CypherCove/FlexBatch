/*******************************************************************************
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
package com.cyphercove.flexbatch.batchable;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.cyphercove.flexbatch.Batchable;
import com.cyphercove.flexbatch.utils.AttributeOffsets;
import com.cyphercove.flexbatch.utils.RenderContextAccumulator;
import org.jetbrains.annotations.NotNull;

/** A {@link Quad} {@link Batchable Batchable} that supports a single texture at a time, with
 * two-dimensional position and color. It is designed to be drawn in a 2D plane, and is commonly called a sprite.
 * <p>
 * The default origin of a Quad2D is its bottom left corner. Its origin is used for positioning, and as the center of rotation and
 * scaling.
 * <p>
 * It may be subclassed to create a Batchable class that supports zero or multiple textures and additional attributes--see
 * {@link #getNumberOfTextures()} and {@link #addVertexAttributes(com.badlogic.gdx.utils.Array) addVertexAttributes()}. Such a
 * subclass would not be compatible with a FlexBatch that was instantiated for the base Quad2D type.
 * 
 * @author cypherdare */
public class Quad2D extends Quad {
	public float rotation;

	protected final boolean isPosition3D () {
		return false;
	}

	protected boolean isTextureCoordinate3D () {
		return false;
	}

	protected void prepareSharedContext (@NotNull RenderContextAccumulator renderContext) {
		super.prepareSharedContext(renderContext);
		renderContext.setDepthMasking(false);
	}

	public void refresh () {
		super.refresh();
		rotation = 0;
	}

	/** Sets the position of the bottom left of the texture region in world space.
	 * @return This object for chaining. */
	public @NotNull Quad2D position (float x, float y) {
		this.x = x;
		this.y = y;
		return this;
	}

	/** Sets the position of the bottom left of the texture region in world space.
	 * @return This object for chaining. */
	public @NotNull Quad2D position (@NotNull Vector2 position) {
		x = position.x;
		y = position.y;
		return this;
	}

	public @NotNull Quad2D rotation (float rotation) {
		this.rotation = rotation;
		return this;
	}

	protected int apply (@NotNull float[] vertices, int vertexStartingIndex, @NotNull AttributeOffsets offsets, int vertexSize) {
		super.apply(vertices, vertexStartingIndex, offsets, vertexSize);

		// bottom left and top right corner points relative to origin
		final float worldOriginX = x + originX;
		final float worldOriginY = y + originY;
		float fx = -originX;
		float fy = -originY;
		float fx2 = width - originX;
		float fy2 = height - originY;

		// scale
		if (scaleX != 1 || scaleY != 1) {
			fx *= scaleX;
			fy *= scaleY;
			fx2 *= scaleX;
			fy2 *= scaleY;
		}

		// construct corner points
		float x1;
		float y1;
		float x2;
		float y2;
		float x3;
		float y3;
		float x4;
		float y4;

		// rotate
		if (rotation != 0) {
			final float cos = MathUtils.cosDeg(rotation);
			final float sin = MathUtils.sinDeg(rotation);

			x1 = cos * fx - sin * fy;
			y1 = sin * fx + cos * fy;

			x2 = cos * fx - sin * fy2;
			y2 = sin * fx + cos * fy2;

			x3 = cos * fx2 - sin * fy2;
			y3 = sin * fx2 + cos * fy2;

			x4 = x1 + (x3 - x2);
			y4 = y3 - (y2 - y1);
		} else {
			x1 = fx;
			y1 = fy;

			x2 = fx;
			y2 = fy2;

			x3 = fx2;
			y3 = fy2;

			x4 = fx2;
			y4 = fy;
		}

		x1 += worldOriginX;
		y1 += worldOriginY;
		x2 += worldOriginX;
		y2 += worldOriginY;
		x3 += worldOriginX;
		y3 += worldOriginY;
		x4 += worldOriginX;
		y4 += worldOriginY;

		int i = vertexStartingIndex;
		vertices[i] = x1;
		vertices[i + 1] = y1;
		i += vertexSize;
		vertices[i] = x2;
		vertices[i + 1] = y2;
		i += vertexSize;
		vertices[i] = x3;
		vertices[i + 1] = y3;
		i += vertexSize;
		vertices[i] = x4;
		vertices[i + 1] = y4;

		return 4;
	}

	// Chain methods must be overridden to allow return of subclass type.

	public @NotNull Quad2D texture (@NotNull Texture texture) {
		super.texture(texture);
		return this;
	}

	public @NotNull Quad2D region (float u, float v, float u2, float v2) {
		super.region(u, v, u2, v2);
		return this;
	}

	public @NotNull Quad2D textureRegion (@NotNull TextureRegion region) {
		super.textureRegion(region);
		return this;
	}

	public @NotNull Quad2D flip (boolean flipX, boolean flipY) {
		super.flip(flipX, flipY);
		return this;
	}

	public @NotNull Quad2D flipAll (boolean flipX, boolean flipY) {
		super.flipAll(flipX, flipY);
		return this;
	}

	public @NotNull Quad2D size (float width, float height) {
		super.size(width, height);
		return this;
	}

	public @NotNull Quad2D origin (float originX, float originY) {
		super.origin(originX, originY);
		return this;
	}

	public @NotNull Quad2D rotateCoordinates90 (boolean clockwise) {
		super.rotateCoordinates90(clockwise);
		return this;
	}

	public @NotNull Quad2D color (@NotNull Color color) {
		super.color(color);
		return this;
	}

	public @NotNull Quad2D color (float r, float g, float b, float a) {
		super.color(r, g, b, a);
		return this;
	}

	public @NotNull Quad2D color (float floatBits) {
		super.color(floatBits);
		return this;
	}

	public @NotNull Quad2D scale (float scaleX, float scaleY) {
		super.scale(scaleX, scaleY);
		return this;
	}
}
