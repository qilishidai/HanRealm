package core;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

public class CatMid extends MIDlet {
    static CatMid instance; // instance (MIDlet单例实例)

    static GameScreen gameScreen = null;

    public CatMid() {
        instance = this;
    }

    public void startApp() {
        if (gameScreen == null) {
            gameScreen = new GameScreen(this);
            Display.getDisplay(instance).setCurrent(gameScreen);
        }
    }

    static Display getDisplay() {
        return Display.getDisplay(instance);
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean z) {
    }

    public static void exitApp() {
        gameScreen.exitGame();
        instance.destroyApp(true);
        instance.notifyDestroyed();
        instance = null;
    }
}
