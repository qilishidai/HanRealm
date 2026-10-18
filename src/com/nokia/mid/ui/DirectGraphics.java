package com.nokia.mid.ui;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Nokia DirectGraphics 兼容桩。
 *
 * 标准 MIDP 的 Graphics 不能直接画多边形，这里用逐行扫描线填充近似实现
 * fillPolygon，供菜单/弹窗的斜边面板等渲染使用。
 */
public class DirectGraphics {

    private final Graphics g;

    public DirectGraphics(Graphics g) {
        this.g = g;
    }

    /**
     * 诺基亚 DirectGraphics 的 5 参 drawImage 重载（第5参是深度/调色信息）。
     * 桩实现按标准的 anchor 语义代理到 Graphics.drawImage，忽略 color 参数。
     */
    public void drawImage(Image image, int x, int y, int anchor, int color) {
        if (g == null || image == null) {
            return;
        }
        g.drawImage(image, x, y, anchor);
    }

    /**
     * 扫描线方式填充多边形。
     *
     * @param xPoints  顶点 x 坐标数组（原点为 xPoints[xOffset]）
     * @param xOffset  x 坐标起点偏移
     * @param yPoints  顶点 y 坐标数组（原点为 yPoints[yOffset]）
     * @param yOffset  y 坐标起点偏移
     * @param nPoints  顶点个数
     * @param fillColor 填充颜色
     */
    public void fillPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset,
                            int nPoints, int fillColor) {
        if (g == null || xPoints == null || yPoints == null || nPoints < 3) {
            return;
        }
        g.setColor(fillColor);

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int i = 0; i < nPoints; i++) {
            int y = yPoints[yOffset + i];
            if (y < minY) {
                minY = y;
            }
            if (y > maxY) {
                maxY = y;
            }
        }

        int[] xs = new int[nPoints];
        for (int yy = minY; yy <= maxY; yy++) {
            int cnt = 0;
            for (int i = 0; i < nPoints; i++) {
                int j = (i + 1) % nPoints;
                int x0 = xPoints[xOffset + i];
                int y0 = yPoints[yOffset + i];
                int x1 = xPoints[xOffset + j];
                int y1 = yPoints[yOffset + j];
                // 水平边跳过；半开区间避免顶点被重复计数
                if (y0 == y1) {
                    continue;
                }
                if (yy >= Math.min(y0, y1) && yy < Math.max(y0, y1)) {
                    xs[cnt++] = x0 + (int) ((long) (yy - y0) * (x1 - x0) / (y1 - y0));
                }
            }
            // 冒泡排序交点后两两配对填充
            for (int a = 0; a < cnt - 1; a++) {
                for (int b = a + 1; b < cnt; b++) {
                    if (xs[a] > xs[b]) {
                        int t = xs[a];
                        xs[a] = xs[b];
                        xs[b] = t;
                    }
                }
            }
            for (int k = 0; k + 1 < cnt; k += 2) {
                int width = xs[k + 1] - xs[k];
                if (width > 0) {
                    g.fillRect(xs[k], yy, width, 1);
                }
            }
        }
    }
}