package algebra;

public class Vector extends Object{
    public int shape;
    public float mag;
    public float[] body;

    public Vector(float... body){
        this.shape = body.length;
        this.body = body;
        /*
        this.body = new float[body.length];
        for(int i = 0; i < body.length; this.body[i] = body[i++]);
         */
        for(int i = 0; i < this.shape; this.mag += body[i] * body[i++]);
        this.mag = (float) Math.sqrt(this.mag);
    }

    public float dot(Vector nxt) throws ArithmeticException{
        if(this.shape != nxt.shape)
            throw new ArithmeticException(
                    "Performing dot product between a " + this.shape + "-d Vector and a " + nxt.shape + "-d Vector"
            );
        float result = 0;
        for(int i = 0; i < this.shape; i++)
            result += this.body[i] * nxt.body[i];
        return result;
    }

    public Vector cross(Vector nxt){
        if(this.shape != 3 && this.shape != 4 || nxt.shape != 3 && nxt.shape != 4)
            throw new ArithmeticException("Performing cross product for Vectors that are not 3-d or 4-d homogeneous");
        if(this.shape == 4 || nxt.shape == 4)
            return new Vector(
                    this.body[1] * nxt.body[2] - this.body[2] * nxt.body[1],
                    this.body[2] * nxt.body[0] - this.body[0] * nxt.body[2],
                    this.body[0] * nxt.body[1] - this.body[1] * nxt.body[0],
                    1
            );
        return new Vector(
                this.body[1] * nxt.body[2] - this.body[2] * nxt.body[1],
                this.body[2] * nxt.body[0] - this.body[0] * nxt.body[2],
                this.body[0] * nxt.body[1] - this.body[1] * nxt.body[0]
        );
    }

    public Vector add(Vector nxt){
        if(this.shape != nxt.shape)
            throw new ArithmeticException(
                    "Performing element-wise operation between a " + this.shape + "-d Vector and a " + nxt.shape + "-d Vector"
            );
        float[] v = new float[shape];
        for(int i = 0; i < shape; i++)
            v[i] = body[i] + nxt.body[i];
        return new Vector(v);
    }

    public Vector subtract(Vector nxt){
        if(this.shape != nxt.shape)
            throw new ArithmeticException(
                    "Performing element-wise operation between a " + this.shape + "-d Vector and a " + nxt.shape + "-d Vector"
            );
        float[] v = new float[shape];
        for(int i = 0; i < shape; i++)
            v[i] = body[i] - nxt.body[i];
        return new Vector(v);
    }

    public Vector mult(float n){
        float[] v = new float[shape];
        for(int i = 0; i < shape; i++)
            v[i] = n * body[i];
        return new Vector(v);
    }

    public float at(int i){
        return body[i];
    }

    public String toString(){
        String str = "[";
        for(int i = 0; i < body.length - 1; i++){
            str += body[i] + ", ";
        }
        if(body.length > 0) str += body[body.length - 1];
        str += "]";
        return str;
    }
}
