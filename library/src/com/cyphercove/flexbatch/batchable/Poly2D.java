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
package com.cyphercove.flexbatch.batchable;

import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.cyphercove.flexbatch.utils.AttributeOffsets;
import com.cyphercove.flexbatch.utils.RenderContextAccumulator;

public class Poly2D extends Poly<Poly2D> {
	public float rotation;

	@Override
	protected boolean isPosition3D () {
		return false;
	}

	@Override
	protected boolean isTextureCoordinate3D () {
		return false;
	}

	@Override
	protected void prepareSharedContext (RenderContextAccumulator renderContext) {
		super.prepareSharedContext(renderContext);
		renderContext.setDepthMasking(false);
	}

	@Override
	public void refresh () {
		super.refresh();
		rotation = 0;
	}

	/** Sets the position of the bottom left of the texture region in world space.
	 * @return This object for chaining. */
	public Poly2D position (float x, float y) {
		this.x = x;
		this.y = y;
		return this;
	}

	/** Sets the position of the bottom left of the texture region in world space.
	 * @return This object for chaining. */
	public Poly2D position (Vector2 position) {
		x = position.x;
		y = position.y;
		return this;
	}

	public Poly2D rotation (float rotation) {
		this.rotation = rotation;
		return this;
	}

	@Override
	protected int apply (float[] vertices, int vertexStartingIndex, AttributeOffsets offsets, int vertexSize) {
		super.apply(vertices, vertexStartingIndex, offsets, vertexSize);
		final PolygonRegion region = this.region;
		final TextureRegion tRegion = region.getRegion();

		final float originX = this.originX;
		final float originY = this.originY;
		final float scaleX = this.scaleX;
		final float scaleY = this.scaleY;
		final float[] regionVertices = region.getVertices();

		final float worldOriginX = x + originX;
		final float worldOriginY = y + originY;
		final float sX = width / tRegion.getRegionWidth();
		final float sY = height / tRegion.getRegionHeight();
		final float cos = MathUtils.cosDeg(rotation);
		final float sin = MathUtils.sinDeg(rotation);

		float fx, fy;
		for (int i = 0, v = vertexStartingIndex + offsets.position, n = regionVertices.length; i < n; i += 2, v += vertexSize) {
			fx = (regionVertices[i] * sX - originX) * scaleX;
			fy = (regionVertices[i + 1] * sY - originY) * scaleY;
			vertices[v] = cos * fx - sin * fy + worldOriginX;
			vertices[v + 1] = sin * fx + cos * fy + worldOriginY;
		}

		return numVertices;
	}

}
