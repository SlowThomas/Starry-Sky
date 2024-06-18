package engine;

import java.awt.image.BufferedImage;

public class ImgFunc {
    public static BufferedImage scale(BufferedImage img, int width, int height){
        BufferedImage ans = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        double xr = (double) img.getWidth() / width;
        double yr = (double) img.getHeight() / height;

        int l, r, t, b;
        int[] color = new int[3];
        int it, colo;

        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                l = (int)(i * xr + 0.5);
                r = (int)(i * xr + xr + 0.5);
                t = (int)(j * yr + 0.5);
                b = (int)(j * yr + yr + 0.5);
                if(r == l) r++;
                if(b == t) b++;
                r = Math.min(r, img.getWidth());
                b = Math.min(b, img.getHeight());

                color[0] = color[1] = color[2] = 0;
                it = 0;

                for(; l < r; l++)
                    for(; t < b; t++){
                        colo = img.getRGB(l, t);
                        color[0] += colo / (1 << 16) % (1 << 8);
                        color[1] += colo / (1 << 8) % (1 << 8);
                        color[2] += colo % (1 << 8);
                        it++;
                    }

                color[0] = (int)((double)color[0] / it + 0.5);
                color[1] = (int)((double)color[1] / it + 0.5);
                color[2] = (int)((double)color[2] / it + 0.5);
                ans.setRGB(i, j, color[0] * (1 << 16) + color[1] * (1 << 8) + color[2]);
            }
        }
        return ans;
    }

    public static int adjustedColor(ImageBuffer img, int width, int height, int x, int y){
        double xr = (double) img.colo.length / width;
        double yr = (double) img.colo[0].length / height;

        int l = (int)(x * xr + 0.5);
        int r = (int)(x * xr + xr + 0.5);
        int t = (int)(y * yr + 0.5);
        int b = (int)(y * yr + yr + 0.5);
        if(r == l) r++;
        if(b == t) b++;
        r = Math.min(r, img.colo.length);
        b = Math.min(b, img.colo[0].length);

        return img.avg(l, r, t, b);
    }
}
