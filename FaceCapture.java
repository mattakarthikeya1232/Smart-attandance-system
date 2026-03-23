import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;

public class FaceCapture {

    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    // ---------- Public API (same names you already call) ----------

    // Teacher flow: capture with preview, allow retake, save to faces/<user>.jpg
    public static void registerFace(String username) {
        ensureFacesDir();
        String path = "faces/" + username + ".jpg";

        VideoCapture cam = new VideoCapture(0);
        if (!cam.isOpened()) {
            JOptionPane.showMessageDialog(null, "Camera not found.", "Camera Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            while (true) {
                Mat frame = captureFrame(cam);
                if (frame == null) {
                    JOptionPane.showMessageDialog(null, "Failed to capture frame.", "Capture Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int choice = showPreviewDialog(frame, "Preview — Register Face for " + username);
                if (choice == JOptionPane.CANCEL_OPTION) {
                    // cancel -> do not save
                    return;
                } else if (choice == JOptionPane.YES_OPTION) {
                    // OK -> save
                    Imgcodecs.imwrite(path, frame);
                    JOptionPane.showMessageDialog(null, "Saved: " + path);
                    return;
                } else {
                    // Retake -> loop again (delete any prior shot if existed)
                    new File(path).delete();
                }
            }
        } finally {
            cam.release();
        }
    }

    // Student flow: capture with preview, allow retake, then compare with saved face
    public static boolean verifyFace(String username) {
        String savedPath = "faces/" + username + ".jpg";
        Mat saved = Imgcodecs.imread(savedPath);
        if (saved.empty()) {
            JOptionPane.showMessageDialog(null,
                    "No registered face found for " + username + ". Ask your teacher to register first.",
                    "Not Registered", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        VideoCapture cam = new VideoCapture(0);
        if (!cam.isOpened()) {
            JOptionPane.showMessageDialog(null, "Camera not found.", "Camera Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            while (true) {
                Mat live = captureFrame(cam);
                if (live == null) {
                    JOptionPane.showMessageDialog(null, "Failed to capture frame.", "Capture Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                int choice = showPreviewDialog(live, "Preview — Mark Attendance");
                if (choice == JOptionPane.CANCEL_OPTION) {
                    return false; // user aborted
                } else if (choice == JOptionPane.YES_OPTION) {
                    // OK -> compare now
                    boolean match = compareByGrayHistogram(saved, live);
                    return match;
                } else {
                    // Retake -> loop again to recapture
                }
            }
        } finally {
            cam.release();
        }
    }

    // ---------- Internal helpers ----------

    private static void ensureFacesDir() {
        new File("faces").mkdirs();
    }

    private static Mat captureFrame(VideoCapture cam) {
        Mat frame = new Mat();
        if (cam.read(frame) && !frame.empty()) {
            return frame;
        }
        return null;
    }

    /**
     * Shows a preview dialog with buttons: [OK] [Retake] [Cancel]
     * Returns:
     *  - JOptionPane.YES_OPTION for OK
     *  - JOptionPane.NO_OPTION for Retake
     *  - JOptionPane.CANCEL_OPTION for Cancel/close
     */
    private static int showPreviewDialog(Mat matBgr, String title) {
        BufferedImage img = matToBufferedImage(matBgr);
        Image scaled = img.getWidth() > 640 || img.getHeight() > 480
                ? img.getScaledInstance(640, -1, Image.SCALE_SMOOTH)
                : img;
        JLabel label = new JLabel(new ImageIcon(scaled));
        label.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        Object[] options = {"OK", "Retake", "Cancel"};
        int idx = JOptionPane.showOptionDialog(
                null,
                label,
                title,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );
        if (idx == 0) return JOptionPane.YES_OPTION;     // OK
        if (idx == 1) return JOptionPane.NO_OPTION;      // Retake
        return JOptionPane.CANCEL_OPTION;                // Cancel or closed
    }

    private static BufferedImage matToBufferedImage(Mat matBgr) {
        Mat matRgb = new Mat();
        Imgproc.cvtColor(matBgr, matRgb, Imgproc.COLOR_BGR2RGB);
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".jpg", matRgb, mob);
        byte[] ba = mob.toArray();
        try {
            return ImageIO.read(new ByteArrayInputStream(ba));
        } catch (Exception e) {
            throw new RuntimeException("Failed converting Mat to BufferedImage", e);
        }
    }

    // Simple similarity by grayscale histogram correlation
    private static boolean compareByGrayHistogram(Mat savedBgr, Mat liveBgr) {
        Mat saved = new Mat();
        Mat live = new Mat();
        Imgproc.cvtColor(savedBgr, saved, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(liveBgr, live, Imgproc.COLOR_BGR2GRAY);

        Mat hist1 = new Mat(), hist2 = new Mat();
        Imgproc.calcHist(java.util.Arrays.asList(saved), new MatOfInt(0), new Mat(),
                hist1, new MatOfInt(256), new MatOfFloat(0, 256));
        Imgproc.calcHist(java.util.Arrays.asList(live), new MatOfInt(0), new Mat(),
                hist2, new MatOfInt(256), new MatOfFloat(0, 256));

        Core.normalize(hist1, hist1);
        Core.normalize(hist2, hist2);

        double score = Imgproc.compareHist(hist1, hist2, Imgproc.HISTCMP_CORREL);
        System.out.println("Similarity score: " + score);
        return score > 0.8; // threshold
    }
}