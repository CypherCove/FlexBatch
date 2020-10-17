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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GLTexture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.NumberUtils;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.cyphercove.flexbatch.Batchable;
import com.cyphercove.flexbatch.Batchable.FixedSizeBatchable;
import com.cyphercove.flexbatch.utils.AttributeOffsets;
import com.cyphercove.flexbatch.utils.BatchablePreparation;
import com.cyphercove.flexbatch.utils.Region2D;
import com.cyphercove.flexbatch.utils.RenderContextAccumulator;

import java.util.Arrays;

/** A Batchable representing a rectangle and supporting zero or more Textures/TextureRegions, and supporting color, position,
 * scale, and an origin offset.
 * <p>
 * By default, one texture is used. It may be subclassed to create a Batchable class that supports zero or multiple textures and
 * additional attributes--see {@link #getNumberOfTextures()} and {@link #addVertexAttributes(com.badlogic.gdx.utils.Array)
 * addVertexAttributes()}.
 * <p>
 * A Quad has fixed size, so its indices do not need to be recalculated for every draw call.
 *
 * @param <T> The type returned by the chain methods. The object must be able to be cast to this type.
 * @author cypherdare */
public abstract class Quad<T extends Quad<T>> extends FixedSizeBatchable {
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

	@Override
	protected int getPrimitiveType() {
		return GL20.GL_TRIANGLES;
	}

	@Override
	protected final void populateIndices(short[] indices) {
		BatchablePreparation.populateQuadrangleIndices(indices);
	}

	@Override
	protected final int getPrimitivesPerBatchable() {
		return 2;
	}

	@Override
	protected final int getVerticesPerBatchable () {
		return 4;
	}

	@Override
	protected void addVertexAttributes (Array<VertexAttribute> attributes) {
		BatchablePreparation.addBaseAttributes(attributes, getNumberOfTextures(), isPosition3D(), isTextureCoordinate3D());
	}

	@Override
	protected int getNumberOfTextures () {
		return 1;
	}

	@Override
	public boolean hasEquivalentTextures(Batchable other) {
		if (other instanceof Quad) {
			Quad<?> quad = (Quad<?>)other;
			if (getNumberOfTextures() == 1)
				return quad.textures[0] == textures[0];
			int count = Math.min(getNumberOfTextures(), quad.getNumberOfTextures());
			for (int i = 0; i < count; i++) {
				if (quad.textures[i] != textures[i]) return false;
			}
			return true;
		}
		return true; // This is arbitrary because Quads should not be compared to non-Quads. Subclasses can customize this.
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

	@Override
	protected boolean prepareContext (RenderContextAccumulator renderContext, int remainingVertices, int remainingIndices) {
		boolean textureChanged = false;
		for (int i = 0; i < textures.length; i++) {
			textureChanged |= renderContext.setTextureUnit(textures[i], i);
		}

		return textureChanged || remainingVertices < 4;
	}

	@Override
	public void refresh () {
		// Does not reset textures, in the interest of speed. There is no need for the concept of default textures.
		x = y = originX = originY = 0;
		coordinatesRotation = 0;
		scaleX = scaleY = 1;
		color = WHITE;
		regionIndex = -1;
		sizeSet = false;
	}

	/** Resets the state of the object and drops Texture references to prepare it for returning to a {@link Pool}. */
	@Override
	public void reset () {
		refresh();
		Arrays.fill(textures, null);
	}

	/** Sets the texture and sets the region to match the size of the texture. If this Batchable supports multi-texturing, multiple
	 * subsequent calls to this method or {@link #textureRegion(TextureRegion)} will sequentially set the textures in order.
	 * <p>
	 * This method must not be called in a Batchable that supports zero textures.
	 * @return This object for chaining. */
	public T texture (GLTexture texture) {
		regionIndex = (regionIndex + 1) % getNumberOfTextures();
		textures[regionIndex] = texture;
		regions[regionIndex].setFull();
		//noinspection unchecked
		return (T)this;
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
	public T region (float u, float v, float u2, float v2) {
		Region2D region = regions[regionIndex];
		region.u = u;
		region.v = v;
		region.u2 = u2;
		region.v2 = v2;
		//noinspection unchecked
		return (T)this;
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
	public T regionTexels (int x, int y, int width, int height) {
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
	public T textureRegion (TextureRegion region) {
		regionIndex = (regionIndex + 1) % getNumberOfTextures();
		textures[regionIndex] = region.getTexture();
		regions[regionIndex].set(region);
		//noinspection unchecked
		return (T)this;
	}

	/** Flips the UV region of the most recently applied texture. This must be called after a texture or region has been set with
	 * {@link #texture(GLTexture)} or {@link #textureRegion(TextureRegion)}.
	 * @return This object for chaining. */
	public T flip (boolean flipX, boolean flipY) {
		regions[regionIndex].flip(flipX, flipY);
		//noinspection unchecked
		return (T)this;
	}

	/** Flips the texture region(s) from their current state. Must be called after regions or texture regions have already been
	 * set.
	 * @return This object for chaining. */
	public T flipAll (boolean flipX, boolean flipY) {
		for (Region2D region : regions) {
			region.flip(flipX, flipY);
		}
		//noinspection unchecked
		return (T)this;
	}

	public T size (float width, float height) {
		this.width = width;
		this.height = height;
		sizeSet = true;
		//noinspection unchecked
		return (T)this;
	}

	/** Sets the center point for transformations (rotation and scale), and for positioning. For {@link Quad2D}, this is
	 *  relative to the bottom left corner of the texture region. For {@link Quad3D}, this is relative to the center of
	 *  the texture region and is in the local coordinate system.
	 * @return This object for chaining. */
	public T origin (float originX, float originY) {
		this.originX = originX;
		this.originY = originY;
		//noinspection unchecked
		return (T)this;
	}

	/** Rotates the texture region(s) 90 degrees in place by rotating the texture coordinates.
	 * @return This object for chaining. */
	public T rotateCoordinates90 (boolean clockwise) {
		coordinatesRotation += clockwise ? 1 : 3;
		//noinspection unchecked
		return (T)this;
	}

	public T color (Color color) {
		this.color = color.toFloatBits();
		//noinspection unchecked
		return (T)this;
	}

	public T color (float r, float g, float b, float a) {
		int intBits = (int)(255 * a) << 24 | (int)(255 * b) << 16 | (int)(255 * g) << 8 | (int)(255 * r);
		color = NumberUtils.intToFloatColor(intBits);
		//noinspection unchecked
		return (T)this;
	}

	public T color (float floatBits) {
		color = floatBits;
		//noinspection unchecked
		return (T)this;
	}

	public T scale (float scaleX, float scaleY) {
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		//noinspection unchecked
		return (T)this;
	}

	@Override
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
			for (Region2D region : regions) {
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
			for (Region2D region : regions) {
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
			for (Region2D region : regions) {
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
			for (Region2D region : regions) {
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
			for (Region2D region : regions) {
				final float layer = (float) region.layer;
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
