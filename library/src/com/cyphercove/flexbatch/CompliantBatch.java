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
package com.cyphercove.flexbatch;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.utils.NumberUtils;
import com.cyphercove.flexbatch.batchable.Quad2D;
import com.cyphercove.flexbatch.utils.BatchablePreparation;

/** A {@link FlexBatch} that implements the {@link Batch} interface, so it is compatible with Stage/Actor, BitmapFont,
 * ParticleEffect, Sprite, and NinePatch. It creates its own default ShaderProgram, which is owned and is disposed automatically
 * with the CompliantBatch.
 * <p>
 * A CompliantBatch must be {@link #dispose() disposed of} when no longer used to avoid leaking memory.
 * <p>
 * In addition to drawing Quad2Ds (or given subclass), if polygon support is specified in the constructor, it may also draw
 * Poly2Ds (or matching subclass) by submitting them to {@link #draw(Batchable)}.
 * <p>
 * A subclass of Quad2D may be passed to the constructor to customize what is drawn (multi-texturing or other attributes).
 * 
 * @param <T> The type of Quad2D that is returned when acquiring one with {@link #draw()}. This must match the class type that is
 *           passed to the constructor.
 * @author cypherdare */
public class CompliantBatch<T extends Quad2D> extends FlexBatch<T> implements Batch {
	private final T tmp;
	private final ShaderProgram defaultShader;
	private float color = Color.WHITE.toFloatBits();
	private final Color tempColor = new Color();
	private final float[] tempVertices = new float[20];

	/** Constructs a CompliantQuadBatch with a default shader and a capacity of 1000 quads that can be drawn per flush. The default
	 * shader is owned by the CompliantQuadBatch, so it is disposed when the CompliantQuadBatch is disposed. If an alternate shader
	 * has been applied with {@link #setShader(ShaderProgram)}, the default can be used again by setting the shader to null.
	 * @param supportPolygons Whether Poly2Ds are supported for drawing. The FlexBatch will not be optimized for
	 *           FixedSizeBatchables. */
	public CompliantBatch (Class<T> batchableType, boolean supportPolygons) {
		this(batchableType, 4000, supportPolygons);
	}

	/** Constructs a CompliantQuadBatch with a default shader. The default shader is owned by the CompliantQuadBatch, so it is
	 * disposed when the CompliantQuadBatch is disposed. If an alternate shader has been applied with
	 * {@link #setShader(ShaderProgram)}, the default can be used again by setting the shader to null.
	 * @param maxVertices The number of vertices this FlexBatch can batch at once. Maximum of 32767.
	 * @param supportPolygons Whether Poly2Ds are supported for drawing. The FlexBatch will not be optimized for
	 *           FixedSizeBatchables. */
	public CompliantBatch (Class<T> batchableType, int maxVertices, boolean supportPolygons) {
		this(batchableType, maxVertices, true, supportPolygons);
	}

	/** Constructs a CompliantQuadBatch with a specified capacity and optional default shader.
	 * @param maxVertices The number of vertices this FlexBatch can batch at once. Maximum of 32767.
	 * @param generateDefaultShader Whether a default shader should be created. The default shader is owned by the
	 *           CompliantQuadBatch, so it is disposed when the CompliantQuadBatch is disposed. If an alternate shader has been
	 *           applied with {@link #setShader(ShaderProgram)}, the default can be used again by setting the shader to null.
	 * @param supportPolygons Whether Poly2Ds are supported for drawing. The FlexBatch will not be optimized for
	 *           FixedSizeBatchables. */
	public CompliantBatch (Class<T> batchableType, int maxVertices, boolean generateDefaultShader, boolean supportPolygons) {
		super(batchableType, maxVertices, supportPolygons ? maxVertices * 2 : 0);
		try {
			tmp = batchableType.newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("Batchable classes must be public and have an empty constructor.", e);
		}
		if (generateDefaultShader) {
			defaultShader = new ShaderProgram(BatchablePreparation.generateGenericVertexShader(1),
				BatchablePreparation.generateGenericFragmentShader(1));
			if (!defaultShader.isCompiled())
				throw new IllegalArgumentException("Error compiling shader: " + defaultShader.getLog());
			setShader(defaultShader);
		} else {
			defaultShader = null;
		}
	}

	/**
	 * Returns the current ShaderProgram, which may be the default. Is never null.
	 *
	 * @return the ShaderProgram set for this batch, or the default if none is set.
	 */
	@Override
	public ShaderProgram getShader() {
		return super.getShader();
	}

	/**
	 * Returns the current ShaderProgram, which may be the default. For CompliantBatch, this is no different than
	 * calling {$link #getShader()}.
	 * @return the ShaderProgram set for this batch, or the default if none is set.
	 */
	@Override
	public ShaderProgram requireShader() {
		return getShader();
	}

	/**
	 * Sets the ShaderProgram to be used with this batch. Can be set to null to restore use of the default shader.
	 * @param shader The ShaderProgram to use, or null to switch to the default shader.
	 */
	@Override
	public void setShader (ShaderProgram shader) {
		if (shader == null) shader = defaultShader;
		super.setShader(shader);
	}

	@Override
	public void dispose () {
		super.dispose();
		if (defaultShader != null) defaultShader.dispose();
	}

	/** Sets the color used to tint images when they are added to the Batch. Default is Color.WHITE. Does not affect the color of
	 * anything drawn with {@link #draw()}, {@link #draw(Batchable)}, or {@link #draw(Texture, float[], int, int)}. */
	@Override
	public void setColor (Color tint) {
		color = tint.toFloatBits();
	}

	/** Sets the color used to tint images when they are added to the Batch. Default is equivalent to Color.WHITE. Does not affect
	 * the color of anything drawn with {@link #draw()}, {@link #draw(Batchable)}, or {@link #draw(Texture, float[], int, int)}. */
	@Override
	public void setColor (float r, float g, float b, float a) {
		int intBits = (int)(255 * a) << 24 | (int)(255 * b) << 16 | (int)(255 * g) << 8 | (int)(255 * r);
		color = NumberUtils.intToFloatColor(intBits);
	}

	/** Sets the color used to tint images when they are added to the Batch. Default is {@link Color#toFloatBits()
	 * Color.WHITE.toFloatBits()}. Does not affect the color of anything drawn with {@link #draw()}, {@link #draw(Batchable)}, or
	 * {@link #draw(Texture, float[], int, int)}. */
	@Override
	public void setPackedColor (float color) {
		this.color = color;
	}

	@Override
	public Color getColor () {
		int intBits = NumberUtils.floatToIntColor(color);
		Color color = tempColor;
		color.r = (intBits & 0xff) / 255f;
		color.g = ((intBits >>> 8) & 0xff) / 255f;
		color.b = ((intBits >>> 16) & 0xff) / 255f;
		color.a = ((intBits >>> 24) & 0xff) / 255f;
		return color;
	}

	@Override
	public float getPackedColor () {
		return color;
	}

	@Override
	public void draw (Texture texture, float[] spriteVertices, int offset, int count) {
		tmp.refresh();
		tmp.texture(texture);
		super.draw(tmp, spriteVertices, offset, count, 5);
	}

	@Override
	public void draw (Texture texture, float x, float y, float originX, float originY, float width, float height, float scaleX,
		float scaleY, float rotation, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
		float invTexWidth = 1.0f / texture.getWidth();
		float invTexHeight = 1.0f / texture.getHeight();
		float u = srcX * invTexWidth;
		float v = (srcY + srcHeight) * invTexHeight;
		float u2 = (srcX + srcWidth) * invTexWidth;
		float v2 = srcY * invTexHeight;
		draw().color(color).texture(texture).position(x, y).origin(originX, originY).size(width, height).scale(scaleX, scaleY)
			.rotation(rotation).region(u, v, u2, v2).flip(flipX, flipY);
	}

	@Override
	public void draw (Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth,
		int srcHeight, boolean flipX, boolean flipY) {
		float invTexWidth = 1.0f / texture.getWidth();
		float invTexHeight = 1.0f / texture.getHeight();
		float u = srcX * invTexWidth;
		float v = (srcY + srcHeight) * invTexHeight;
		float u2 = (srcX + srcWidth) * invTexWidth;
		float v2 = srcY * invTexHeight;
		draw().color(color).texture(texture).position(x, y).size(width, height).region(u, v, u2, v2).flip(flipX, flipY);
	}

	@Override
	public void draw (Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight) {
		float invTexWidth = 1.0f / texture.getWidth();
		float invTexHeight = 1.0f / texture.getHeight();
		float u = srcX * invTexWidth;
		float v = (srcY + srcHeight) * invTexHeight;
		float u2 = (srcX + srcWidth) * invTexWidth;
		float v2 = srcY * invTexHeight;
		draw().color(color).texture(texture).position(x, y).region(u, v, u2, v2);
	}

	@Override
	public void draw (Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
		draw().color(color).texture(texture).position(x, y).size(width, height).region(u, v, u2, v2);
	}

	@Override
	public void draw (Texture texture, float x, float y) {
		draw().color(color).texture(texture).position(x, y);
	}

	@Override
	public void draw (Texture texture, float x, float y, float width, float height) {
		draw().color(color).texture(texture).position(x, y).size(width, height);
	}

	@Override
	public void draw (TextureRegion region, float x, float y) {
		draw().color(color).textureRegion(region).position(x, y);
	}

	@Override
	public void draw (TextureRegion region, float x, float y, float width, float height) {
		draw().color(color).textureRegion(region).position(x, y).size(width, height);
	}

	@Override
	public void draw (TextureRegion region, float x, float y, float originX, float originY, float width, float height,
		float scaleX, float scaleY, float rotation) {
		draw().color(color).textureRegion(region).position(x, y).origin(originX, originY).size(width, height).scale(scaleX, scaleY)
			.rotation(rotation);
	}

	@Override
	public void draw (TextureRegion region, float x, float y, float originX, float originY, float width, float height,
		float scaleX, float scaleY, float rotation, boolean clockwise) {
		draw().color(color).textureRegion(region).position(x, y).origin(originX, originY).size(width, height).scale(scaleX, scaleY)
			.rotation(rotation).rotateCoordinates90(clockwise);
	}

	@Override
	public void draw (TextureRegion region, float width, float height, Affine2 transform) {
		float[] vertices = tempVertices;

		vertices[U1] = region.getU();
		vertices[V1] = region.getV2();
		vertices[U2] = region.getU();
		vertices[V2] = region.getV();
		vertices[U3] = region.getU2();
		vertices[V3] = region.getV();
		vertices[U4] = region.getU2();
		vertices[V4] = region.getV2();

		float color = this.color;
		vertices[C1] = color;
		vertices[C2] = color;
		vertices[C3] = color;
		vertices[C4] = color;

		// construct corner points
		vertices[X1] = transform.m02;
		vertices[Y1] = transform.m12;
		vertices[X2] = transform.m01 * height + transform.m02;
		vertices[Y2] = transform.m11 * height + transform.m12;
		vertices[X3] = transform.m00 * width + transform.m01 * height + transform.m02;
		vertices[Y3] = transform.m10 * width + transform.m11 * height + transform.m12;
		vertices[X4] = transform.m00 * width + transform.m02;
		vertices[Y4] = transform.m10 * width + transform.m12;

		draw(region.getTexture(), vertices, 0, 20);
	}

}
