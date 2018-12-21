public class Profiler {
    SDRaytracer sdRaytracer;
    int maxRec;
    Profiler(SDRaytracer sdr, int maxR){
        sdRaytracer = sdr;
        maxRec = maxR;
    }

    void profileRenderImage() {
        long end, start, time;

        sdRaytracer.renderImage(); // initialisiere Datenstrukturen, erster Lauf verfälscht sonst Messungen

        for (int procs = 1; procs < 6; procs++) {

            maxRec = procs - 1;
            System.out.print(procs);
            for (int i = 0; i < 10; i++) {
                start = System.currentTimeMillis();

                sdRaytracer.renderImage();

                end = System.currentTimeMillis();
                time = end - start;
                System.out.print(";" + time);
            }
            System.out.println("");
        }
    }
}
