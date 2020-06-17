/*
 ******************************************************************************
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

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.cyphercove.flexbatch.Batchable;
import com.cyphercove.flexbatch.utils.AttributeOffsets;
import com.cyphercove.flexbatch.utils.RenderContextAccumulator;
import com.cyphercove.flexbatch.utils.SortableBatchable;
import org.jetbrains.annotations.NotNull;

/** A {@link Quad} {@link Batchable Batchable} that supports a single texture at a time, with
 * two-dimensional position and color. It is designed to be drawn in 3D space, and is commonly called a decal.
 * <p>
 * The default origin of a Quad3D is its center. Its origin is used for positioning, and as the center of rotation and scaling.
 * <p>
 * Quad3D manages its own blending, so calls to the FlexBatch's blend function setters will be ineffective.
 * <p>
 * It may be subclassed to create a Batchable class that supports multiple textures and additional attributes--see
 * {@link #getNumberOfTextures()} and {@link #addVertexAttributes(com.badlogic.gdx.utils.Array) addVertexAttributes()}. Such a
 * subclass would not be compatible with a FlexBatch that was instantiated for the base Sprite type.
 *
 * @author cypherdare */
public class Quad3D extends Quad implements SortableBatchable {

	public float z;
	public final @NotNull Quaternion rotation = new Quaternion();
	public boolean opaque = true;
	public int srcBlendFactor = GL20.GL_SRC_ALPHA;
	public int dstBlendFactor = GL20.GL_ONE_MINUS_SRC_ALPHA;

	private static final Quaternion TMPQ = new Quaternion();
	private static final Vector3 TMP1 = new Vector3();
	private static final Vector3 TMP2 = new Vector3();
	private static final Vector3 TMP3 = new Vector3();

	/** Commonly used blend function factor pairs, for convenience. */
	public enum Blending {
		/** GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA */
		Alpha,
		/** GL_ONE, GL_ONE_MINUS_SRC_ALPHA */
		PremultipliedAlpha,
		/** GL_ONE, GL_ONE */
		Additive
	}

	/** A Quad3D that starts opaque. */
	public Quad3D () {
	}

	/** A Quad3D that starts with blending enabled, with the specified blend factors. */
	public Quad3D (int srcBlendFactor, int dstBlendFactor) {
		opaque = false;
		this.srcBlendFactor = srcBlendFactor;
		this.dstBlendFactor = dstBlendFactor;
	}

	/** A Quad3D that starts with blending enabled, and a common set of blend factors. */
	public Quad3D (@NotNull Blending blending) {
		opaque = false;
		switch (blending) {
		case PremultipliedAlpha:
			srcBlendFactor = GL20.GL_ONE;
			dstBlendFactor = GL20.GL_ONE_MINUS_SRC_ALPHA;
			break;
		case Additive:
			srcBlendFactor = dstBlendFactor = GL20.GL_ONE;
			break;
		case Alpha:
			srcBlendFactor = GL20.GL_ALPHA;
			dstBlendFactor = GL20.GL_ONE_MINUS_SRC_ALPHA;
			break;
		}
	}

	protected final boolean isPosition3D () {
		return true;
	}

	protected boolean isTextureCoordinate3D () {
		return false;
	}

	public boolean isOpaque () {
		return opaque;
	}

	public float calculateDistanceSquared (@NotNull Vector3 camPosition) {
		return camPosition.dst2(x, y, z);
	}

	protected void prepareSharedContext (@NotNull RenderContextAccumulator renderContext) {
		super.prepareSharedContext(renderContext);
		renderContext.setDepthTesting(true);
	}

	protected boolean prepareContext (@NotNull RenderContextAccumulator renderContext, int remainingVertices, int remainingTriangles) {
		boolean needsFlush = super.prepareContext(renderContext, remainingVertices, remainingTriangles);
		needsFlush |= renderContext.setBlending(!opaque);
		if (!opaque) {
			needsFlush |= renderContext.setBlendFunction(srcBlendFactor, dstBlendFactor);
		}
		return needsFlush;
	}

	public void refresh () {
		super.refresh();
		z = 0;
		rotation.set(0f, 0f, 0f, 1f);
		opaque = true; // refresh most commonly used for on-the-fly batch.draw(), so default to mode that doesn't need sorting
		srcBlendFactor = GL20.GL_SRC_ALPHA;
		dstBlendFactor = GL20.GL_ONE_MINUS_SRC_ALPHA;
	}

	/** Disables blending. Blending is disabled by default.
	 * @return This object for chaining. */
	public @NotNull Quad3D opaque () {
		opaque = true;
		return this;
	}

	/** Enables blending. Blending is disabled by default.
	 * @return This object for chaining. */
	public @NotNull Quad3D blend () {
		opaque = false;
		return this;
	}

	/** Enables blending and sets the blend function parameters. Blending is disabled by default.
	 * @return This object for chaining. */
	public Quad3D blend (int srcBlendFactor, int dstBlendFactor) {
		opaque = false;
		this.srcBlendFactor = srcBlendFactor;
		this.dstBlendFactor = dstBlendFactor;
		return this;
	}

	/** Enables blending and sets the blend function parameters to a commonly used pair. Blending is disabled by default.
	 * @return This object for chaining. */
	public @NotNull Quad3D blend (@NotNull Blending blending) {
		opaque = false;
		switch (blending) {
		case Alpha:
			srcBlendFactor = GL20.GL_SRC_ALPHA;
			dstBlendFactor = GL20.GL_ONE_MINUS_SRC_ALPHA;
			break;
		case PremultipliedAlpha:
			srcBlendFactor = GL20.GL_ONE;
			dstBlendFactor = GL20.GL_ONE_MINUS_SRC_ALPHA;
			break;
		case Additive:
			srcBlendFactor = dstBlendFactor = GL20.GL_ONE;
			break;
		}
		return this;
	}

	/** Sets the position of the center (plus current origin offset) of the texture region in world space.
	 * @return This object for chaining. */
	public @NotNull Quad3D position (float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	/** Sets the position of the center (plus current origin offset) of the texture region in world space.
	 * @return This object for chaining. */
	public @NotNull Quad3D position (@NotNull Vector3 position) {
		this.x = position.x;
		this.y = position.y;
		this.z = position.z;
		return this;
	}

	/** Translates the current position by the given amount.
	 * @return This object for chaining. */
	public @NotNull Quad3D translate (float x, float y, float z) {
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}

	/** Translates the current position by the given amount.
	 * @return This object for chaining. */
	public @NotNull Quad3D translate (@NotNull Vector3 amount) {
		this.x += amount.x;
		this.y += amount.y;
		this.z += amount.z;
		return this;
	}

	/** Sets the rotation to a specific Quaternion.
	 * @return This object for chaining. */
	public @NotNull Quad3D rotation (@NotNull Quaternion rotation) {
		this.rotation.set(rotation);
		return this;
	}

	/** Sets the rotation to a specific Quaternion.
	 * @return This object for chaining. */
	public @NotNull Quad3D rotation (float x, float y, float z, float w) {
		rotation.set(x, y, z, w);
		return this;
	}

	/** Sets the rotation to a specific angle about an axis.
	 * @return This object for chaining. */
	public @NotNull Quad3D rotation (@NotNull Vector3 axis, float angle) {
		rotation.setFromAxis(axis, angle);
		return this;
	}

	/** Sets the rotation to specific Euler angles.
	 * @return This object for chaining. */
	public @NotNull Quad3D rotation (float yaw, float pitch, float roll) {
		rotation.setEulerAngles(yaw, pitch, roll);
		return this;
	}

	/** Sets the rotation based on a direction vector and up vector. The input vectors <b>do not</b> need to be normalized.
	 * @return This object for chaining. */
	public @NotNull Quad3D rotation (float directionX, float directionY, float directionZ, float upX, float upY, float upZ) {
		TMP2.set(directionX, directionY, directionZ).nor();
		TMP1.set(upX, upY, upZ).nor().crs(TMP2).nor();
		TMP2.crs(TMP1).nor();
		rotation.setFromAxes(TMP1.x, TMP2.x, directionX, TMP1.y, TMP2.y, directionY, TMP1.z, TMP2.z, directionZ);
		return this;
	}

	/** Sets the rotation based on a direction vector and up vector. The input vectors <b>must be</b> normalized.
	 * @return This object for chaining. */
	public @NotNull Quad3D rotation (@NotNull Vector3 direction, @NotNull Vector3 up) {
		TMP1.set(up).crs(direction).nor();
		TMP2.set(direction).crs(TMP1).nor();
		rotation.setFromAxes(TMP1.x, TMP2.x, direction.x, TMP1.y, TMP2.y, direction.y, TMP1.z, TMP2.z, direction.z);
		return this;
	}

	/** Sets the rotation to look at the give position, relative to the current position.
	 * @param up The direction the top of the quad should be pointing.
	 * @return This object for chaining. */
	public @NotNull Quad3D lookAt (@NotNull Vector3 position, @NotNull Vector3 up) {
		TMP3.set(position).sub(x, y, z).nor();
		TMP1.set(up).crs(TMP3).nor();
		TMP2.set(TMP3).crs(TMP1).nor();
		rotation.setFromAxes(TMP1.x, TMP2.x, TMP3.x, TMP1.y, TMP2.y, TMP3.y, TMP1.z, TMP2.z, TMP3.z);
		return this;
	}

	/** Sets the rotation to look at the camera, relative to the current position. The quad's top side will be oriented to match
	 * the camera's.
	 * @return This object for chaining. */
	public @NotNull Quad3D billboard (@NotNull Camera camera) {
		return lookAt(camera.position, camera.up);
	}

	/** Sets the rotation to look at the camera, relative to the current position.
	 * @param up The direction the top of the quad should be pointing.
	 * @return This object for chaining. */
	public @NotNull Quad3D billboard (@NotNull Camera camera, @NotNull Vector3 up) {
		return lookAt(camera.position, up);
	}

	/** Rotates the current orientation by a specific Quaternion.
	 * @return This object for chaining. */
	public @NotNull Quad3D rotate (@NotNull Quaternion rotation) {
		this.rotation.mul(rotation);
		return this;
	}

	/** Rotates from the current orientation by a specific angle about the X axis.
	 * @return This object for chaining. */
	public @NotNull Quad3D rotateX (float angle) {
		rotation.mul(TMPQ.setFromAxis(1, 0, 0, angle));
		return this;
	}

	/** Rotates from the current orientation by a specific angle about the Y axis.
	 * @return This object for chaining. */
	public @NotNull Quad3D rotateY (float angle) {
		rotation.mul(TMPQ.setFromAxis(0, 1, 0, angle));
		return this;
	}

	/** Rotates from the current orientation by a specific angle about the Z axis.
	 * @return This object for chaining. */
	public @NotNull Quad3D rotateZ (float angle) {
		rotation.mul(TMPQ.setFromAxis(0, 0, 1, angle));
		return this;
	}

	protected int apply (@NotNull float[] vertices, int vertexStartingIndex, @NotNull AttributeOffsets offsets, int vertexSize) {
		super.apply(vertices, vertexStartingIndex, offsets, vertexSize);
		float left = -width / 2f;
		float right = left + width;
		float bottom = -height / 2f;
		float top = bottom + height;

		int i = vertexStartingIndex;

		// bottom left
		TMP1.set((left - originX) * scaleX, (bottom - originY) * scaleY, 0); // local vertex position
		rotation.transform(TMP1); // local vertex position rotated
		vertices[i] = TMP1.x + x; // world position
		vertices[i + 1] = TMP1.y + y;
		vertices[i + 2] = TMP1.z + z;
		i += vertexSize;

		// top left
		TMP1.set((left - originX) * scaleX, (top - originY) * scaleY, 0);
		rotation.transform(TMP1);
		vertices[i] = TMP1.x + x;
		vertices[i + 1] = TMP1.y + y;
		vertices[i + 2] = TMP1.z + z;
		i += vertexSize;

		// top right
		TMP1.set((right - originX) * scaleX, (top - originY) * scaleY, 0);
		rotation.transform(TMP1);
		vertices[i] = TMP1.x + x;
		vertices[i + 1] = TMP1.y + y;
		vertices[i + 2] = TMP1.z + z;
		i += vertexSize;

		// bottom right
		TMP1.set((right - originX) * scaleX, (bottom - originY) * scaleY, 0);
		rotation.transform(TMP1);
		vertices[i] = TMP1.x + x;
		vertices[i + 1] = TMP1.y + y;
		vertices[i + 2] = TMP1.z + z;

		return 4;
	}

	// Chain methods must be overridden to allow return of subclass type.

	public @NotNull Quad3D texture (@NotNull Texture texture) {
		super.texture(texture);
		return this;
	}

	public @NotNull Quad3D region (float u, float v, float u2, float v2) {
		super.region(u, v, u2, v2);
		return this;
	}

	public @NotNull Quad3D textureRegion (@NotNull TextureRegion region) {
		super.textureRegion(region);
		return this;
	}

	public @NotNull Quad3D flip (boolean flipX, boolean flipY) {
		super.flip(flipX, flipY);
		return this;
	}

	public @NotNull Quad3D size (float width, float height) {
		super.size(width, height);
		return this;
	}

	public @NotNull Quad3D origin (float originX, float originY) {
		super.origin(originX, originY);
		return this;
	}

	public @NotNull Quad3D rotateCoordinates90 (boolean clockwise) {
		super.rotateCoordinates90(clockwise);
		return this;
	}

	public @NotNull Quad3D color (@NotNull Color color) {
		super.color(color);
		return this;
	}

	public @NotNull Quad3D color (float r, float g, float b, float a) {
		super.color(r, g, b, a);
		return this;
	}

	public @NotNull Quad3D color (float floatBits) {
		super.color(floatBits);
		return this;
	}

	public @NotNull Quad3D scale (float scaleX, float scaleY) {
		super.scale(scaleX, scaleY);
		return this;
	}
}
