package engine;

import algebra.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class Flat_Obj {

    public BufferedImage img;
    public ImageBuffer img_buffer;
    public double scale = 1;

    public Vector pos = new Vector(0, 0, 0, 1);

    public Flat_Obj(String filename){
        try{
            img = ImageIO.read(new File("img/" + filename));
        }
        catch (Exception e){
            System.err.println(e.getMessage());
        }
        img_buffer = new ImageBuffer(img);
    }

    public Flat_Obj(Flat_Obj obj){
        img = obj.img;
        img_buffer = obj.img_buffer;
        scale = obj.scale;
        pos = obj.pos;
    }

    public void move(Vector trail){
        pos = new Matrix(new float[][]{
                {1, 0, 0, trail.at(0)},
                {0, 1, 0, trail.at(1)},
                {0, 0, 1, trail.at(2)},
                {0, 0, 0, 1}
        }).dot(pos);
    }

    public void move(float x, float y, float z){
        pos = new Matrix(new float[][]{
                {1, 0, 0, x},
                {0, 1, 0, y},
                {0, 0, 1, z},
                {0, 0, 0, 1}
        }).dot(pos);
    }

    public Vector getPos(){
        return new Vector(pos.at(0), pos.at(1), pos.at(2));
    }

    public void cd(Vector destination){
        move(destination.subtract(getPos()));
    }

    public void scale(double scale){
        this.scale *= scale;
    }
}
