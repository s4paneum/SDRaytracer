import java.util.ArrayList;
import java.util.List;

public class Scene {
    Scene(){
    }
    List<Triangle> createScene(int x_angle_factor, int y_angle_factor) {
        List<Triangle> triangles = new ArrayList<Triangle>();
        Shape.addCube(triangles, 0, 35, 0, 10, 10, 10, new RGB(0.3f, 0, 0), 0.4f);       //rot, klein
        Shape.addCube(triangles, -70, -20, -20, 20, 100, 100, new RGB(0f, 0, 0.3f), .4f);
        Shape.addCube(triangles, -30, 30, 40, 20, 20, 20, new RGB(0, 0.4f, 0), 0.2f);        // grün, klein
        Shape.addCube(triangles, 50, -20, -40, 10, 80, 100, new RGB(.5f, .5f, .5f), 0.2f);
        Shape.addCube(triangles, -70, -26, -40, 130, 3, 40, new RGB(.5f, .5f, .5f), 0.2f);

        Matrix mRx = Matrix.createXRotation((float) (x_angle_factor * Math.PI / 16));
        Matrix mRy = Matrix.createYRotation((float) (y_angle_factor * Math.PI / 16));
        Matrix mT = Matrix.createTranslation(0, 0, 200);
        Matrix m = mT.mult(mRx).mult(mRy);
        m.print();
        m.apply(triangles);

        return triangles;
    }
}
