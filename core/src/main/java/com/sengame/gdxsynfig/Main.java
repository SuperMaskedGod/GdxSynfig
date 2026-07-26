package com.sengame.gdxsynfig;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.sengame.gdxsynfig.synfig.SifAnimation;
import com.sengame.gdxsynfig.synfig.SifParser;

public class Main extends Game {
    Stage stage;
    @Override
    public void create() {
        stage = new Stage();
        stage.setViewport(new FitViewport(1080,1920));

        SifAnimation sifAnimation = new SifAnimation(SifParser.parse(Gdx.files.internal("testAnim6.sif")), Gdx.files.internal(""));
        sifAnimation.showBeforeStart();
        sifAnimation.setOnFinished(new Runnable() {
            @Override
            public void run() {
                sifAnimation.restart();
            }
        });
        sifAnimation.play();
        stage.addActor(sifAnimation);
    }

    public Main() {
        super();
    }

    @Override
    public void dispose() {
        stage.dispose();
        super.dispose();
    }

    @Override
    public void pause() {
        super.pause();
    }

    @Override
    public void resume() {
        super.resume();
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.2f,0.2f,0.2f,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
        super.render();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width,height,true);
        stage.getViewport().apply();
        super.resize(width, height);
    }

    @Override
    public void setScreen(Screen screen) {
        super.setScreen(screen);
    }

    @Override
    public Screen getScreen() {
        return super.getScreen();
    }
}
