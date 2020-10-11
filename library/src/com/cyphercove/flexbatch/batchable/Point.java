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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.NumberUtils;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.cyphercove.flexbatch.Batchable;
import com.cyphercove.flexbatch.utils.AttributeOffsets;
import com.cyphercove.flexbatch.utils.GLConstants;
import com.cyphercove.flexbatch.utils.RenderContextAccumulator;

import java.nio.FloatBuffer;
import java.util.Arrays;

/** A Batchable representing a point drawn as a square after projection. Supports zero or more Textures, but not
 * TextureRegions. Supports color, position and size (with shader attribute name {@code "a_size"}. The square is centered
 * on the position.
 * <p>
 * By default, one texture is used. It may be subclassed to create a Batchable class that supports zero or multiple textures and
 * additional attributes--see {@link #getNumberOfTextures()} and {@link #addVertexAttributes(Array)
 * addVertexAttributes()}.
 * <p>
 * The shader used to draw a Point must be designed for use with {@code glPoint}s.
 *
 * @param <T> The type returned by the chain methods. The object must be able to be cast to this type.
 * @author cypherdare */
public abstract class Point<T> extends Batchable implements Poolable {
	protected final GLTexture[] textures;
	private int textureIndex = -1;
	public float x, y, color = WHITE, size;
	protected static final float WHITE = Color.WHITE.toFloatBits();

	protected Point() {
		textures = new GLTexture[getNumberOfTextures()];
	}

	@Override
	protected int getPrimitiveType() {
		return GL20.GL_POINTS;
	}

	@Override
	protected void addVertexAttributes (Array<VertexAttribute> attributes) {
		attributes.add(new VertexAttribute(VertexAttributes.Usage.Position, isPosition3D() ? 3 : 2, ShaderProgram.POSITION_ATTRIBUTE));
		attributes.add(new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
		attributes.add(new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_size"));
	}

	@Override
	protected int getNumberOfTextures () {
		return 1;
	}

	@Override
	public boolean hasEquivalentTextures(Batchable other) {
		if (other instanceof Point) {
			Point<?> point = (Point<?>)other;
			if (getNumberOfTextures() == 1)
				return point.textures[0] == textures[0];
			int count = Math.min(getNumberOfTextures(), point.getNumberOfTextures());
			for (int i = 0; i < count; i++) {
				if (point.textures[i] != textures[i]) return false;
			}
			return true;
		}
		return true; // This is arbitrary because Points should not be compared to non-Points. Subclasses can customize this.
	}

	/** Determines whether the position data has a Z component. Must return the same constant value for every instance of the
	 * class.
	 * <p>
	 * Overriding this method will produce a subclass that is incompatible with a FlexBatch that was instantiated for the
	 * superclass type. */
	protected abstract boolean isPosition3D ();

	protected boolean prepareContext (RenderContextAccumulator renderContext, int remainingVertices, int remainingIndices) {
		boolean textureChanged = false;
		for (int i = 0; i < textures.length; i++) {
			textureChanged |= renderContext.setTextureUnit(textures[i], i);
		}
		return textureChanged || remainingVertices < 1;
	}

	public void refresh () {
		// Does not reset textures, in the interest of speed. There is no need for the concept of default textures.
		x = y = 0f;
		size = 1f;
		color = WHITE;
		textureIndex = -1;
	}

	/** Resets the state of the object and drops Texture references to prepare it for returning to a {@link Pool}. */
	public void reset () {
		refresh();
		Arrays.fill(textures, null);
	}

	/** Sets the texture and sets the region to match the size of the texture. If this Batchable supports multi-texturing, multiple
	 * subsequent calls to this method will sequentially set the textures in order.
	 * <p>
	 * This method must not be called in a Batchable that supports zero textures.
	 * @return This object for chaining. */
	public T texture (GLTexture texture) {
		textureIndex = (textureIndex + 1) % getNumberOfTextures();
		textures[textureIndex] = texture;
		//noinspection unchecked
		return (T)this;
	}

	public T size (float size) {
		this.size = size;
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

	@Override
	protected int apply(short[] indices, int startingIndex, short firstVertex) {
		return 0; // Points do not use indices
	}

	/**
	 * Note: Point uses generic attribute {@link AttributeOffsets#generic0} internally. Additional generic attributes
	 * should start at {@link AttributeOffsets#generic1}.
	 */
	@Override
	protected int apply (float[] vertices, int vertexStartingIndex, AttributeOffsets offsets, int vertexSize) {
		vertices[vertexStartingIndex + offsets.color0] = color;
		vertices[vertexStartingIndex + offsets.generic0] = size;
		return 1;
	}

	private static float minimumPointSize = -1, maximumPointSize, minimumSmoothPointSize, maximumSmoothPointSize, pointSizeGranularity;

	private static void initializeImplementationConstants () {
		if (minimumPointSize == -1) {
			FloatBuffer buffer = BufferUtils.newFloatBuffer(16);
			buffer.position(0);
			buffer.limit(buffer.capacity());
			Gdx.gl20.glGetFloatv(GL20.GL_ALIASED_POINT_SIZE_RANGE, buffer);
			minimumPointSize = buffer.get(0);
			maximumPointSize = buffer.get(1);
			buffer.position(0);
			Gdx.gl20.glGetFloatv(GLConstants.GL_SMOOTH_POINT_SIZE_RANGE, buffer);
			minimumSmoothPointSize = buffer.get(0);
			maximumSmoothPointSize = buffer.get(1);
			buffer.position(0);
			Gdx.gl20.glGetFloatv(GLConstants.GL_SMOOTH_POINT_SIZE_GRANULARITY , buffer);
			pointSizeGranularity = buffer.get(0);
		}
	}

	public static float getMinimumPointSize() {
		initializeImplementationConstants();
		return minimumPointSize;
	}

	public static float getMaximumPointSize() {
		initializeImplementationConstants();
		return maximumPointSize;
	}

	public static float getMinimumSmoothPointSize() {
		initializeImplementationConstants();
		return minimumSmoothPointSize;
	}

	public static float getMaximumSmoothPointSize() {
		initializeImplementationConstants();
		return maximumSmoothPointSize;
	}

	public static float getPointSizeGranularity() {
		initializeImplementationConstants();
		return pointSizeGranularity;
	}
}
