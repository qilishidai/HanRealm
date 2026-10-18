package core;

import com.nokia.mid.sound.Sound;
import com.nokia.mid.ui.DirectUtils;
import com.nokia.mid.ui.FullCanvas;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.StringItem;
import javax.microedition.rms.RecordStore;

public class GameScreen extends FullCanvas implements Runnable, CommandListener {

    private Game game;
    private Image tileImage; //瓦片/地图块图片

    private Menu menu;

    private Image splashImage; //启动/加载画面图片

    private Thread gameThread;

    private byte gameState; //游戏状态: 0-4运行, 5退出

    public static boolean soundEnabled = true;
    
    public static int screenWidth = 176;
    public static int screenHeight = 208;

    private String[] mainMenuItems = {"继续游戏", "新游戏", "帮助", "制作组", "退出"};

    private String recordStoreName = "china";

    private Font font = Font.getFont(0, 0, 8);

    private int menuState = 23; //菜单状态: 21主菜单, 22游戏中, 23加载中

    private int splashCounter = 60; //启动画面倒计时

    private boolean isPaused = false; //是否暂停

    private int tileSize = 9; //瓦片大小

    void drawTile(Graphics graphics, int x, int y, int tileIndex) {// 绘制单个瓦片
        graphics.setClip(x, y, this.tileSize, this.tileSize);
        graphics.drawImage(this.tileImage, x - (tileIndex * this.tileSize), y, 16 | 4);
        graphics.setClip(0, 0, screenWidth, screenHeight);
    }

    void drawSelector(Graphics graphics, int x, int y, int frame) {// 绘制菜单选择器
        graphics.setClip(x, y, 18, 11);
        graphics.drawImage(this.tileImage, (x - (frame * 18)) - 36, y, 16 | 4);
        graphics.setClip(0, 0, screenWidth, screenHeight);
    }

    private void drawBottomBar(Graphics graphics, int state) {// 绘制底部状态栏
        int i2 = this.tileSize;
        int i3 = screenWidth - i2;
        int i4 = screenHeight - i2;
        if (this.menu.isVisible) {
            drawTile(graphics, 1, i4, 0);
            if (state != 0) {
                drawTile(graphics, i3, i4, 1);
                return;
            }
            return;
        }
        drawTile(graphics, 1, i4, 2);
        if (state == 0) {
            drawTile(graphics, i3, i4, 0);
        } else {
            drawTile(graphics, i3, i4, 3);
        }
    }

    public void run() {// 主游戏循环
        long jCurrentTimeMillis = System.currentTimeMillis();
        while (this.gameState != 5) {
            try {
                if (this.isPaused) {
                    Thread.sleep(100L);
                } else {
                    updateGame();
                    long jCurrentTimeMillis2 = System.currentTimeMillis();
                    if (jCurrentTimeMillis2 - jCurrentTimeMillis < 50) {
                        Thread.sleep(((long) 50) - (jCurrentTimeMillis2 - jCurrentTimeMillis));
                    }
                    jCurrentTimeMillis = System.currentTimeMillis();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.gameThread = null;
    }

    void exitGame() {
        this.gameState = (byte) 5;
        this.gameThread = null;
    }

    protected void hideNotify() {// 屏幕隐藏时暂停
        this.isPaused = true;
    }

    protected void showNotify() {// 屏幕显示时恢复
        this.isPaused = false;
    }

    private void drawSplash(Graphics graphics) {// 绘制启动画面
        int i = screenWidth;
        int i2 = screenHeight;
        if (this.splashImage != null) {
            if (this.splashCounter <= 0) {
                graphics.setColor(0);
            } else {
                graphics.setColor(16777215);
            }
            graphics.fillRect(0, 0, i, i2);
            graphics.drawImage(this.splashImage, i / 2, i2 / 2, 1 | 2);
            graphics.setFont(Font.getFont(0, 0, 8));
            int height = this.font.getHeight() + 5;
            if (this.splashCounter <= 0) {
                drawText("载入中，请等待....", 4, i2 - height, graphics, 0, 16777215);
            } else {
                drawText(String.valueOf(String.valueOf(new StringBuffer("按键继续(").append(this.splashCounter / 6).append(")"))), 4, i2 - height, graphics, 0, 16777215);
            }
        }
    }

    public GameScreen(CatMid catMid) {
        this.splashImage = null;
        this.gameThread = null;
        this.gameState = (byte) 0;
        try {
            screenWidth = getWidth();
            screenHeight = getHeight();
            loadGame(false);
            this.splashImage = Image.createImage("/mt.png");
            this.gameThread = new Thread(this);
            this.gameThread.start();
            this.gameState = (byte) 1;
            Sound sound = new Sound(new byte[]{2, 74, 58, 64, 4, 0, 49, 38, 70, 36, 98, 69, -92, 32, -125, 13, 33, 4, -107, -119, 88, -107, 105, 8, 17, 105, 8, 17, -119, 88, -104, 105, 8, 24, 105, 8, 22, -119, 88, -109, 72, 0}, 1);
            sound.setGain(240);
            sound.play(1);
        } catch (Exception e) {
        }
    }

    void repaintScreen() {// 刷新屏幕
        repaint(0, 0, screenWidth, screenHeight);
        serviceRepaints();
    }

    private void initGame() {
        try {
            this.tileImage = Image.createImage("/mmm.png");
            this.menu = new Menu(this);
            this.game = new Game(this.font, this);
            this.menuState = 21;
            this.menu.setMenu(this.mainMenuItems, true);
            repaintScreen();
        } catch (Exception e) {
        }
    }

    private void processMainMenu() {
        int iM80if = this.menu.processMenu();
        switch (iM80if) {
            case 1:
                if (this.menuState != 22) {
                    this.splashImage = null;
                    if (!loadGame(true)) {
                        this.game.initLevel(true);
                    }
                    this.menuState = 22;
                }
                break;
            case 2:
                this.splashImage = null;
                this.game.initLevel(true);
                this.menuState = 22;
                break;
            case 3:
                showHelp();
                break;
            case 4:
                this.isPaused = true;
                Form form = new Form("制作组");
                form.append(new StringItem("汉风工作室", "【策划】\n龙猫,大款\n【设计】\n大款\n【美工】\n古月天声\n【测试】\n小雅,小芯\n【特别感谢】\nlingyu_ma,kingsgame等"));
                form.setCommandListener(this);
                form.addCommand(new Command("了解", 3, 1));
                Display.getDisplay(CatMid.instance).setCurrent(form);
                break;
            case 5:
                if (this.menuState == 22) {
                    saveGame();
                }
                CatMid.exitApp();
                return;
        }
        if (iM80if != 0) {
            repaintScreen();
        }
    }

    protected void updateGame() {
        try {
            switch (this.menuState) {
                case 21:
                    if (!this.menu.isVisible) {
                        this.menu.toggleVisibility();
                    }
                    processMainMenu();
                    break;
                case 22:
                    if (!this.game.isBusy()) {
                        processMainMenu();
                        if (this.menu.isVisible) {
                        }
                    }
                    if (this.game.update(this)) {
                        try {
                            this.splashImage = Image.createImage("/st.png");
                            break;
                        } catch (Exception e) {
                        }
                        this.menuState = 21;
                    }
                    break;
                case 23:
                    if (this.splashCounter <= 0) {
                        initGame();
                    } else {
                        try {
                            this.splashCounter--;
                            if (this.splashCounter == 0) {
                                this.splashImage = Image.createImage("/st.png");
                            }
                            repaint();
                        } catch (Exception e2) {
                            return;
                        }
                    }
                    break;
            }
        } catch (Exception e3) {
        }
    }

    protected void paint(Graphics graphics) {
        graphics.setFont(this.font);
        switch (this.menuState) {
            case 21:
                graphics.setColor(0);
                graphics.fillRect(0, 0, screenWidth, screenHeight);
                if (this.splashImage != null) {
                    graphics.drawImage(this.splashImage, screenWidth / 2, screenHeight / 2, 1 | 2);
                }
                drawBottomBar(graphics, this.gameState);
                this.menu.draw(graphics);
                break;
            case 22:
                this.game.draw(graphics);
                if (!this.game.isBusy()) {
                    drawBottomBar(graphics, this.gameState);
                    this.menu.draw(graphics);
                }
                break;
            case 23:
                drawSplash(graphics);
                break;
        }
    }

    public void commandAction(Command command, Displayable displayable) {// 命令回调
        Display.getDisplay(CatMid.instance).setCurrent(this);
    }

    private void showHelp() {
        this.isPaused = true;
        Form form = new Form("帮助");
        form.append(new StringItem("", "【描述】\n这款游戏将带你回到两千年前的遥远西域，让你在手机上再现大汉王朝的辉煌!游戏中有趣的外交，逼真的战斗，多种特产资源，丝丝入扣的任务系统以及八种不同的城市建筑，让你真正领略大型策略游戏的无限魅力！\n【规则】\n策略战棋类游戏，类似经典游戏<文明>的回合制操作方式。游戏共有6关，每一关有特定的任务。游戏中还有“指挥战斗”和“自动战斗”两种模式供玩家选择。若此回合内不想再进行任何操作，可以选择“回合结束”。光标放在主城上时，可以查看这个城市的相关信息，其中“建造”菜单建造的是光标最后指向的选项。民工可以建造各种建筑物。游戏中有多个兵种，按右功能键选择该兵种部队时，再按左功能键可以查看其信息。\n【操作】\n左功能键弹出游戏菜单。方向键移动光标。当把光标移到本方范围时，按右功能键弹出建筑菜单；当把光标移到其他的空地时，按右功能键弹出音效等各种选项供玩家选择。选择“退出”时，游戏自动保存当前的状态。\n【提示】\n一个国家的发展可离不开文化的发展哦。如果玩家觉得这款游戏很经典的话，请推荐给您身边的亲友呀。\n【客服】\n电话：021-65873552。E-MAIL：service@magma-digital.com"));
        form.setCommandListener(this);
        form.addCommand(new Command("了解", 3, 1));
        Display.getDisplay(CatMid.instance).setCurrent(form);
    }

    protected void keyPressed(int i) {// 按键按下
        i = translateKey(i);
        switch (this.menuState) {
            case 21:
                if (i == -7) {
                    return;
                }
                break;
            case 23:
                if (this.splashCounter > 1) {
                    this.splashCounter = 1;
                    return;
                }
                return;
        }
        if (!(this.menuState == 22 && this.game.isBusy()) && this.menu.handleKeyPress(i)) {
            return;
        }
        this.game.keyPressed(i);
    }

    protected void keyReleased(int i) {// 按键释放
        if (this.menuState != 22 || this.menu.isVisible) {
            return;
        }
        this.game.keyReleased();
    }

    /**
     * 把标准 MIDP 键盘码翻译回 Nokia FullCanvas 的负码，使反编译游戏原有的
     * 按键逻辑（Menu/Game 中针对 -1/-2/-5/-6/-7 等负码的 switch）能正确触发。
     * 若模拟器已传负码则原样返回。
     */
    private int translateKey(int i) {
        switch (i) {
            case Canvas.UP:
                return KEY_UP;
            case Canvas.DOWN:
                return KEY_DOWN;
            case Canvas.LEFT:
                return KEY_LEFT;
            case Canvas.RIGHT:
                return KEY_RIGHT;
            case Canvas.FIRE:
                return KEY_SELECT;
            case Canvas.GAME_A:
            case Canvas.GAME_C:
                return KEY_SOFTKEY0;
            case Canvas.GAME_B:
            case Canvas.GAME_D:
                return KEY_SOFTKEY1;
            case Canvas.KEY_STAR:
                return KEY_STAR;
            default:
                return i;
        }
    }

    static void drawText(String str, int i, int i2, Graphics graphics, int i3, int i4) {
        if (i4 != 0) {
            graphics.setColor(i4);
            graphics.drawString(str, i + 1, i2 + 1, 16 | 4);
        }
        graphics.setColor(i3);
        graphics.drawString(str, i, i2, 16 | 4);
    }

    static void drawRect(Graphics graphics, int i, int i2, int i3, int i4, int i5, int i6) {
        int[] iArr = {i, i + i3, i + i3, i, i2, i2, i2 + i4, i2 + i4};
        DirectUtils.getDirectGraphics(graphics).fillPolygon(iArr, 0, iArr, 4, 4, i5);
        if (i6 != 0) {
            graphics.setColor(i6);
            graphics.drawRect(i, i2, i3, i4);
        }
    }

    boolean loadGame(boolean loadData) {
        byte[] record;
        boolean z2 = false;
        try {
            RecordStore recordStoreOpenRecordStore = RecordStore.openRecordStore(this.recordStoreName, true);
            int i = 1;
            while (true) {
                if (i >= recordStoreOpenRecordStore.getNextRecordID()) {
                    break;
                }
                if (recordStoreOpenRecordStore.getRecordSize(i) > 0 && (record = recordStoreOpenRecordStore.getRecord(i)) != null) {
                    DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(record, 0, record.length));
                    try {
                        soundEnabled = dataInputStream.readBoolean();
                        Game.currentLevel = dataInputStream.readInt();
                        if (loadData) {
                            this.game.loadFromStream(dataInputStream);
                            z2 = true;
                        }
                    } catch (Exception e) {
                    }
                    dataInputStream.close();
                }
                i++;
            }
            recordStoreOpenRecordStore.closeRecordStore();
        } catch (Exception e2) {
        }
        return z2;
    }

    void saveGame() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try {
            try {
                dataOutputStream.writeBoolean(soundEnabled);
                dataOutputStream.writeInt(Game.currentLevel);
                if (this.menuState == 22) {
                    this.game.saveToStream(dataOutputStream);
                }
                RecordStore.deleteRecordStore(this.recordStoreName);
            } catch (Exception e) {
                return;
            }
        } catch (Exception e2) {
        }
        try {
            dataOutputStream.close();
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            RecordStore recordStoreOpenRecordStore = RecordStore.openRecordStore(this.recordStoreName, true);
            recordStoreOpenRecordStore.addRecord(byteArray, 0, byteArray.length);
            recordStoreOpenRecordStore.closeRecordStore();
        } catch (Exception e3) {
        }
    }

    void sleep(int millis) {// 线程休眠
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
        }
    }
}
