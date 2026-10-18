package core;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class City {
    private Game game;

    protected int ownerId; //城市所属势力ID
    protected int x; //城市地图X坐标
    protected int y;
    protected int ownerIndex; //势力索引

    protected int level; //城市等级/规模

    protected int buildTarget; //当前建造/训练目标
    protected int productionProgress; //生产进度累计

    protected int techLevel; //科技等级

    protected String cityName;

    // 静态资源数据
    static String[] resourceNames = {"马", "金", "铁", "中药", "铜"};
    static int[] resourcePrices = {5000, 2000, 2000, 6000, 2000};
    static int[] resourceConsumptions = {40, 100, 20, 60, 80};
    
    static String[] cityAttrs = {"文化", "防御", "人口", "食物", "士兵", "训练", "金"};
    static int[] attrIndices = {5, -1, 4, 1, 3, 2, 6};

    static int[] baseValues = {500, 0, 1, 10, 10, 50, 20};

    static int[][] techRequirements = {
        new int[]{5, 150},
        new int[]{-1, 1000},
        new int[]{4, 100000},
        new int[]{-1, 1000000},
        new int[]{3, 10000},
        new int[]{2, 5000, 4},
        new int[]{-1, 1000000}
    };

    static int[] valueWeights = {500, 1000, 1, 5, 15, 60, 20};

    static int[] deltaCache = {0, 0, 0, 0, 0, 0, 0}; //增量缓冲
    
    static String[] buildingNames = {"农田", "牧场", "基地", "兵营", "民房", "书院", "市场", "探索"};

    static int[] buildingCosts = {100, 80, 300, 200, 160, 400, 250, 4000};
    static int[][] buildingEffects = {
        new int[]{3, 200},
        new int[]{3, 180},
        new int[]{5, 20, 6, -10},
        new int[]{4, 200, 6, -10},
        new int[]{2, 400, 6, -4},
        new int[]{0, 1, 6, -16},
        new int[]{6, 50}
    };

    protected int[] resources = {0, 0, 0, 0, 0}; //特产资源存量: 马/金/铁/中药/铜
    protected int[] attributes = {0, 0, 0, 0, 0, 0, 0}; //城市属性: 文化/防御/人口/食物/士兵/训练/金
    protected int[] buildings = {0, 0, 0, 0, 0, 0, 0}; //建筑等级: 农田/牧场/基地/兵营/民房/书院/市场

    protected int animationFrame = -1; //动画帧/特效状态

    int onCityAttacked(int damage, int attackerIndex, GameScreen screen) {// 城市被攻击时的处理
        int iA = clamp(damage / 100, 5, 50);
        int[] iArr = this.attributes;
        iArr[1] = iArr[1] - iA;
        if (this.attributes[1] < 0) {
            this.attributes[1] = 0;
        } else {
            damage = (damage - this.attributes[1]) / 3;
            if (damage < 1) {
                damage = 1;
            }
        }
        int[] iArr2 = this.attributes;
        iArr2[4] = iArr2[4] - damage;
        if (this.attributes[4] < 0) {
            this.attributes[4] = 0;
        }
        if (this.attributes[5] > this.attributes[4]) {
            this.attributes[5] = this.attributes[4];
        }
        int[] iArr3 = this.attributes;
        iArr3[2] = iArr3[2] - damage;
        if (this.attributes[2] < 10000) {
            this.attributes[2] = 10000;
        }
        if (this.attributes[1] > 0 || this.attributes[4] > 0) {
            playAnimation(50, screen);
            return calculateAttackPower();
        }
        playAnimation(52, screen);
        this.game.showDialog("战争信息", Game.formatMessage("%0攻陷了%1的城市[%2]", new String[]{Game.factionNames[attackerIndex], Game.factionNames[this.ownerIndex], this.cityName}), true, 0);
        this.ownerIndex = attackerIndex;
        clearDomain();
        this.attributes[0] = 0;
        expandDomain();
        this.game.destroyUnitsByOwner(this.ownerId);
        return 0;
    }

    private void playAnimation(int frameId, GameScreen screen) {
        this.animationFrame = frameId;
        for (int i2 = 0; i2 < 6; i2++) {
            screen.repaintScreen();
            screen.sleep(50);
            this.animationFrame = (frameId + 1) - (this.animationFrame - frameId);
        }
        this.animationFrame = -1;
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            value = min;
        }
        if (value > max) {
            value = max;
        }
        return value;
    }

    private int calculateAttackPower() {// 计算攻击力/反击伤害
        int i = (this.attributes[4] + this.attributes[5]) / 20;
        if (i < 1) {
            i = 1;
        }
        return i;
    }

    void updateCity(GameScreen eVar) {// 城市每回合更新
        int iM88int = getDomainRadius();
        this.game.moveCursor(this.x, this.y);
        calculateDelta(deltaCache);
        for (int i = 0; i < deltaCache.length; i++) {
            int[] iArr = this.attributes;
            int i2 = i;
            iArr[i2] = iArr[i2] + deltaCache[i];
        }
        if (this.attributes[6] < 0) {
            Unit aVarM60do = this.game.findUnitByOwner(this.ownerId);
            if (aVarM60do != null) {
                aVarM60do.die(eVar);
                this.attributes[6] = 100;
                if (this.ownerIndex == 0) {
                    this.game.showDialog("城市信息", String.valueOf(String.valueOf(new StringBuffer("由于资金不够，").append(this.cityName).append("的一些部队被解散了"))), true, 0);
                }
            } else {
                if (this.ownerIndex == 0 && this.attributes[4] > 0) {
                    this.game.showDialog("城市信息", String.valueOf(String.valueOf(new StringBuffer("军饷短缺，").append(this.cityName).append("的士兵大量开小差"))), true, 0);
                }
                int[] iArr2 = this.attributes;
                iArr2[4] = iArr2[4] - 2000;
                if (this.attributes[4] < 0) {
                    this.attributes[4] = 0;
                }
            }
        }
        if (this.attributes[3] < 0) {
            if (this.ownerIndex == 0) {
                this.game.showDialog("城市信息", String.valueOf(String.valueOf(new StringBuffer("由于粮食短缺，").append(this.cityName).append("的居民大量逃亡。"))), true, 0);
            }
            int[] iArr3 = this.attributes;
            iArr3[2] = iArr3[2] + this.attributes[3];
            if (this.attributes[2] < 20000) {
                this.attributes[2] = 20000;
            }
            this.attributes[3] = 0;
        }
        if (this.buildTarget >= Unit.unitNames.length || this.game.getUnitId(this.x, this.y) != 0) {
            this.productionProgress = 0;
        } else {
            this.productionProgress += getProductionRate();
            Unit aVarA = this.game.getUnit(-1, false);
            if (this.productionProgress >= Unit.unitStats[this.buildTarget][4] && aVarA != null) {
                int i3 = this.attributes[4];
                int i4 = Unit.unitStats[this.buildTarget][0];
                int i5 = this.attributes[5];
                switch (this.buildTarget) {
                    case 3:
                    case 4:
                        i3 = this.attributes[2] - 20000;
                        if (i3 >= i4) {
                            int[] iArr4 = this.attributes;
                            iArr4[2] = iArr4[2] - i4;
                        }
                        break;
                    default:
                        if (i3 > i4) {
                            int[] iArr5 = this.attributes;
                            iArr5[4] = iArr5[4] - i4;
                        }
                        break;
                }
                if (i3 >= i4) {
                    this.productionProgress = 0;
                    Unit aVarA2 = this.game.getUnit(-1, true);
                    if (i5 > i4) {
                        i5 = i4;
                    }
                    int[] iArr6 = this.attributes;
                    iArr6[5] = iArr6[5] - i5;
                    aVarA2.initUnit(i4, i5, this.buildTarget, this.ownerId, this.ownerIndex);
                    aVarA2.mapX = this.x;
                    aVarA2.mapY = this.y;
                    this.game.setUnitId(this.x, this.y, aVarA2.unitId);
                    if (this.ownerIndex == 0) {
                        this.game.showDialog("部队", String.valueOf(String.valueOf(new StringBuffer(String.valueOf(String.valueOf(this.cityName))).append("的").append(Unit.unitNames[this.buildTarget]).append("组建完成了"))), true, 0);
                    }
                }
            }
        }
        int iM88int2 = getDomainRadius();
        if (iM88int2 > iM88int) {
            expandDomain();
            this.game.showDialog("城市信息", String.valueOf(String.valueOf(new StringBuffer("由于文化的发展，").append(this.cityName).append("的领域扩充了!"))), true, 0);
        } else if (iM88int2 < iM88int) {
            clearDomain();
            expandDomain();
        }
        if (this.ownerIndex == 0) {
            return;
        }
        this.game.countUnitsByType(this.ownerIndex, -1);
        boolean z = this.game.getDiplomacyValue(this.ownerIndex, this.game.findBestAlly(this.ownerIndex), 1) == 1;
        this.buildTarget = 8;
        if (this.game.countUnitsByOwner(this.ownerId, 3) < 1) {
            this.buildTarget = 3;
        } else {
            if (!z) {
                if (this.game.countUnitsByOwner(this.ownerId, 3) < 2) {
                    this.buildTarget = 3;
                } else if (this.attributes[1] < 300) {
                    this.buildTarget = 8;
                } else {
                    z = true;
                }
            }
            if (z && this.game.countUnitsByOwner(this.ownerId, -1) < 6) {
                int i6 = 0;
                while (true) {
                    if (i6 < Unit.trainingOrder.length) {
                        if (hasRequiredResources(Unit.trainingOrder[i6])) {
                            this.buildTarget = Unit.trainingOrder[i6];
                        } else {
                            i6++;
                        }
                    }
                }
            }
        }
        int i7 = 0;
        int i8 = 0;
        for (int i9 = 1; i9 < deltaCache.length; i9++) {
            if (attrIndices[i9] >= 0) {
                if (deltaCache[i7] * baseValues[i7] < deltaCache[i9] * baseValues[i9]) {
                    i7 = i9;
                }
                if (deltaCache[i8] * baseValues[i8] > deltaCache[i9] * baseValues[i9]) {
                    i8 = i9;
                }
            }
        }
        int i10 = attrIndices[i7];
        int i11 = attrIndices[i8];
        while (true) {
            Unit aVarM61try = this.game.findIdleWorker(this.ownerId);
            if (aVarM61try == null || this.attributes[6] < buildingCosts[i11] / 5) {
                return;
            }
            int[] iArr7 = this.attributes;
            iArr7[6] = iArr7[6] - (buildingCosts[i11] / 5);
            if (!findTargetPosition(i10, this.game.cursorPos, aVarM61try.unitId)) {
                return;
            }
            aVarM61try.moveToPosition(this.game.cursorPos[0], this.game.cursorPos[1], eVar);
            aVarM61try.startBuilding(i11, this);
        }
    }

    private boolean findTargetPosition(int buildingType, int[] outPos, int unitId) {// 查找可移动/攻击的目标位置
        int iF;
        int iM88int = getDomainRadius();
        boolean z = false;
        for (int i3 = -iM88int; i3 <= iM88int; i3++) {
            for (int i4 = -iM88int; i4 <= iM88int; i4++) {
                int i5 = this.x + i4;
                int i6 = this.y + i3;
                if (!this.game.isOutOfBounds(i5, i6) && this.game.getOwnerId(i5, i6) == this.ownerId && ((iF = this.game.getUnitId(i5, i6)) == 0 || iF == unitId)) {
                    int iE = this.game.getObjectType(i5, i6);
                    if (iE == 0) {
                        outPos[0] = i5;
                        outPos[1] = i6;
                        z = true;
                    } else if (!z && getBuildingIndex(iE) == buildingType) {
                        z = true;
                        outPos[0] = i5;
                        outPos[1] = i6;
                    }
                }
            }
        }
        return z;
    }

    int calculateCityValue() {// 计算城市总价值（用于评分）
        int i = 0;
        for (int i2 = 0; i2 < this.resources.length; i2++) {
            i += this.resources[i2] * resourcePrices[i2];
        }
        for (int i3 = 0; i3 < this.attributes.length; i3++) {
            i += (this.attributes[i3] * valueWeights[i3]) / 1000;
        }
        for (int i4 = 0; i4 < this.buildings.length; i4++) {
            i += 500 * this.buildings[i4];
        }
        return i;
    }

    private int getMaxAttribute(int attrIndex) {// 获取属性上限值
        int iM83for;
        int i2 = techRequirements[attrIndex][0];
        int i3 = i2 >= 0 ? (this.buildings[i2] + 1) * techRequirements[attrIndex][1] : techRequirements[attrIndex][1];
        if (techRequirements[attrIndex].length > 2 && i3 > (iM83for = getMaxAttribute(techRequirements[attrIndex][2]))) {
            i3 = iM83for;
        }
        return i3;
    }

    void calculateDelta(int[] delta) {// 计算每回合属性变化量
        int iM83for;
        for (int i = 0; i < delta.length; i++) {
            delta[i] = 0;
        }
        for (int i2 = 0; i2 < this.resources.length; i2++) {
            delta[6] = delta[6] + (this.resources[i2] * resourceConsumptions[i2]);
        }
        for (int i3 = 0; i3 < this.buildings.length; i3++) {
            for (int i4 = 0; i4 < buildingEffects[i3].length; i4 += 2) {
                int i5 = buildingEffects[i3][i4];
                delta[i5] = delta[i5] + (buildingEffects[i3][i4 + 1] * (this.buildings[i3] + 1));
            }
        }
        delta[3] = delta[3] - (this.attributes[2] / 500);
        delta[6] = delta[6] + (this.attributes[2] / 10000);
        delta[3] = delta[3] - (this.attributes[4] / 200);
        if (this.buildTarget == Unit.unitNames.length) {
            delta[1] = delta[1] + (getProductionRate() / 10);
        }
        this.game.calculateUnitUpkeep(this.ownerId, delta);
        for (int i6 = 0; i6 < this.attributes.length; i6++) {
            if (delta[i6] >= 0 && delta[i6] + this.attributes[i6] > (iM83for = getMaxAttribute(i6))) {
                int i7 = iM83for - this.attributes[i6];
                if (i7 < 0) {
                    i7 /= 20;
                    if (i7 > -1) {
                        i7 = -1;
                    }
                }
                delta[i6] = i7;
            }
        }
    }

    int getProductionTurns(int unitType) {// 计算生产某兵种所需回合数
        int i2 = Unit.unitStats[unitType][4];
        if (i2 <= this.productionProgress) {
            return 1;
        }
        int iM85do = getProductionRate();
        return (((i2 + iM85do) - 1) - this.productionProgress) / iM85do;
    }

    int getProductionRate() {// 获取生产速率（基于人口）
        int i = this.attributes[2] / 10000;
        if (i < 1) {
            i = 1;
        }
        return i;
    }

    boolean hasRequiredResources(int unitType) {// 检查是否拥有训练某兵种所需的资源
        boolean z = true;
        int i2 = 0;
        while (true) {
            if (i2 < this.resources.length) {
                if (((Unit.resourceRequirements[unitType] >> i2) & 1) != 0 && this.resources[i2] <= 0) {
                    z = false;
                    break;
                }
                i2++;
            } else {
                break;
            }
        }
        if (z && unitType == 4) {
            z = this.game.getCity(-1, false) != null;
        }
        return z;
    }

    public City(Game bVar, int i) {
        this.game = bVar;
        this.ownerId = i;
    }

    int getBuildingTileId(int buildingType) {// 获取建筑对应的地图瓦片ID
        switch (buildingType) {
            case 0:
                return 14;
            case 1:
                return 15;
            case 7:
                return -1;
            default:
                return 14 + buildingType + (this.level * 6);
        }
    }

    void loadFromStream(DataInputStream dataInputStream) throws Exception {// 从数据流加载
        this.animationFrame = -1;
        this.techLevel = dataInputStream.readInt();
        if (this.techLevel <= 0) {
            return;
        }
        this.buildTarget = dataInputStream.readInt();
        this.productionProgress = dataInputStream.readInt();
        this.cityName = dataInputStream.readUTF();
        this.x = dataInputStream.readInt();
        this.y = dataInputStream.readInt();
        this.level = dataInputStream.readInt();
        this.ownerIndex = dataInputStream.readInt();
        for (int i = 0; i < this.attributes.length; i++) {
            this.attributes[i] = dataInputStream.readInt();
        }
        for (int i2 = 0; i2 < this.buildings.length; i2++) {
            this.buildings[i2] = dataInputStream.readInt();
        }
        for (int i3 = 0; i3 < this.resources.length; i3++) {
            this.resources[i3] = dataInputStream.readInt();
        }
    }

    void saveToStream(DataOutputStream dataOutputStream) throws Exception {// 保存到数据流
        dataOutputStream.writeInt(this.techLevel);
        if (this.techLevel <= 0) {
            return;
        }
        dataOutputStream.writeInt(this.buildTarget);
        dataOutputStream.writeInt(this.productionProgress);
        dataOutputStream.writeUTF(this.cityName);
        dataOutputStream.writeInt(this.x);
        dataOutputStream.writeInt(this.y);
        dataOutputStream.writeInt(this.level);
        dataOutputStream.writeInt(this.ownerIndex);
        for (int i = 0; i < this.attributes.length; i++) {
            dataOutputStream.writeInt(this.attributes[i]);
        }
        for (int i2 = 0; i2 < this.buildings.length; i2++) {
            dataOutputStream.writeInt(this.buildings[i2]);
        }
        for (int i3 = 0; i3 < this.resources.length; i3++) {
            dataOutputStream.writeInt(this.resources[i3]);
        }
    }

    void initCity(String name, int x, int y, int level, int size, int ownerIndex) {
        this.animationFrame = -1;
        this.buildTarget = 3;
        this.productionProgress = 0;
        this.cityName = name;
        this.x = x;
        this.level = 1 - level;
        this.y = y;
        this.techLevel = 11 + level;
        this.ownerIndex = ownerIndex;
        this.attributes[0] = size * 100;
        this.attributes[1] = size * 50;
        this.attributes[2] = size * 100000;
        this.attributes[3] = size * 1000;
        this.attributes[4] = size * 400;
        this.attributes[5] = size * 50;
        this.attributes[6] = size * 500;
    }

    private int getDomainRadius() {// 获取城市领域半径（文化范围）
        int i = 1;
        while (i * i * 50 < this.attributes[0]) {
            i++;
        }
        int i2 = i + 1;
        int i3 = (this.attributes[2] / 80000) + 1;
        if (i2 > i3) {
            i2 = i3;
        }
        return i2;
    }

    void clearDomain() {// 清除城市领域标记
        int iM88int = getDomainRadius() + 2;
        for (int i = -iM88int; i <= iM88int; i++) {
            for (int i2 = -iM88int; i2 <= iM88int; i2++) {
                int i3 = this.x + i2;
                int i4 = this.y + i;
                if (!this.game.isOutOfBounds(i3, i4) && ((i != 0 || i2 != 0) && this.game.getOwnerId(i3, i4) == this.ownerId)) {
                    this.game.setOwnerId(i3, i4, 0);
                }
            }
        }
    }

    static int getResourceIndex(int tileId) {// 获取瓦片对应的资源索引
        if (tileId < 6 || tileId > 11) {
            return -1;
        }
        return tileId - 6;
    }

    static int getBuildingIndex(int tileId) {// 获取瓦片对应的建筑索引
        if (tileId < 14) {
            return -1;
        }
        if (tileId <= 20) {
            return tileId - 14;
        }
        if (tileId <= 26) {
            return tileId - 20;
        }
        return -1;
    }

    void expandDomain() {// 扩张城市领域（探索/文化扩张）
        int iB;
        int[] iArr = new int[this.resources.length];
        int iM88int = getDomainRadius();
        for (int i = 0; i < this.buildings.length; i++) {
            this.buildings[i] = 0;
        }
        for (int i2 = 0; i2 < this.resources.length; i2++) {
            iArr[i2] = this.resources[i2];
            this.resources[i2] = 0;
        }
        for (int i3 = -iM88int; i3 <= iM88int; i3++) {
            for (int i4 = -iM88int; i4 <= iM88int; i4++) {
                int i5 = this.x + i4;
                int i6 = this.y + i3;
                if (Math.abs(i3) + Math.abs(i4) <= iM88int && !this.game.isOutOfBounds(i5, i6) && this.game.getTerrain(i5, i6) == 0 && ((iB = this.game.getOwnerId(i5, i6)) == 0 || iB == this.ownerId)) {
                    int iE = this.game.getObjectType(i5, i6);
                    if (iE <= 0) {
                        this.game.setOwnerId(i5, i6, this.ownerId);
                    } else {
                        int iA = getResourceIndex(iE);
                        if (iA >= 0) {
                            if (this.ownerIndex == 0 && iArr[iA] == 0) {
                                this.game.moveCursor(i5, i6);
                                this.game.showDialog("城市信息", String.valueOf(String.valueOf(new StringBuffer(String.valueOf(String.valueOf(this.cityName))).append("发现了特产:").append(resourceNames[iA]))), true, 0);
                            }
                            int[] iArr2 = this.resources;
                            iArr2[iA] = iArr2[iA] + 1;
                        } else if (iE != 50) {
                            int iM89do = getBuildingIndex(iE);
                            if (iM89do >= 0) {
                                int[] iArr3 = this.buildings;
                                iArr3[iM89do] = iArr3[iM89do] + 1;
                            }
                        }
                        this.game.setOwnerId(i5, i6, this.ownerId);
                    }
                }
            }
        }
    }
}
