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
import com.badlogic.gdx.graphics.GLTexture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.NumberUtils;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.cyphercove.flexbatch.Batchable.FixedSizeBatchable;
import com.cyphercove.flexbatch.utils.AttributeOffsets;
import com.cyphercove.flexbatch.utils.BatchablePreparation;
import com.cyphercove.flexbatch.utils.Region2D;
import com.cyphercove.flexbatch.utils.RenderContextAccumulator;

/** A Batchable representing a rectangle and supporting zero or more Textures/TextureRegions, and supporting color, position,
 * scale, and an origin offset.
 * <p>
 * By default, one texture is used. It may be subclassed to create a Batchable class that supports zero or multiple textures and
 * additional attributes--see {@link #getNumberOfTextures()} and {@link #addVertexAttributes(com.badlogic.gdx.utils.Array)
 * addVertexAttributes()}.
 * <p>
 * A Quad has fixed size, so its indices do not need to be recalculated for every draw call.
 * 
 * @author cypherdare */
public abstract class Quad extends FixedSizeBatchable implements Poolable {
	protected final GLTexture[] textures;
	protected final Region2D[] regions;
	private int regionIndex = -1;
	public float x, y, color = WHITE, originX, originY, scaleX = 1, scaleY = 1;
	/** Width and height must be set with {@link #size(float, float)}. If they are not set, they default to the size of the first
	 * texture region. */
	protected float width, height;
	/** Whether the width and height have been set since the last call to {@link #refresh()}. If they have not been set, they will
	 * be set to match the size of the first texture region when drawing occurs. */
	protected boolean sizeSet;
	/** The number of times the texture coordinates should be rotated clockwise by 90 degrees. Must be positive. */
	public int coordinatesRotation;
	protected static final float WHITE = Color.WHITE.toFloatBits();

	protected Quad () {
		textures = new GLTexture[getNumberOfTextures()];
		regions = new Region2D[getNumberOfTextures()];
		for (int i = 0; i < regions.length; i++)
			regions[i] = new Region2D();
	}

	protected final void populateTriangleIndices (short[] triangles) {
		BatchablePreparation.populateQuadrangleIndices(triangles);
	}

	protected final int getTrianglesPerBatchable () {
		return 2;
	}

	protected final int getVerticesPerBatchable () {
		return 4;
	}

	protected void addVertexAttributes (Array<VertexAttribute> attributes) {
		BatchablePreparation.addBaseAttributes(attributes, getNumberOfTextures(), isPosition3D(), isTextureCoordinate3D());
	}

	protected int getNumberOfTextures () {
		return 1;
	}

	/** Determines whether the position data has a Z component. Must return the same constant value for every instance of the
	 * class.
	 * <p>
	 * Overriding this method will produce a subclass that is incompatible with a FlexBatch that was instantiated for the
	 * superclass type. */
	protected abstract boolean isPosition3D ();

	/** Determines whether the texture coordinate data has a third component (for TextureArray layers). Must return the same
	 * constant value for every instance of the class.
	 * <p>
	 * Overriding this method will produce a subclass that is incompatible with a FlexBatch that was instantiated for the
	 * superclass type. */
	protected abstract boolean isTextureCoordinate3D ();

	protected boolean prepareContext (RenderContextAccumulator renderContext, int remainingVertices, int remainingIndices) {
		boolean textureChanged = false;
		for (int i = 0; i < textures.length; i++) {
			textureChanged |= renderContext.setTextureUnit(textures[i], i);
		}

		return textureChanged || remainingVertices < 4;
	}

	public void refresh () { // Does not reset textures, in the interest of speed. There is no need for the concept of default
										// textures.
		x = y = originX = originY = 0;
		coordinatesRotation = 0;
		scaleX = scaleY = 1;
		color = WHITE;
		regionIndex = -1;
		sizeSet = false;
	}

	/** Resets the state of the object and drops Texture references to prepare it for returning to a {@link Pool}. */
	public void reset () {
		refresh();
		for (int i = 0; i < textures.length; i++) {
			textures[i] = null;
		}
	}

	/** Sets the texture and sets the region to match the size of the texture. If this Batchable supports multi-texturing, multiple
	 * subsequent calls to this method or {@link #textureRegion(TextureRegion)} will sequentially set the textures in order.
	 * <p>
	 * This method must not be called in a Batchable that supports zero textures.
	 * @return This object for chaining. */
	public Quad texture (GLTexture texture) {
		regionIndex = (regionIndex + 1) % getNumberOfTextures();
		textures[regionIndex] = texture;
		regions[regionIndex].setFull();
		return this;
	}

	/** Sets the UV region of the most recently applied texture. This must be called after a texture or texture region has been set
	 * with {@link #texture(GLTexture)} or {@link #textureRegion(TextureRegion)}. Note that TextureRegions have a Y-down coordinate
	 * system for UVs.
	 * <p>
	 * This method must not be called in a Batchable that supports zero textures.
	 * @param u The left side of the region.
	 * @param v The top side of the region.
	 * @param u2 The right side of the region.
	 * @param v2 The bottom side of the region.
	 * @return This object for chaining. */
	public Quad region (float u, float v, float u2, float v2) {
		Region2D region = regions[regionIndex];
		region.u = u;
		region.v = v;
		region.u2 = u2;
		region.v2 = v2;
		return this;
	}

	/** Sets the UV region of the most recently applied texture, using texel units. This must be called after a Texture or
	 * TextureRegion has been set with {@link #texture(GLTexture)} or {@link #textureRegion(TextureRegion)}. Note that
	 * TextureRegions have a Y-down coordinate system for UVs.
	 * <p>
	 * This method must not be called in a Batchable that supports zero textures.
	 * @param x The left side of the region.
	 * @param y The top side of the region.
	 * @param width The width of the region. May be negative to flip the region in place.
	 * @param height The height of the region. May be negative to flip the region in place.
	 * @return This object for chaining. */
	public Quad regionTexels (int x, int y, int width, int height) {
		GLTexture texture = textures[regionIndex];
		float invTexWidth = 1f / texture.getWidth();
		float invTexHeight = 1f / texture.getHeight();
		return region(x * invTexWidth, y * invTexHeight, (x + width) * invTexWidth, (y + height) * invTexHeight);
	}

	/** Sets the texture region. If this Batchable supports multi-texturing, multiple subsequent calls to this method or
	 * {@link #texture(GLTexture)} will sequentially set the textures in order.
	 * <p>
	 * This method must not be called in a Batchable that supports zero textures.
	 * @return This object for chaining. */
	public Quad textureRegion (TextureRegion region) {
		regionIndex = (regionIndex + 1) % getNumberOfTextures();
		textures[regionIndex] = region.getTexture();
		regions[regionIndex].set(region);
		return this;
	}

	/** Flips the UV region of the most recently applied texture. This must be called after a texture or region has been set with
	 * {@link #texture(GLTexture)} or {@link #textureRegion(TextureRegion)}.
	 * @return This object for chaining. */
	public Quad flip (boolean flipX, boolean flipY) {
		regions[regionIndex].flip(flipX, flipY);
		return this;
	}

	/** Flips the texture region(s) from their current state. Must be called after regions or texture regions have already been
	 * set.
	 * @return This object for chaining. */
	public Quad flipAll (boolean flipX, boolean flipY) {
		for (int i = 0; i < regions.length; i++) {
			regions[i].flip(flipX, flipY);
		}
		return this;
	}

	public Quad size (float width, float height) {
		this.width = width;
		this.height = height;
		sizeSet = true;
		return this;
	}

	/** Sets the center point for transformations (rotation and scale). For {@link Quad2D}, this is relative to the bottom left
	 * corner of the texture region. For {@link Quad3D}, this is relative to the center of the texture region and is in the local
	 * coordinate system.
	 * @return This object for chaining. */
	public Quad origin (float originX, float originY) {
		this.originX = originX;
		this.originY = originY;
		return this;
	}

	/** Rotates the texture region(s) 90 degrees in place by rotating the texture coordinates.
	 * @return This object for chaining. */
	public Quad rotateCoordinates90 (boolean clockwise) {
		coordinatesRotation += clockwise ? 1 : 3;
		return this;
	}

	public Quad color (Color color) {
		this.color = color.toFloatBits();
		return this;
	}

	public Quad color (float r, float g, float b, float a) {
		int intBits = (int)(255 * a) << 24 | (int)(255 * b) << 16 | (int)(255 * g) << 8 | (int)(255 * r);
		color = NumberUtils.intToFloatColor(intBits);
		return this;
	}

	public Quad color (float floatBits) {
		color = floatBits;
		return this;
	}

	public Quad scale (float scaleX, float scaleY) {
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		return this;
	}

	protected int apply (float[] vertices, int vertexStartingIndex, AttributeOffsets offsets, int vertexSize) {
		if (!sizeSet && regions.length > 0) {
			Region2D region = regions[0];
			width = (region.u2 - region.u) * textures[0].getWidth();
			height = (region.v2 - region.v) * textures[0].getHeight();
		}

		float color = this.color;
		int ci = vertexStartingIndex + offsets.color0;
		vertices[ci] = color;
		ci += vertexSize;
		vertices[ci] = color;
		ci += vertexSize;
		vertices[ci] = color;
		ci += vertexSize;
		vertices[ci] = color;

		int tci = vertexStartingIndex + offsets.textureCoordinate0;
		int tcSize = isTextureCoordinate3D() ? 3 : 2;

		switch (coordinatesRotation % 4) {
		case 0:
			for (int i = 0; i < regions.length; i++) {
				Region2D region = regions[i];
				final float u = region.u;
				final float v = region.v;
				final float u2 = region.u2;
				final float v2 = region.v2;

				int temp = tci;
				vertices[tci] = u;
				vertices[tci + 1] = v2;
				tci += vertexSize;
				vertices[tci] = u;
				vertices[tci + 1] = v;
				tci += vertexSize;
				vertices[tci] = u2;
				vertices[tci + 1] = v;
				tci += vertexSize;
				vertices[tci] = u2;
				vertices[tci + 1] = v2;

				tci = temp + tcSize;
			}
			break;
		case 1:
			for (int i = 0; i < regions.length; i++) {
				Region2D region = regions[i];
				final float u = region.u;
				final float v = region.v;
				final float u2 = region.u2;
				final float v2 = region.v2;

				int temp = tci;
				vertices[tci] = u2;
				vertices[tci + 1] = v2;
				tci += vertexSize;
				vertices[tci] = u;
				vertices[tci + 1] = v2;
				tci += vertexSize;
				vertices[tci] = u;
				vertices[tci + 1] = v;
				tci += vertexSize;
				vertices[tci] = u2;
				vertices[tci + 1] = v;

				tci = temp + tcSize;
			}
			break;
		case 2:
			for (int i = 0; i < regions.length; i++) {
				Region2D region = regions[i];
				final float u = region.u;
				final float v = region.v;
				final float u2 = region.u2;
				final float v2 = region.v2;

				int temp = tci;
				vertices[tci] = u2;
				vertices[tci + 1] = v;
				tci += vertexSize;
				vertices[tci] = u2;
				vertices[tci + 1] = v2;
				tci += vertexSize;
				vertices[tci] = u;
				vertices[tci + 1] = v2;
				tci += vertexSize;
				vertices[tci] = u;
				vertices[tci + 1] = v;

				tci = temp + tcSize;
			}
			break;
		case 3:
			for (int i = 0; i < regions.length; i++) {
				Region2D region = regions[i];
				final float u = region.u;
				final float v = region.v;
				final float u2 = region.u2;
				final float v2 = region.v2;
				int temp = tci;
				vertices[tci] = u;
				vertices[tci + 1] = v;
				tci += vertexSize;
				vertices[tci] = u2;
				vertices[tci + 1] = v;
				tci += vertexSize;
				vertices[tci] = u2;
				vertices[tci + 1] = v2;
				tci += vertexSize;
				vertices[tci] = u;
				vertices[tci + 1] = v2;

				tci = temp + tcSize;
			}
			break;
		}

		if (isTextureCoordinate3D()) {
			int tci3 = ci + 3;
			for (int i = 0; i < regions.length; i++) {
				Region2D region = regions[i];
				final float layer = (float)region.layer;
				int temp = tci3;
				vertices[tci3] = layer;
				tci3 += vertexSize;
				vertices[tci3] = layer;
				tci3 += vertexSize;
				vertices[tci3] = layer;
				tci3 += vertexSize;
				vertices[tci3] = layer;

				tci3 = temp + tcSize;
			}
		}

		return 4;
	}
}
