
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Dimension;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/* Implementation of a very simple Raytracer
   Stephan Diehl, Universit�t Trier, 2010-2016
*/


public class SDRaytracer extends JFrame {
    private static final long serialVersionUID = 1L;
    boolean profiling = false;
    int width = 1000;
    int height = 1000;

    Future[] futureList = new Future[width];
    int nrOfProcessors = Runtime.getRuntime().availableProcessors();
    ExecutorService eservice = Executors.newFixedThreadPool(nrOfProcessors);

    int maxRec = 3;
    int rayPerPixel = 1;
    int startX, startY, startZ;

    Scene scene;

    List<Triangle> triangles;

    Light mainLight = new Light(new Vec3D(0, 100, 0), new RGB(0.1f, 0.1f, 0.1f));

    Light lights[] = new Light[]{mainLight
            , new Light(new Vec3D(100, 200, 300), new RGB(0.5f, 0, 0.0f))
            , new Light(new Vec3D(-100, 200, 300), new RGB(0.0f, 0, 0.5f))
            //,new Light(new Vec3D(-100,0,0), new RGB(0.0f,0.8f,0.0f))
    };

    RGB[][] image = new RGB[width][height];

    float fovx = (float) 0.628;
    float fovy = (float) 0.628;
    RGB ambient_color = new RGB(0.01f, 0.01f, 0.01f);
    RGB black = new RGB(0.0f, 0.0f, 0.0f);
    int y_angle_factor = 4, x_angle_factor = -4;

    public static void main(String argv[]) {
        long start = System.currentTimeMillis();
        SDRaytracer sdr = new SDRaytracer();
        long end = System.currentTimeMillis();
        long time = end - start;
        System.out.println("time: " + time + " ms");
        System.out.println("nrprocs=" + sdr.nrOfProcessors);
    }

    SDRaytracer() {
        scene = new Scene();
        triangles = scene.createScene(x_angle_factor, y_angle_factor);

        if (!profiling) renderImage();
        else {
            new Profiler(this, maxRec).profileRenderImage();
        }

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout());
        JPanel area = new JPanel() {
            public void paint(Graphics g) {
                System.out.println("fovx=" + fovx + ", fovy=" + fovy + ", xangle=" + x_angle_factor + ", yangle=" + y_angle_factor);
                if (image == null) return;
                for (int i = 0; i < width; i++)
                    for (int j = 0; j < height; j++) {
                        g.setColor(image[i][j].color());
                        // zeichne einzelnen Pixel
                        g.drawLine(i, height - j, i, height - j);
                    }
            }
        };

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                boolean redraw = false;
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    x_angle_factor--;
                    //mainLight.position.y-=10;
                    //fovx=fovx+0.1f;
                    //fovy=fovx;
                    //maxRec--; if (maxRec<0) maxRec=0;
                    redraw = true;
                }
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    x_angle_factor++;
                    //mainLight.position.y+=10;
                    //fovx=fovx-0.1f;
                    //fovy=fovx;
                    //maxRec++;if (maxRec>10) return;
                    redraw = true;
                }
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    y_angle_factor--;
                    //mainLight.position.x-=10;
                    //startX-=10;
                    //fovx=fovx+0.1f;
                    //fovy=fovx;
                    redraw = true;
                }
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    y_angle_factor++;
                    //mainLight.position.x+=10;
                    //startX+=10;
                    //fovx=fovx-0.1f;
                    //fovy=fovx;
                    redraw = true;
                }
                if (redraw) {
                    triangles = scene.createScene(x_angle_factor, y_angle_factor);
                    renderImage();
                    repaint();
                }
            }
        });

        area.setPreferredSize(new Dimension(width, height));
        contentPane.add(area);
        this.pack();
        this.setVisible(true);
    }

    double tan_fovx;
    double tan_fovy;

    void renderImage() {
        tan_fovx = Math.tan(fovx);
        tan_fovy = Math.tan(fovy);
        for (int i = 0; i < width; i++) {
            futureList[i] = eservice.submit(new RaytraceTask(this, i));
        }

        for (int i = 0; i < width; i++) {
            try {
                RGB[] col = (RGB[]) futureList[i].get();
                for (int j = 0; j < height; j++)
                    image[i][j] = col[j];
            } catch (InterruptedException e) {
            } catch (ExecutionException e) {
            }
        }
    }


    RGB rayTrace(Ray ray, int rec) {
        if (rec > maxRec) return black;
        IPoint ip = ray.hitObject(triangles);  // (ray, p, n, triangle);
        if (ip.dist > IPoint.epsilon)
            return lighting(ray, ip, rec);
        else
            return black;
    }

    RGB lighting(Ray ray, IPoint ip, int rec) {
        Vec3D point = ip.ipoint;
        Triangle triangle = ip.triangle;
        RGB color = triangle.color;
        color.addColors(ambient_color, 1);
        Ray shadow_ray = new Ray();
        for (Light light : lights) {
            shadow_ray.start = point;
            shadow_ray.dir = light.position.minus(point).mult(-1);
            shadow_ray.dir.normalize();
            IPoint ip2 = shadow_ray.hitObject(triangles);
            if (ip2.dist < IPoint.epsilon) {
                float ratio = Math.max(0, shadow_ray.dir.dot(triangle.normal));
                color.addColors(light.color, ratio);
            }
        }
        Ray reflection = new Ray();
        //R = 2N(N*L)-L)    L ausgehender Vektor
        Vec3D L = ray.dir.mult(-1);
        reflection.start = point;
        reflection.dir = triangle.normal.mult(2 * triangle.normal.dot(L)).minus(L);
        reflection.dir.normalize();
        RGB rcolor = rayTrace(reflection, rec + 1);
        float ratio = (float) Math.pow(Math.max(0, reflection.dir.dot(L)), triangle.shininess);
        color.addColors(rcolor, ratio);
        return (color);
    }

}









