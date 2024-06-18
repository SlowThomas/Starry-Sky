package engine;

import java.awt.image.BufferedImage;

public class ImageBuffer {
    public int[][][] colo; // use psa since there are few updates.

    public ImageBuffer(BufferedImage img){
        colo = new int[img.getWidth()][img.getHeight()][4];
        for(int i = 0; i < img.getWidth(); i++){
            for(int j = 0, color; j < img.getHeight(); j++){
                color = img.getRGB(i, j);
                colo[i][j][0] = color / (1 << 16) % (1 << 8);
                colo[i][j][1] = color / (1 << 8) % (1 << 8);
                colo[i][j][2] = color % (1 << 8);
                colo[i][j][3] = color >>> 24; // opacity
            }
        }

        for(int i = 0; i < colo.length; i++){
            for(int j = 0; j < colo[0].length; j++){
                for(int k = 0; k < colo[0][0].length; k++){
                    if(i > 0) colo[i][j][k] += colo[i - 1][j][k];
                    if(j > 0) colo[i][j][k] += colo[i][j - 1][k];
                    if(i > 0 && j > 0) colo[i][j][k] -= colo[i - 1][j - 1][k];
                }
            }
        }
    }

    public int avg(int l, int r, int t, int b){
        if(r == l || b == l) return 0;
        int[] color = {colo[r - 1][b - 1][0], colo[r - 1][b - 1][1], colo[r - 1][b - 1][2], colo[r - 1][b - 1][3]};
        if(l > 0) for(int i = 0; i < colo[0][0].length; i++) color[i] -= colo[l - 1][b - 1][i];
        if(t > 0) for(int i = 0; i < colo[0][0].length; i++) color[i] -= colo[r - 1][t - 1][i];
        if(l > 0 && t > 0) for(int i = 0; i < colo[0][0].length; i++) color[i] += colo[l - 1][t - 1][i];
        for(int i = 0; i < colo[0][0].length; i++) color[i] /= (r - l) * (b - t);
        return (color[3] << 24) + (color[0] << 16) + (color[1] << 8) + color[2];
    }
}
