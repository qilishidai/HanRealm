package core;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public class Menu {

    protected boolean isVisible;

    private GameScreen gameScreen;

    private String[] menuItems;

    private int selectedIndex;

    private int blinkCounter = 0;

    private int frameCounter = 0;
    private int actionCode = 0;

    public Menu(GameScreen eVar) {
        this.gameScreen = eVar;
    }

    void toggleVisibility() {
        this.isVisible = !this.isVisible;
        if (this.isVisible) {
        }
    }

    boolean handleKeyPress(int key) {
        switch (key) {
            case -10:
            case -7:
            case -5:
            case -2:
            case -1:
                if (this.isVisible) {
                    this.actionCode = key;
                }
                return this.isVisible;
            case -9:
            case -8:
            case -4:
            case -3:
            default:
                return false;
            case -6:
                this.actionCode = key;
                return true;
        }
    }

    void setMenu(String[] items, boolean visible) {
        this.isVisible = visible;
        this.menuItems = items;
        this.selectedIndex = 0;
        this.actionCode = 0;
    }

    int processMenu() {
        int result = 0;
        
        if (this.isVisible) {
            if (this.frameCounter < 2) {
                this.frameCounter++;
            } else {
                this.frameCounter = 0;
                if (this.blinkCounter < 2) {
                    this.blinkCounter++;
                } else {
                    this.blinkCounter = 0;
                }
                result = -1;
            }
        }
        
        switch (this.actionCode) {
            case -10:
            case -5:
                if (this.isVisible) {
                    toggleVisibility();
                    result = this.selectedIndex + 1;
                }
                break;
            case -7:
                if (this.isVisible) {
                    toggleVisibility();
                }
                result = -1;
                break;
            case -6:
                toggleVisibility();
                if (!this.isVisible) {
                    result = this.selectedIndex + 1;
                } else {
                    this.selectedIndex = 0;
                }
                break;
            case -2:
                if (this.selectedIndex + 1 < this.menuItems.length) {
                    this.selectedIndex++;
                } else {
                    this.selectedIndex = 0;
                }
                result = -1;
                break;
            case -1:
                if (this.selectedIndex > 0) {
                    this.selectedIndex--;
                } else {
                    this.selectedIndex = this.menuItems.length - 1;
                }
                result = -1;
                break;
        }
        
        this.actionCode = 0;
        return result;
    }

    void draw(Graphics graphics) {
        if (this.isVisible) {
            Font font = graphics.getFont();
            int maxWidthIdx = 0;
            int maxWidth = this.menuItems[0].length();
            
            for (int i = 1; i < this.menuItems.length; i++) {
                int len = this.menuItems[i].length();
                if (maxWidth < len) {
                    maxWidthIdx = i;
                    maxWidth = len;
                }
            }
            
            int boxWidth = font.stringWidth(this.menuItems[maxWidthIdx]) + 24;
            int itemHeight = font.getHeight() + 2;
            int boxHeight = (this.menuItems.length * itemHeight) + 4;
            int x = (GameScreen.screenWidth - boxWidth) >> 1;
            int y = (GameScreen.screenHeight - boxHeight) >> 1;
            GameScreen.drawRect(graphics, x, y, boxWidth, boxHeight, -1358954496, 16760703);
            int currentY = y + 2;
            
            for (int i2 = 0; i2 < this.menuItems.length; i2++) {
                int color = 16777215;
                
                if (i2 == this.selectedIndex) {
                    this.gameScreen.drawSelector(graphics, x + 2, (currentY + (itemHeight >> 1)) - 5, this.blinkCounter);
                    color = 16777088;
                }
                
                GameScreen.drawText(this.menuItems[i2], x + 22, currentY + 1, graphics, color, 1052688);
                currentY += itemHeight;
            }
        }
    }
}
