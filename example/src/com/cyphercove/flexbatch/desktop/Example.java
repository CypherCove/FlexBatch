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
package com.cyphercove.flexbatch.desktop;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cyphercove.covetools.assets.TextureAtlasCacher;
import com.cyphercove.covetools.utils.Disposal;
import com.cyphercove.flexbatch.CompliantBatch;
import com.cyphercove.flexbatch.FlexBatch;
import com.cyphercove.flexbatch.batchable.*;
import com.cyphercove.flexbatch.utils.*;

import static com.badlogic.gdx.math.MathUtils.*;
import static com.badlogic.gdx.math.MathUtils.random;

public class Example extends ApplicationAdapter {
	Texture texture, treeTexture, egg, wheel;
	SpriteBatch spriteBatch;
	FlexBatch<BumpQuad> bumpBatch;
	CompliantBatch<BumpQuad> quad2dBatch;
	FlexBatch<SolidQuad> solidQuadBatch;
	FlexBatch<Poly2D> poly2dBatch;
	FlexBatch<Quad3D> quad3dBatch;
	FlexBatch<Point3D> point3DBatch;
	FlexBatch<TripleQuad> tripleOverlayBatch;
	BatchableSorter<Quad3D> quad3dSorter;
	BatchableSorter<Point3D> point3dSorter;
	PerspectiveCamera pCam;
	ShaderProgram solidShader, typicalShader, bumpShader, point3dShader, tripleOverlayShader;
	Stage stage;
	Skin skin;
	Viewport viewport;
	TextureAtlas atlas, bumpAtlas, tripleOverlayAtlas;
	Test test = Test.values()[0];
	Array<BumpQuad> quad2ds = new Array<>(), bumpQuads = new Array<>();
	Array<Quad3D> quad3ds = new Array<>();
	Array<Point3D> point3ds = new Array<>();
	Sprite testSprite;
	BitmapFont testFont;
	PolygonRegion polygonRegion;
	final TripleOverlayRegions tripleOverlayRegions = new TripleOverlayRegions();
	private static final int W = 800, H = 480;
	float elapsed;
	Array<Item> items = new Array<>();
	Vector3 bumpLightPosition = new Vector3();
	boolean bumpLightFollowCursor;
	float bumpLightPathRadius = 0.4f * Math.min(W, H);
	float bumpOrbitRadius = 0.45f * Math.min(W, H);

	private enum Test {
		BumpMapped2D, Poly2D, Quad3D, Point3D, CompliantBatch, SolidQuads, TripleOverlay
	}

	public static class SolidQuad extends Quad2D {
		protected int getNumberOfTextures() {
			return 0;
		}
	}

	public static class BumpQuad extends Quad2D {
		float shininess;

		public BumpQuad shininess(float shininess) {
			this.shininess = shininess;
			return this;
		}

		// Must have texture set first.
		public BumpQuad centerOrigin() {
			Region2D region = regions[0];
			width = (region.u2 - region.u) * textures[0].getWidth();
			height = (region.v2 - region.v) * textures[0].getHeight();
			origin(width / 2f, height / 2f);
			return this;
		}

		public BumpQuad positionByOrigin(float x, float y) {
			position(x - originX, y - originY);
			return this;
		}

		protected int getNumberOfTextures() {
			return 2;
		}

		protected void addVertexAttributes(Array<VertexAttribute> attributes) {
			super.addVertexAttributes(attributes);
			attributes.add(new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_rotation"));
			attributes.add(new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_shininess"));
		}

		protected int apply(float[] vertices, int vertexStartingIndex, AttributeOffsets offsets, int vertexSize) {
			super.apply(vertices, vertexStartingIndex, offsets, vertexSize);

			float rotation = this.rotation * MathUtils.degRad;
			int rotationIndex = vertexStartingIndex + offsets.generic0;
			vertices[rotationIndex] = rotation;
			rotationIndex += vertexSize;
			vertices[rotationIndex] = rotation;
			rotationIndex += vertexSize;
			vertices[rotationIndex] = rotation;
			rotationIndex += vertexSize;
			vertices[rotationIndex] = rotation;

			float shininess = this.shininess;
			int shininessIndex = vertexStartingIndex + offsets.generic1;
			vertices[shininessIndex] = shininess;
			shininessIndex += vertexSize;
			vertices[shininessIndex] = shininess;
			shininessIndex += vertexSize;
			vertices[shininessIndex] = shininess;
			shininessIndex += vertexSize;
			vertices[shininessIndex] = shininess;

			return 4;
		}
	}

	public static class TripleQuad extends Quad2D {
		protected int getNumberOfTextures() {
			return 3;
		}
	}

	@Override
	public void create() {
		random.setSeed(0);

		viewport = new ExtendViewport(W, H);

		texture = new Texture("badlogic.jpg");
		atlas = new TextureAtlas(Gdx.files.internal("pack"));

		testSprite = new Sprite(texture);
		testSprite.setPosition(50, 102);
		testSprite.setColor(0, 1, 0, 0.6f);

		testFont = new BitmapFont(Gdx.files.internal("arial-32-pad.fnt"), false);
		testFont.setColor(Color.CYAN);

		treeTexture = new Texture(Gdx.files.internal("tree.png"));
		PolygonRegionLoader loader = new PolygonRegionLoader();
		polygonRegion = loader.load(new TextureRegion(treeTexture), Gdx.files.internal("tree.psh"));

		for (int i = 0; i < 100; i++) {
			Item item = new Item();
			item.x = random();
			item.y = random();
			item.color.set(random(), random(), random(), 1.0f);
			item.scaleX = random(0.5f, 1.5f);
			item.scaleY = random(0.5f, 1.5f);
			items.add(item);
		}

		spriteBatch = new SpriteBatch(100);
		spriteBatch.enableBlending();

		quad2dBatch = new CompliantBatch<>(BumpQuad.class, 4000, true);
		quad2dBatch.enableBlending();
		for (int i = 0; i < 80; i++) { // The second texture and extra
			// attributes for these will go unused
			BumpQuad sprite = new BumpQuad();
			sprite.texture(texture).color(random(), random(), random(), random(0.5f, 1f)).position(random(W), random(H))
					.rotation(random(360)).size(random(50, 100), random(50, 100));
			quad2ds.add(sprite);
		}

		solidQuadBatch = new FlexBatch<>(SolidQuad.class, 10, 0);
		solidShader = new ShaderProgram(BatchablePreparation.generateGenericVertexShader(0),
				BatchablePreparation.generateGenericFragmentShader(0));
		solidQuadBatch.setShader(solidShader);

		egg = new Texture(Gdx.files.internal("egg.png"));
		egg.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		wheel = new Texture(Gdx.files.internal("wheel.png"));
		wheel.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

		typicalShader = new ShaderProgram(BatchablePreparation.generateGenericVertexShader(1),
				BatchablePreparation.generateGenericFragmentShader(1));
		pCam = new PerspectiveCamera();
		pCam.near = 0.1f;
		pCam.far = 10000f;
		pCam.position.set(0, 20, -20);
		pCam.lookAt(0, 0, 0);
		pCam.update();
		quad3dBatch = new FlexBatch<>(Quad3D.class, 4000, 0);
		quad3dBatch.setShader(typicalShader);
		quad3dSorter = new BatchableSorter<>(pCam);
		for (int i = 0; i < 500; i++) {
			quad3ds.add(makeQuad3D(10, 40));
		}

		point3dShader = new ShaderProgram(Gdx.files.internal("point3d.vert").readString(),
				Gdx.files.internal("point3d.frag").readString());
		if (!point3dShader.isCompiled())
			Gdx.app.error("point3d shader error", point3dShader.getLog());
		point3DBatch = new FlexBatch<>(Point3D.class, 1000, 0);
		point3DBatch.setShader(point3dShader);
		point3dSorter = new BatchableSorter<>(pCam);
		for (int i = 0; i < 500; i++) {
			point3ds.add(makePoint3D(10, 40));
		}

		poly2dBatch = new FlexBatch<>(Poly2D.class, 1000, 2000);
		poly2dBatch.setShader(typicalShader);

		bumpShader = new ShaderProgram(Gdx.files.internal("bump.vert").readString(),
				Gdx.files.internal("bump.frag").readString());
		if (!bumpShader.isCompiled())
			Gdx.app.error("bump shader error", bumpShader.getLog());
		bumpShader.begin();
		bumpShader.setUniformf("u_ambient", new Color(0.05f, 0.05f, 0.1f, 1));
		bumpShader.setUniformf("u_specularStrength", 0.7f);
		bumpShader.setUniformf("u_attenuation", 0.002f);
		bumpShader.end();
		bumpAtlas = new TextureAtlas("normalMappedSprites.atlas");
		for (int i = 0; i < 6; i++) {
			String baseName;
			float shininess;
			switch (i % 3) {
				case 0:
					baseName = "wood";
					shininess = 30;
					break;
				case 1:
					baseName = "brick";
					shininess = 3;
					break;
				default:
					baseName = "brickRound";
					shininess = 3;
					break;
			}
			BumpQuad quad = new BumpQuad();
			quad.shininess(shininess).textureRegion(bumpAtlas.findRegion(baseName + "_diffuse"))
					.textureRegion(bumpAtlas.findRegion(baseName + "_norm"));
			quad.centerOrigin();
			bumpQuads.add(quad);
		}
		bumpBatch = new FlexBatch<>(BumpQuad.class, 100, 0);
		bumpBatch.setShader(bumpShader);

		tripleOverlayShader = new ShaderProgram(Gdx.files.internal("tripleOverlay.vert").readString(),
				Gdx.files.internal("tripleOverlay.frag").readString());
		if (!tripleOverlayShader.isCompiled())
			Gdx.app.error("triple overlay shader error", tripleOverlayShader.getLog());
		tripleOverlayAtlas = new TextureAtlas("multipageFruit.atlas");
		TextureAtlasCacher.cacheRegions(tripleOverlayAtlas, tripleOverlayRegions, true);

		tripleOverlayBatch = new FlexBatch<>(TripleQuad.class, 100, 0);
		tripleOverlayBatch.setShader(tripleOverlayShader);

		setupUI();

		Gdx.input.setInputProcessor(new InputMultiplexer(stage, bumpInputAdapter));

		Gdx.gl.glEnable(GLConstants.GL_POINT_SPRITE_OES);
		if (Gdx.app.getType() == Application.ApplicationType.Desktop)
			Gdx.gl.glEnable(GLConstants.GL_PROGRAM_POINT_SIZE);
		Gdx.app.log("Min point size", Point.getMinimumPointSize() + "");
		Gdx.app.log("Max point size", Point.getMaximumPointSize() + "");
		Gdx.app.log("Min smooth point size", Point.getMinimumSmoothPointSize() + "");
		Gdx.app.log("Max smooth point size", Point.getMaximumSmoothPointSize() + "");
		Gdx.app.log("Point granularity", Point.getPointSizeGranularity() + "");
	}

	public void resize(int width, int height) {
		viewport.update(width, height, true);
		stage.getViewport().update(width, height, true);
		pCam.viewportWidth = width;
		pCam.viewportHeight = height;
		pCam.update();
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void render() {
		float dt = Gdx.graphics.getDeltaTime();
		elapsed += dt;

		Gdx.gl.glClearColor(0.1f, 0.125f, 0.35f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		viewport.apply();

		for (Item item : items) {
			item.rotation += 45 * dt;
			item.x += 0.25f * dt;
		}
		Camera cam = viewport.getCamera();

		switch (test) {
			case BumpMapped2D:
				for (int i = 0; i < bumpQuads.size; i++) {
					bumpQuads.get(i).rotation(-10 * elapsed);
					float t = 25f * elapsed + 360f * (float) i / bumpQuads.size;
					bumpQuads.get(i).positionByOrigin(cam.position.x + bumpOrbitRadius * cosDeg(t),
							cam.position.y + bumpOrbitRadius * sinDeg(t));
				}
				if (!bumpLightFollowCursor) {
					float t = -0.3f * elapsed;
					bumpLightPosition.set(cam.position.x + bumpLightPathRadius * cos(t),
							cam.position.y + bumpLightPathRadius * sin(t), 100);
				}
				bumpBatch.setProjectionMatrix(cam.combined);
				bumpBatch.begin();
				bumpShader.setUniformf("u_camPosition", cam.position);
				bumpShader.setUniformf("u_lightPosition", bumpLightPosition);
				for (BumpQuad quad : bumpQuads)
					bumpBatch.draw(quad);
				BumpQuad bumpQuad = (BumpQuad) bumpBatch.draw().shininess(30)
						.textureRegion(bumpAtlas.findRegion("badlogic_diffuse"))
						.textureRegion(bumpAtlas.findRegion("badlogic_norm")).rotation(20f * elapsed);
				bumpQuad.centerOrigin().positionByOrigin(cam.position.x, cam.position.y);
				bumpBatch.end();
				break;
			case Poly2D:
				poly2dBatch.setProjectionMatrix(cam.combined);
				poly2dBatch.begin();
				for (Item item : items) {
					poly2dBatch.draw().region(polygonRegion).position(item.getWorldPosition(cam))
							.scale(item.scaleX, item.scaleY).rotation(item.rotation).color(item.color);
				}
				poly2dBatch.end();
				break;
			case Quad3D:
				pCam.position.rotate(Vector3.Z, Gdx.graphics.getDeltaTime() * 90f);
				pCam.lookAt(0, 0, 0);
				pCam.update();
				for (Quad3D quad : quad3ds) {
					quad.billboard(pCam);
					quad3dSorter.add(quad);
				}
				quad3dBatch.setProjectionMatrix(pCam.combined);
				quad3dBatch.begin();
				quad3dSorter.flush(quad3dBatch);
				quad3dBatch.end();
				break;
			case CompliantBatch: {
				quad2dBatch.setProjectionMatrix(cam.combined);
				quad2dBatch.begin();
				quad2dBatch.draw().texture(wheel).color(0, 0.5f, 1, 1).size(100, 100).rotation(45);
				for (BumpQuad sprite : quad2ds) {
					sprite.rotation += Gdx.graphics.getDeltaTime() * 30;
					quad2dBatch.draw(sprite);
				}
				testFont.draw(quad2dBatch, "BitmapFont", 50, 100);
				testSprite.draw(quad2dBatch);
				quad2dBatch.end();

				break;
			}
			case Point3D:
				pCam.position.rotate(Vector3.Z, Gdx.graphics.getDeltaTime() * 90f);
				pCam.lookAt(0, 0, 0);
				pCam.update();
				for (Point3D point : point3ds) {
					point3dSorter.add(point);
				}
				point3DBatch.setProjectionMatrix(pCam.combined);
				point3DBatch.begin();
				point3dShader.setUniformf("u_screenHeight", pCam.viewportHeight);
				point3dShader.setUniformf("u_up", pCam.up);
				point3dSorter.flush(point3DBatch);
				point3DBatch.end();
				break;
			case SolidQuads:
				solidQuadBatch.setProjectionMatrix(viewport.getCamera().combined);
				solidQuadBatch.begin();
				for (Item item : items) {
					solidQuadBatch.draw().size(50, 50).position(item.getWorldPosition(cam)).scale(item.scaleX, item.scaleY)
							.rotation(item.rotation).color(item.color);
				}
				solidQuadBatch.draw().color(0, 0.5f, 1, 1).size(100, 100).position(600, 100).rotation(45);
				solidQuadBatch.draw().size(50, 50).color(Color.BLUE).position(30, 30).rotation(30);
				solidQuadBatch.draw().size(20, 70).color(Color.MAGENTA).position(400, 430).origin(10, 35)
						.rotation(-elapsed * 45);
				solidQuadBatch.end();
				break;
			case TripleOverlay:
				tripleOverlayBatch.setProjectionMatrix(viewport.getCamera().combined);
				tripleOverlayBatch.begin();
				// Mix and match the pieces that come from different pages
				float size = H / 4f;
				float leftPad = 0.05f * W;
				float horizontalPad = (0.9f * W - 4 * size) / 3f;
				float bottomPad = 0.05f * H;
				float verticalPad = 0.9f * H - 2 * size;
				tripleOverlayBatch.draw()
						.textureRegion(tripleOverlayRegions.apple)
						.textureRegion(tripleOverlayRegions.plainFace)
						.textureRegion(tripleOverlayRegions.hat)
						.position(leftPad, bottomPad)
						.size(size, size);
				tripleOverlayBatch.draw()
						.textureRegion(tripleOverlayRegions.apple)
						.textureRegion(tripleOverlayRegions.plainFace)
						.textureRegion(tripleOverlayRegions.mohawk)
						.position(leftPad + size + horizontalPad, bottomPad)
						.size(size, size);
				tripleOverlayBatch.draw()
						.textureRegion(tripleOverlayRegions.apple)
						.textureRegion(tripleOverlayRegions.blinkFace)
						.textureRegion(tripleOverlayRegions.hat)
						.position(leftPad + 2 * (size + horizontalPad), bottomPad)
						.size(size, size);
				tripleOverlayBatch.draw()
						.textureRegion(tripleOverlayRegions.apple)
						.textureRegion(tripleOverlayRegions.blinkFace)
						.textureRegion(tripleOverlayRegions.mohawk)
						.position(leftPad + 3 * (size + horizontalPad), bottomPad)
						.size(size, size);
				tripleOverlayBatch.draw()
						.textureRegion(tripleOverlayRegions.orange)
						.textureRegion(tripleOverlayRegions.plainFace)
						.textureRegion(tripleOverlayRegions.hat)
						.position(leftPad, bottomPad + size + verticalPad)
						.size(size, size);
				tripleOverlayBatch.draw()
						.textureRegion(tripleOverlayRegions.orange)
						.textureRegion(tripleOverlayRegions.plainFace)
						.textureRegion(tripleOverlayRegions.mohawk)
						.position(leftPad + size + horizontalPad, bottomPad + size + verticalPad)
						.size(size, size);
				tripleOverlayBatch.draw()
						.textureRegion(tripleOverlayRegions.orange)
						.textureRegion(tripleOverlayRegions.blinkFace)
						.textureRegion(tripleOverlayRegions.hat)
						.position(leftPad + 2 * (size + horizontalPad), bottomPad + size + verticalPad)
						.size(size, size);
				tripleOverlayBatch.draw()
						.textureRegion(tripleOverlayRegions.orange)
						.textureRegion(tripleOverlayRegions.blinkFace)
						.textureRegion(tripleOverlayRegions.mohawk)
						.position(leftPad + 3 * (size + horizontalPad), bottomPad + size + verticalPad)
						.size(size, size);
				tripleOverlayBatch.end();
				break;
		}

		stage.act();
		stage.draw();
	}

	private void setupUI() {
		stage = new Stage(new ScreenViewport(), quad2dBatch);
		skin = new Skin(Gdx.files.internal("uiskin.json"));

		final SelectBox<Test> selectBox = new SelectBox<>(skin);
		selectBox.setItems(Test.values());
		selectBox.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				test = selectBox.getSelected();
				elapsed = 0;
			}
		});
		Table table = new Table();
		table.setFillParent(true);
		table.defaults().padTop(5).left();
		table.top().left().padLeft(5);
		table.add(selectBox).row();
		table.add(new Label("", skin) {
			int fps = -1;

			public void act(float delta) {
				super.act(delta);
				if (Gdx.graphics.getFramesPerSecond() != fps) {
					fps = Gdx.graphics.getFramesPerSecond();
					setText("" + fps);
				}
			}
		}).row();
		stage.addActor(table);
	}

	public void dispose() {
		Disposal.clear(this);
	}

	static final Vector3 tmp = new Vector3();

	private Quad3D makeQuad3D(float radius, float height) {
		Quad3D quad = new Quad3D();
		if (randomBoolean()) {
			quad.texture(egg).blend();
		} else {
			quad.texture(wheel).opaque();
		}
		tmp.set(radius * random(), 0, 0).rotate(Vector3.Y, random() * 360f).add(0, (random() - 0.5f) * height, 0);
		quad.position(tmp).size(1f, 1f);
		return quad;
	}

	private Point3D makePoint3D(float radius, float height) {
		Point3D point = new Point3D();
		if (randomBoolean()) {
			point.texture(egg).blend();
		} else {
			point.texture(wheel).opaque();
		}
		tmp.set(radius * random(), 0, 0).rotate(Vector3.Y, random() * 360f).add(0, (random() - 0.5f) * height, 0);
		point.position(tmp).size(1f);
		return point;
	}

	static class TripleOverlayRegions {
		public TextureRegion apple;
		public TextureRegion orange;
		public TextureRegion plainFace;
		public TextureRegion blinkFace;
		public TextureRegion mohawk;
		public TextureRegion hat;
	}

	static class Item {
		public final Color color = new Color();
		float x, y, width, height, scaleX = 1, scaleY = 1, rotation;
		int info;
		static final Vector2 tmp = new Vector2();

		Vector2 getWorldPosition(Camera cam) {
			tmp.x = ((x % 1f) - 0.5f) * (cam.viewportWidth + 300) + cam.position.x;
			tmp.y = (y - 0.5f) * (cam.viewportHeight + 100) + cam.position.y;
			return tmp;
		}
	}

	private final InputAdapter bumpInputAdapter = new InputAdapter() {

		final Vector3 tmp = new Vector3();

		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			bumpLightFollowCursor = true;
			viewport.unproject(tmp.set(screenX, screenY, 0));
			bumpLightPosition.x = tmp.x;
			bumpLightPosition.y = tmp.y;
			return true;
		}

		public boolean touchDragged(int screenX, int screenY, int pointer) {
			viewport.unproject(tmp.set(screenX, screenY, 0));
			bumpLightPosition.x = tmp.x;
			bumpLightPosition.y = tmp.y;
			return true;
		}

		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			bumpLightFollowCursor = false;
			return false;
		}
	};
}
