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
package com.cyphercove.flexbatch.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GLTexture;
import com.badlogic.gdx.utils.IntMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Stores up pending GL state changes and textures to bind, and executes them on demand. Minimizes actual state changes.
 * Remembers and can restore state between uses.
 * <p>
 * Pending state changes can be made at any time, but can only be applied by calling {@link #executeChanges()} between calls to
 * {@link #begin()} and {@link #end()}.
 * 
 * @author cypherdare */
public class RenderContextAccumulator {

	static final class State {
		boolean depthMasking, depthTesting, blending, culling;
		int blendSrcFuncColor, blendDstFuncColor, blendEquationColor;
		int blendSrcFuncAlpha, blendDstFuncAlpha, blendEquationAlpha;
		int depthFunc;
		float depthRangeNear, depthRangeFar;
		int cullFace;
		final @NotNull IntMap<GLTexture> textureUnits = new IntMap<GLTexture>(32);

		public void applyDefaults () {
			blending = depthTesting = culling = false;
			depthMasking = true;
			depthRangeNear = 0f;
			depthRangeFar = 1f;
			cullFace = GL20.GL_BACK;
			blendSrcFuncColor = blendSrcFuncAlpha = GL20.GL_ONE;
			blendDstFuncColor = blendDstFuncAlpha = GL20.GL_ZERO;
			blendEquationColor = blendEquationAlpha = GL20.GL_FUNC_ADD;
			depthFunc = GL20.GL_LESS;
			textureUnits.clear();
		}

		/** Set invalid values on parameters to force them to be applied on the first call to
		 * {@link RenderContextAccumulator#executeChanges()}. This reduces unnecessary state restoration calls in
		 * {@link RenderContextAccumulator#begin()}. */
		public void invalidateParameters () {
			cullFace = blendSrcFuncColor = blendSrcFuncAlpha = blendDstFuncColor = blendDstFuncAlpha = depthFunc = -1;
			depthRangeNear = depthRangeFar = -2f;
		}
	}

	private final State current = new State();
	private State pending = new State();
	private static final State DEF = new State();

	static {
		DEF.applyDefaults();
	}

	public RenderContextAccumulator () {
		pending.applyDefaults();
	}

	/** Begin tracking GL state. If any pending changes to state have been made, they will be applied on the first call to
	 * {@link #executeChanges()}.
	 * <p>
	 * This call must be matched with a call to {@link #end()}, which returns OpenGL states to their defaults. The
	 * {@link #executeChanges()} method may only be called in between {@link #begin()} and {@link #end()}. */
	public void begin () {
		current.applyDefaults();
		current.invalidateParameters(); // Avoids having to forcibly set defaults here for parameters that can hold an invalid state
		Gdx.gl.glDepthMask(true);
		Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
		Gdx.gl.glDisable(GL20.GL_CULL_FACE);
		Gdx.gl.glDisable(GL20.GL_BLEND);
	}

	public boolean hasPendingChanges () {
		State pending = this.pending;
		State current = this.current;

		if (pending.depthMasking != current.depthMasking) return true;

		if (pending.depthTesting != current.depthTesting) return true;

		if (pending.depthTesting && pending.depthFunc != current.depthFunc) return true;

		if (pending.blending != current.blending) return true;

		if (pending.blending) {
			if (pending.blendSrcFuncColor != current.blendSrcFuncColor || pending.blendDstFuncColor != current.blendDstFuncColor
				|| pending.blendSrcFuncAlpha != current.blendSrcFuncAlpha || pending.blendDstFuncAlpha != current.blendDstFuncAlpha)
				return true;
			if (pending.blendEquationColor != current.blendEquationColor || pending.blendEquationAlpha != current.blendEquationAlpha)
				return true;
		}

		IntMap<GLTexture> actualTextureUnits = current.textureUnits;
		for (IntMap.Entry<GLTexture> entry : pending.textureUnits) {
			if (actualTextureUnits.get(entry.key) != entry.value) return true;
		}

		return false;
	}

	/** Applies the pending state changes and texture bindings to GL. This must be called in between {@link #begin()} and
	 * {@link #end()}. */
	public void executeChanges () {
		State pending = this.pending;
		State current = this.current;

		if (pending.depthMasking != current.depthMasking) {
			Gdx.gl.glDepthMask(pending.depthMasking);
			current.depthMasking = pending.depthMasking;
		}

		if (pending.depthTesting != current.depthTesting) {
			if (pending.depthTesting)
				Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
			else
				Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
			current.depthTesting = pending.depthTesting;
		}

		if (pending.depthTesting) {
			if (pending.depthFunc != current.depthFunc) {
				Gdx.gl.glDepthFunc(pending.depthFunc);
				current.depthFunc = pending.depthFunc;
			}

			if (pending.depthRangeNear != current.depthRangeNear || pending.depthRangeFar != current.depthRangeFar) {
				Gdx.gl.glDepthRangef(pending.depthRangeNear, pending.depthRangeFar);
				current.depthRangeNear = pending.depthRangeNear;
				current.depthRangeFar = pending.depthRangeFar;
			}
		}

		if (pending.blending != current.blending) {
			if (pending.blending)
				Gdx.gl.glEnable(GL20.GL_BLEND);
			else
				Gdx.gl.glDisable(GL20.GL_BLEND);
			current.blending = pending.blending;
		}

		if (pending.blending) {
			if (pending.blendSrcFuncColor != current.blendSrcFuncColor || pending.blendDstFuncColor != current.blendDstFuncColor
				|| pending.blendSrcFuncAlpha != current.blendSrcFuncAlpha || pending.blendDstFuncAlpha != current.blendDstFuncAlpha) {
				if (pending.blendSrcFuncColor == pending.blendSrcFuncAlpha
					&& pending.blendDstFuncColor == pending.blendDstFuncAlpha) {
					Gdx.gl.glBlendFunc(pending.blendSrcFuncColor, pending.blendDstFuncColor);
				} else {
					Gdx.gl.glBlendFuncSeparate(pending.blendSrcFuncColor, pending.blendDstFuncColor, pending.blendSrcFuncAlpha,
						pending.blendDstFuncAlpha);
				}
				current.blendSrcFuncColor = pending.blendSrcFuncColor;
				current.blendDstFuncColor = pending.blendDstFuncColor;
				current.blendSrcFuncAlpha = pending.blendSrcFuncAlpha;
				current.blendDstFuncAlpha = pending.blendDstFuncAlpha;
			}

			if (pending.blendEquationColor != current.blendEquationColor
				|| pending.blendEquationAlpha != current.blendEquationAlpha) {
				if (pending.blendEquationColor == pending.blendEquationAlpha)
					Gdx.gl.glBlendEquation(pending.blendEquationColor);
				else
					Gdx.gl.glBlendEquationSeparate(pending.blendEquationColor, pending.blendEquationAlpha);
			}
		}

		IntMap<GLTexture> currentTextureUnits = current.textureUnits;
		for (IntMap.Entry<GLTexture> entry : pending.textureUnits) {
			if (currentTextureUnits.get(entry.key) != entry.value) {
				entry.value.bind(entry.key);
				currentTextureUnits.put(entry.key, entry.value);
			}
		}

	}

	/** Returns actual OpenGL states to defaults. The blend function parameters, depth test function parameters, and culled face
	 * parameter are left unchanged. */
	public void end () {
		State temp = pending;
		pending = DEF;
		executeChanges();
		pending = temp;
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
	}

	/** Enables or disables depth buffer writing.
	 * @return Whether the pending depth masking state was changed. */
	public boolean setDepthMasking (boolean enabled) {
		if (pending.depthMasking != enabled) {
			pending.depthMasking = enabled;
			return true;
		}
		return false;
	}

	/** Enables or disables depth testing.
	 * @return Whether the pending depth testing state was changed. */
	public boolean setDepthTesting (boolean enabled) {
		if (pending.depthTesting != enabled) {
			pending.depthTesting = enabled;
			return true;
		}
		return false;
	}

	/** Sets the depth test function and a depth range of 0 to 1. These parameters will only be applied if depth testing is
	 * enabled.
	 * @return Whether the pending depth function state or parameters were changed. */
	public boolean setDepthFunction (int depthFunction) {
		return setDepthFunction(depthFunction, 0f, 1f);
	}

	/** Sets the depth test function and range. These parameters will only be applied if depth testing is enabled.
	 * @return Whether the pending depth function state or parameters were changed. */
	public boolean setDepthFunction (int depthFunc, float depthRangeNear, float depthRangeFar) {
		if (pending.depthFunc != depthFunc || pending.depthRangeNear != depthRangeNear || pending.depthRangeFar != depthRangeFar) {
			pending.depthFunc = depthFunc;
			pending.depthRangeNear = depthRangeNear;
			pending.depthRangeFar = depthRangeFar;
			return true;
		}
		return false;
	}

	/** Enables or disables blending.
	 * @return Whether the pending blending state was changed. */
	public boolean setBlending (boolean enabled) {
		if (pending.blending != enabled) {
			pending.blending = enabled;
			return true;
		}
		return false;
	}

	/** Sets the blendFunc parameters. They will only be applied when blending is enabled.
	 * @return Whether the pending blend parameters were changed while the pending blending state is true. */
	public boolean setBlendFunction (int sFactor, int dFactor) {
		return setBlendFunction(sFactor, dFactor, sFactor, dFactor);
	}

	/** Sets the blendFunc, using separate parameters for the color and alpha components. They will only be applied when blending
	 * is enabled.
	 * @return Whether the pending blend parameters were changed while the pending blending state is true. */
	public boolean setBlendFunction (int sColorFactor, int dColorFactor, int sAlphaFactor, int dAlphaFactor) {
		if (pending.blendSrcFuncColor != sColorFactor || pending.blendDstFuncColor != dColorFactor
			|| pending.blendSrcFuncAlpha != sAlphaFactor || pending.blendDstFuncAlpha != dAlphaFactor) {
			pending.blendSrcFuncColor = sColorFactor;
			pending.blendDstFuncColor = dColorFactor;
			pending.blendSrcFuncAlpha = sAlphaFactor;
			pending.blendDstFuncAlpha = dAlphaFactor;
			return pending.blending;
		}
		return false;
	}

	/** Sets the blend equation. It will only be applied when blending is enabled.
	 * @return Whether the pending blend equations were changed while the pending blending state is true. */
	public boolean setBlendEquation (int blendEquation) {
		return setBlendEquation(blendEquation, blendEquation);
	}

	/** Sets the blend equations, using separate equations for the color and alpha components. They will only be applied when
	 * blending is enabled.
	 * @return Whether the pending blend equations were changed while the pending blending state is true. */
	public boolean setBlendEquation (int blendEquationColor, int blendEquationAlpha) {
		if (pending.blendEquationColor != blendEquationColor || pending.blendEquationAlpha != blendEquationColor) {
			pending.blendEquationColor = blendEquationColor;
			pending.blendEquationAlpha = blendEquationAlpha;
			return pending.blending;
		}
		return false;
	}

	/** Enables or disables face culling.
	 * @return Whether the pending face culling state was changed. */
	public boolean setFaceCulling (boolean enabled) {
		if (pending.culling != enabled) {
			pending.culling = enabled;
			return true;
		}
		return false;
	}

	/** Sets which face(s) is culled when face culling is enabled. It will only be applied when face culling is enabled.
	 * @return Whether the pending cull face parameter was changed while face culling is true; */
	public boolean setCullFace (int face) {
		if (pending.cullFace != current.cullFace) {
			pending.cullFace = current.cullFace;
			return pending.culling;
		}
		return false;
	}

	/** Sets the texture to be bound to the given texture unit.
	 * @param texture The texture to bind, or null to clear any bound texture for the unit.
	 * @param unit The texture unit to use.
	 * @return Whether the pending texture for the unit was changed. */
	public boolean setTextureUnit (@Nullable GLTexture texture, int unit) {
		if (pending.textureUnits.get(unit) != texture) {
			if (texture == null)
				pending.textureUnits.remove(unit);
			else
				pending.textureUnits.put(unit, texture);
			return true;
		}
		return false;
	}

	/** Cancels any pending texture that is to be bound to the given texture unit.
	 * @param unit The texture unit to use.
	 * @return whether a unit was cleared. */
	public boolean clearTextureUnit (int unit) {
		return null != pending.textureUnits.remove(unit);
	}

	/** Cancels all pending texture bindings and drops all Texture references held by RenderContextAccumulator. */
	public void clearAllTextureUnits () {
		pending.textureUnits.clear();
		current.textureUnits.clear();
	}

	/** @return Whether depth buffer writing is enabled. This state may not have been applied yet. */
	public boolean isDepthMaskEnabled () {
		return pending.depthMasking;
	}

	/** @return Whether depth testing is enabled. This state may not have been applied yet. */
	public boolean isDepthTestingEnabled () {
		return pending.depthTesting;
	}

	/** @return The depth test function. This parameter may not have been applied yet. */
	public int getDepthFunction () {
		return pending.depthFunc;
	}

	public float getDepthRangeNear () {
		return pending.depthRangeNear;
	}

	public float getDepthRangeFar () {
		return pending.depthRangeFar;
	}

	/** @return Whether blending is enabled. This state may not have been applied yet. */
	public boolean isBlendingEnabled () {
		return pending.blending;
	}

	public int getBlendFuncSrcColor () {
		return pending.blendDstFuncColor;
	}

	public int getBlendFuncDstColor () {
		return pending.blendDstFuncColor;
	}

	public boolean isBlendFuncSeparate () {
		return pending.blendSrcFuncAlpha != -1;
	}

	public int getBlendFuncSrcAlpha () {
		return pending.blendSrcFuncAlpha;
	}

	public int getBlendFuncDstAlpha () {
		return pending.blendDstFuncAlpha;
	}

	/** @return Whether face culling is enabled. This state may not have been applied yet. */
	public boolean getFaceCulling () {
		return pending.culling;
	}

	/** @return The cull face. This parameter may not have been applied yet. */
	public int getCullFace () {
		return pending.cullFace;
	}
}
