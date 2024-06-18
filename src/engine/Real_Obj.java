package engine;
import algebra.*;

import java.util.Scanner;
import java.io.File;

public class Real_Obj {

    private static class Consts{
        public static float epsilon = 1E-10f; // another choice: 1E-14
    }

    public int[][] f;
    public Vector[] v;
    public int[] mtl;
    public Mtl material;
    
    public Vector pos = new Vector(0, 0, 0, 1);

    public Matrix T_model = new Matrix(new float[][]{
            {1, 0, 0, 0},
            {0, 1, 0, 0},
            {0, 0, 1, 0},
            {0, 0, 0, 1}
    }); // model space transformation
    public Matrix T_world = new Matrix(new float[][]{
            {1, 0, 0, 0},
            {0, 1, 0, 0},
            {0, 0, 1, 0},
            {0, 0, 0, 1}
    }); // world space transformation

    public Real_Obj(String object_name){
        material = new Mtl("objects/" + object_name + "/obj.mtl");

        Scanner file;
        try{
            file = new Scanner(new File("objects/" + object_name + "/tinker.obj"));
        } catch(Exception e){
            System.err.printf("Cannot find object: \"%s\"\n", object_name);
            return;
        }

        int v_len = 0, f_len = 0;
        String[] para;

        while(file.hasNextLine()){
            para = (" " + file.nextLine()).split("\\h+");
            if(para.length < 2 || para[1].startsWith("#")) continue;
            if(para[1].equals("v")) v_len++;
            if(para[1].equals("f")) f_len++;
        }
        file.close();
        try{
            file = new Scanner(new File("objects/" + object_name + "/tinker.obj"));
        } catch(Exception e){
            System.err.printf("Cannot find object: \"%s\"\n", object_name);
            return;
        }
        v = new Vector[v_len];
        f = new int[f_len][3];
        mtl = new int[f_len];
        v_len = 0;
        f_len = 0;

        int mtl_idx = 0;
        while(file.hasNextLine()){
            para = (" " + file.nextLine()).split("\\h+");
            if(para.length < 2 || para[1].startsWith("#")) continue;

            if(para[1].equals("usemtl")){
                mtl_idx = material.find(para[2]);
                if(mtl_idx == -1){
                    System.err.println("Warning: material " + para[2] + " not found.");
                }
            }
            else if(para[1].equals("v")){
                // vertex
                v[v_len++] = new Vector(Float.parseFloat(para[2]), Float.parseFloat(para[3]), Float.parseFloat(para[4]), 1);
            }
            else if(para[1].equals("f")){
                // face
                // Assume a TinkerCAD object with 3 vertecies each face
                for(int i = 2; i < 5; i++){
                    f[f_len][i - 2] = Integer.parseInt(para[i]);
                }
                mtl[f_len] = mtl_idx;

                f_len++;
            }
        }

    }

    // world space transformation
    public void rotate(Vector axis, float angle){
        if(axis.mag < Consts.epsilon) return;

        // pre-compute common terms
        float ux = axis.at(0) / axis.mag;
        float uy = axis.at(1) / axis.mag;
        float uz = axis.at(2) / axis.mag;
        float x = pos.at(0);
        float y = pos.at(1);
        float z = pos.at(2);
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);
        float component_product = x*ux + y*uy + z*uz;

        // Rotation matrix
        // https://en.wikipedia.org/wiki/Rotation_matrix
        Matrix T_rotate = new Matrix(new float[][]{
                {cos + ux*ux*(1-cos),           ux*uy*(1-cos)-uz*sin,       ux*uz*(1-cos) + uy*sin,     sin*(y*uz - z*uy) - (1 - cos) * ux * component_product + x * (1 - cos)},
                {uy*ux*(1 - cos) + uz * sin,    cos + uy*uy*(1 - cos),      uy*uz*(1 - cos) - ux*sin,   sin*(z*ux - x*uz) - (1 - cos) * uy * component_product + y * (1 - cos)},
                {uz*ux*(1 - cos) - uy*sin,      uz*uy*(1 - cos) + ux*sin,   cos + uz*uz*(1 - cos),      sin*(x*uy - y*ux) - (1 - cos) * uz * component_product + z * (1 - cos)},
                {0, 0, 0, 1}
        });

        T_world = T_rotate.dot(T_world);
    }

    public void rotate(Vector center, Vector axis, float angle){
        if(axis.mag < Consts.epsilon) return;

        // pre-compute common terms
        float ux = axis.at(0) / axis.mag;
        float uy = axis.at(1) / axis.mag;
        float uz = axis.at(2) / axis.mag;
        float x = center.at(0);
        float y = center.at(1);
        float z = center.at(2);
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);
        float component_product = x*ux + y*uy + z*uz;

        // Rotation matrix
        // https://en.wikipedia.org/wiki/Rotation_matrix
        Matrix T_rotate = new Matrix(new float[][]{
                {cos + ux*ux*(1-cos),           ux*uy*(1-cos)-uz*sin,       ux*uz*(1-cos) + uy*sin,     sin*(y*uz - z*uy) - (1 - cos) * ux * component_product + x * (1 - cos)},
                {uy*ux*(1 - cos) + uz * sin,    cos + uy*uy*(1 - cos),      uy*uz*(1 - cos) - ux*sin,   sin*(z*ux - x*uz) - (1 - cos) * uy * component_product + y * (1 - cos)},
                {uz*ux*(1 - cos) - uy*sin,      uz*uy*(1 - cos) + ux*sin,   cos + uz*uz*(1 - cos),      sin*(x*uy - y*ux) - (1 - cos) * uz * component_product + z * (1 - cos)},
                {0, 0, 0, 1}
        });

        T_world = T_rotate.dot(T_world);
    }

    public void move(Vector trail){
        Matrix T = new Matrix(new float[][]{
                {1, 0, 0, trail.at(0)},
                {0, 1, 0, trail.at(1)},
                {0, 0, 1, trail.at(2)},
                {0, 0, 0, 1}
        });

        T_world = T.dot(T_world);
        pos = T.dot(pos);
    }

    public void move(float x, float y, float z){
        Matrix T = new Matrix(new float[][]{
                {1, 0, 0, x},
                {0, 1, 0, y},
                {0, 0, 1, z},
                {0, 0, 0, 1}
        });

        T_world = T.dot(T_world);
        pos = T.dot(pos);
    }

    public void cd(Vector destination){
        move(destination.subtract(getPos()));
    }

    public void scale(float scale){
        T_model = new Matrix(new float[][]{
                {scale, 0, 0, 0},
                {0, scale, 0, 0},
                {0, 0, scale, 0},
                {0, 0, 0, 1}
        }).dot(T_model);
    }

    public void set_origin(float x, float y, float z){
        T_model = new Matrix(new float[][]{
                {1, 0, 0, -x},
                {0, 1, 0, -y},
                {0, 0, 1, -z},
                {0, 0, 0, 1}
        }).dot(T_model);
    }

    // returns the 3-d version of pos vector — the intended vector to be used
    public Vector getPos(){
        return new Vector(pos.at(0), pos.at(1), pos.at(2));
    }

    public void auto_origin(){
        Vector mean = new Vector(0, 0, 0, 0);
        for (Vector vertex : v) mean = mean.add(T_model.dot(vertex));
        mean = mean.mult(1/(float)v.length);
        set_origin(mean.at(0), mean.at(1), mean.at(2));
    }
}
