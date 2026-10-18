package core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import javax.microedition.lcdui.Graphics;

public class Unit {

    static int animFrame;
    
    protected int maxHp; // 最大兵力/生命值
    protected int currentHp; // 当前兵力
    protected int originalMaxHp; // 初始最大兵力
    protected int moveX; // 移动目标X
    protected int moveY; // 移动目标Y
    protected int buildProgress; // 建造进度

    protected int unitType;
    protected int ownerId; //所属势力ID
    protected int factionIndex; //势力索引

    protected int unitId; //部队唯一ID

    protected int movePhase; //移动相位/移动动画交替

    protected int moveTick; //移动计时器

    protected int exp;
    protected int level;
    private Game game;

    protected int mapX;

    protected int mapY;
    protected int directionMask; //方向掩码: 4方向可移动性
    static int screenX;
    static int screenY;
    protected boolean isDead;

    protected boolean showAnim; //是否显示动画特效
    static int[] trainingOrder = {7, 2, 5, 6, 1, 0}; //训练优先级顺序
    
    static int[][] unitStats = {       // (兵种属性: {造价, 攻击, 机动, 防御, 训练耗时})
        new int[]{10000, 6, 1, 40, 100},
        new int[]{6000, 12, 1, 10, 150},
        new int[]{5000, 30, 3, 20, 200},
        new int[]{5000, 2, 3, 0, 60},
        new int[]{30000, 1, 3, 0, 800},
        new int[]{8000, 8, 1, 60, 160},
        new int[]{5000, 24, 1, 25, 240},
        new int[]{4000, 50, 2, 35, 320}
    };
    static String[] unitNames = {"步兵", "弓兵", "骑兵", "民工", "移民", "重步兵", "弩兵", "重骑兵"};

    static int[] resourceRequirements = {0, 0, 1, 0, 0, 4, 16, 3}; // 兵种资源需求位掩码

    static String[] rankNames = {"丙", "乙", "甲", "特"};

    static int[][] dirOffsets = {      // (方向偏移量: 上右下左)
        new int[]{-1, 0},
        new int[]{0, 1},
        new int[]{1, 0},
        new int[]{0, -1}
    };

    static int[][] arrowOffsets = {    // (箭头偏移量)
        new int[]{-10, 5},
        new int[]{10, 5},
        new int[]{10, -5},
        new int[]{-10, -5}
    };
    
    protected int damageBonus = 0; // 伤害加成/经验奖励
    protected int attackCooldown = 0; // 攻击冷却/剩余移动力

    protected Unit targetUnit = null; // 目标部队/攻击目标

    void convertToWorker(int ownerId) {// 转换为民工
        this.ownerId = ownerId;
        this.unitType = 3;
        int i3 = unitStats[this.unitType][0];
        this.maxHp = i3;
        this.originalMaxHp = i3;
        this.currentHp /= 5;
        this.attackCooldown = 3;
    }

    public Unit(Game bVar, int i2) {
        this.game = bVar;
        this.unitId = i2;
    }

    boolean updateMovement(boolean isTurnEnd) {// 更新移动方向掩码，检测可移动方向和目标
        this.directionMask = 0;
        if (isTurnEnd) {
            return false;
        }
        for (int i2 = 0; i2 < dirOffsets.length; i2++) {
            if (this.game.isPassable(this.mapX + dirOffsets[i2][0], this.mapY + dirOffsets[i2][1], this.factionIndex)) {
                this.directionMask |= 1 << i2;
            }
        }
        this.targetUnit = findRangedTarget();
        return this.directionMask > 0;
    }

    Unit findRangedTarget() {// 远程兵种寻找攻击目标（弓兵/弩兵）
        Unit aVarA;
        if (this.unitType != 1 && this.unitType != 6) {
            return null;
        }
        for (int i2 = 0; i2 < dirOffsets.length; i2++) {
            for (int i3 = 1; i3 <= 2; i3++) {
                int i4 = this.mapX + (dirOffsets[i2][0] * i3);
                int i5 = this.mapY + (dirOffsets[i2][1] * i3);
                if (!this.game.isOutOfBounds(i4, i5) && (aVarA = this.game.getUnit(this.game.getUnitId(i4, i5), false)) != null && aVarA.factionIndex != this.factionIndex && this.game.getDiplomacyValue(this.factionIndex, aVarA.factionIndex, 1) == 1) {
                    return aVarA;
                }
            }
        }
        return null;
    }

    private void attackCity(City city, GameScreen screen) {// 攻击城市
        if (this.factionIndex == city.ownerIndex || this.game.getDiplomacyValue(this.factionIndex, city.ownerIndex, 1) != 1) {
            return;
        }
        this.game.moveCursor(city.x, city.y);
        this.attackCooldown = -1;
        if (this.factionIndex == city.ownerIndex) {
            return;
        }
        int iA = city.onCityAttacked(calcRandomDamage(calcAttack()), this.factionIndex, screen);
        this.currentHp += this.maxHp / 10;
        if (iA > 0) {
            takeDamage(iA, screen, true);
        } else {
            this.currentHp += this.maxHp / 5;
            if (this.currentHp > this.maxHp) {
                this.currentHp = this.maxHp;
            }
            this.ownerId = city.ownerId;
            gainExp(4, screen);
        }
        this.game.moveCursor(this.mapX, this.mapY);
    }

    private void takeDamage(int damage, GameScreen screen, boolean isRanged) {
        if ((this.unitType == 1 || this.unitType == 6) && !isRanged) {
            return;
        }
        this.maxHp -= calcRandomDamage(damage);
        if (this.maxHp <= 0) {
            this.maxHp = 0;
            this.currentHp = 0;
            die(screen);
        } else if (this.currentHp > this.maxHp) {
            this.currentHp = this.maxHp;
        }
    }

    private void gainExp(int expGain, GameScreen screen) {// 获得经验，检查升级
        this.exp += expGain;
        if (this.game.randomInt(this.exp) > 8 * this.level) {
            this.exp = 0;
            if (this.level < rankNames.length) {
                this.level++;
                this.game.getCursorScreenPos(this.mapX, this.mapY, this.game.cursorPos, true);
                showEffect(this.game.cursorPos[0], this.game.cursorPos[1], screen, 54);
            }
        }
    }

    void clearDeadFlag() {// 清除死亡标记
        this.isDead = false;
        this.game.setObjectType(this.mapX, this.mapY, 0);
    }

    void die(GameScreen screen) {// 部队死亡/销毁
        if (this.isDead) {
            clearDeadFlag();
        }
        this.directionMask = 0;
        this.game.getCursorScreenPos(this.mapX, this.mapY, this.game.cursorPos, true);
        showEffect(this.game.cursorPos[0], this.game.cursorPos[1], screen, 52);
        resetUnit();
        this.game.setUnitId(this.mapX, this.mapY, 0);
    }

    private int calcRandomDamage(int baseDamage) {// 计算随机伤害（带随机浮动）
        int i3 = baseDamage / 5;
        if (i3 < 4) {
            i3 = 4;
        }
        return baseDamage + (this.game.randomInt(i3) - (i3 >> 1));
    }

    private void showEffect(int x, int y, GameScreen screen, int effectId) {// 显示特效动画
        animFrame = effectId;
        screenX = x;
        screenY = y - 12;
        this.showAnim = true;
        for (int i5 = 0; i5 < 8; i5++) {
            screen.repaintScreen();
            screen.sleep(50);
            animFrame = (effectId + 1) - (animFrame - effectId);
        }
        this.showAnim = false;
    }

    private void attackUnit(Unit target, GameScreen screen) {// 攻击敌方部队
        if (this.factionIndex == target.factionIndex || this.game.getDiplomacyValue(this.factionIndex, target.factionIndex, 1) != 1) {
            return;
        }
        this.game.moveCursor(target.mapX, target.mapY);
        this.attackCooldown = -1;
        if (this.factionIndex == target.factionIndex) {
            return;
        }
        int iM6if = target.onAttacked(calcRandomDamage(calcAttack()), screen);
        int i2 = this.maxHp / 10;
        if (iM6if == 0) {
            i2 = this.maxHp / 5;
            if (target.unitType != 3) {
                gainExp(1, screen);
            }
        } else {
            takeDamage(iM6if, screen, false);
        }
        this.currentHp += i2;
        if (this.currentHp > this.maxHp) {
            this.currentHp = this.maxHp;
        }
        this.game.moveCursor(this.mapX, this.mapY);
    }

    private int onAttacked(int damage, GameScreen screen) {// 被攻击时的响应，返回反击伤害
        this.game.getCursorScreenPos(this.mapX, this.mapY, this.game.cursorPos, true);
        int iM9if = (damage * (100 - calcDefense())) / 100;
        if (iM9if < 1) {
            iM9if = 1;
        }
        showEffect(this.game.cursorPos[0], this.game.cursorPos[1], screen, 50);
        this.maxHp -= iM9if;
        if (this.maxHp <= 0) {
            die(screen);
            return 0;
        }
        if (this.currentHp > this.maxHp) {
            this.currentHp = this.maxHp;
        }
        return (calcAttack() / 4) + 1;
    }

    void showUnitInfo() {
        this.game.showDialog(String.valueOf(String.valueOf(Game.factionNames[this.factionIndex])).concat(String.valueOf(String.valueOf(unitNames[this.unitType]))), "国家:".concat(String.valueOf(String.valueOf(Game.factionNames[this.factionIndex]))), false, 0);
        City dVarM59if = this.game.getCity(this.ownerId, false);
        if (dVarM59if != null) {
            this.game.addDialogLine("城市:".concat(String.valueOf(String.valueOf(dVarM59if.cityName))), 8995872);
        }
        this.game.addDialogLine("兵员:".concat(String.valueOf(String.valueOf(this.maxHp))), 9321);
        this.game.addDialogLine("老兵:".concat(String.valueOf(String.valueOf(this.currentHp))), 8995872);
        this.game.addDialogLine(String.valueOf(String.valueOf(new StringBuffer("等级:").append(rankNames[this.level - 1]).append("级部队"))), 9321);
        this.game.addDialogLine("攻击:".concat(String.valueOf(String.valueOf(calcAttack()))), 8995872);
        this.game.addDialogLine("防御:".concat(String.valueOf(String.valueOf(calcDefense() * 50))), 9321);
        this.game.addDialogLine("机动:".concat(String.valueOf(String.valueOf(unitStats[this.unitType][2]))), 8995872);
    }

    int calcAttack() {
        return ((unitStats[this.unitType][1] * (this.maxHp + (this.currentHp * 2))) * (1 + this.level)) / 400;
    }

    int calcDefense() {
        int i2 = (unitStats[this.unitType][3] * (this.maxHp + (this.currentHp * 2))) / (3 * this.maxHp);
        int i3 = (100 - i2) / 3;
        for (int i4 = 1; i4 < this.level; i4++) {
            i2 += i3;
            i3 = (100 - i2) / 3;
        }
        return i2;
    }

    void resetUnit() {
        this.ownerId = -1;
        this.movePhase = 0;
        this.attackCooldown = unitStats[this.unitType][2];
        this.directionMask = 0;
        this.showAnim = false;
    }

    boolean updateMoveAnim() {// 更新移动动画相位
        if (this.ownerId < 0) {
            return false;
        }
        if (this.moveTick < 6) {
            this.moveTick++;
            return false;
        }
        this.moveTick = 0;
        this.movePhase = 1 - this.movePhase;
        return true;
    }

    static int getTileId(int unitType) {// 获取兵种对应的瓦片ID
        int i3 = 27 + unitType;
        switch (unitType) {
            case 5:
            case 6:
            case 7:
                i3 -= 5;
                break;
        }
        return i3;
    }

    int getDrawFrame(boolean isMoving) {// 获取绘制帧索引
        int i2 = isMoving ? this.movePhase : 0;
        return this.isDead ? 37 + i2 : this.game.isWater(this.mapX, this.mapY) ? 45 + i2 : getTileId(this.unitType) + (i2 * 5);
    }

    boolean moveToDirection(int dirIndex, GameScreen screen) {
        if (((this.directionMask >> dirIndex) & 1) == 0) {
            return true;
        }
        int i3 = this.mapX + dirOffsets[dirIndex][0];
        int i4 = this.mapY + dirOffsets[dirIndex][1];
        Unit aVarA = this.game.getUnit(this.game.getUnitId(i3, i4), false);
        if (aVarA != null) {
            this.directionMask = 0;
            attackUnit(aVarA, screen);
            return false;
        }
        City dVarM59if = this.game.getCity(this.game.getCityOwner(i3, i4), false);
        if (dVarM59if == null) {
            moveToPosition(i3, i4, screen);
            int iE = this.game.getObjectType(i3, i4);
            if (iE <= 0 || iE > 5) {
                this.attackCooldown--;
            } else {
                this.attackCooldown = 0;
            }
            return this.attackCooldown > 0;
        }
        if (dVarM59if.ownerIndex != this.factionIndex) {
            this.directionMask = 0;
            attackCity(dVarM59if, screen);
            return false;
        }
        if (this.game.showDialog("部队", "回城的部队将被解散，你确认吗?", true, 1) != 1) {
            return true;
        }
        moveToPosition(i3, i4, screen);
        die(screen);
        return false;
    }

    void drawArrows(Graphics graphics) {// 绘制移动方向箭头
        if (this.directionMask == 0) {
            return;
        }
        this.game.getCursorScreenPos(this.mapX, this.mapY, this.game.cursorPos, true);
        int i2 = this.game.cursorPos[0];
        int i3 = this.game.cursorPos[1];
        for (int i4 = 0; i4 < arrowOffsets.length; i4++) {
            if (((this.directionMask >> i4) & 1) != 0) {
                this.game.drawSprite(graphics, i2 + ((arrowOffsets[i4][0] * (5 + this.movePhase)) / 5), i3 + ((arrowOffsets[i4][1] * (5 + this.movePhase)) / 5), 56 + i4, (int[]) null, true);
            }
        }
        if (this.targetUnit != null) {
            this.game.drawSprite(graphics, i2, i3, 60, (int[]) null, true);
        }
    }

    void drawUnit(Graphics graphics, int x, int y) {
        int[] iArr = {0, 0, 0, 0};
        if (this.game.drawSprite(graphics, x, y, getDrawFrame(true), iArr, true) != 0 && iArr[1] >= this.game.screenOffsetY) {
            int i4 = x - 12;
            int i5 = y + 6;
            iArr[1] = iArr[1] - 5;
            iArr[2] = this.game.spriteFrames[getDrawFrame(false)][2];
            this.game.drawSprite(graphics, iArr[0], iArr[1], this.attackCooldown == 0 ? 48 : 47, (int[]) null, true);
            iArr[0] = iArr[0] + 7;
            iArr[2] = iArr[2] - 7;
            int i6 = (iArr[2] * this.maxHp) / this.originalMaxHp;
            graphics.setColor(Game.factionColors[this.factionIndex]);
            graphics.fillRect(iArr[0], iArr[1] + 2, i6, 2);
            graphics.setColor(0);
            graphics.drawRect(iArr[0], iArr[1] + 1, iArr[2], 3);
            if (this.showAnim) {
                this.game.drawSprite(graphics, screenX, screenY, animFrame, (int[]) null, true);
            }
            if (this.damageBonus != 0) {
                GameScreen.drawText("+".concat(String.valueOf(String.valueOf(this.damageBonus & 65535))), i4 - 8, i5 - (this.damageBonus >> 24), graphics, 16711680, 16777215);
            }
        }
    }

    void loadFromStream(DataInputStream dataInputStream) throws Exception {
        this.ownerId = dataInputStream.readInt();
        this.directionMask = 0;
        if (this.ownerId < 0) {
            return;
        }
        int i2 = dataInputStream.readInt();
        this.isDead = (i2 & 1) != 0;
        this.showAnim = ((i2 >> 2) & 1) != 0;
        this.attackCooldown = dataInputStream.readInt();
        this.movePhase = 0;
        this.moveTick = this.unitId % 6;
        this.mapX = dataInputStream.readInt();
        this.mapY = dataInputStream.readInt();
        this.maxHp = dataInputStream.readInt();
        this.originalMaxHp = dataInputStream.readInt();
        this.currentHp = dataInputStream.readInt();
        this.unitType = dataInputStream.readInt();
        this.factionIndex = dataInputStream.readInt();
        this.exp = dataInputStream.readInt();
        this.level = dataInputStream.readInt();
        if (this.isDead) {
            this.moveX = dataInputStream.readInt();
            this.buildProgress = dataInputStream.readInt();
            this.moveY = dataInputStream.readInt();
        }
    }

    void saveToStream(DataOutputStream dataOutputStream) throws Exception {
        dataOutputStream.writeInt(this.ownerId);
        if (this.ownerId < 0) {
            return;
        }
        dataOutputStream.writeInt((this.isDead ? 1 : 0) | (this.showAnim ? 4 : 0));
        dataOutputStream.writeInt(this.attackCooldown);
        dataOutputStream.writeInt(this.mapX);
        dataOutputStream.writeInt(this.mapY);
        dataOutputStream.writeInt(this.maxHp);
        dataOutputStream.writeInt(this.originalMaxHp);
        dataOutputStream.writeInt(this.currentHp);
        dataOutputStream.writeInt(this.unitType);
        dataOutputStream.writeInt(this.factionIndex);
        dataOutputStream.writeInt(this.exp);
        dataOutputStream.writeInt(this.level);
        if (this.isDead) {
            dataOutputStream.writeInt(this.moveX);
            dataOutputStream.writeInt(this.buildProgress);
            dataOutputStream.writeInt(this.moveY);
        }
    }

    void initUnit(int maxHp, int currentHp, int unitType, int ownerId, int factionIndex) {
        this.targetUnit = null;
        this.directionMask = 0;
        this.level = 1;
        this.exp = 0;
        this.isDead = false;
        this.showAnim = false;
        this.movePhase = 0;
        this.moveTick = this.unitId % 6;
        this.maxHp = maxHp;
        this.originalMaxHp = this.maxHp;
        this.currentHp = currentHp;
        this.unitType = unitType;
        this.ownerId = ownerId;
        this.factionIndex = factionIndex;
        this.attackCooldown = unitStats[this.unitType][2];
    }

    void startBuilding(int buildingType, City city) {// 开始建造（民工/移民）
        int iM89do;
        this.isDead = true;
        this.attackCooldown = 0;
        this.moveX = buildingType;
        int iE = this.game.getObjectType(this.mapX, this.mapY);
        if (iE > 0 && (iM89do = City.getBuildingIndex(iE)) >= 0) {
            int[] iArr = city.buildings;
            iArr[iM89do] = iArr[iM89do] - 1;
        }
        this.moveY = city.getBuildingTileId(buildingType);
        this.buildProgress = City.buildingCosts[buildingType];
        int[] iArr2 = city.attributes;
        iArr2[6] = iArr2[6] - (this.buildProgress / 5);
        this.game.setObjectType(this.mapX, this.mapY, 50);
    }

    int getAttackTurns(int targetDefense) {// 计算攻击所需回合数
        int iM8new = calcAttack();
        return ((targetDefense + iM8new) - 1) / iM8new;
    }

    private void showHealEffect(int healAmount, GameScreen screen) {// 显示治疗/恢复特效
        this.damageBonus = healAmount;
        this.maxHp += healAmount;
        this.currentHp += healAmount;
        if (this.currentHp > this.maxHp) {
            this.currentHp = this.maxHp;
        }
        for (int i3 = 0; i3 < 6; i3++) {
            screen.repaintScreen();
            this.damageBonus &= 65535;
            this.damageBonus |= (i3 + 10) << 24;
            screen.sleep(50);
        }
        this.damageBonus = 0;
    }

    void updateUnit(GameScreen screen) {// 部队每回合更新（治疗、建造、探索等）
        this.game.moveCursor(this.mapX, this.mapY);
        City dVarM59if = this.game.getCity(this.ownerId, false);
        if (dVarM59if != null && dVarM59if.resources[3] > 0) {
            int i2 = (this.originalMaxHp * dVarM59if.resources[3]) / 20;
            if (i2 + this.maxHp > this.originalMaxHp) {
                i2 = this.originalMaxHp - this.maxHp;
            }
            if (i2 > 0) {
                showHealEffect(i2, screen);
            }
        }
        if (this.isDead) {
            this.buildProgress -= calcAttack();
            City dVarM59if2 = this.game.getCity(this.ownerId, false);
            this.currentHp += 100;
            if (this.currentHp > this.maxHp) {
                this.currentHp = this.maxHp;
            }
            if (this.buildProgress > 0) {
                return;
            }
            this.buildProgress = 0;
            if (this.moveX == 7) {
                int iM75int = this.game.randomInt(20);
                if (iM75int < City.resourceNames.length) {
                    this.game.showDialog("好消息", String.valueOf(String.valueOf(new StringBuffer("经过长时间探索，在").append(dVarM59if2.cityName).append("附近发现了新特产:").append(City.resourceNames[iM75int]))), true, 0);
                    this.game.setObjectType(this.mapX, this.mapY, iM75int + 6);
                    int[] iArr = dVarM59if2.resources;
                    iArr[iM75int] = iArr[iM75int] + 1;
                } else {
                    this.game.setObjectType(this.mapX, this.mapY, 0);
                    this.game.showDialog("提示", String.valueOf(String.valueOf(dVarM59if2.cityName)).concat("附近漫长的探索探索结束了，耗资巨大但一无所获"), true, 0);
                }
            } else {
                if (this.game.getOwnerId(this.mapX, this.mapY) == dVarM59if2.ownerId) {
                    int[] iArr2 = dVarM59if2.buildings;
                    int i3 = this.moveX;
                    iArr2[i3] = iArr2[i3] + 1;
                }
                this.game.setObjectType(this.mapX, this.mapY, this.moveY);
            }
            this.isDead = false;
        }
    }

    private int findBestDirection(int targetX, int targetY) {// 寻找最优移动方向（AI寻路）
        int i4;
        int i5;
        int i6;
        City dVarM59if;
        int i7 = 0;
        int i8 = -1;
        for (int i9 = 0; i9 < dirOffsets.length; i9++) {
            if (((this.directionMask >> i9) & 1) != 0 && ((i6 = this.game.getCityOwner((i4 = this.mapX + dirOffsets[i9][0]), (i5 = this.mapY + dirOffsets[i9][1]))) == 0 || (dVarM59if = this.game.getCity(i6, false)) == null || dVarM59if.ownerIndex != this.factionIndex)) {
                int iAbs = Math.abs(i4 - targetX) + Math.abs(i5 - targetY);
                if (i8 < 0 || i7 > iAbs) {
                    i8 = i9;
                    i7 = iAbs;
                }
            }
        }
        return i8;
    }

    boolean attackTarget(GameScreen screen) {// 攻击当前锁定的目标
        if (this.targetUnit == null) {
            return false;
        }
        attackUnit(this.targetUnit, screen);
        this.attackCooldown = 0;
        return true;
    }

    void moveToTarget(int targetX, int targetY, GameScreen screen) {// 向目标位置移动（AI自动移动）
        while (this.attackCooldown > 0 && updateMovement(false)) {
            if (this.targetUnit != null) {
                this.directionMask = 0;
                attackUnit(this.targetUnit, screen);
                this.targetUnit = null;
                return;
            } else {
                int iA = findBestDirection(targetX, targetY);
                if (iA < 0) {
                    return;
                }
                if (!moveToDirection(iA, screen)) {
                    this.game.processUnitTurn(this, false);
                }
                this.directionMask = 0;
            }
        }
    }

    void moveToPosition(int targetX, int targetY, GameScreen screen) {// 移动到指定位置（逐格移动）
        this.game.moveCursor(this.mapX, this.mapY);
        screen.repaintScreen();
        while (true) {
            if (targetX == this.mapX && targetY == this.mapY) {
                return;
            }
            if (this.game.getUnitId(this.mapX, this.mapY) == this.unitId) {
                this.game.setUnitId(this.mapX, this.mapY, 0);
            }
            if (this.mapX < targetX) {
                this.mapX++;
            } else if (this.mapX > targetX) {
                this.mapX--;
            }
            if (this.mapY > targetY) {
                this.mapY--;
            } else if (this.mapY < targetY) {
                this.mapY++;
            }
            if (this.game.getUnitId(this.mapX, this.mapY) == 0) {
                this.game.setUnitId(this.mapX, this.mapY, this.unitId);
            }
            this.game.moveCursor(this.mapX, this.mapY);
            this.movePhase = 1 - this.movePhase;
            screen.repaintScreen();
            screen.sleep(50);
        }
    }
}
