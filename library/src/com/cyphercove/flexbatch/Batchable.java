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
package com.cyphercove.flexbatch;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.cyphercove.flexbatch.utils.AttributeOffsets;
import com.cyphercove.flexbatch.utils.BatchableSorter;
import com.cyphercove.flexbatch.utils.RenderContextAccumulator;

/** A Batchable is an object that can be drawn by a FlexBatch. It also serves as a template object for the FlexBatch to set itself
 * up. All subclasses of Batchable must have an empty constructor if they are to be used as a template for FlexBatch.
 * 
 * @author cypherdare */
public abstract class Batchable implements Poolable {
	// This is an abstract class instead of interface so most of these methods can be hidden from the public API to make
	// built-in implementations of Batchable easier to use.

	/** Prepares the rendering context for this type of Batchable to be drawn. This method should set state changes that will be
	 * the same for all instances of this Batchable type. It must not call {@link RenderContextAccumulator#begin() begin()},
	 * {@link RenderContextAccumulator#executeChanges() executeChanges()}, or {@link RenderContextAccumulator#end() end()} on
	 * {@code renderContext}, as these are handled by the FlexBatch. It should not change anything in a way that is unique to any
	 * particular instance of this Batchable type.
	 * 
	 * @param renderContext The FlexBatch's manager of GL state changes and texture bindings. */
	protected void prepareSharedContext (RenderContextAccumulator renderContext) {
	}

	/** Called before {@link #apply(float[], int, AttributeOffsets, int)} to set render context states and texture bindings, and to
	 * determine if a flush is necessary before this Batchable can be drawn.
	 * 
	 * Overriding this method may produce a subclass that is incompatible with a FlexBatch that was instantiated for the superclass
	 * type.
	 * 
	 * @param renderContext The FlexBatch's manager of GL state changes and texture bindings. Pending state changes and texture
	 *           bindings can be made on this context, and the context will return whether the context is now changed, which means
	 *           a flush is required and this method needs to return true.
	 * @param remainingVertices The number of vertices that can be drawn before the next flush is required.
	 * @param remainingIndices The number of triangle indices that can be drawn before the next flush is required. This value is
	 *           undefined for {@link FixedSizeBatchable} and should not be used or checked against.
	 * 
	 * @return Whether FlexBatch needs to be flushed before this Batchable can be drawn. This may be true because it binds new
	 *         textures, there isn't enough vertex or triangle capacity left, or because this Batchable uses new render context
	 *         parameters. */
	protected abstract boolean prepareContext (RenderContextAccumulator renderContext, int remainingVertices,
		int remainingIndices);

	/** A Batchable implementation calls this to populate a list of vertex attributes that will be used by the FlexBatch. This is
	 * called only on one of the FlexBatch's internal Batchable instances. All instances of a class must have an equivalent set of
	 * attributes. Any subclass that does not have equivalent attributes to the superclass cannot be drawn by a FlexBatch that was
	 * instantiated with the superclass type.
	 * @param attributes An array to which attributes can be added. */
	protected abstract void addVertexAttributes (Array<VertexAttribute> attributes);

	/** @return The number of simultaneous textures that are drawn. This is used by the FlexBatch to determine how many texture
	 *         uniforms to bind to the shader. Must always return the same value. */
	protected abstract int getNumberOfTextures ();

	/** Used by {@link BatchableSorter} to determine if two Batchables can be drawn without flushing in between because
	 * their textures are equivalent. Usually, only Batchables of the same type are compared in this way, so subclasses
	 * should only need to cast and check for {@code other} being their own type.
	 * See {@link com.cyphercove.flexbatch.batchable.Quad#hasEquivalentTextures(Batchable) Quad.hasEquivalentTextures()}
	 * for an example.
	 * @param other Another Batchable
	 * @return Whether this Batchable and the other have the same texture configuration such that they could be drawn sequentially
	 *         without forcing the FlexBatch to flush in between. */
	public abstract boolean hasEquivalentTextures (Batchable other);

	/** Resets the state and default parameters of the Batchable so it can be reused for an entirely new image. */
	public abstract void refresh ();

	/** Called by FlexBatch. Applies the Batchable data to the vertex array that will be sent to the Mesh.
	 * 
	 * Overriding this method may produce a subclass that is incompatible with a FlexBatch that was instantiated for the superclass
	 * type.
	 * @param vertices Vertex data of the backing Mesh.
	 * @param startingIndex Index in the data from which this Batchable's data will be written.
	 * @param offsets The offsets of the vertex attributes.
	 * @param vertexSize The size of a vertex in floats.
	 * @return The number of vertices that were added. The value is unused if this is a {@link FixedSizeBatchable}, as it is
	 *         assumed to match {@link FixedSizeBatchable#getVerticesPerBatchable()}. */
	protected abstract int apply (float[] vertices, int startingIndex, AttributeOffsets offsets, int vertexSize);

	/** Called by FlexBatch. Applies the triangle vertex data indices to the array that will be sent to the Mesh. This method is
	 * never called on {@link FixedSizeBatchable FixedSizeBatchables}.
	 * 
	 * Overriding this method may produce a subclass that is incompatible with a FlexBatch that was instantiated for the superclass
	 * type.
	 * @param indices Vertex data of the backing Mesh.
	 * @param startingIndex Index in the data from which this Batchable's data will be written.
	 * @param firstVertex The first vertex value that should be used.
	 * @return The number of triangle indices that were added. */
	protected abstract int apply (short[] indices, int startingIndex, short firstVertex);

	/** @return The type of OpenGL primitive to draw. Must always return the same value among all instances of a
	 *         class. Should be one of
	 * <ul>
	 *     <li>{@link GL20#GL_POINTS}</li>
	 *     <li>{@link GL20#GL_LINES}</li>
	 *     <li>{@link GL20#GL_TRIANGLES}</li>
	 * </ul>
	 */
	protected abstract int getPrimitiveType ();

	/** @return The number of indices used for each OpenGL primitive, where primitive type is determined by
	 * {@link #getPrimitiveType()}. Since {@link com.badlogic.gdx.graphics.GL20#GL_POINTS GL20.GL_POINTS} do not use
	 * indices, 0 is returned if the primitive type is points.
	 */
	protected final int getIndicesPerPrimitive () {
		switch (getPrimitiveType()) {
			case GL20.GL_POINTS: return 0;
			case GL20.GL_LINES: return 2;
			default: return 3;
		}
	}

	/** Parent class for Batchables that all have the same number of vertices and lines/triangles. This allows all
	 * indices for a FlexBatch to be generated one time so they don't have to be repeatedly updated when drawing.
	 * This type should not be used for a primitive type of {@link GL20#GL_POINTS}, since points do not need indices. */
	public static abstract class FixedSizeBatchable extends Batchable {

		private static final ObjectMap<Class<? extends FixedSizeBatchable>, short[]> indicesModels = new ObjectMap<>();

		/** Primes a FixedSizeBatchable implementation for drawing with FlexBatches that are not limited to FixedSizeBatchables.
		 * Calling this directly before beginning drawing the first time may avoid a one-time delay the first time one
		 * is drawn. */
		public static <T extends FixedSizeBatchable> void prepareIndices (Class<T> fixedSizeBatchableType) {
			T instance;
			try {
				instance = fixedSizeBatchableType.newInstance();
			} catch (Exception e) {
				throw new IllegalArgumentException("Batchable classes must be public and have an empty constructor.", e);
			}
			short[] model = new short[instance.getPrimitivesPerBatchable() * instance.getIndicesPerPrimitive()];
			instance.populateIndices(model);
			indicesModels.put(fixedSizeBatchableType, model);
		}

		/** @return The number of OpenGL primitives drawn for each Batchable. Must always return the same value among
		 *         all instances of a class. */
		protected abstract int getPrimitivesPerBatchable();

		/** @return The number of vertices used for each Batchable. Must always return the same value among all instances of a
		 *         class. */
		protected abstract int getVerticesPerBatchable ();

		/** Populate the fixed triangle array for the FlexBatch's mesh. This is called only once on one of the FlexBatch's internal
		 * Batchable instances, or once the first time an instance is drawn with a FlexBatch that is not limited to
		 * FixedSizeBatchables.
		 * <p>
		 * This method is unused if {@link #getPrimitiveType()} is {@link GL20#GL_POINTS}.
		 * @param indices An array of indices that, before this method returns, must be fully populated for drawing a
		 *           series of this Batchable type. */
		protected abstract void populateIndices(short[] indices);

		/** Called by FlexBatch to apply vertex index data, only if this FixedSizeBatchable is drawn by a FlexBatch that is not
		 * limited to drawing FixedSizeBatchables. See {@link Batchable#apply(short[], int, short)} */
		@Override
		protected final int apply (short[] indices, int indicesStartingIndex, short firstVertex) {
			short[] model = indicesModels.get(getClass());
			if (model == null) {
				model = new short[getPrimitivesPerBatchable() * getIndicesPerPrimitive()];
				populateIndices(model);
				indicesModels.put(getClass(), model);
			}

			for (short value : model) {
				indices[indicesStartingIndex++] = (short) (value + firstVertex);
			}

			return model.length;
		}
	}

}
