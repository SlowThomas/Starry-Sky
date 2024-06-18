import algebra.Vector;
import engine.*;

import java.awt.*;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.event.*;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Scanner;

public class Starry_Sky extends JPanel implements Runnable, KeyListener, MouseListener, MouseMotionListener{

    private static class Record_Board {
        public int[] scores = new int[3];
        public boolean[] survived = new boolean[3];

        public Record_Board() throws IOException {
            Scanner fin = new Scanner(new File("data/records.txt"));
            for(int i = 0; i < 3; i++){
                if(!fin.hasNextInt()) {
                    scores[i] = -1;
                    continue;
                }
                scores[i] = fin.nextInt();
                survived[i] = fin.nextBoolean();
            }
            fin.close();
        }

        public String get_score(int idx){
            if(scores[idx] == -1) {
                return "-";
            }
            return "" + scores[idx];
        }

        public boolean record(int score, boolean survived){ // returns whether new record is achieved
            for(int i = 0; i < 3; i++){
                if(scores[i] < score || scores[i] == score && !this.survived[i] && survived){
                    for(int j = 2; j > i; j--){
                        scores[j] = scores[j - 1];
                        this.survived[j] = this.survived[j - 1];
                    }
                    scores[i] = score;
                    this.survived[i] = survived;
                    return i == 0;
                }
            }
            return false;
        }

        public void save(){
            try{
                PrintWriter fout = new PrintWriter(new FileWriter("data/records.txt"));
                for(int i = 0; i < 3 && scores[i] != -1; i++){
                    fout.println(scores[i] + " " + survived[i]);
                }
                fout.close();
            }
            catch(Exception e){ System.err.println(e.getMessage()); }
        }
    }

    public static class Calc implements Runnable {
        public Real_Obj plane;
        public Real_Obj plane_acc;

        public Flat_Obj enemy_proto;
        public Flat_Obj bullet_proto;
        public Flat_Obj enemy_bullet_proto;

        public Label_Obj crosshair;
        public Label_Obj origin_label;
        public Label_Obj enemy_label;

        public Camera camera;
        public Camera camera2;
        public Camera plane_origin;

        public Scene scene;

        public LinkedList<Bullet> bullets = new LinkedList<>();
        public ListIterator<Bullet> bullet_it;
        public Bullet bullet;

        public LinkedList<Enemy> enemies = new LinkedList<>();
        public ListIterator<Enemy> enemy_it;
        public Enemy enemy;

        public LinkedList<Enemy_Bullet> enemy_bullets = new LinkedList<>();
        public ListIterator<Enemy_Bullet> enemy_bullet_it;
        public Enemy_Bullet enemy_bullet;

        public int fps = 0;

        public int camera_mode = 1;

        public double mouse_dx;
        public double mouse_dy;
        public boolean[] pressed_keys = new boolean['z' + 1];
        public boolean mouse_left_down = false;
        public boolean mouse_right_down = false;

        public float rot_speed_max;
        public float rot_acc;
        public float rot_speed_x = 0;
        public float rot_speed_y = 0;
        public float rot_speed_z = 0;

        public boolean accelerating;
        public boolean decelerating;

        public Vector velocity = new Vector(0, 0, 0);
        public Vector x_norm;
        public Vector y_norm;
        public Vector z_norm;

        public int enemy_timer = 0;
        public int invincible_timer = 0;

        // --------------------------- parameters -----------------------------

        public long update_time = 20;

        public double sensitivity = 0.005;
        public float acc = 2f;
        public int invincible_time = 5000;

        public int enemy_amount = 3;

        public int score = 0;
        public int life = 3;

        public static class Bullet{
            public Vector pos;
            public Vector velocity;
            public long time = 10000; // bullet life

            public Bullet(Vector pos, Vector velocity){
                this.pos = pos;
                this.velocity = velocity;
            }

            public void update(long delta){
                time -= delta;
                pos = pos.add(velocity.mult(delta / 50f));
            }
        }

        public static class Enemy_Bullet{
            public Vector pos;
            public Vector velocity;
            public Vector original_velocity;
            public float velocity_mag = 1000;
            public long time = 10000; // bullet life

            public Enemy_Bullet(Vector pos, Vector velocity){
                this.pos = pos;
                this.velocity = velocity;
                original_velocity = velocity;
            }

            public void update(long delta, Camera plane_origin){
                time -= delta;
                pos = pos.add(velocity.mult(delta / 50f));
                if(velocity.dot(original_velocity) > 1e-10){
                    velocity = velocity.add(plane_origin.getPos().subtract(pos).mult(1f / 1000f));
                    if(velocity.mag > velocity_mag){
                        velocity = velocity.mult(velocity_mag / velocity.mag);
                    }
                }
            }
        }

        public static class Enemy{
            public Vector pos;
            public long fire_time_delta = 7000;
            public float bullet_speed = 500;
            public int bullet_quantity = 5;
            public long fire_countdown = fire_time_delta;

            public Enemy(Vector pos){
                this.pos = pos;
            }

            public void update(long delta){
                fire_countdown -= delta;
            }
        }


        public Calc(){
            plane = new Real_Obj("Ship");
            plane.scale(3);
            plane_acc = new Real_Obj("Ship_Accelerating");
            plane_acc.scale(3);

            enemy_proto = new Flat_Obj("ghost_red.png");
            bullet_proto = new Flat_Obj("Bullet.png");
            bullet_proto.scale(0.5);
            enemy_bullet_proto = new Flat_Obj("purple_lightning.png");
            enemy_bullet_proto.scale(2);

            crosshair = new Label_Obj("crosshair.png");
            crosshair.scale(0.8);
            origin_label = new Label_Obj("white_label.png");
            origin_label.scale(0.5);
            enemy_label = new Label_Obj("red_label.png");
            enemy_label.scale(0.5);

            camera = new Camera(0, 0, -530);
            camera2 = new Camera(0, 100, -3000);
            plane_origin = new Camera(0, 0, 0);

            scene = new Scene(800, 450, 5);
            scene.mount_camera(camera2);
        }

        public float adjust(float n, long time){
            return n * time / 1000f * update_time;
        }

        public Vector adjust(Vector v, long time){
            return v.mult(time / 1000f * update_time);
        }

        public void rotate(Vector axis, float angle){
            plane.rotate(axis, angle);
            plane_acc.rotate(axis, angle);
            plane_origin.rotate(axis, angle);
            camera.rotate(plane_origin.getPos(), axis, angle);
        }

        public void move(Vector velocity){
            plane.move(velocity);
            plane_acc.move(velocity);
            camera.move(velocity);
            camera2.move(velocity);
            plane_origin.move(velocity);
        }

        public void shoot(){
            bullets.add(new Bullet(plane_origin.getPos(), velocity.add(z_norm.mult(1000))));
        }

        public void epoch(long time){
            if(camera_mode == 0){
                rot_speed_max = 0.01f;
                rot_acc = adjust(0.001f, time);
            }
            else if(camera_mode == 1){
                rot_speed_max = 0.1f;
                rot_acc = adjust(0.01f, time);
            }

            if(score < 2){
                enemy_amount = 1;
            }
            else if(score < 5){
                enemy_amount = 2;
            }
            else{
                enemy_amount = 3;
            }

            // Generate enemy
            if(enemy_timer <= 0 && enemies.size() < enemy_amount){
                enemy_timer = 2000 + 10000 / (score / 10 + 1);

                float alpha = (float)(Math.random() * 2 * Math.PI);
                float beta = (float)(Math.random() * 2 * Math.PI);
                Vector dir = new Vector((float)(-Math.cos(beta) * Math.sin(alpha)), (float)(-Math.sin(beta)), (float)(Math.cos(alpha) * Math.cos(beta)));
                dir = dir.mult((float)(Math.random() * 1e5 + 1e4));
                enemies.add(new Enemy(plane_origin.getPos().add(dir)));
            }
            if(enemies.size() < enemy_amount){ enemy_timer -= (int) time;}

            x_norm = plane_origin.x_norm;
            y_norm = plane_origin.y_norm;
            z_norm = plane_origin.z_norm;

            if(pressed_keys['w']){
                if(rot_speed_x < 0) rot_speed_x = 0;
                rot_speed_x += rot_acc;
            }
            if(pressed_keys['s']){
                if(rot_speed_x > 0) rot_speed_x = 0;
                rot_speed_x -= rot_acc;
            }
            if(pressed_keys['a']){
                if(rot_speed_z < 0) rot_speed_z = 0;
                rot_speed_z += rot_acc;
            }
            if(pressed_keys['d']){
                if(rot_speed_z > 0) rot_speed_z = 0;
                rot_speed_z -= rot_acc;
            }
            if(pressed_keys['q']){
                if(rot_speed_y > 0) rot_speed_y = 0;
                rot_speed_y -= rot_acc;
            }
            if(pressed_keys['e']){
                if(rot_speed_y < 0) rot_speed_y = 0;
                rot_speed_y += rot_acc;
            }
            if(!pressed_keys['w'] && !pressed_keys['s']) rot_speed_x = 0;
            if(!pressed_keys['a'] && !pressed_keys['d']) rot_speed_z = 0;
            if(!pressed_keys['q'] && !pressed_keys['e']) rot_speed_y = 0;
            rot_speed_x = Math.max(Math.min(rot_speed_x, rot_speed_max), -rot_speed_max);
            rot_speed_y = Math.max(Math.min(rot_speed_y, rot_speed_max), -rot_speed_max);
            rot_speed_z = Math.max(Math.min(rot_speed_z, rot_speed_max), -rot_speed_max);

            accelerating = mouse_left_down || pressed_keys['j'];
            decelerating = mouse_right_down;

            if(accelerating){
                velocity = velocity.add(z_norm.mult(adjust(acc, time)));
            }
            if(decelerating){
                velocity = velocity.subtract(z_norm.mult(adjust(acc, time)));
            }


            if(pressed_keys['1']){
                camera_mode = 0;
            }
            if(pressed_keys['2']){
                camera_mode = 1;
            }

            if(camera_mode == 0){
                scene.mount_camera(camera);
            }
            else if(camera_mode == 1){
                scene.mount_camera(camera2);
                camera2.rotate(plane_origin.getPos(), camera2.y_norm, (float) (sensitivity * mouse_dx));
                camera2.rotate(plane_origin.getPos(), camera2.x_norm, (float) (sensitivity * mouse_dy));
            }
            mouse_dx = 0;
            mouse_dy = 0;

            if(pressed_keys[' '] || pressed_keys['k']) shoot();

            // ------------------------- update -------------------------------------

            rotate(x_norm, adjust(rot_speed_x, time));
            rotate(y_norm, adjust(rot_speed_y, time));
            rotate(z_norm, adjust(rot_speed_z, time));
            move(adjust(velocity, time));
            crosshair.cd(plane.getPos());
            crosshair.move(z_norm.mult(10000f)); // 100 meters
            if(!bullets.isEmpty()){
                bullet_it = bullets.listIterator();
                while(bullet_it.hasNext()){
                    bullet = bullet_it.next();
                    if(bullet.time <= 0) {
                        bullet_it.remove();
                        continue;
                    }
                    bullet.update(time);
                }
            }
            if(!enemies.isEmpty()){
                enemy_it = enemies.listIterator();
                while(enemy_it.hasNext()){
                    enemy = enemy_it.next();
                    if(enemy.fire_countdown <= 0){
                        enemy.fire_countdown = enemy.fire_time_delta;
                        Vector enemy_bullet_v = plane_origin.getPos().subtract(enemy.pos);
                        for(int i = 0; i < enemy.bullet_quantity; i++){
                            Vector this_bullet_v = enemy_bullet_v.add(velocity.mult((float)Math.random() * 1000));
                            if(this_bullet_v.mag < 1e-11) enemy_bullets.add(new Enemy_Bullet(enemy.pos, this_bullet_v));
                            else enemy_bullets.add(new Enemy_Bullet(enemy.pos, this_bullet_v.mult(enemy.bullet_speed / this_bullet_v.mag)));
                        }
                    }
                    enemy.update(time);
                    if(!bullets.isEmpty()){
                        bullet_it = bullets.listIterator();
                        while(bullet_it.hasNext()) {
                            bullet = bullet_it.next();
                            double distance = 140;
                            if(
                                    bullet.pos.subtract(enemy.pos).cross(bullet.velocity).mag <= bullet.velocity.mag * distance &&
                                            ((bullet.pos.subtract(enemy.pos).dot(bullet.velocity) > 0) != (bullet.pos.subtract(bullet.velocity).subtract(enemy.pos).dot(bullet.velocity) > 0))
                            ){
                                enemy_it.remove();
                                score++;
                                break;
                            }
                        }
                    }
                }
            }
            if(!enemy_bullets.isEmpty()){
                enemy_bullet_it = enemy_bullets.listIterator();
                while(enemy_bullet_it.hasNext()){
                    enemy_bullet = enemy_bullet_it.next();
                    double distance = 150;
                    if(
                            invincible_timer == 0 &&
                                    enemy_bullet.pos.subtract(plane_origin.getPos()).cross(enemy_bullet.velocity).mag <= enemy_bullet.velocity.mag * distance &&
                                    ((enemy_bullet.pos.subtract(plane_origin.getPos()).dot(enemy_bullet.velocity) > 0) != (enemy_bullet.pos.subtract(enemy_bullet.velocity).subtract(plane_origin.getPos()).dot(enemy_bullet.velocity) > 0))
                    ){
                        life--;
                        invincible_timer = invincible_time;
                    }
                    invincible_timer -= time;
                    invincible_timer = Math.max(0, invincible_timer);

                    if(enemy_bullet.time <= 0){
                        enemy_bullet_it.remove();
                        continue;
                    }
                    enemy_bullet.update(time, plane_origin);
                }
            }

            // ------------------------- rendering ------------------------------
            scene.rasterize_indicator(origin_label);
            if(camera_mode != 0){
                if(accelerating)
                    scene.rasterize(plane_acc);
                else
                    scene.rasterize(plane);
            }
            scene.rasterize(crosshair);
            for(Bullet bullet : bullets){
                bullet_proto.cd(bullet.pos);
                scene.rasterize(bullet_proto);
            }
            for(Enemy enemy : enemies){
                enemy_proto.cd(enemy.pos);
                enemy_label.cd(enemy.pos);
                scene.rasterize(enemy_proto);
                scene.rasterize_indicator(enemy_label);
            }
            for(Enemy_Bullet enemy_bullet : enemy_bullets){
                enemy_bullet_proto.cd(enemy_bullet.pos);
                scene.rasterize(enemy_bullet_proto);
            }

            scene.render();
        }

        private boolean running = true;
        public void terminate(){
            running = false;
        }

        public void run() {
            long start = System.currentTimeMillis(), end = start, time;

            while(running){
                time = end - start;
                start = end;
                if(time <= update_time){
                    fps = (int)(1000f / update_time + 0.5);
                    try { Thread.sleep(update_time - time); }
                    catch(Exception e){}
                    epoch(update_time);
                }
                else{
                    fps = (int)(1000f / time + 0.5);
                    epoch(time);
                }
                end = System.currentTimeMillis();
            }
        }
    }

    public Robot automation;
    public Cursor blankCursor;

    public Scene scene;
    public Calc calc;

    Thread in_game_processing;


    private void initialize_game(){
        automation.mouseMove(getLocationOnScreen().x + getWidth() / 2, getLocationOnScreen().y + getHeight() / 2);

        calc = new Calc();
        scene = calc.scene;

        in_game_processing = new Thread(calc);
        in_game_processing.start();
        // -----------------------------------------


        this.setCursor(blankCursor);
    }

    private void end_game(){
        calc.terminate();
        calc = null;

        this.setCursor(Cursor.getDefaultCursor());
    }

    // Assets
    private final BufferedImage main_screen;
    private final BufferedImage rules_screen;
    private final BufferedImage controls_screen;
    private final BufferedImage credits_screen;
    private final BufferedImage game_over_screen;
    private final BufferedImage special_ending_screen;
    private final BufferedImage records_screen;

    // System Stats
    private int game_state = 0;
    private final boolean[] pressed_keys = new boolean['z' + 1];

    private final Record_Board record_board;
    private int score;
    private boolean new_high;


    public Starry_Sky() throws IOException, AWTException {
        setPreferredSize(new Dimension(800, 450));
        // Add KeyListener
        this.setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        // Add Thread
        Thread thread = new Thread(this);
        thread.start();

        // Load image
        main_screen = ImageIO.read(new File("img/Main.png"));
        rules_screen = ImageIO.read(new File("img/Rules.png"));
        controls_screen = ImageIO.read(new File("img/Controls.png"));
        credits_screen = ImageIO.read(new File("img/Credits.png"));
        game_over_screen = ImageIO.read(new File("img/Game_Over.png"));
        special_ending_screen = ImageIO.read(new File("img/Survival.png"));
        records_screen = ImageIO.read(new File("img/Records.png"));

        record_board = new Record_Board();

        automation = new Robot();


        blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "blank cursor");

    }



    public void run() {
        while(true){
            try { Thread.sleep(20); }
            catch(Exception e){}

            repaint();
        }
    }

    public void paintComponent(Graphics g){
        super.paintComponent(g);

        if(game_state == 0){
            g.drawImage(main_screen, 0, 0, null);
        }
        else if(game_state == 1){
            g.drawImage(rules_screen, 0, 0, null);
        }
        else if(game_state == 2){
            g.drawImage(controls_screen, 0, 0, null);
        }
        else if(game_state == 3){
            g.drawImage(credits_screen, 0, 0, null);
        }
        else if(game_state == 4){
            in_game_event_handler(g);
        }
        else if(game_state == 5){
            g.drawImage(game_over_screen, 0, 0, null);
        }
        else if(game_state == 6){
            g.drawImage(special_ending_screen, 0, 0, null);
        }
        else if(game_state == 7){
            g.drawImage(records_screen, 0, 0, null);
            g.setColor(new Color(255, 255, 255));
            g.setFont(new Font("Courier New", Font.PLAIN, 28));
            g.drawString(record_board.get_score(0), 351, 234);
            g.drawString(record_board.get_score(1), 351, 276);
            g.drawString(record_board.get_score(2), 351, 318);

            g.setColor(new Color(255, 0, 0));
            g.setFont(new Font("Courier New", Font.PLAIN, 21));
            if(record_board.survived[0]) g.drawString("survived!", 400, 230);
            if(record_board.survived[1]) g.drawString("survived!", 400, 272);
            if(record_board.survived[2]) g.drawString("survived!", 400, 314);

            g.setColor(new Color(255, 255, 255));
            g.setFont(new Font("Courier New", Font.PLAIN, 28));
            g.drawString("" + score, 405, 370);

            g.setColor(new Color(255, 0, 0));
            g.setFont(new Font("Courier New", Font.BOLD, 21));
            if(new_high) g.drawString("new record!", 333, 150);
        }

    }

    public int last_score = 0;
    public long notification_countdown = 0;

    private void in_game_event_handler(Graphics g){
        if(calc == null){
            initialize_game();
        }

        if(calc.life <= 0){
            game_state = 5;
            score = calc.score;
            new_high = record_board.record(score, false);
            record_board.save();
            end_game();
            return;
        }

        if(calc.plane_origin.getPos().mag > 1e6){
            game_state = 6;
            score = calc.score;
            new_high = record_board.record(score, true);
            record_board.save();
            end_game();
            return;
        }

        g.drawImage(scene.canvas, 0, 0, null);
        g.setColor(new Color(255, 255, 255));
        if(calc.camera_mode == 0){
            g.drawString("Aiming Mode", 50, 100);
        }
        else if(calc.camera_mode == 1){
            g.drawString("Maneuver Mode", 50, 100);
        }
        g.drawString(calc.fps + " FPS", 10, 20);
        g.drawString("score: " + calc.score, 10, 50);
        g.drawString("life: " + calc.life, 10, 70);

        g.setColor(new Color(50, 50, 50));
        g.drawArc(getWidth() / 2 - 200, getHeight() / 2 - 200, 400, 400, 0, 360);

        if(calc.score > last_score) notification_countdown = 60;
        if(notification_countdown > 0){
            g.setColor(new Color(255, 0, 0));
            g.drawString("Eliminated", 350, 70);
            notification_countdown--;
        }
        last_score = calc.score;
    }

    public static void main(String[] args) throws IOException, AWTException{
        JFrame frame = new JFrame("Starry Sky");
        Starry_Sky panel = new Starry_Sky();
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
    }

    // Input Handling
    public boolean dragging = true;

    public void keyPressed(KeyEvent e) {
        if(e.getKeyChar() <= 'z') pressed_keys[e.getKeyChar()] = true;
        if(calc != null){
            if(e.getKeyChar() <= 'z') calc.pressed_keys[e.getKeyChar()] = true;

            if(e.getKeyCode() == 16){
                this.setCursor(Cursor.getDefaultCursor());
                dragging = false;
            }
        }
    }
    public void keyReleased(KeyEvent e) {
        if(e.getKeyChar() <= 'z') pressed_keys[e.getKeyChar()] = false;
        if(calc != null){
            if(e.getKeyChar() <= 'z') calc.pressed_keys[e.getKeyChar()] = false;

            if(e.getKeyCode() == 16){
                this.setCursor(blankCursor);
                automation.mouseMove(getLocationOnScreen().x + getWidth() / 2, getLocationOnScreen().y + getHeight() / 2);
                dragging = true;
            }
        }
    }

    private boolean on_button(MouseEvent e, int x, int y, int width, int height){
        return e.getButton() == 1 &&
                e.getX() >= x && e.getX() <= x + width &&
                e.getY() >= y && e.getY() <= y + height;
    }
    public void mouseClicked(MouseEvent e) {
        if(game_state == 0){
            if(on_button(e, 371, 174, 58, 29)){ game_state = 4; } // start button
            else if(on_button(e, 368, 233, 64, 29)){ game_state = 1;} // rules button
            else if(on_button(e, 350, 292, 100, 29)){ game_state = 2;} // controls button
            else if(on_button(e, 358, 351, 85, 29)){ game_state = 3;} // credits button
        }
        else if(game_state == 1 || game_state == 2 || game_state == 3 || game_state == 7){
            game_state = 0;
        }
        else if(game_state == 5 || game_state == 6){
            game_state = 7;
        }
    }

    public void mousePressed(MouseEvent e) {
        if(calc != null){
            if(e.getButton() == 1){
                calc.mouse_left_down = true;
            }
            if(e.getButton() == 3){
                calc.mouse_right_down = true;
            }
        }
    }
    public void mouseReleased(MouseEvent e) {
        if(calc != null){
            if(e.getButton() == 1){
                calc.mouse_left_down = false;
            }
            if(e.getButton() == 3){
                calc.mouse_right_down = false;
            }
        }
    }

    public void rotate_cam(MouseEvent e){
        if(calc != null && dragging) {
            calc.mouse_dx += e.getX() - getWidth() / 2.0;
            calc.mouse_dy += e.getY() - getHeight() / 2.0;

            automation.mouseMove(getLocationOnScreen().x + getWidth() / 2, getLocationOnScreen().y + getHeight() / 2);
        }
    }
    public void mouseDragged(MouseEvent e) {
        rotate_cam(e);
    }
    public void mouseMoved(MouseEvent e) {
        rotate_cam(e);
    }

    // Useless Methods
    public void keyTyped(KeyEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
}
