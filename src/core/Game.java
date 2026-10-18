package core;

import com.nokia.mid.sound.Sound;
import com.nokia.mid.ui.DirectGraphics;
import com.nokia.mid.ui.DirectUtils;

import core.GameScreen;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Random;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class Game {

    private GameScreen gameScreen;
    private Font font;

    private Image objectImage; //对象图片 /objs.png
    
    protected int screenOffsetY; //屏幕Y偏移
    protected int infoBarHeight; //信息栏高度
    protected int mapScrollX; //地图滚动X
    protected int mapScrollY;
    
    private int dialogWidth;
    private int dialogHeight;
    private String[] dialogLines; //对话框文本行

    private int[][] mapData;
    private String missionTitle; //任务标题
    private int dialogLineCount; //对话框行数

    private int[][] diplomacyData; //外交关系数据
    private String[] cityNames;
    private City[] cities;
    private City selectedCity;

    private Unit[] units;
    private Unit selectedUnit;
    private int unitCount;
    private int cityCount;
    private Menu menu;
    
    public static int currentLevel = 1;
    public static boolean battleMode = true; //战斗模式: true指挥/false自动
    static String[] factionNames = {"汉朝", "匈奴", "大月氏", "楼兰", "大宛"};
    static int[] factionColors = {2147483392, 2130771712, 2130706687, 134152447, 2147418112};
    
    private boolean isGameOver = false;

    byte[] soundData = {2, 74, 58, 64, 4, 0, 17, 38, 70, 36, 98, 70, -124, 32, 66, -120, 35, 0, 0};
    private Random random = new Random();
    private boolean needRepaint = true;

    protected int cursorX = 0;
    protected int cursorY = 0;
    protected int viewScale = 25; //视野缩放/物价指数
    protected int[] cursorPos = {0, 0}; //光标屏幕坐标
    
    private int keyCode = 0;
    private int keyState = 0;

    private int gameState = 0;
    private int dialogResult = 0;
    private int frameCounter = 0; //帧计数器
    private int animCounter = 0; //动画计数器
    private int turnCounter = 0; //回合计数器
    private int menuState = 1; //菜单状态
    private int subMenuIndex = 0;
    
    // ==================== UI颜色 ====================
    private int[] statusBarColors = {6786015, 9218541, 9875173, 11124208, 12044017, 12832755, 14739193};
    
    private String[] missionNames = {"一.修养生息", "二.骑兵", "三.结盟", "四.反击", "五.西征", "六.虽远必诛"};
    private int[][] tileOffsets = {         // 瓦片偏移量
            new int[]{0, 0}, new int[]{1, 0}, new int[]{2, 0}, new int[]{3, 0},
            new int[]{2, 16384}, new int[]{1, 16384}, new int[]{2, 24576},
            new int[]{3, 8192}, new int[]{2, 8192}, new int[]{4, 0}
        };
    private int[] menuOptionIndex = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private int menuOptionCount = 0;
    
    protected int[][] spriteFrames = { // 精灵帧数据
    		new int[]{0, 0, 26, 26, 13, 20}, new int[]{26, 0, 29, 30, 15, 24}, 
    		new int[]{0, 26, 25, 14}, new int[]{62, 21, 38, 22}, 
    		new int[]{55, 0, 22, 20}, new int[]{77, 0, 24, 21}, 
    		new int[]{101, 0, 24, 21}, new int[]{125, 0, 24, 21}, 
    		new int[]{149, 0, 24, 21}, new int[]{173, 0, 23, 21}, 
    		new int[0], new int[]{0, 68, 32, 28, 16, 18}, 
    		new int[]{0, 42, 36, 26, 18, 16}, new int[]{100, 21, 30, 15}, 
    		new int[]{130, 21, 32, 15}, new int[]{101, 68, 32, 22, 15, 13}, 
    		new int[]{32, 121, 32, 19, 13, 10}, new int[]{164, 85, 32, 20, 16, 11}, 
    		new int[]{164, 38, 32, 28, 16, 18}, new int[]{164, 66, 32, 19, 16, 10}, 
    		new int[0], new int[]{164, 105, 32, 20, 16, 13}, 
    		new int[]{68, 73, 32, 19, 16, 11}, new int[]{132, 119, 32, 20, 16, 12}, 
    		new int[]{69, 92, 31, 19, 16, 11}, new int[]{132, 65, 32, 19, 14, 9}, 
    		new int[0], new int[]{64, 43, 23, 30, 12, 25}, 
    		new int[]{64, 111, 18, 28, 9, 24}, new int[]{32, 62, 32, 34, 16, 30}, 
    		new int[]{96, 113, 18, 25, 9, 21}, new int[]{132, 37, 32, 27, 16, 23}, 
    		new int[]{87, 43, 21, 28, 10, 24}, new int[]{110, 38, 22, 31, 10, 25}, 
    		new int[]{133, 84, 31, 35, 16, 31}, new int[]{114, 113, 18, 25, 9, 21}, 
    		new int[]{0, 112, 32, 28, 16, 24}, new int[]{37, 95, 15, 26, 9, 22}, 
    		new int[]{52, 95, 16, 26, 6, 22}, new int[0], new int[0], 
    		new int[]{25, 30, 11, 11, 11, 11}, new int[]{122, 105, 7, 4, 4, 4}, 
    		new int[]{122, 90, 7, 10, 4, 10}, new int[]{122, 100, 7, 4, 4, 10}, 
    		new int[]{36, 30, 26, 31, 13, 22}, new int[]{36, 30, 26, 31, 13, 23}, 
    		new int[]{100, 107, 6, 6, 0, 0}, new int[]{105, 107, 6, 6, 0, 0}, 
    		new int[]{164, 21, 31, 17}, new int[]{82, 125, 13, 13}, 
    		new int[]{82, 111, 14, 14}, new int[]{100, 90, 21, 16, 10, 8}, 
    		new int[]{100, 90, 21, 16, 10, 9}, new int[]{32, 97, 5, 13, 3, 13}, 
    		new int[]{32, 97, 5, 13, 3, 14}, new int[]{0, 104, 15, 8}, 
    		new int[]{17, 104, 15, 8}, new int[]{17, 96, 15, 8}, 
    		new int[]{0, 96, 15, 8}, new int[]{111, 108, 11, 5}, 
    		new int[]{166, 133, 13, 7}, new int[]{181, 133, 13, 7}, 
    		new int[]{181, 125, 13, 7}, new int[]{166, 125, 13, 7}};
    
    private int screenWidth = GameScreen.screenWidth;
    private int screenHeight = GameScreen.screenHeight;
    
    private int diplomatIndex = 0; //外交列表索引
    private int diplomacyAction = 0; //外交动作
    private int diplomacyTarget = 0; //外交目标

    private String[] diplomacyActions = {"送礼", "结盟", "勒索", "毁约", "宣战", "停战"};
    private String[] diplomacyMessages = {"", "", "陛下,%0威胁我们立即进贡，同意吗？", "陛下,%0提前终止了和我们的军事同盟。", "陛下,%0向我们宣战了!", ""};
    private String[] envoyResults = {"使者:成功了，%0接收了我国的%1！", "使者:我没能完成对%0的%1任务,请陛下治罪。"};
    private String[] confirmTexts = {"确定", "同意", "建造", "接收"};
    private String[] cancelTexts = {"取消", "拒绝", "取消", "拒绝"};
    private String[] menuLabels = {"选择", "执行", "政策", "买粮", "卖粮", "购买", "外交信息"};
    private String[] relationNames = {"正常", "战争", "结盟"};
    private String[] cityInfoLabels = {"城市", "归属", "人口"};
    private String[] nationInfoLabels = {"国家", "关系", "友好"};
    
    // ==================== 关卡任务条件 ====================
    private int[][] missionConditions = {
        new int[]{0, 3, 1000000, 1, -1, 5},
        new int[]{0, -1, 3, 1, 2, 5},
        new int[]{2, 2, 2},
        new int[]{0, 0, 4},
        new int[]{0, 0, 5},
        new int[0],
        new int[0]
    };
    
    // ==================== 外交关系矩阵 ====================
    private int[][][] diplomacyMatrix = {
        new int[][]{new int[]{100, 50}, new int[]{40, 100}},
        new int[][]{new int[]{100, 50}, new int[]{20, 100}},
        new int[][]{new int[]{100, 0, 100}, new int[]{0, 100, 276}, new int[]{50, 276, 100}},
        new int[][]{new int[]{100, 256}, new int[]{256, 100}},
        new int[][]{new int[]{100, 256, 612}, new int[]{256, 100, 256}, new int[]{612, 256, 100}},
        new int[][]{new int[]{100, 256, 100, 50, 50}, new int[]{256, 100, 40, 50, 50}, new int[]{100, 50, 100, 50, 50}, new int[]{50, 50, 50, 100, 50}, new int[]{50, 50, 50, 50, 100}}
    };
    
    private int cityMenuIndex = 0;
    private int cityInfoIndex = 0;
    
    private Unit tempUnit = null;
    private String[] actionNames = {"制造", "信息", "城市", "外交"};
    private String[] dialogTexts = new String[24];
    private int[] dialogColors = new int[24];
    
    private Sound soundEffect = new Sound(this.soundData, 1);

    private Image mapImage = Image.createImage("/map.png");

    public Game(Font font, GameScreen eVar) throws Exception {
        this.font = font;
        this.gameScreen = eVar;
        this.menu = new Menu(eVar);
        this.infoBarHeight = GameScreen.screenHeight - (font.getHeight() + 6);
        this.screenOffsetY = 0;
        if (this.infoBarHeight > 80) {
            this.screenOffsetY = (this.infoBarHeight - 80) >> 1;
            this.infoBarHeight = 80 + this.screenOffsetY;
        }
        if (this.screenOffsetY < 0) {
            this.screenOffsetY = 0;
        }
        this.cities = new City[8];
        for (int i = 0; i < this.cities.length; i++) {
            this.cities[i] = new City(this, i + 1);
        }
        this.units = new Unit[96];
        for (int i2 = 0; i2 < this.units.length; i2++) {
            this.units[i2] = new Unit(this, i2 + 1);
        }
    }

    void playSound() {
        if (GameScreen.soundEnabled && this.soundEffect.getState() != 0) {
            this.soundEffect.setGain(240);
            this.soundEffect.play(1);
        }
    }

    void initLevel(boolean z) { // 初始化关卡
        this.subMenuIndex = 0;
        this.isGameOver = false;
        this.viewScale = 25;
        if (z) {
            try {
                if (this.objectImage == null) {
                    this.objectImage = Image.createImage("/objs.png");
                }
            } catch (Exception e) {
            }
            if (currentLevel > 1) {
                if (currentLevel > this.missionNames.length) {
                    currentLevel = this.missionNames.length;
                }
                String[] strArr = new String[currentLevel];
                for (int i = 0; i < strArr.length; i++) {
                    strArr[i] = this.missionNames[i];
                }
                this.menu.setMenu(strArr, true);
                this.menuState = 11;
                return;
            }
            this.dialogResult = 0;
        } else {
            this.dialogResult++;
            if (currentLevel < this.dialogResult + 1) {
                currentLevel = this.dialogResult + 1;
            }
            if (currentLevel > this.missionNames.length) {
                currentLevel = this.missionNames.length;
            }
            if (this.dialogResult >= this.missionNames.length) {
                showDialog("胜利", "经过近百年的浴血奋战，汉朝终于切地击败匈奴。你完成了全部任务。", true, 0);
                this.isGameOver = true;
                return;
            }
        }
        resetGameState();
    }

    private void resetGameState() {
        this.gameState = 0;
        this.needRepaint = true;
        this.isGameOver = false;
        this.dialogWidth = 0;
        this.dialogHeight = 0;
        loadMapData(true);
        this.tempUnit = null;
        this.mapScrollX = 20 * (this.mapData.length + this.mapData[0].length);
        this.mapScrollY = 10 * (this.mapData.length + this.mapData[0].length);
        moveCursor(this.keyCode, this.keyState);
        this.menuState = 0;
    }

    void loadFromStream(DataInputStream dataInputStream) throws Exception {
        battleMode = dataInputStream.readBoolean();
        this.subMenuIndex = dataInputStream.readInt();
        this.viewScale = dataInputStream.readInt();
        if (this.objectImage == null) {
            this.objectImage = Image.createImage("/objs.png");
        }
        this.dialogResult = dataInputStream.readInt();
        this.keyCode = dataInputStream.readInt();
        this.keyState = dataInputStream.readInt();
        this.needRepaint = true;
        this.isGameOver = false;
        this.dialogWidth = 0;
        this.dialogHeight = 0;
        int i = dataInputStream.readInt();
        int i2 = dataInputStream.readInt();
        this.mapData = new int[i2][i];
        for (int i3 = 0; i3 < i2; i3++) {
            for (int i4 = 0; i4 < i; i4++) {
                this.mapData[i3][i4] = dataInputStream.readInt() & Integer.MAX_VALUE;
            }
        }
        this.cityInfoIndex = dataInputStream.readInt();
        this.cityMenuIndex = dataInputStream.readInt();
        for (int i5 = 0; i5 < this.units.length; i5++) {
            this.units[i5].resetUnit();
        }
        for (int i6 = 0; i6 < this.cities.length; i6++) {
            this.cities[i6].techLevel = 0;
        }
        for (int i7 = 0; i7 < this.cityInfoIndex; i7++) {
            this.units[i7].loadFromStream(dataInputStream);
        }
        for (int i8 = 0; i8 < this.cityMenuIndex; i8++) {
            this.cities[i8].loadFromStream(dataInputStream);
        }
        int i9 = dataInputStream.readInt();
        this.diplomacyData = new int[i9][];
        for (int i10 = 0; i10 < i9; i10++) {
            int i11 = dataInputStream.readInt();
            this.diplomacyData[i10] = new int[i11];
            for (int i12 = 0; i12 < i11; i12++) {
                this.diplomacyData[i10][i12] = dataInputStream.readInt();
            }
        }
        this.gameState = selectTile(this.keyCode, this.keyState);
        this.mapScrollX = 20 * (i2 + i);
        this.mapScrollY = 10 * (i2 + i);
        moveCursor(this.keyCode, this.keyState);
        this.menuState = 1;
        loadMapData(false);
    }

    void saveToStream(DataOutputStream dataOutputStream) throws Exception {
        dataOutputStream.writeBoolean(battleMode);
        dataOutputStream.writeInt(this.subMenuIndex);
        dataOutputStream.writeInt(this.viewScale);
        dataOutputStream.writeInt(this.dialogResult);
        dataOutputStream.writeInt(this.keyCode);
        dataOutputStream.writeInt(this.keyState);
        int length = this.mapData[0].length;
        int length2 = this.mapData.length;
        dataOutputStream.writeInt(length);
        dataOutputStream.writeInt(length2);
        for (int i = 0; i < length2; i++) {
            for (int i2 = 0; i2 < length; i2++) {
                dataOutputStream.writeInt(this.mapData[i][i2]);
            }
        }
        dataOutputStream.writeInt(this.cityInfoIndex);
        dataOutputStream.writeInt(this.cityMenuIndex);
        for (int i3 = 0; i3 < this.cityInfoIndex; i3++) {
            this.units[i3].saveToStream(dataOutputStream);
        }
        for (int i4 = 0; i4 < this.cityMenuIndex; i4++) {
            this.cities[i4].saveToStream(dataOutputStream);
        }
        int length3 = this.diplomacyData.length;
        dataOutputStream.writeInt(length3);
        for (int i5 = 0; i5 < length3; i5++) {
            int length4 = this.diplomacyData[i5].length;
            dataOutputStream.writeInt(length4);
            for (int i6 = 0; i6 < length4; i6++) {
                dataOutputStream.writeInt(this.diplomacyData[i5][i6]);
            }
        }
    }

    private void loadMapData(boolean z) {
        DataInputStream dataInputStream = new DataInputStream(getClass().getResourceAsStream("/data.png"));
        try {
            dataInputStream.readInt();
            dataInputStream.skipBytes(this.dialogResult * 4);
            dataInputStream.skipBytes(dataInputStream.readInt());
            this.cityNames = new String[dataInputStream.readInt()];
            for (int i = 0; i < this.cityNames.length; i++) {
                this.cityNames[i] = dataInputStream.readUTF();
            }
            this.dialogLines = new String[2];
            for (int i2 = 0; i2 < this.dialogLines.length; i2++) {
                this.dialogLines[i2] = dataInputStream.readUTF();
            }
            if (z) {
                int[][] iArr = this.diplomacyMatrix[this.dialogResult];
                this.diplomacyData = new int[iArr.length][];
                for (int i3 = 0; i3 < iArr.length; i3++) {
                    this.diplomacyData[i3] = new int[iArr[i3].length];
                    for (int i4 = 0; i4 < iArr[i3].length; i4++) {
                        this.diplomacyData[i3][i4] = iArr[i3][i4];
                    }
                }
                showDialog(this.missionNames[this.dialogResult], this.dialogLines[0], false, 0);
                addDialogLine(this.dialogLines[1], 11468800);
                int i5 = dataInputStream.readInt();
                int i6 = dataInputStream.readInt();
                this.mapData = new int[i6][i5];
                for (int i7 = 0; i7 < this.cities.length; i7++) {
                    this.cities[i7].techLevel = 0;
                }
                for (int i8 = 0; i8 < this.units.length; i8++) {
                    this.units[i8].resetUnit();
                }
                this.cityMenuIndex = 0;
                this.cityInfoIndex = 0;
                for (int i9 = 0; i9 < i6; i9++) {
                    for (int i10 = 0; i10 < i5; i10++) {
                        int i11 = dataInputStream.readInt();
                        int i12 = (i11 >> 8) & 255;
                        switch (i12) {
                            case 12:
                            case 13:
                                if (this.cityMenuIndex < this.cities.length) {
                                    int i13 = this.cities[this.cityMenuIndex].ownerId;
                                    int i14 = (i11 >> 20) & 15;
                                    this.cities[this.cityMenuIndex].initCity(this.cityNames[i13 - 1], i10, i9, i12 - 12, (i11 >> 16) & 15, i14);
                                    this.cityMenuIndex++;
                                    this.mapData[i9][i10] = (i11 & 255) | 3072 | (i13 << 16);
                                    if (i14 == 0) {
                                        this.keyCode = i10;
                                        this.keyState = i9;
                                        this.gameState = i13;
                                    }
                                } else {
                                    this.mapData[i9][i10] = i11 & 255;
                                }
                                break;
                            case 14:
                            case 15:
                            case 16:
                            case 17:
                            case 18:
                            case 19:
                            case 20:
                            case 21:
                            case 22:
                            case 23:
                            case 24:
                            case 25:
                            case 26:
                            case 27:
                            default:
                                this.mapData[i9][i10] = i11;
                                break;
                            case 28:
                            case 29:
                            case 30:
                            case 31:
                            case 32:
                                this.mapData[i9][i10] = (i11 & 255) | (createUnitFromMap(i11 >> 8, i10, i9) << 24);
                                break;
                        }
                    }
                }
            }
            dataInputStream.close();
        } catch (Exception e) {
        }
    }

    private int createUnitFromMap(int i, int i2, int i3) {
        if (this.cityInfoIndex >= this.units.length) {
            return 0;
        }
        int i4 = (i & 255) - 28;
        int i5 = this.units[this.cityInfoIndex].unitId;
        int i6 = Unit.unitStats[i4][0];
        this.units[this.cityInfoIndex].initUnit(i6, (i6 * ((i >> 12) & 15)) / 10, i4, (i >> 16) & 15, (i >> 8) & 15);
        this.units[this.cityInfoIndex].mapX = i2;
        this.units[this.cityInfoIndex].mapY = i3;
        this.cityInfoIndex++;
        return i5;
    }

    private void updateAnimationCounter() { //更新动画计数器
        if (this.frameCounter < 3) {
            this.frameCounter++;
            return;
        }
        this.frameCounter = 0;
        this.needRepaint = true;
        if (this.animCounter < 6) {
            this.animCounter++;
        } else {
            this.animCounter = 0;
        }
    }

    private void showGameMenu() {
        String[] strArr = new String[4];
        strArr[0] = "回合结束";
        strArr[1] = battleMode ? "指挥战斗" : "自动战斗";
        strArr[2] = GameScreen.soundEnabled ? "关闭音效" : "开启音效";
        strArr[3] = "任务描述";
        this.menu.setMenu(strArr, true);
        this.menuState = 9;
        this.needRepaint = true;
        this.dialogWidth = 0;
    }

    private void processCursorInput() { //处理光标输入
        updateAnimationCounter();
        if (this.turnCounter > 0) {
            this.turnCounter--;
            return;
        }
        if (this.dialogWidth != 0) {
            this.keyState = scrollDown(this.keyState, this.mapData.length);
            this.keyCode = scrollUp(this.keyCode, this.mapData[0].length);
            switch (this.dialogWidth) {
                case -10:
                case -7:
                case -5:
                    this.needRepaint = true;
                    this.menuState = 2;
                    this.dialogWidth = 0;
                    if (this.tempUnit == null) {
                        City dVarM59if = getCity(getCityOwner(this.keyCode, this.keyState), false);
                        if (dVarM59if != null) {
                            this.selectedCity = dVarM59if;
                            this.cityCount = 0;
                            if (this.selectedCity.ownerIndex == 0) {
                                this.menuState = 5;
                            } else {
                                this.menuState = 6;
                            }
                            this.menuOptionIndex[0] = 8;
                            this.menuOptionCount = 1;
                            this.unitCount = 0;
                            this.diplomatIndex = 0;
                            this.diplomacyTarget = 0;
                            if (getUnit(-1, false) != null) {
                                for (int i = 0; i < Unit.resourceRequirements.length; i++) {
                                    if (this.selectedCity.hasRequiredResources(i)) {
                                        this.menuOptionIndex[this.menuOptionCount] = i;
                                        if (this.selectedCity.buildTarget == i) {
                                            this.unitCount = this.menuOptionCount;
                                        }
                                        this.menuOptionCount++;
                                    }
                                }
                            }
                        } else if (tryBuild(this.keyCode, this.keyState)) {
                            this.menuState = 1;
                        } else {
                            showGameMenu();
                        }
                    } else if (this.tempUnit.factionIndex != 0 || this.tempUnit.attackCooldown == 0) {
                        this.tempUnit.showUnitInfo();
                        this.menuState = 1;
                    } else if (!this.tempUnit.isDead) {
                        if (this.tempUnit.updateMovement(false)) {
                            this.menuState = 3;
                        } else {
                            this.tempUnit.showUnitInfo();
                            this.menuState = 1;
                        }
                        this.selectedUnit = this.tempUnit;
                    } else if (showDialog("指挥确认", String.valueOf(String.valueOf(new StringBuffer("该民工队伍正在建造").append(City.buildingNames[this.tempUnit.moveX]).append(",是否终止工作?"))), true, 1) != 1) {
                        this.menuState = 1;
                    } else {
                        this.tempUnit.clearDeadFlag();
                        if (this.tempUnit.updateMovement(false)) {
                            this.menuState = 3;
                        }
                        this.selectedUnit = this.tempUnit;
                    }
                    break;
                case 49:
                    selectNextCity(true);
                    this.dialogWidth = 0;
                    this.needRepaint = true;
                    break;
                case 50:
                    selectNextUnit();
                    this.dialogWidth = 0;
                    this.needRepaint = true;
                    break;
                case 51:
                    showGameMenu();
                    this.dialogWidth = 0;
                    break;
                case 52:
                    selectNextCity(false);
                    this.dialogWidth = 0;
                    this.needRepaint = true;
                    break;
            }
            if (this.needRepaint) {
                this.gameState = selectTile(this.keyCode, this.keyState);
                moveCursor(this.keyCode, this.keyState);
            }
            if (this.turnCounter < 0) {
                this.turnCounter = 4;
            }
        }
    }

    boolean tryBuild(int i, int i2) { //尝试建造
        int iE;
        this.menuOptionCount = 0;
        if (this.selectedCity == null || this.selectedCity.ownerIndex != 0 || (iE = getObjectType(i, i2)) == 50 || City.getResourceIndex(iE) >= 0) {
            return false;
        }
        this.cityCount = City.getBuildingIndex(iE);
        this.selectedUnit = findIdleWorker(this.selectedCity.ownerId);
        if (this.selectedUnit == null) {
            return false;
        }
        for (int i3 = 0; i3 < City.buildingNames.length; i3++) {
            if (i3 != this.cityCount && this.selectedCity.attributes[6] >= City.buildingCosts[i3] / 5) {
                this.menuOptionIndex[this.menuOptionCount] = i3;
                this.menuOptionCount++;
            }
        }
        if (this.menuOptionCount == 0) {
            showDialog("建造信息", "缺少足够的资金!", true, 0);
            return true;
        }
        this.menuState = 7;
        int iF = selectBuilding();
        this.menuState = 2;
        if (iF < 0) {
            return true;
        }
        this.selectedUnit.moveToPosition(i, i2, this.gameScreen);
        this.selectedUnit.startBuilding(this.menuOptionIndex[iF], this.selectedCity);
        this.gameState = selectTile(this.keyCode, this.keyState);
        return true;
    }

    private int selectBuilding() { //选择建筑菜单
        this.unitCount = 0;
        this.needRepaint = true;
        while (true) {
            if (this.turnCounter > 0) {
                this.turnCounter--;
            } else if (this.dialogWidth != 0) {
                this.unitCount = scrollDown(this.unitCount, this.menuOptionCount);
                if (isConfirmKey()) {
                    this.dialogWidth = 0;
                    return this.unitCount;
                }
                if (this.dialogWidth == -7) {
                    this.dialogWidth = 0;
                    return -1;
                }
                if (this.turnCounter < 0) {
                    this.turnCounter = 4;
                }
            }
            if (this.needRepaint) {
                this.gameScreen.repaintScreen();
            }
            if (this.dialogHeight != 0) {
                this.dialogWidth = 0;
                this.dialogHeight = 0;
            }
            this.gameScreen.sleep(50);
        }
    }

    private boolean canBuildCity(int i, int i2) {
        return getCity(-1, false) != null && getOwnerId(i, i2) == 0 && getTerrain(i, i2) == 0;
    }

    boolean processUnitTurn(Unit aVar, boolean z) { //处理部队回合
        int iM89do;
        if (aVar.ownerId <= 0 || aVar.attackCooldown < 0) {
            aVar.attackCooldown = 0;
            return true;
        }
        if (!z && aVar.attackCooldown > 0) {
            return false;
        }
        this.menuState = 1;
        aVar.updateMovement(true);
        if (tryFoundCity(aVar)) {
            return true;
        }
        if (aVar.targetUnit != null && ((showDialog("指挥", "指挥弓兵射击敌人吗?", true, 1) == 1 || battleMode || aVar.factionIndex != 0) && aVar.attackTarget(this.gameScreen))) {
            return true;
        }
        int i = aVar.mapX;
        int i2 = aVar.mapY;
        City dVarM59if = getCity(getOwnerId(i, i2), false);
        if (dVarM59if == null || (iM89do = City.getBuildingIndex(getObjectType(aVar.mapX, i2))) < 0 || getDiplomacyValue(aVar.factionIndex, dVarM59if.ownerIndex, 1) != 1) {
            return true;
        }
        if (!battleMode && aVar.factionIndex == 0 && showDialog("指挥确认", "此处有敌人的建筑，指挥军队破坏他们吗?", true, 1) != 1) {
            return true;
        }
        setObjectType(i, i2, 0);
        if (dVarM59if.buildings[iM89do] > 0) {
            int[] iArr = dVarM59if.buildings;
            iArr[iM89do] = iArr[iM89do] - 1;
        }
        aVar.attackCooldown = 0;
        return true;
    }

    private boolean tryFoundCity(Unit aVar) { //尝试建立城市
        int i = aVar.mapX;
        int i2 = aVar.mapY;
        if (aVar.unitType != 4 || !canBuildCity(i, i2) || showDialog("建造城市", "此地看来非常适合建造新的城镇，确认吗?", true, 1) != 1) {
            return false;
        }
        City dVarM59if = getCity(-1, true);
        dVarM59if.initCity(this.cityNames[dVarM59if.ownerId - 1], i, i2, 1 - this.cities[aVar.ownerId - 1].level, 1, aVar.factionIndex);
        dVarM59if.expandDomain();
        setCityTile(i, i2, dVarM59if.ownerId);
        aVar.convertToWorker(dVarM59if.ownerId);
        return true;
    }

    private void processUnitMovement() {
        boolean z;
        updateUnitAnimations();
        if (this.turnCounter > 0) {
            this.turnCounter--;
            return;
        }
        if (this.dialogWidth != 0) {
            switch (this.dialogWidth) {
                case -4:
                    z = !this.selectedUnit.moveToDirection(2, this.gameScreen);
                    break;
                case -3:
                    z = !this.selectedUnit.moveToDirection(0, this.gameScreen);
                    break;
                case -2:
                    z = !this.selectedUnit.moveToDirection(1, this.gameScreen);
                    break;
                case -1:
                    z = !this.selectedUnit.moveToDirection(3, this.gameScreen);
                    break;
                case 53:
                    this.tempUnit.showUnitInfo();
                    this.needRepaint = true;
                    return;
                default:
                    z = true;
                    this.dialogWidth = 0;
                    break;
            }
            this.dialogWidth = 0;
            this.needRepaint = true;
            this.selectedUnit.updateMovement(false);
            if (processUnitTurn(this.selectedUnit, z)) {
                this.menuState = 1;
                this.keyCode = this.selectedUnit.mapX;
                this.keyState = this.selectedUnit.mapY;
                this.gameState = selectTile(this.keyCode, this.keyState);
            }
            if (this.turnCounter < 0) {
                this.turnCounter = 4;
            }
        }
    }

    private int processDialogInput() {
        int height = this.font.getHeight() + 2;
        int i = (this.screenHeight - (this.diplomacyAction == 0 ? height + 8 : height * 2)) / height;
        if (this.turnCounter > 0) {
            this.turnCounter--;
            return 0;
        }
        if (this.dialogWidth == 0) {
            return 0;
        }
        switch (this.dialogWidth) {
            case -7:
            case -6:
            case -5:
                this.dialogLineCount = 0;
                this.needRepaint = true;
                int i2 = this.dialogWidth == -7 ? 2 : 1;
                this.dialogWidth = 0;
                if (this.diplomacyAction == 0) {
                    return 1;
                }
                return i2;
            case -2:
                if (this.diplomatIndex + i < this.dialogLineCount) {
                    this.diplomatIndex++;
                    this.needRepaint = true;
                }
                break;
            case -1:
                if (this.diplomatIndex > 0) {
                    this.diplomatIndex--;
                    this.needRepaint = true;
                }
                break;
        }
        if (this.turnCounter >= 0) {
            return 0;
        }
        this.turnCounter = 4;
        return 0;
    }

    private void updateUnitAnimations() {
        for (int i = 0; i < this.cityInfoIndex; i++) {
            if (this.units[i].updateMoveAnim()) {
                this.needRepaint = true;
            }
        }
    }

    private void processCityMenu() {
        if (this.turnCounter > 0) {
            this.turnCounter--;
            return;
        }
        if (this.dialogWidth != 0) {
            this.cityCount = scrollUp(this.cityCount, this.actionNames.length);
            if (this.needRepaint) {
                if (this.cityCount == 2) {
                    this.diplomatIndex = this.selectedCity.ownerId - 1;
                } else {
                    this.diplomatIndex = 0;
                }
            } else if (this.dialogWidth == -7 && (this.cityCount == 2 || this.cityCount == 0)) {
                this.menuState = 1;
                this.gameState = selectTile(this.keyCode, this.keyState);
                this.dialogWidth = 0;
            }
            switch (this.cityCount) {
                case 0:
                    processProduction();
                    break;
                case 1:
                    processTrade();
                    break;
                case 2:
                    processCityList();
                    break;
                case 3:
                    processDiplomacyMenu();
                    break;
            }
            if (this.turnCounter < 0) {
                this.turnCounter = 4;
            }
        }
    }

    private void executeDiplomacy(int i, int i2, int i3) { //执行外交动作
        switch (i3) {
            case 0:
                doGift(i, i2);
                break;
            case 1:
                doAlly(i, i2);
                break;
            case 2:
                doExtort(i, i2);
                break;
            case 3:
                doBreakAlliance(i, i2);
                break;
            case 4:
                doDeclareWar(i, i2);
                break;
            case 5:
                doMakePeace(i, i2);
                break;
        }
    }

    private boolean canMakePeace(int i, int i2) { //能否停战
        return i2 == 0 ? showDialog(this.menuLabels[6], formatMessage(this.diplomacyMessages[5], new String[]{factionNames[i]}), true, 2) == 1 : randomInt(calculateFactionPower(i)) > (calculateFactionPower(i2) * 2) / 3;
    }

    private boolean declareWar(int i, int i2) { //宣战
        if (i2 != 0) {
            return true;
        }
        showDialog(this.menuLabels[6], formatMessage(this.diplomacyMessages[4], new String[]{factionNames[i]}), true, 0);
        return true;
    }

    private boolean canExtort(int i, int i2) { //能否勒索
        if (i2 == 0) {
            return showDialog(this.menuLabels[6], formatMessage(this.diplomacyMessages[2], new String[]{factionNames[i]}), true, 2) == 1;
        }
        int iM68else = getDiplomacyValue(i2, i, 0);
        int iM69byte = calculateFactionPower(i);
        int iM69byte2 = calculateFactionPower(i2);
        return iM69byte > iM69byte2 + ((randomInt(110 - iM68else) * iM69byte2) / 100);
    }

    private boolean canAlly(int i, int i2) { //能否结盟
        if (i2 == 0) {
            return showDialog(this.menuLabels[6], formatMessage(this.diplomacyMessages[1], new String[]{factionNames[i]}), true, 2) == 1;
        }
        if (randomInt(getDiplomacyValue(i2, i, 0)) < 95) {
            return false;
        }
        int iM69byte = calculateFactionPower(i);
        int iM69byte2 = calculateFactionPower(i2);
        return iM69byte > iM69byte2 + randomInt(iM69byte2);
    }

    private boolean canGift(int i, int i2) { //能否送礼
        return i2 == 0 ? showDialog(this.menuLabels[6], formatMessage(this.diplomacyMessages[0], new String[]{factionNames[i]}), true, 2) == 1 : randomInt(getDiplomacyValue(i2, i, 0) + 50) > 40;
    }

    private void deductGold(int i, int i2, int i3) { //扣除金币
        if (i != 0 || this.selectedCity == null) {
            City dVarA = findFactionCity(i);
            if (dVarA != null) {
                int[] iArr = dVarA.attributes;
                iArr[6] = iArr[6] - i3;
            }
        } else {
            int[] iArr2 = this.selectedCity.attributes;
            iArr2[6] = iArr2[6] - i3;
        }
        City dVarA2 = findFactionCity(i2);
        if (dVarA2 != null) {
            int[] iArr3 = dVarA2.attributes;
            iArr3[6] = iArr3[6] - i3;
        }
    }

    private void adjustRelation(int i, int i2, int i3) { //调整关系值
        int iM68else = getDiplomacyValue(i, i2, 0) + i3;
        if (iM68else < 0) {
            iM68else = 0;
        } else if (iM68else > 100) {
            iM68else = 100;
        }
        setDiplomacyValue(i, i2, 0, iM68else);
    }

    private void doGift(int i, int i2) {
        boolean zM31case = canGift(i, i2);
        if (zM31case) {
            adjustRelation(i2, i, 3);
            deductGold(i, i2, 100);
        } else {
            adjustRelation(i, i2, -4);
        }
        if (i == 0) {
            String[] strArr = {factionNames[i2], this.diplomacyActions[0]};
            if (i == 0) {
                showDialog(this.menuLabels[6], formatMessage(this.envoyResults[zM31case ? (char) 0 : (char) 1], strArr), true, 0);
            }
        }
    }

    private void doAlly(int i, int i2) { //执行结盟
        boolean zM30new = canAlly(i, i2);
        if (zM30new) {
            setDiplomacyValue(i, i2, 0, 100);
            setDiplomacyValue(i2, i, 0, 100);
            setDiplomacyValue(i, i2, 1, 2);
            setDiplomacyValue(i2, i, 1, 2);
            spreadRelationEffect(i, i2, 5);
        } else {
            adjustRelation(i, i2, -2);
        }
        if (i == 0) {
            String[] strArr = {factionNames[i2], this.diplomacyActions[1]};
            if (i == 0) {
                showDialog(this.menuLabels[6], formatMessage(this.envoyResults[zM30new ? (char) 0 : (char) 1], strArr), true, 0);
            }
        }
    }

    private void doExtort(int i, int i2) { //执行勒索
        boolean zH = canExtort(i, i2);
        if (zH) {
            deductGold(i2, i, 100);
            adjustRelation(i2, i, -3);
            spreadRelationEffect(i, i2, -1);
        } else {
            adjustRelation(i, i2, -5);
            adjustRelation(i2, i, -2);
        }
        if (i == 0) {
            String[] strArr = {factionNames[i2], this.diplomacyActions[2]};
            if (i == 0) {
                showDialog(this.menuLabels[6], formatMessage(this.envoyResults[zH ? (char) 0 : (char) 1], strArr), true, 0);
            }
        }
    }

    private void doBreakAlliance(int i, int i2) { //执行毁约
        if (i2 == 0) {
            showDialog(this.menuLabels[6], formatMessage(this.diplomacyMessages[3], new String[]{factionNames[i]}), true, 0);
        }
        setDiplomacyValue(i, i2, 1, 0);
        setDiplomacyValue(i2, i, 1, 0);
        adjustRelation(i2, i, -20);
        spreadRelationEffect(i, i2, -10);
    }

    private void spreadRelationEffect(int i, int i2, int i3) { //传播关系影响
        for (int i4 = 0; i4 < this.diplomacyData.length; i4++) {
            if (i4 != i && i4 != i2) {
                int iM68else = getDiplomacyValue(i4, i2, 0);
                int iM68else2 = getDiplomacyValue(i4, i2, 1);
                if (iM68else2 == 1) {
                    adjustRelation(i, i4, -i3);
                } else if (iM68else2 == 2) {
                    adjustRelation(i, i4, i3);
                } else if (iM68else > 95) {
                    adjustRelation(i, i4, i3);
                } else if (iM68else < 15) {
                    adjustRelation(i, i4, -i3);
                }
            }
        }
    }

    private void doDeclareWar(int i, int i2) { //执行宣战
        declareWar(i, i2);
        setDiplomacyValue(i, i2, 1, 1);
        setDiplomacyValue(i2, i, 1, 1);
        adjustRelation(i2, i, -50);
        spreadRelationEffect(i, i2, -20);
        for (int i3 = 0; i3 < this.diplomacyData.length; i3++) {
            if (i3 != i && i3 != i2) {
                if (getDiplomacyValue(i3, i, 1) == 2) {
                    declareWar(i3, i2);
                    setDiplomacyValue(i2, i3, 1, 1);
                    setDiplomacyValue(i3, i2, 1, 1);
                }
                if (getDiplomacyValue(i3, i2, 1) == 2) {
                    declareWar(i3, i);
                    setDiplomacyValue(i2, i3, 1, 1);
                    setDiplomacyValue(i3, i2, 1, 1);
                }
            }
        }
        if (i == 0) {
            showDialog("战争", String.valueOf(String.valueOf(new StringBuffer("我们对").append(factionNames[i2]).append("宣战了！"))), true, 0);
        }
    }

    private void doMakePeace(int i, int i2) { //执行停战
        boolean zD = canMakePeace(i, i2);
        if (zD) {
            setDiplomacyValue(i, i2, 1, 0);
            setDiplomacyValue(i2, i, 1, 0);
            adjustRelation(i, i2, 10);
            adjustRelation(i2, i, 5);
            spreadRelationEffect(i, i2, 2);
        } else {
            adjustRelation(i, i2, -5);
        }
        if (i == 0) {
            String[] strArr = {factionNames[i2], this.diplomacyActions[5]};
            if (i == 0) {
                showDialog(this.menuLabels[6], formatMessage(this.envoyResults[zD ? (char) 0 : (char) 1], strArr), true, 0);
            }
        }
    }

    int findBestAlly(int i) { //寻找最佳盟友
        int i2 = 100;
        int i3 = 0;
        for (int i4 = 0; i4 < this.diplomacyData.length; i4++) {
            if (i4 != i) {
                if (getDiplomacyValue(i, i4, 1) == 1) {
                    return i4;
                }
                int iM68else = getDiplomacyValue(i, i4, 0);
                if (i3 == 0 || i2 > iM68else) {
                    i3 = i4;
                    i2 = iM68else;
                }
            }
        }
        return i3;
    }

    boolean findTarget(Unit aVar, int[] iArr) { //寻找AI目标
        iArr[0] = aVar.mapX;
        iArr[1] = aVar.mapY;
        int iM38case = findBestAlly(aVar.factionIndex);
        if (getDiplomacyValue(aVar.factionIndex, iM38case, 1) != 1 && randomInt(10) != 2 && getCityOwner(aVar.mapX, aVar.mapY) == 0) {
            return false;
        }
        int i = -1;
        for (int i2 = 0; i2 < this.cityInfoIndex; i2++) {
            if (this.units[i2].ownerId > 0 && this.units[i2].factionIndex == iM38case && this.units[i2] != aVar) {
                int iAbs = Math.abs(this.units[i2].mapX - aVar.mapX) + Math.abs(this.units[i2].mapY - aVar.mapY);
                if (i < 0 || i > iAbs) {
                    iArr[0] = this.units[i2].mapX;
                    iArr[1] = this.units[i2].mapY;
                    i = iAbs;
                }
            }
        }
        for (int i3 = 0; i3 < this.cityMenuIndex; i3++) {
            if (this.cities[i3].techLevel > 0 && this.cities[i3].ownerIndex == iM38case) {
                int iAbs2 = Math.abs(this.cities[i3].x - aVar.mapX) + Math.abs(this.cities[i3].y - aVar.mapY);
                if (i < 0 || i > iAbs2) {
                    iArr[0] = this.cities[i3].x;
                    iArr[1] = this.cities[i3].y;
                    i = iAbs2;
                }
            }
        }
        return true;
    }

    private void processMenuInput() {
        int iM80if = this.menu.processMenu();
        if (iM80if != 0) {
            this.needRepaint = true;
        }
        if (!this.menu.isVisible) {
            this.menuState = 1;
        }
        switch (iM80if) {
            case 1:
                this.menuState = 10;
                break;
            case 2:
                battleMode = !battleMode;
                break;
            case 3:
                GameScreen.soundEnabled = !GameScreen.soundEnabled;
                break;
            case 4:
                showDialog(this.missionNames[this.dialogResult], this.dialogLines[0], false, 0);
                addDialogLine(this.dialogLines[1], 11468800);
                break;
        }
    }

    private void processLevelSelect() { //处理关卡选择
        int iM80if = this.menu.processMenu();
        if (iM80if == 0) {
            return;
        }
        if (iM80if >= 0) {
            this.dialogResult = iM80if - 1;
            resetGameState();
        } else {
            this.needRepaint = true;
            if (this.menu.isVisible) {
                return;
            }
            this.isGameOver = true;
        }
    }

    boolean update(GameScreen eVar) {
        if (this.dialogLineCount > 0) {
            processDialogInput();
        } else {
            if (this.isGameOver) {
                this.objectImage = null;
                return true;
            }
            switch (this.menuState) {
                case 0:
                    for (int i = 0; i < this.cityMenuIndex; i++) {
                        this.cities[i].expandDomain();
                    }
                    this.menuState = 1;
                    break;
                case 1:
                    updateUnitAnimations();
                    processCursorInput();
                    break;
                case 3:
                    processUnitMovement();
                    break;
                case 5:
                case 6:
                    processCityMenu();
                    break;
                case 8:
                    executeDiplomacy(0, this.diplomatIndex + 1, this.diplomacyTarget);
                    this.menuState = 1;
                    this.gameState = selectTile(this.keyCode, this.keyState);
                    break;
                case 9:
                    updateUnitAnimations();
                    processMenuInput();
                    break;
                case 10:
                    eVar.saveGame();
                    endTurn();
                    break;
                case 11:
                    processLevelSelect();
                    break;
            }
        }
        if (this.needRepaint) {
            eVar.repaintScreen();
        }
        if (this.dialogHeight == 0 || this.turnCounter < 0) {
            return false;
        }
        this.dialogWidth = 0;
        this.dialogHeight = 0;
        return false;
    }

    private void drawDialog(Graphics graphics) {
        int height = this.font.getHeight() + 2;
        graphics.setColor(20223);
        int i = (GameScreen.screenHeight - this.screenHeight) >> 1;
        int i2 = height + 2;
        int i3 = (GameScreen.screenWidth - this.screenWidth) >> 1;
        graphics.setColor(4144959);
        GameScreen.drawRect(graphics, i3 + 2, i + 3, this.screenWidth, this.screenHeight, 2130706432, 0);
        int i4 = i;
        for (int i5 = 0; i5 < this.statusBarColors.length; i5++) {
            graphics.setColor(this.statusBarColors[i5]);
            graphics.fillRect(i3, i4, this.screenWidth, i2);
            i4++;
            i2 -= 2;
            if (i2 <= 0) {
                break;
            }
        }
        GameScreen.drawText(this.missionTitle, i3 + 4, i + 2, graphics, 0, 16777215);
        graphics.setColor(16644325);
        int i6 = i + height + 2;
        graphics.fillRect(i3, i6, this.screenWidth, (this.screenHeight - height) - 2);
        graphics.setColor(0);
        graphics.drawRect(i3, i, this.screenWidth - 1, this.screenHeight - 1);
        int i7 = i6 + 2;
        int i8 = this.diplomacyAction == 0 ? height : height * 2;
        int i9 = (this.screenHeight - (this.diplomacyAction == 0 ? height + 8 : height * 2)) / height;
        for (int i10 = this.diplomatIndex; i10 < this.dialogLineCount; i10++) {
            GameScreen.drawText(this.dialogTexts[i10], i3 + 4, i7, graphics, this.dialogColors[i10], 0);
            i7 += height;
            if (i7 + i8 > (this.screenHeight + i) - 6) {
                break;
            }
        }
        int i11 = (i + this.screenHeight) - 2;
        int i12 = i3 + (this.screenWidth >> 1);
        if (i9 < this.dialogLineCount) {
            drawSprite(graphics, i12, i11, this.diplomatIndex > 0 ? this.diplomatIndex + i9 < this.dialogLineCount ? 43 : 44 : 42, (int[]) null, false);
        }
        if (this.diplomacyAction == 0) {
            drawSprite(graphics, (i3 + this.screenWidth) - 2, i11, 41, (int[]) null, false);
            return;
        }
        int iStringWidth = this.font.stringWidth(this.confirmTexts[0]) + 6;
        int i13 = i3 + 6;
        int i14 = ((i + this.screenHeight) - height) - 6;
        graphics.setColor(0);
        graphics.drawRect(i13, i14, iStringWidth, height + 2);
        graphics.drawString(this.confirmTexts[this.diplomacyAction - 1], i13 + 3, i14 + 2, 16 | 4);
        int i15 = ((i3 + this.screenWidth) - 6) - iStringWidth;
        graphics.drawRect(i15, i14, iStringWidth, height + 2);
        graphics.drawString(this.cancelTexts[this.diplomacyAction - 1], i15 + 3, i14 + 2, 16 | 4);
    }

    String getTileDescription(int i, boolean z) {
        String strConcat = "";
        int iA = City.getResourceIndex(i);
        if (iA < 0) {
            int iM89do = City.getBuildingIndex(i);
            if (iM89do < 0) {
                switch (i) {
                    case 1:
                    case 2:
                        strConcat = "树木";
                        break;
                    case 3:
                        strConcat = "巨石";
                        break;
                    case 4:
                        strConcat = "山丘";
                        break;
                    case 5:
                        strConcat = "树桩";
                        break;
                    case 50:
                        strConcat = "工地";
                        break;
                }
            } else {
                strConcat = City.buildingNames[iM89do];
                if (z) {
                    String strConcat2 = String.valueOf(String.valueOf(strConcat)).concat("(");
                    int[] iArr = City.buildingEffects[iM89do];
                    for (int i2 = 0; i2 < iArr.length; i2 += 2) {
                        if (i2 != 0) {
                            strConcat2 = String.valueOf(String.valueOf(strConcat2)).concat(String.valueOf(String.valueOf(',')));
                        }
                        if (iArr[i2 + 1] > 0) {
                            strConcat2 = String.valueOf(String.valueOf(strConcat2)).concat("+");
                        }
                        strConcat2 = String.valueOf(String.valueOf(String.valueOf(String.valueOf(strConcat2)).concat(String.valueOf(String.valueOf(iArr[i2 + 1]))))).concat(String.valueOf(String.valueOf(City.cityAttrs[iArr[i2]])));
                    }
                    strConcat = String.valueOf(String.valueOf(strConcat2)).concat(")");
                }
            }
        } else {
            strConcat = City.resourceNames[iA];
            if (z) {
                String strConcat3 = String.valueOf(String.valueOf(String.valueOf(String.valueOf(strConcat)).concat("(+"))).concat(String.valueOf(String.valueOf(String.valueOf(String.valueOf(City.resourceConsumptions[iA])).concat("金"))));
                if (iA == 3) {
                    strConcat = String.valueOf(String.valueOf(strConcat3)).concat(",医疗)");
                } else {
                    for (int i3 = 0; i3 < Unit.resourceRequirements.length; i3++) {
                        if (((Unit.resourceRequirements[i3] >> iA) & 1) != 0) {
                            strConcat3 = String.valueOf(String.valueOf(strConcat3)).concat(String.valueOf(String.valueOf(",".concat(String.valueOf(String.valueOf(Unit.unitNames[i3]))))));
                        }
                    }
                    strConcat = String.valueOf(String.valueOf(strConcat3)).concat(")");
                }
            }
        }
        return strConcat;
    }

    private void drawStatusBar(Graphics graphics) {
        String strM41do;
        int height = this.font.getHeight() + 6;
        int i = GameScreen.screenHeight - height;
        drawPanel(graphics, 0, i, GameScreen.screenWidth, height, true);
        City dVarM59if = getCity(getCityOwner(this.keyCode, this.keyState), false);
        if (this.menuState == 3 && this.selectedUnit != null) {
            int i2 = this.selectedUnit.attackCooldown;
            if (i2 < 0) {
                i2 = 0;
            }
            strM41do = "机动:".concat(String.valueOf(String.valueOf(i2)));
        } else if (dVarM59if != null) {
            strM41do = String.valueOf(String.valueOf(new StringBuffer(String.valueOf(String.valueOf(dVarM59if.cityName))).append("(").append(factionNames[dVarM59if.ownerIndex]).append(")").append(dVarM59if.attributes[2] / 10000).append("万人")));
        } else {
            Unit aVarA = getUnit(getUnitId(this.keyCode, this.keyState), false);
            if (aVarA != null) {
                String strConcat = String.valueOf(String.valueOf(String.valueOf(String.valueOf(String.valueOf(String.valueOf(Unit.unitNames[aVarA.unitType])).concat("("))).concat(String.valueOf(String.valueOf(getCity(aVarA.ownerId, false).cityName))))).concat(")");
                strM41do = aVarA.isDead ? String.valueOf(String.valueOf(strConcat)).concat(String.valueOf(String.valueOf(String.valueOf(String.valueOf(new StringBuffer(String.valueOf(String.valueOf(City.buildingNames[aVarA.moveX]))).append("(").append(aVarA.getAttackTurns(aVarA.buildProgress)).append(")")))))) : aVarA.maxHp >= 10000 ? String.valueOf(String.valueOf(strConcat)).concat(String.valueOf(String.valueOf(String.valueOf(String.valueOf(aVarA.maxHp / 10000)).concat("万人")))) : aVarA.maxHp >= 1000 ? String.valueOf(String.valueOf(strConcat)).concat(String.valueOf(String.valueOf(String.valueOf(String.valueOf(aVarA.maxHp / 1000)).concat("千人")))) : String.valueOf(String.valueOf(strConcat)).concat(String.valueOf(String.valueOf(String.valueOf(String.valueOf(aVarA.maxHp)).concat("人"))));
            } else {
                int iE = getObjectType(this.keyCode, this.keyState);
                if (iE != 0) {
                    strM41do = getTileDescription(iE, true);
                } else {
                    strM41do = getTerrain(this.keyCode, this.keyState) == 0 ? "平地" : "湖泊";
                }
            }
        }
        GameScreen.drawText(strM41do, 6, i + 3, graphics, 16777135, 2039583);
    }

    private void drawBuildMenu(Graphics graphics) {
        int height = this.font.getHeight() + 2;
        int i = GameScreen.screenWidth - 8;
        if (i > 160) {
            i = 160;
        }
        int i2 = (height * 7) + 6;
        int i3 = (GameScreen.screenWidth - i) >> 1;
        int i4 = ((this.infoBarHeight - i2) + this.screenOffsetY) / 2;
        int iStringWidth = this.font.stringWidth(City.buildingNames[0]) + 4;
        GameScreen.drawRect(graphics, i3 + 1, i4 + 2, i, i2, 2130706432, 0);
        drawPanel(graphics, i3, i4, i, i2, true);
        drawPanel(graphics, i3 + 3, i4 + 3, iStringWidth + 2, (i2 - 4) - (height * 2), false);
        int i5 = this.unitCount - 4;
        if (i5 < 0) {
            i5 = 0;
        }
        int i6 = i4 + 4;
        for (int i7 = i5; i7 < i5 + 5 && i7 < this.menuOptionCount; i7++) {
            if (i7 == this.unitCount) {
                graphics.setColor(16766976);
                graphics.fillRect(i3 + 4, i6, iStringWidth, height);
            }
            GameScreen.drawText(City.buildingNames[this.menuOptionIndex[i7]], i3 + 5, i6, graphics, 16777215, 0);
            i6 += height;
        }
        int i8 = i3 + iStringWidth + 8;
        int i9 = this.menuOptionIndex[this.unitCount];
        int iM87new = this.selectedCity.getBuildingTileId(this.menuOptionIndex[this.unitCount]);
        if (iM87new > 0) {
            drawSprite(graphics, i8 + 36, i4 + 16, iM87new - 1, (int[]) null, false);
        } else {
            drawSprite(graphics, i8 + 36, i4 + 16, 49, (int[]) null, false);
        }
        int i10 = i4 + 26;
        int i11 = City.buildingCosts[i9];
        GameScreen.drawText(String.valueOf(String.valueOf(new StringBuffer("造价:").append(i11 / 5).append("金"))), i8, i10, graphics, 15133695, 1052688);
        int i12 = i10 + height;
        GameScreen.drawText(String.valueOf(String.valueOf(new StringBuffer("时间:").append(this.selectedUnit.getAttackTurns(i11)).append("回合"))), i8, i12, graphics, 15133695, 1052688);
        int i13 = i12 + height;
        if (iM87new > 0) {
            for (int i14 = 0; i14 < City.buildingEffects[i9].length; i14 += 2) {
                int i15 = City.buildingEffects[i9][i14 + 1];
                int i16 = City.buildingEffects[i9][i14];
                if (i15 > 0) {
                    GameScreen.drawText(String.valueOf(String.valueOf(new StringBuffer("效果:+").append(i15).append(City.cityAttrs[i16]))), i8, i13, graphics, 15133695, 1052688);
                } else {
                    GameScreen.drawText(String.valueOf(String.valueOf(new StringBuffer("维护:").append(Math.abs(i15)).append(City.cityAttrs[i16]))), i8, i13, graphics, 15133695, 1052688);
                }
                i13 += height;
            }
        } else {
            GameScreen.drawText("搜索新特产", i8, i13, graphics, 15133695, 1052688);
        }
        int iStringWidth2 = this.font.stringWidth(this.confirmTexts[2]) + 10;
        int i17 = height + 3;
        int i18 = ((i4 + i2) - i17) - 3;
        int i19 = i3 + 12;
        drawPanel(graphics, i19, i18, iStringWidth2, i17, true);
        GameScreen.drawText(this.confirmTexts[2], i19 + 6, i18 + 2, graphics, 16777200, 1052688);
        int i20 = ((i3 + i) - 12) - iStringWidth2;
        drawPanel(graphics, i20, i18, iStringWidth2, i17, true);
        GameScreen.drawText(this.cancelTexts[2], i20 + 6, i18 + 2, graphics, 16777200, 1052688);
    }

    private int drawButton(Graphics graphics, int i, int i2, String str, Font font, boolean z) {
        int iStringWidth = font.stringWidth(str) + 4;
        int height = font.getHeight() + 2;
        if (!z) {
            i2 += 2;
            height -= 2;
        }
        int[] iArr = {i, i, i + 2, (i + iStringWidth) - 2, i + iStringWidth, i + iStringWidth};
        int[] iArr2 = {i2 + height, i2 + 2, i2, i2, i2 + 2, i2 + height};
        graphics.setColor(z ? 16711680 : 8323072);
        for (int i3 = 0; i3 < iArr.length - 1; i3++) {
            graphics.drawLine(iArr[i3], iArr2[i3], iArr[i3 + 1], iArr2[i3 + 1]);
        }
        graphics.setColor(z ? 16777215 : 8355711);
        graphics.drawString(str, i + 2, i2 + 1, 16 | 4);
        return iStringWidth;
    }

    private void drawCityMenuTabs(Graphics graphics) {
        int i;
        int iA;
        Font font = this.font;
        int height = font.getHeight() + 2;
        graphics.setColor(16711680);
        graphics.drawLine(0, height, 0, GameScreen.screenHeight - 1);
        graphics.drawLine(0, GameScreen.screenHeight - 1, GameScreen.screenWidth - 1, GameScreen.screenHeight - 1);
        graphics.drawLine(GameScreen.screenWidth - 1, height, GameScreen.screenWidth - 1, GameScreen.screenHeight - 1);
        int i2 = 0;
        int i3 = 0;
        int length = this.actionNames.length;
        for (int i4 = 0; i4 < length; i4++) {
            if (i4 == this.cityCount) {
                i3 = i2;
                i = i2;
                iA = font.stringWidth(this.actionNames[i4]) + 4;
            } else {
                i = i2;
                iA = drawButton(graphics, i2, 0, this.actionNames[i4], font, false);
            }
            i2 = i + iA;
        }
        graphics.setColor(16711680);
        if (i3 > 0) {
            graphics.drawLine(0, height, i3, height);
        }
        int iA2 = i3 + drawButton(graphics, i3, 0, this.actionNames[this.cityCount], font, true);
        graphics.setColor(16711680);
        graphics.drawLine(iA2, height, GameScreen.screenWidth, height);
    }

    private int scrollDown(int i, int i2) {
        switch (this.dialogWidth) {
            case -2:
                i = i + 1 < i2 ? i + 1 : 0;
                this.needRepaint = true;
                break;
            case -1:
                i = i > 0 ? i - 1 : i2 - 1;
                this.needRepaint = true;
                break;
        }
        return i;
    }

    private int scrollUp(int i, int i2) {
        switch (this.dialogWidth) {
            case -4:
                i = i + 1 < i2 ? i + 1 : 0;
                this.needRepaint = true;
                break;
            case -3:
                i = i > 0 ? i - 1 : i2 - 1;
                this.needRepaint = true;
                break;
        }
        return i;
    }

    private boolean isConfirmKey() {
        switch (this.dialogWidth) {
            case -10:
            case -6:
            case -5:
                this.dialogWidth = 0;
                return true;
            default:
                return false;
        }
    }

    private void processProduction() {
        int i;
        if (this.menuState == 5) {
            this.unitCount = scrollDown(this.unitCount, this.menuOptionCount);
            this.selectedCity.buildTarget = this.menuOptionIndex[this.unitCount];
        }
        if (isConfirmKey()) {
            int i2 = this.selectedCity.buildTarget;
            if (i2 == Unit.unitNames.length || this.selectedCity.ownerIndex != 0 || (i = (Unit.unitStats[i2][4] - this.selectedCity.productionProgress) * 5) > this.selectedCity.attributes[6] || i <= 0) {
                this.menuState = 1;
                this.gameState = selectTile(this.keyCode, this.keyState);
            } else {
                this.selectedCity.productionProgress = Unit.unitStats[i2][4];
                int[] iArr = this.selectedCity.attributes;
                iArr[6] = iArr[6] - i;
                this.needRepaint = true;
            }
        }
    }

    private void adjustPrice(int i) {
        this.viewScale += i;
        if (this.viewScale < 1) {
            this.viewScale = 1;
        }
        if (this.viewScale > 50) {
            this.viewScale = 50;
        }
    }

    private void processTrade() {
        this.diplomatIndex = scrollDown(this.diplomatIndex, this.selectedCity.attributes.length + this.selectedCity.buildings.length);
        if (isConfirmKey()) {
            if (this.selectedCity.ownerIndex != 0) {
                this.menuState = 1;
                this.gameState = selectTile(this.keyCode, this.keyState);
                return;
            } else {
                if (this.selectedCity.attributes[3] < 100) {
                    return;
                }
                int[] iArr = this.selectedCity.attributes;
                iArr[3] = iArr[3] - 100;
                int[] iArr2 = this.selectedCity.attributes;
                iArr2[6] = iArr2[6] + ((this.viewScale * 95) / 100);
                adjustPrice(-1);
                this.needRepaint = true;
                return;
            }
        }
        if (this.dialogWidth == -7) {
            if (this.selectedCity.ownerIndex != 0) {
                this.menuState = 1;
                this.gameState = selectTile(this.keyCode, this.keyState);
            } else {
                if (this.selectedCity.attributes[6] < this.viewScale) {
                    return;
                }
                int[] iArr3 = this.selectedCity.attributes;
                iArr3[3] = iArr3[3] + 95;
                int[] iArr4 = this.selectedCity.attributes;
                iArr4[6] = iArr4[6] - this.viewScale;
                adjustPrice(1);
                this.needRepaint = true;
            }
        }
    }

    private void processCityList() {
        this.diplomatIndex = scrollDown(this.diplomatIndex, this.cityMenuIndex);
        if (isConfirmKey()) {
            this.selectedCity = this.cities[this.diplomatIndex];
            this.keyCode = this.selectedCity.x;
            this.keyState = this.selectedCity.y;
            this.menuState = 1;
            this.gameState = selectTile(this.keyCode, this.keyState);
            this.needRepaint = true;
        }
    }

    private void processDiplomacyMenu() {
        this.diplomatIndex = scrollDown(this.diplomatIndex, this.diplomacyData[0].length - 1);
        if (isConfirmKey()) {
            if (this.subMenuIndex > 0) {
                this.menuState = 1;
                return;
            } else {
                if (canExecuteDiplomacy(0, this.diplomatIndex + 1, this.diplomacyTarget, this.selectedCity)) {
                    this.subMenuIndex++;
                    this.menuState = 8;
                    return;
                }
                return;
            }
        }
        if (this.dialogWidth == -7) {
            if (this.subMenuIndex > 0) {
                this.menuState = 1;
                return;
            }
            if (this.diplomacyTarget + 1 < this.diplomacyActions.length) {
                this.diplomacyTarget++;
            } else {
                this.diplomacyTarget = 0;
            }
            this.needRepaint = true;
            this.dialogWidth = 0;
        }
    }

    private void drawCityList(Graphics graphics, int i, int i2) {
        int i4 = i2 + 2;
        int iStringWidth = 4 + this.font.stringWidth(Unit.unitNames[5]) + 4;
        int i5 = iStringWidth * 2;
        int i6 = (this.diplomatIndex - i) + 3;
        if (i6 < 0) {
            i6 = 0;
        }
        graphics.setColor(6250335);
        graphics.fillRect(1, i4, GameScreen.screenWidth - 2, i2);
        graphics.setColor(16777215);
        graphics.drawString(this.cityInfoLabels[0], 4, i4 + 1, 16 | 4);
        graphics.drawString(this.cityInfoLabels[1], iStringWidth, i4 + 1, 16 | 4);
        graphics.drawString(this.cityInfoLabels[2], i5, i4 + 1, 16 | 4);
        int i7 = i4 + i2 + 1;
        for (int i8 = i6; i8 < (i6 + i) - 2 && i8 < this.cityMenuIndex; i8++) {
            if (i8 == this.diplomatIndex) {
                graphics.setColor(127);
                graphics.fillRect(1, i7, GameScreen.screenWidth - 2, i2);
            }
            graphics.setColor(this.cities[i8].ownerIndex == 0 ? 16777184 : 8355711);
            graphics.drawString(this.cities[i8].cityName, 4, i7, 16 | 4);
            graphics.drawString(factionNames[this.cities[i8].ownerIndex], iStringWidth, i7, 16 | 4);
            graphics.drawString(String.valueOf(String.valueOf(this.cities[i8].attributes[2] / 10000)).concat("万"), i5, i7, 16 | 4);
            i7 += i2;
        }
        int i9 = (GameScreen.screenHeight - i2) - 8;
        drawPanel(graphics, 8, i9, this.font.stringWidth(this.confirmTexts[0]) + 16, i2 + 3, true);
        GameScreen.drawText(this.menuLabels[0], 16, i9 + 2, graphics, 16777215, 1052688);
    }

    private void drawDiplomacyList(Graphics graphics, int i, int i2) { // 绘制外交列表
        int i4 = i2 + 2;
        int iStringWidth = 4 + this.font.stringWidth(Unit.unitNames[5]) + 4;
        int i5 = iStringWidth * 2;
        graphics.setColor(6250335);
        graphics.fillRect(1, i4, GameScreen.screenWidth - 2, i2);
        graphics.setColor(16777215);
        graphics.drawString(this.nationInfoLabels[0], 4, i4 + 1, 16 | 4);
        graphics.drawString(this.nationInfoLabels[1], iStringWidth, i4 + 1, 16 | 4);
        graphics.drawString(this.nationInfoLabels[2], i5, i4 + 1, 16 | 4);
        int i6 = i4 + i2 + 1;
        int[] iArr = this.diplomacyData[0];
        for (int i7 = 1; i7 < iArr.length; i7++) {
            if (i7 == this.diplomatIndex + 1) {
                graphics.setColor(127);
                graphics.fillRect(1, i6, GameScreen.screenWidth - 2, i2);
            }
            graphics.setColor(16777184);
            graphics.drawString(factionNames[i7], 4, i6, 16 | 4);
            graphics.drawString(this.relationNames[getDiplomacyValue(i7, 0, 1)], iStringWidth, i6, 16 | 4);
            graphics.drawString("".concat(String.valueOf(String.valueOf(getDiplomacyValue(i7, 0, 0)))), i5, i6, 16 | 4);
            i6 += i2;
        }
        if (this.subMenuIndex > 0) {
            return;
        }
        int i8 = (GameScreen.screenHeight - i2) - 8;
        int iStringWidth2 = this.font.stringWidth(this.confirmTexts[0]) + 8;
        int i9 = (GameScreen.screenWidth - iStringWidth2) - 6;
        drawPanel(graphics, i9, i8, iStringWidth2, i2 + 3, true);
        GameScreen.drawText(this.menuLabels[2], i9 + 4, i8 + 2, graphics, 16777215, 1052688);
        int i10 = canExecuteDiplomacy(0, this.diplomatIndex + 1, this.diplomacyTarget, this.selectedCity) ? 16777215 : 8355711;
        drawPanel(graphics, 6, i8, iStringWidth2, i2 + 3, true);
        GameScreen.drawText(this.menuLabels[1], 10, i8 + 2, graphics, i10, 1052688);
        int i11 = (GameScreen.screenWidth - iStringWidth2) / 2;
        graphics.setColor(11468800);
        graphics.drawRect(i11, i8, iStringWidth2, i2 + 3);
        graphics.setColor(i10);
        graphics.drawString(this.diplomacyActions[this.diplomacyTarget], i11 + 4, i8 + 3, 16 | 4);
    }

    private boolean canExecuteDiplomacy(int i, int i2, int i3, City dVar) { // 能否执行外交
        getDiplomacyValue(i, i2, 1);
        int iM68else = getDiplomacyValue(i2, i, 1);
        switch (i3) {
            case 0:
                if (dVar.attributes[6] >= 100) {
                }
                break;
            case 1:
                if (iM68else == 1 || iM68else == 2) {
                }
                break;
            case 2:
                if (iM68else == 1 || iM68else == 2) {
                }
                break;
            case 3:
                if (iM68else == 2) {
                }
                break;
            case 4:
                if (iM68else != 1) {
                }
                break;
            case 5:
                if (iM68else == 1) {
                }
                break;
        }
        return false;
    }

    private int countCities(int i) {
        int i2 = 0;
        for (int i3 = 0; i3 < this.cityMenuIndex; i3++) {
            if (this.cities[i3].techLevel > 0 && this.cities[i3].ownerIndex == i) {
                i2++;
            }
        }
        return i2;
    }

    private void drawCityInfo(Graphics graphics, int i, int i2) {
        int length = this.selectedCity.attributes.length + this.selectedCity.buildings.length + 4;
        int i3 = this.diplomatIndex;
        if (i3 < 0) {
            i3 = 0;
        }

        int i5 = i2 + 2;
        int iStringWidth = this.font.stringWidth(Unit.unitNames[5]) + 8;
        this.selectedCity.calculateDelta(City.deltaCache);
        graphics.setColor(127);
        graphics.fillRect(1, i5, GameScreen.screenWidth - 2, i2);
        for (int i6 = i3; i6 < (i3 + i) - 1 && i6 < length; i6++) {
            graphics.setColor(i6 % 2 == 0 ? 16777184 : 15133695);
            if (i6 == 0) {
                graphics.drawString("城市", 4, i5, 16 | 4);
                graphics.drawString(this.selectedCity.cityName, 4 + iStringWidth, i5, 16 | 4);
            } else if (i6 == 1) {
                graphics.drawString("物价", 4, i5, 16 | 4);
                graphics.drawString("".concat(String.valueOf(String.valueOf(this.viewScale))), 4 + iStringWidth, i5, 16 | 4);
            } else if (i6 == 2) {
                graphics.drawString("生产力", 4, i5, 16 | 4);
                graphics.drawString("".concat(String.valueOf(String.valueOf(this.selectedCity.getProductionRate()))), 4 + iStringWidth, i5, 16 | 4);
            } else if (i6 == 3) {
                graphics.drawString("特产", 4, i5, 16 | 4);
                String strConcat = "";
                for (int i7 = 0; i7 < this.selectedCity.resources.length; i7++) {
                    if (this.selectedCity.resources[i7] > 0) {
                        if (strConcat.length() > 0) {
                            strConcat = String.valueOf(String.valueOf(strConcat)).concat(String.valueOf(String.valueOf(',')));
                        }
                        strConcat = String.valueOf(String.valueOf(strConcat)).concat(String.valueOf(String.valueOf(City.resourceNames[i7])));
                    }
                }
                if (strConcat.length() == 0) {
                    strConcat = "无";
                }
                graphics.drawString(strConcat, 4 + iStringWidth, i5, 16 | 4);
            } else if (i6 < this.selectedCity.attributes.length + 4) {
                int length2 = (City.cityAttrs.length - i6) + 3;
                graphics.drawString(City.cityAttrs[length2], 4, i5, 16 | 4);
                int i8 = this.selectedCity.attributes[length2];
                String strConcat2 = i8 >= 10000 ? String.valueOf(String.valueOf(i8 / 10000)).concat("万(") : String.valueOf(String.valueOf(i8)).concat("(");
                if (City.deltaCache[length2] >= 0) {
                    strConcat2 = String.valueOf(String.valueOf(strConcat2)).concat(String.valueOf(String.valueOf('+')));
                }
                graphics.drawString(String.valueOf(String.valueOf(String.valueOf(String.valueOf(strConcat2)).concat(String.valueOf(String.valueOf(City.deltaCache[length2]))))).concat(")"), 4 + iStringWidth, i5, 16 | 4);
            } else {
                graphics.drawString(City.buildingNames[(i6 - 4) - City.cityAttrs.length], 4, i5, 16 | 4);
                int length3 = (i6 - 4) - City.cityAttrs.length;
                graphics.drawString(String.valueOf(String.valueOf(String.valueOf(String.valueOf(String.valueOf(String.valueOf("".concat(String.valueOf(String.valueOf(this.selectedCity.buildings[length3]))))).concat("(+"))).concat(String.valueOf(String.valueOf(City.cityAttrs[City.buildingEffects[length3][0]]))))).concat(")"), 4 + iStringWidth, i5, 16 | 4);
            }
            i5 += i2;
        }
        if (this.selectedCity.ownerIndex != 0) {
            return;
        }
        int i9 = (GameScreen.screenHeight - i2) - 8;
        int iStringWidth2 = this.font.stringWidth(this.confirmTexts[0]) + 8;
        int i10 = (GameScreen.screenWidth - iStringWidth2) - 6;
        drawPanel(graphics, i10, i9, iStringWidth2, i2 + 3, true);
        GameScreen.drawText(this.menuLabels[3], i10 + 4, i9 + 2, graphics, this.selectedCity.attributes[6] >= this.viewScale ? 16777215 : 8355711, 1052688);
        drawPanel(graphics, 6, i9, iStringWidth2, i2 + 3, true);
        GameScreen.drawText(this.menuLabels[4], 10, i9 + 2, graphics, this.selectedCity.attributes[3] >= 100 ? 16777215 : 8355711, 1052688);
    }

    private void drawProductionMenu(Graphics graphics, int rows, int rowHeight) { // 绘制生产/训练菜单（城市制造面板）
        int iStringWidth = this.font.stringWidth(Unit.unitNames[5]) + 10;
        int i3 = rowHeight + 2;
        graphics.setColor(8614);
        graphics.drawRect(4, i3, iStringWidth, ((rows - 1) * rowHeight) + 2);
        int i4 = (this.unitCount - rows) + 3;
        if (i4 < 0) {
            i4 = 0;
        }
        int i5 = i3 + 1;
        int i6 = this.menuState == 6 ? 11513775 : 16777184;
        for (int i7 = i4; i7 < (i4 + rows) - 1 && i7 < this.menuOptionCount; i7++) {
            if (i7 == this.unitCount) {
                graphics.setColor(1865065);
                graphics.fillRect(4 + 1, i5, iStringWidth, rowHeight);
            }
            if (this.menuOptionIndex[i7] < Unit.unitNames.length) {
                GameScreen.drawText(Unit.unitNames[this.menuOptionIndex[i7]], 4 + 5, i5, graphics, i6, 0);
            } else {
                GameScreen.drawText("扩建", 4 + 5, i5, graphics, i6, 0);
            }
            i5 += rowHeight;
        }
        int i8 = 4 + iStringWidth + 4;
        int i9 = this.menuOptionIndex[this.unitCount];
        int i10 = i9 == Unit.unitNames.length ? 0 : (Unit.unitStats[i9][4] - this.selectedCity.productionProgress) * 5;
        if (i10 < 0) {
            i10 = 0;
        }
        if (i9 < Unit.unitNames.length) {
            drawSprite(graphics, i8 + 30, i3 + 25, Unit.getTileId(i9), (int[]) null, false);
            int i11 = i3 + 30;
            GameScreen.drawText("已完成:".concat(String.valueOf(String.valueOf(this.selectedCity.productionProgress))), i8, i11, graphics, i6, 0);
            int i12 = i11 + rowHeight;
            GameScreen.drawText("生产力:".concat(String.valueOf(String.valueOf(this.selectedCity.getProductionRate()))), i8, i12, graphics, i6, 0);
            int i13 = i12 + rowHeight;
            GameScreen.drawText(String.valueOf(String.valueOf(new StringBuffer("完成:").append(this.selectedCity.getProductionTurns(i9)).append("回合"))), i8, i13, graphics, i6, 0);
            GameScreen.drawText("购买价:".concat(String.valueOf(String.valueOf(i10))), i8, i13 + rowHeight, graphics, i6, 0);
        } else {
            drawSprite(graphics, i8 + 30, i3 + 30, this.selectedCity.techLevel, (int[]) null, false);
            int i14 = i3 + 40;
            GameScreen.drawText("增加城市防御", i8, i14, graphics, i6, 0);
            GameScreen.drawText("效果:".concat(String.valueOf(String.valueOf(this.selectedCity.getProductionRate() / 10))), i8, i14 + rowHeight, graphics, i6, 0);
        }
        if (i9 == Unit.unitNames.length || this.selectedCity.ownerIndex != 0 || i10 > this.selectedCity.attributes[6] || i10 <= 0) {
            return;
        }
        int i15 = (GameScreen.screenHeight - rowHeight) - 8;
        drawPanel(graphics, 6, i15, this.font.stringWidth(this.menuLabels[5]) + 8, rowHeight + 3, true);
        GameScreen.drawText(this.menuLabels[5], 6 + 4, i15 + 2, graphics, 16777215, 1052688);
    }

    private void drawCityPanel(Graphics graphics) {
        drawCityMenuTabs(graphics);
        int height = this.font.getHeight() + 2;
        int i = ((GameScreen.screenHeight - 6) - height) / height;
        switch (this.cityCount) {
            case 0:
                drawProductionMenu(graphics, i, height);
                break;
            case 1:
                drawCityInfo(graphics, i, height);
                break;
            case 2:
                drawCityList(graphics, i, height);
                break;
            case 3:
                drawDiplomacyList(graphics, i, height);
                break;
        }
    }

    void draw(Graphics graphics) {
        graphics.setColor(0);
        graphics.fillRect(0, 0, GameScreen.screenWidth, GameScreen.screenHeight);
        if (this.menuState == 11) {
            graphics.setColor(16777184);
            graphics.drawString("选择起始关:", 2, 2, 16 | 4);
            this.menu.draw(graphics);
            this.gameScreen.drawTile(graphics, 1, GameScreen.screenHeight - 10, 0);
            this.gameScreen.drawTile(graphics, GameScreen.screenWidth - 10, GameScreen.screenHeight - 10, 1);
            return;
        }
        if (this.menuState != 5 && this.menuState != 6) {
            drawMap(graphics, this.cursorX, this.cursorY, this.mapData);
            drawMapOverlay(graphics, this.cursorX, this.cursorY, this.mapData);
            drawStatusBar(graphics);
        }
        if (this.dialogLineCount > 0) {
            drawDialog(graphics);
            this.needRepaint = false;
            return;
        }
        switch (this.menuState) {
            case 1:
                getCursorScreenPos(this.keyCode, this.keyState, this.cursorPos, true);
                for (int i = 0; i < Unit.arrowOffsets.length; i++) {
                    int i2 = 5 + (this.animCounter % 2);
                    drawSprite(graphics, this.cursorPos[0] + ((Unit.arrowOffsets[i][0] * i2) / 6), this.cursorPos[1] + ((Unit.arrowOffsets[i][1] * i2) / 6), 61 + i, (int[]) null, true);
                }
                break;
            case 3:
                this.gameScreen.drawTile(graphics, GameScreen.screenWidth - 10, GameScreen.screenHeight - 10, 3);
                this.selectedUnit.drawArrows(graphics);
                break;
            case 5:
            case 6:
                drawCityPanel(graphics);
                break;
            case 7:
                drawBuildMenu(graphics);
                break;
            case 9:
                this.menu.draw(graphics);
                this.gameScreen.drawTile(graphics, 1, GameScreen.screenHeight - 10, 0);
                this.gameScreen.drawTile(graphics, GameScreen.screenWidth - 10, GameScreen.screenHeight - 10, 1);
                break;
        }
        this.needRepaint = false;
    }

    void keyPressed(int i) {
        if (this.menuState == 9 || this.menuState == 11) {
            this.menu.handleKeyPress(i);
        } else if (this.dialogWidth != i) {
            if (this.dialogWidth == 0) {
                this.turnCounter = -1;
            }
            this.dialogWidth = i;
            this.dialogHeight = 0;
        }
    }

    void keyReleased() {
        if (this.menuState == 9 || this.menuState == 11 || this.dialogWidth == 0) {
            return;
        }
        this.dialogHeight = 1;
    }

    void getCursorScreenPos(int i, int i2, int[] iArr, boolean z) {
        iArr[0] = 22 * (i + i2 + 1);
        iArr[1] = (11 * ((this.mapData[0].length - i) + i2)) - 1;
        if (z) {
            iArr[0] = iArr[0] + this.cursorX;
            iArr[1] = iArr[1] + this.cursorY;
        }
    }

    private void drawMapOverlay(Graphics graphics, int i, int i2, int[][] iArr) {
        Unit aVarA;
        int i3 = i2 + 10;
        int length = (22 * iArr[0].length) + i;
        for (int i4 = 0; i4 < iArr.length; i4++) {
            int i5 = i3;
            int i6 = length;
            for (int length2 = iArr[i4].length - 1; length2 >= 0; length2--) {
                int iE = getObjectType(length2, i4);
                if (iE > 0) {
                    switch (iE) {
                        case 12:
                            City dVarM59if = getCity(getCityOwner(length2, i4), false);
                            if (dVarM59if != null) {
                                drawSprite(graphics, i6, i5, dVarM59if.techLevel, (int[]) null, true);
                                if (dVarM59if.animationFrame >= 0) {
                                    drawSprite(graphics, i6, i5 - 11, dVarM59if.animationFrame, (int[]) null, true);
                                }
                            }
                            break;
                        default:
                            drawSprite(graphics, i6, i5, iE - 1, (int[]) null, true);
                            break;
                    }
                }
                int iF = getUnitId(length2, i4);
                if (iF > 0 && (aVarA = getUnit(iF, false)) != null) {
                    aVarA.drawUnit(graphics, i6, i5);
                }
                i6 -= 22;
                i5 += 11;
            }
            length += 22;
            i3 += 11;
        }
        graphics.setClip(0, 0, GameScreen.screenWidth, GameScreen.screenHeight);
    }

    private void drawMap(Graphics graphics, int i, int i2, int[][] iArr) {
        int i3 = i2;
        int length = (22 * (iArr[0].length - 1)) + i;
        DirectGraphics directGraphics = DirectUtils.getDirectGraphics(graphics);
        int width = this.mapImage.getWidth();
        for (int i4 = 0; i4 < iArr.length; i4++) {
            int i5 = i3;
            int i6 = length;
            for (int length2 = iArr[i4].length - 1; length2 >= 0; length2--) {
                if (i6 < GameScreen.screenWidth && i5 < this.infoBarHeight && i6 + 44 > 0 && i5 + 21 > this.screenOffsetY) {
                    int i7 = iArr[i4][length2] & 255;
                    int i8 = i6 + 44 < GameScreen.screenWidth ? 44 : GameScreen.screenWidth - i6;
                    int i9 = i5 + 21 < this.infoBarHeight ? 21 : this.infoBarHeight - i5;
                    if (i5 > this.screenOffsetY) {
                        graphics.setClip(i6, i5, i8, i9);
                    } else {
                        graphics.setClip(i6, this.screenOffsetY, i8, i9);
                    }
                    int i10 = this.tileOffsets[i7][0];
                    int i11 = this.tileOffsets[i7][1];
                    if (((iArr[i4][length2] >> 16) & 255) == this.gameState && this.gameState != 0) {
                        graphics.drawImage(this.mapImage, i6 - 220, i5, 16 | 4);
                    } else if (i10 == 0) {
                        graphics.drawImage(this.mapImage, i6 - (i10 * 44), i5, 16 | 4);
                    } else {
                        int i12 = i10 * 44;
                        if ((i11 & 8192) != 0) {
                            i12 = width - ((i10 + 1) * 44);
                        }
                        directGraphics.drawImage(this.mapImage, i6 - i12, i5, 16 | 4, i11);
                    }
                }
                i6 -= 22;
                i5 += 11;
            }
            length += 22;
            i3 += 11;
        }
        graphics.setClip(0, 0, GameScreen.screenWidth, GameScreen.screenHeight);
    }

    boolean moveCursor(int i, int i2) {
        int i3 = this.cursorX;
        int i4 = this.cursorY;
        getCursorScreenPos(i, i2, this.cursorPos, false);
        int i5 = this.cursorPos[0];
        int i6 = this.cursorPos[1];
        this.cursorX = (GameScreen.screenWidth >> 1) - i5;
        this.cursorY = ((this.infoBarHeight + this.screenOffsetY) >> 1) - i6;
        if (this.cursorX > 0) {
            this.cursorX = 0;
        }
        if (this.cursorY > this.screenOffsetY) {
            this.cursorY = this.screenOffsetY;
        }
        if (i3 == this.cursorX && i4 == this.cursorY) {
            return false;
        }
        this.needRepaint = true;
        return true;
    }

    int drawSprite(Graphics graphics, int i, int i2, int i3, int[] iArr, boolean z) {
        if (i3 < 0 || i3 >= this.spriteFrames.length) {
            return 0;
        }
        int i4 = z ? this.screenOffsetY : 0;
        int i5 = z ? this.infoBarHeight : GameScreen.screenHeight;
        int[] iArr2 = this.spriteFrames[i3];
        int i6 = iArr2.length > 4 ? iArr2[4] : iArr2[2] >> 1;
        int i7 = i2 - (iArr2.length > 5 ? iArr2[5] : iArr2[3] >> 1);
        int i8 = i - i6;
        int i9 = i8 + iArr2[2] > GameScreen.screenWidth ? GameScreen.screenWidth - i8 : iArr2[2];
        int i10 = i7 + iArr2[3] > i5 ? i5 - i7 : iArr2[3];
        if (i8 >= GameScreen.screenWidth || i7 >= i5 || i8 + i9 <= 0 || i7 + i10 <= i4) {
            return 0;
        }
        if (i7 >= i4) {
            graphics.setClip(i8, i7, i9, i10);
        } else {
            graphics.setClip(i8, i4, i9, i10 + (i7 - i4));
        }
        graphics.drawImage(this.objectImage, i8 - iArr2[0], i7 - iArr2[1], 16 | 4);
        graphics.setClip(0, 0, GameScreen.screenWidth, GameScreen.screenHeight);
        if (iArr != null) {
            iArr[0] = i8;
            iArr[1] = i7;
            iArr[2] = iArr2[2];
            iArr[3] = iArr2[3];
        }
        return iArr2[2];
    }

    int showDialog(String str, String str2, boolean z, int i) {
        playSound();
        int i2 = GameScreen.screenWidth - 24;
        if (i2 > 140) {
            i2 = 140;
        }
        this.diplomacyAction = i;
        this.dialogLineCount = 0;
        this.diplomatIndex = 0;
        this.missionTitle = str;
        this.screenWidth = i2;
        if (i == 0) {
            this.screenHeight = this.font.getHeight() + 16;
        } else {
            this.screenHeight = (this.font.getHeight() * 2) + 16;
        }
        addDialogLine(str2, 9321);
        int iK = 0;
        if (z) {
            this.gameScreen.repaintScreen();
            int i3 = 0;
            while (true) {
                iK = processDialogInput();
                if (iK != 0) {
                    break;
                }
                if (this.needRepaint) {
                    this.gameScreen.repaintScreen();
                }
                this.gameScreen.sleep(50);
                i3++;
                if (i3 > 50 && this.diplomacyAction == 0) {
                    break;
                }
            }
            this.dialogLineCount = 0;
        }
        return iK;
    }

    void addDialogLine(String str, int i) {
        if (str == null || str.length() <= 0 || this.dialogLineCount > this.dialogTexts.length) {
            return;
        }
        int height = this.font.getHeight() + 2;
        int i2 = this.screenWidth - 4;
        while (str.length() > 0 && this.dialogLineCount < this.dialogTexts.length) {
            this.dialogTexts[this.dialogLineCount] = "";
            this.dialogColors[this.dialogLineCount] = i;
            int i3 = 0;
            int i4 = 0;
            while (true) {
                if (i3 >= i2 || i4 >= str.length()) {
                    break;
                }
                char cCharAt = str.charAt(i4);
                if (cCharAt == '|') {
                    i4++;
                    break;
                }
                int iCharWidth = this.font.charWidth(cCharAt);
                if (i3 + iCharWidth > i2) {
                    break;
                }
                i3 += iCharWidth;
                i4++;
                String[] strArr = this.dialogTexts;
                int i5 = this.dialogLineCount;
                strArr[i5] = String.valueOf(String.valueOf(strArr[i5])).concat(String.valueOf(String.valueOf(cCharAt)));
            }
            str = str.substring(i4);
            this.dialogLineCount++;
            this.screenHeight += height;
        }
        if (this.screenHeight > this.infoBarHeight - 10) {
            this.screenHeight = this.infoBarHeight - 10;
        }
    }

    boolean isBusy() { // 是否繁忙
        if (this.dialogLineCount > 0) {
            return true;
        }
        return (this.isGameOver || this.menuState == 3 || this.menuState == 1) ? false : true;
    }

    City findFactionCity(int i) { //查找势力城市
        for (int i2 = 0; i2 < this.cityMenuIndex; i2++) {
            if (this.cities[i2].techLevel > 0 && this.cities[i2].ownerIndex == i) {
                return this.cities[i2];
            }
        }
        return null;
    }

    City getCity(int i, boolean z) {
        if (i >= 0) {
            if (i <= 0 || i > this.cities.length) {
                return null;
            }
            City dVar = this.cities[i - 1];
            if (dVar.techLevel <= 0) {
                return null;
            }
            return dVar;
        }
        for (int i2 = 0; i2 < this.cityNames.length; i2++) {
            if (this.cities[i2].techLevel <= 0) {
                if (z && this.cityMenuIndex < i2 + 1) {
                    this.cityMenuIndex = i2 + 1;
                }
                return this.cities[i2];
            }
        }
        return null;
    }

    Unit findUnitByOwner(int i) { //按势力查找部队
        for (int i2 = 0; i2 < this.cityInfoIndex; i2++) {
            if (this.units[i2].ownerId > 0 && this.units[i2].ownerId == i) {
                return this.units[i2];
            }
        }
        return null;
    }

    Unit getUnit(int i, boolean z) {
        if (i >= 0) {
            if (i <= 0 || i > this.units.length) {
                return null;
            }
            Unit aVar = this.units[i - 1];
            if (aVar.ownerId <= 0) {
                return null;
            }
            return aVar;
        }
        for (int i2 = 0; i2 < this.units.length; i2++) {
            if (this.units[i2].ownerId <= 0) {
                if (this.cityInfoIndex < i2 + 1) {
                    this.cityInfoIndex = i2 + 1;
                }
                return this.units[i2];
            }
        }
        return null;
    }

    boolean isWater(int i, int i2) {
        return (this.mapData[i2][i] & 255) != 0;
    }

    Unit findIdleWorker(int i) { //	查找空闲民工
        for (int i2 = 0; i2 < this.cityInfoIndex; i2++) {
            if (this.units[i2].ownerId > 0 && this.units[i2].ownerId == i && this.units[i2].unitType == 3 && !this.units[i2].isDead && this.units[i2].attackCooldown != 0) {
                return this.units[i2];
            }
        }
        return null;
    }

    int getTerrain(int i, int i2) {
        return this.mapData[i2][i] & 255;
    }

    int getObjectType(int i, int i2) {
        return (this.mapData[i2][i] >> 8) & 255;
    }

    int getCityOwner(int i, int i2) {
        if (getObjectType(i, i2) != 12) {
            return 0;
        }
        return (this.mapData[i2][i] >> 16) & 255;
    }

    int getOwnerId(int i, int i2) {
        return (this.mapData[i2][i] >> 16) & 255;
    }

    int getUnitId(int i, int i2) {
        return (this.mapData[i2][i] >> 24) & 127;
    }

    boolean isHighlighted(int i, int i2) { //是否高亮
        return (this.mapData[i2][i] >> 31) != 0;
    }

    boolean setHighlight(int i, int i2, boolean z, int i3, int i4) { //设置高亮
        if (!z) {
            int[] iArr = this.mapData[i2];
            iArr[i] = iArr[i] & Integer.MAX_VALUE;
            return true;
        }
        Unit aVarA = getUnit(getUnitId(i, i2), false);
        if (aVarA != null) {
            if (aVarA.unitId != i4 && (aVarA.factionIndex == i3 || getDiplomacyValue(i3, aVarA.factionIndex, 1) != 1)) {
                return false;
            }
            int[] iArr2 = this.mapData[i2];
            iArr2[i] = iArr2[i] | Integer.MIN_VALUE;
            return true;
        }
        City dVarM59if = getCity(getCityOwner(i, i2), false);
        if (dVarM59if == null) {
            int[] iArr3 = this.mapData[i2];
            iArr3[i] = iArr3[i] | Integer.MIN_VALUE;
            return true;
        }
        if (dVarM59if.ownerIndex != i3 && getDiplomacyValue(i3, dVarM59if.ownerIndex, 1) != 1) {
            return false;
        }
        int[] iArr4 = this.mapData[i2];
        iArr4[i] = iArr4[i] | Integer.MIN_VALUE;
        return true;
    }

    void setUnitId(int i, int i2, int i3) {
        this.mapData[i2][i] = (this.mapData[i2][i] & 16777215) | (i3 << 24);
    }

    void setOwnerId(int i, int i2, int i3) {
        this.mapData[i2][i] = (this.mapData[i2][i] & (-16711681)) | (i3 << 16);
    }

    void setCityTile(int i, int i2, int i3) {
        this.mapData[i2][i] = (this.mapData[i2][i] & (-16776961)) | (i3 << 16) | 3072;
    }

    void setObjectType(int i, int i2, int i3) {
        this.mapData[i2][i] = (this.mapData[i2][i] & (-65281)) | (i3 << 8);
    }

    boolean isOutOfBounds(int i, int i2) { //是否越界
        return i < 0 || i2 < 0 || i2 >= this.mapData.length || i >= this.mapData[i2].length;
    }

    int selectTile(int i, int i2) {
        if (this.tempUnit != null) {
            this.tempUnit = null;
        }
        int iB = 0;
        this.tempUnit = getUnit(getUnitId(i, i2), false);
        if (this.tempUnit == null) {
            int i3 = getCityOwner(i, i2);
            if (i3 != 0) {
                this.selectedCity = getCity(i3, false);
                if (this.selectedCity == null) {
                    return 0;
                }
                return this.selectedCity.ownerId;
            }
            iB = getOwnerId(i, i2);
            this.selectedCity = getCity(iB, false);
        } else if (this.tempUnit.attackCooldown == 0) {
            return 0;
        }
        return iB;
    }

    private void setDiplomacyValue(int i, int i2, int i3, int i4) { //设置外交值
        int[] iArr = this.diplomacyData[i];
        switch (i3) {
            case 0:
                iArr[i2] = iArr[i2] & (-256);
                iArr[i2] = iArr[i2] | i4;
                break;
            case 1:
                iArr[i2] = iArr[i2] & (-65281);
                iArr[i2] = iArr[i2] | (i4 << 8);
                break;
            case 2:
                iArr[i2] = iArr[i2] & (-16711681);
                iArr[i2] = iArr[i2] | (i4 << 16);
                break;
        }
    }

    int getDiplomacyValue(int i, int i2, int i3) { //获取外交值
        int i4 = this.diplomacyData[i][i2];
        switch (i3) {
            case 0:
                i4 &= 255;
                break;
            case 1:
                i4 = (i4 >> 8) & 255;
                break;
            case 2:
                i4 = (i4 >> 16) & 255;
                break;
        }
        return i4;
    }

    private void drawPanel(Graphics graphics, int i, int i2, int i3, int i4, boolean z) {
        graphics.setColor(z ? 1583194 : 0);
        if (z) {
            graphics.fillRect(i, i2, i3, i4);
        } else {
            graphics.fillRect(i + 1, i2 + 1, i3 - 2, i4 - 2);
        }
        graphics.setColor(z ? 6583200 : 1121081);
        graphics.drawLine(i + 1, i2, (i + i3) - 2, i2);
        graphics.drawLine(i, i2 + 1, i, (i2 + i4) - 2);
        graphics.setColor(z ? 1121081 : 6583200);
        graphics.drawLine((i + i3) - 1, i2 + 1, (i + i3) - 1, (i2 + i4) - 2);
        graphics.drawLine(i + 1, (i2 + i4) - 1, (i + i3) - 2, (i2 + i4) - 1);
    }

    private void selectNextCity(boolean z) {
        int i = this.selectedCity == null ? 0 : this.selectedCity.ownerId - 1;
        for (int i2 = 0; i2 < this.cityMenuIndex; i2++) {
            i++;
            if (i > this.cityMenuIndex) {
                i = 0;
            }
            if (this.cities[i].techLevel > 0 && (!z || this.cities[i].ownerIndex == 0)) {
                moveCursor(this.cities[i].x, this.cities[i].y);
                this.selectedCity = this.cities[i];
                break;
            }
        }
        if (this.selectedCity == null) {
            return;
        }
        this.keyCode = this.selectedCity.x;
        this.keyState = this.selectedCity.y;
        this.gameState = selectTile(this.keyCode, this.keyState);
    }

    private boolean selectNextUnit() {
        int i = this.tempUnit == null ? -1 : this.tempUnit.unitId - 1;
        boolean z = false;
        int i2 = 0;
        while (true) {
            if (i2 >= this.cityInfoIndex) {
                break;
            }
            i++;
            if (i >= this.cityInfoIndex) {
                i = 0;
            }
            if (this.units[i].ownerId > 0 && this.units[i].factionIndex == 0 && !((battleMode && this.units[i].unitType != 3 && this.units[i].unitType != 4) || this.units[i].isDead || this.units[i].attackCooldown == 0)) {
                moveCursor(this.units[i].mapX, this.units[i].mapY);
                this.tempUnit = this.units[i];
                z = true;
                break;
            }
            i2++;
        }
        if (!z) {
            if (showDialog("操作提示", "结束本回合？", true, 1) != 1) {
                return false;
            }
            this.menuState = 10;
            return false;
        }
        if (this.tempUnit == null) {
            return false;
        }
        this.keyCode = this.tempUnit.mapX;
        this.keyState = this.tempUnit.mapY;
        this.gameState = selectTile(this.keyCode, this.keyState);
        return true;
    }

    void calculateUnitUpkeep(int i, int[] iArr) { // 计算部队维护费
        for (int i2 = 0; i2 < this.cityInfoIndex; i2++) {
            if (this.units[i2].ownerId > 0 && this.units[i2].ownerId == i) {
                iArr[3] = iArr[3] - (this.units[i2].maxHp / 100);
                iArr[6] = iArr[6] - (this.units[i2].maxHp / 400);
            }
        }
    }

    private int calculateFactionPower(int i) { //计算势力实力
        int iM8new = 0;
        for (int i2 = 0; i2 < this.cityMenuIndex; i2++) {
            if (this.cities[i2].techLevel > 0 && this.cities[i2].ownerIndex == i) {
                iM8new += this.cities[i2].calculateCityValue();
            }
        }
        for (int i3 = 0; i3 < this.cityInfoIndex; i3++) {
            if (this.units[i3].ownerId > 0 && this.units[i3].factionIndex == i) {
                iM8new += this.units[i3].calcAttack();
            }
        }
        return iM8new;
    }

    void destroyUnitsByOwner(int i) { //销毁势力所属部队
        for (int i2 = 0; i2 < this.cityInfoIndex; i2++) {
            if (this.units[i2].ownerId > 0 && this.units[i2].ownerId == i) {
                moveCursor(this.units[i2].mapX, this.units[i2].mapY);
                this.units[i2].die(this.gameScreen);
            }
        }
        cleanupDeadUnits();
    }

    private void cleanupDeadUnits() {
        while (this.cityInfoIndex > 0 && this.units[this.cityInfoIndex].ownerId <= 0) {
            this.cityInfoIndex--;
        }
    }

    static String formatMessage(String str, String[] strArr) {
        String strConcat = "";
        int i = 0;
        while (i < str.length()) {
            char cCharAt = str.charAt(i);
            if (cCharAt != '%') {
                strConcat = String.valueOf(String.valueOf(strConcat)).concat(String.valueOf(String.valueOf(cCharAt)));
            } else {
                i++;
                char cCharAt2 = str.charAt(i);
                if (cCharAt2 < '0' || cCharAt2 > '9') {
                    str = String.valueOf(String.valueOf(str)).concat(String.valueOf(String.valueOf(cCharAt2)));
                } else {
                    int i2 = cCharAt2 - '0';
                    if (strArr[i2] != null) {
                        strConcat = String.valueOf(String.valueOf(strConcat)).concat(String.valueOf(String.valueOf(strArr[i2])));
                    }
                }
            }
            i++;
        }
        return strConcat;
    }

    int countUnitsByType(int i, int i2) {
        int i3 = 0;
        for (int i4 = 0; i4 < this.cityInfoIndex; i4++) {
            if (this.units[i4].ownerId > 0 && this.units[i4].factionIndex == i) {
                int i5 = this.units[i4].unitType;
                if (i2 < 0) {
                    if (i5 != 3 && i5 != 4) {
                        i3++;
                    }
                } else if (i2 == i5 || i2 == i5 - 5) {
                    i3++;
                }
            }
        }
        return i3;
    }

    int countUnitsByOwner(int i, int i2) {
        int i3 = 0;
        for (int i4 = 0; i4 < this.cityInfoIndex; i4++) {
            if (this.units[i4].ownerId > 0 && this.units[i4].ownerId == i) {
                if (i2 < 0) {
                    i3++;
                } else if (this.units[i4].unitType == i2) {
                    i3++;
                }
            }
        }
        return i3;
    }

    private boolean isAllEnemyDefeated() { //是否所有敌人被击败
        for (int i = 0; i < this.cityMenuIndex; i++) {
            if (this.cities[i].techLevel > 0 && this.cities[i].ownerIndex != 0) {
                return false;
            }
        }
        return true;
    }

    private boolean isPlayerDefeated() {
        for (int i = 0; i < this.cityMenuIndex; i++) {
            if (this.cities[i].techLevel > 0 && this.cities[i].ownerIndex == 0) {
                return false;
            }
        }
        return true;
    }

    private int sumFactionStats(int i, int i2) { //统计势力数据
        int i3 = 0;
        for (int i4 = 0; i4 < this.cityMenuIndex; i4++) {
            if (this.cities[i4].techLevel > 0 && this.cities[i4].ownerIndex == i) {
                i3 = i2 == 0 ? i3 + 1 : i2 > 0 ? i3 + this.cities[i4].attributes[i2 - 1] : i3 + this.cities[i4].resources[(-i2) - 1];
            }
        }
        return i3;
    }

    int randomInt(int i) {
        int iNextInt = this.random.nextInt();
        if (i > 0) {
            iNextInt = Math.abs(iNextInt) % i;
        }
        return iNextInt;
    }

    private boolean checkMissionComplete() { //检查任务完成
        if (isAllEnemyDefeated()) {
            return true;
        }
        if (isPlayerDefeated()) {
            showDialog("失败", "失去了所有的城镇，你失败了!", true, 0);
            this.isGameOver = true;
            return true;
        }
        boolean z = this.missionConditions[this.dialogResult].length > 0;
        for (int i = 0; i < this.missionConditions[this.dialogResult].length; i += 3) {
            switch (this.missionConditions[this.dialogResult][i]) {
                case 0:
                    if (sumFactionStats(0, this.missionConditions[this.dialogResult][i + 1]) < this.missionConditions[this.dialogResult][i + 2]) {
                        z = false;
                    }
                    break;
                case 1:
                    if (countUnitsByType(0, this.missionConditions[this.dialogResult][i + 1]) < this.missionConditions[this.dialogResult][i + 2]) {
                        z = false;
                    }
                    break;
                case 2:
                    int i2 = this.missionConditions[this.dialogResult][i + 1];
                    if (countCities(i2) == 0) {
                        showDialog("失败", String.valueOf(String.valueOf(factionNames[i2])).concat("被消灭了，任务失败!"), true, 0);
                        this.isGameOver = true;
                        return true;
                    }
                    if (getDiplomacyValue(0, this.missionConditions[this.dialogResult][1], 1) != this.missionConditions[this.dialogResult][i + 2]) {
                        z = false;
                    }
                    break;
            }
        }
        return z;
    }

    private void endTurn() {
        adjustPrice(this.random.nextInt() % 10);
        if (checkMissionComplete()) {
            if (this.isGameOver) {
                return;
            }
            showDialog("胜利", "你胜利完成了任务!", true, 0);
            initLevel(false);
            return;
        }
        for (int i = 0; i < this.cityMenuIndex; i++) {
            if (this.cities[i].techLevel > 0) {
                this.cities[i].updateCity(this.gameScreen);
            }
        }
        for (int i2 = 1; i2 < this.diplomacyData.length; i2++) {
            if (countCities(i2) != 0) {
                int iM68else = getDiplomacyValue(i2, 0, 0);
                if (randomInt(80) > iM68else) {
                    int iM68else2 = getDiplomacyValue(i2, 0, 1);
                    if (iM68else2 == 2) {
                        executeDiplomacy(i2, 0, 3);
                    } else {
                        int iM75int = randomInt(iM68else + 1);
                        int iM38case = findBestAlly(i2);
                        if (iM68else2 != 1) {
                            if (iM75int >= 4 || iM68else >= 30) {
                                if (iM75int < 15) {
                                    executeDiplomacy(i2, 0, 2);
                                }
                            } else if (iM38case == 0) {
                                if (randomInt(3) == 1) {
                                    executeDiplomacy(i2, 0, 4);
                                }
                            } else if (randomInt(10) == 1) {
                                executeDiplomacy(i2, 0, 4);
                            }
                        }
                    }
                } else {
                    if (iM68else > 0) {
                        iM68else--;
                    }
                    setDiplomacyValue(i2, 0, 0, iM68else);
                }
            }
        }
        for (int i3 = 0; i3 < this.cityInfoIndex; i3++) {
            if (this.units[i3].ownerId > 0) {
                moveCursor(this.units[i3].mapX, this.units[i3].mapY);
                this.units[i3].updateUnit(this.gameScreen);
                if (this.units[i3].unitType != 3 && this.units[i3].unitType != 4 && ((this.units[i3].factionIndex != 0 || battleMode) && findTarget(this.units[i3], this.cursorPos))) {
                    this.units[i3].moveToTarget(this.cursorPos[0], this.cursorPos[1], this.gameScreen);
                }
            }
        }
        for (int i4 = 0; i4 < this.cityInfoIndex; i4++) {
            this.units[i4].attackCooldown = Unit.unitStats[this.units[i4].unitType][2];
        }
        for (int i5 = 0; i5 < this.mapData.length; i5++) {
            for (int i6 = 0; i6 < this.mapData[i5].length; i6++) {
                int[] iArr = this.mapData[i5];
                int i7 = i6;
                iArr[i7] = iArr[i7] & Integer.MAX_VALUE;
            }
        }
        this.menuState = 1;
        this.subMenuIndex = 0;
        moveCursor(this.keyCode, this.keyState);
        this.gameState = selectTile(this.keyCode, this.keyState);
    }

    boolean isPassable(int i, int i2, int i3) {
        if (isOutOfBounds(i, i2)) {
            return false;
        }
        Unit aVarA = getUnit(getUnitId(i, i2), false);
        if (aVarA != null && (aVarA.factionIndex == i3 || getDiplomacyValue(i3, aVarA.factionIndex, 1) != 1)) {
            return false;
        }
        City dVarM59if = getCity(getCityOwner(i, i2), false);
        return dVarM59if == null || dVarM59if.ownerIndex == i3 || getDiplomacyValue(i3, dVarM59if.ownerIndex, 1) == 1;
    }
}
