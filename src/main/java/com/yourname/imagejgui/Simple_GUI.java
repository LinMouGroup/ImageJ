package com.yourname.imagejgui;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.OvalRoi;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.RankFilters;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class Simple_GUI implements PlugIn {

    private final Font chineseFont = new Font("Microsoft YaHei", Font.PLAIN, 16);
    private final Font titleFont = new Font("Microsoft YaHei", Font.BOLD, 22);

    private JComboBox<String> denoiseMethodBox;
    private JTextField denoiseParameterField;

    private JTextField detectionThresholdField;
    private JTextField localMaxRadiusField;
    private JTextField minDistanceField;

    private final List<Detection> lastDetections = new ArrayList<>();

    @Override
    public void run(String arg) {
        SwingUtilities.invokeLater(this::createAndShowGUI);
    }

    private void createAndShowGUI() {

        JFrame frame = new JFrame("My ImageJ GUI Plugin");

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);

        JLabel title = new JLabel("单颗粒分子识别 GUI Demo", SwingConstants.CENTER);
        title.setFont(titleFont);

        JButton generateButton = new JButton("生成测试图像");
        JButton imageButton = new JButton("检查当前图像");
        JButton denoiseButton = new JButton("执行降噪");
        JButton detectButton = new JButton("识别颗粒");
        JButton trackButton = new JButton("简单追踪");
        JButton closeButton = new JButton("关闭");

        generateButton.setFont(chineseFont);
        imageButton.setFont(chineseFont);
        denoiseButton.setFont(chineseFont);
        detectButton.setFont(chineseFont);
        trackButton.setFont(chineseFont);
        closeButton.setFont(chineseFont);

        denoiseMethodBox = new JComboBox<>(new String[]{
                "Gaussian Blur 高斯滤波",
                "Median Filter 中值滤波"
        });
        denoiseMethodBox.setFont(chineseFont);

        denoiseParameterField = new JTextField("1.0", 6);
        denoiseParameterField.setFont(chineseFont);

        detectionThresholdField = new JTextField("80", 6);
        detectionThresholdField.setFont(chineseFont);

        localMaxRadiusField = new JTextField("2", 6);
        localMaxRadiusField.setFont(chineseFont);

        minDistanceField = new JTextField("6", 6);
        minDistanceField.setFont(chineseFont);

        JLabel denoiseMethodLabel = new JLabel("降噪方法：");
        JLabel denoiseParameterLabel = new JLabel("降噪参数：");
        JLabel thresholdLabel = new JLabel("识别阈值：");
        JLabel radiusLabel = new JLabel("局部极大半径：");
        JLabel minDistanceLabel = new JLabel("最小距离：");

        denoiseMethodLabel.setFont(chineseFont);
        denoiseParameterLabel.setFont(chineseFont);
        thresholdLabel.setFont(chineseFont);
        radiusLabel.setFont(chineseFont);
        minDistanceLabel.setFont(chineseFont);

        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));

        generateButton.addActionListener(e -> generateTestImage(logArea));

        imageButton.addActionListener(e -> readCurrentImage(logArea));

        denoiseButton.addActionListener(e -> denoiseCurrentImage(logArea));

        detectButton.addActionListener(e -> detectParticles(logArea));

        trackButton.addActionListener(e -> {
            logArea.append("简单追踪功能将在 v0.5 添加。\n\n");
        });

        closeButton.addActionListener(e -> frame.dispose());

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(4, 1, 10, 10));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel row1 = new JPanel();
        row1.add(generateButton);
        row1.add(imageButton);
        row1.add(denoiseButton);

        JPanel row2 = new JPanel();
        row2.add(denoiseMethodLabel);
        row2.add(denoiseMethodBox);
        row2.add(denoiseParameterLabel);
        row2.add(denoiseParameterField);

        JPanel row3 = new JPanel();
        row3.add(thresholdLabel);
        row3.add(detectionThresholdField);
        row3.add(radiusLabel);
        row3.add(localMaxRadiusField);
        row3.add(minDistanceLabel);
        row3.add(minDistanceField);

        JPanel row4 = new JPanel();
        row4.add(detectButton);
        row4.add(trackButton);
        row4.add(closeButton);

        controlPanel.add(row1);
        controlPanel.add(row2);
        controlPanel.add(row3);
        controlPanel.add(row4);

        frame.setLayout(new BorderLayout());
        frame.add(title, BorderLayout.NORTH);
        frame.add(controlPanel, BorderLayout.CENTER);
        frame.add(new JScrollPane(logArea), BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void readCurrentImage(JTextArea logArea) {
        try {
            ImagePlus imp = IJ.getImage();

            logArea.append("当前图像：" + imp.getTitle() + "\n");
            logArea.append("宽度：" + imp.getWidth() + "\n");
            logArea.append("高度：" + imp.getHeight() + "\n");
            logArea.append("切片/帧数：" + imp.getStackSize() + "\n\n");

        } catch (Exception ex) {
            logArea.append("请先在 Fiji/ImageJ 中打开一张图像。\n\n");
        }
    }

    private void denoiseCurrentImage(JTextArea logArea) {
        try {
            ImagePlus original = IJ.getImage();

            if (original == null) {
                logArea.append("没有检测到当前图像，请先打开或生成一张图像。\n\n");
                return;
            }

            String method = (String) denoiseMethodBox.getSelectedItem();
            double parameter = Double.parseDouble(denoiseParameterField.getText());

            if (parameter <= 0) {
                logArea.append("降噪参数必须大于 0。\n\n");
                return;
            }

            ImagePlus denoised = original.duplicate();
            denoised.setTitle(original.getTitle() + " - Denoised");

            ImageStack stack = denoised.getStack();
            int totalSlices = stack.getSize();

            for (int s = 1; s <= totalSlices; s++) {

                ImageProcessor ip = stack.getProcessor(s);

                if (method.contains("Gaussian")) {
                    ip.blurGaussian(parameter);
                } else if (method.contains("Median")) {
                    RankFilters rankFilters = new RankFilters();
                    rankFilters.rank(ip, parameter, RankFilters.MEDIAN);
                }

                IJ.showProgress(s, totalSlices);
            }

            denoised.show();

            logArea.append("降噪完成。\n");
            logArea.append("原始图像：" + original.getTitle() + "\n");
            logArea.append("降噪方法：" + method + "\n");
            logArea.append("参数：" + parameter + "\n");
            logArea.append("处理帧数：" + totalSlices + "\n");
            logArea.append("已生成新图像：" + denoised.getTitle() + "\n\n");

        } catch (NumberFormatException ex) {
            logArea.append("参数输入错误，请输入数字，例如 1.0 或 2.0。\n\n");
        } catch (Exception ex) {
            logArea.append("降噪失败：" + ex.getMessage() + "\n\n");
        }
    }

    private void detectParticles(JTextArea logArea) {
        try {
            ImagePlus imp = IJ.getImage();

            if (imp == null) {
                logArea.append("没有检测到当前图像，请先打开或生成一张图像。\n\n");
                return;
            }

            double threshold = Double.parseDouble(detectionThresholdField.getText());
            int localRadius = Integer.parseInt(localMaxRadiusField.getText());
            double minDistance = Double.parseDouble(minDistanceField.getText());

            if (threshold <= 0) {
                logArea.append("识别阈值必须大于 0。\n\n");
                return;
            }

            if (localRadius < 1) {
                logArea.append("局部极大半径至少为 1。\n\n");
                return;
            }

            if (minDistance < 1) {
                logArea.append("最小距离至少为 1。\n\n");
                return;
            }

            lastDetections.clear();

            ImageStack stack = imp.getStack();
            int totalSlices = stack.getSize();

            Overlay overlay = new Overlay();
            ResultsTable resultsTable = new ResultsTable();

            for (int frame = 1; frame <= totalSlices; frame++) {

                ImageProcessor ip = stack.getProcessor(frame);
                FloatProcessor fp = ip.convertToFloatProcessor();

                List<Detection> candidates = findLocalMaxima(
                        fp,
                        frame,
                        threshold,
                        localRadius
                );

                candidates.sort(
                        Comparator.comparingDouble((Detection d) -> d.intensity).reversed()
                );

                List<Detection> acceptedInThisFrame = new ArrayList<>();

                for (Detection candidate : candidates) {

                    boolean tooClose = false;

                    for (Detection accepted : acceptedInThisFrame) {
                        double distance = distance(
                                candidate.x,
                                candidate.y,
                                accepted.x,
                                accepted.y
                        );

                        if (distance < minDistance) {
                            tooClose = true;
                            break;
                        }
                    }

                    if (!tooClose) {
                        acceptedInThisFrame.add(candidate);
                        lastDetections.add(candidate);

                        resultsTable.incrementCounter();
                        resultsTable.addValue("Frame", candidate.frame);
                        resultsTable.addValue("X", candidate.x);
                        resultsTable.addValue("Y", candidate.y);
                        resultsTable.addValue("Intensity", candidate.intensity);

                        OvalRoi roi = new OvalRoi(
                                candidate.x - 4,
                                candidate.y - 4,
                                8,
                                8
                        );

                        roi.setStrokeColor(Color.RED);
                        roi.setPosition(frame);
                        overlay.add(roi);
                    }
                }

                IJ.showProgress(frame, totalSlices);
            }

            imp.setOverlay(overlay);
            resultsTable.show("Particle Detections");

            logArea.append("颗粒识别完成。\n");
            logArea.append("图像：" + imp.getTitle() + "\n");
            logArea.append("识别阈值：" + threshold + "\n");
            logArea.append("局部极大半径：" + localRadius + "\n");
            logArea.append("最小距离：" + minDistance + "\n");
            logArea.append("总帧数：" + totalSlices + "\n");
            logArea.append("检测到颗粒总数：" + lastDetections.size() + "\n");
            logArea.append("结果已显示在 Particle Detections 表格中。\n\n");

        } catch (NumberFormatException ex) {
            logArea.append("识别参数输入错误，请输入数字。\n\n");
        } catch (Exception ex) {
            logArea.append("颗粒识别失败：" + ex.getMessage() + "\n\n");
        }
    }

    private List<Detection> findLocalMaxima(
            FloatProcessor fp,
            int frame,
            double threshold,
            int radius
    ) {

        List<Detection> detections = new ArrayList<>();

        int width = fp.getWidth();
        int height = fp.getHeight();

        for (int y = radius; y < height - radius; y++) {
            for (int x = radius; x < width - radius; x++) {

                float centerValue = fp.getf(x, y);

                if (centerValue < threshold) {
                    continue;
                }

                boolean isLocalMaximum = true;

                for (int yy = y - radius; yy <= y + radius; yy++) {
                    for (int xx = x - radius; xx <= x + radius; xx++) {

                        if (xx == x && yy == y) {
                            continue;
                        }

                        if (fp.getf(xx, yy) > centerValue) {
                            isLocalMaximum = false;
                            break;
                        }
                    }

                    if (!isLocalMaximum) {
                        break;
                    }
                }

                if (isLocalMaximum) {
                    detections.add(
                            new Detection(
                                    frame,
                                    x,
                                    y,
                                    centerValue
                            )
                    );
                }
            }
        }

        return detections;
    }

    private double distance(
            double x1,
            double y1,
            double x2,
            double y2
    ) {
        double dx = x1 - x2;
        double dy = y1 - y2;

        return Math.sqrt(dx * dx + dy * dy);
    }

    private void generateTestImage(JTextArea logArea) {

        int width = 256;
        int height = 256;
        int frames = 30;
        int particles = 8;

        double sigma = 2.0;
        double amplitude = 180.0;
        double background = 20.0;
        double noiseLevel = 8.0;

        Random random = new Random(12345);

        double[] x = new double[particles];
        double[] y = new double[particles];
        double[] vx = new double[particles];
        double[] vy = new double[particles];

        for (int i = 0; i < particles; i++) {
            x[i] = 30 + random.nextDouble() * (width - 60);
            y[i] = 30 + random.nextDouble() * (height - 60);

            vx[i] = -1.5 + random.nextDouble() * 3.0;
            vy[i] = -1.5 + random.nextDouble() * 3.0;
        }

        ImageStack stack = new ImageStack(width, height);

        for (int t = 0; t < frames; t++) {

            float[] pixels = new float[width * height];

            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = (float) (background + random.nextGaussian() * noiseLevel);
            }

            for (int p = 0; p < particles; p++) {

                drawGaussianSpot(
                        pixels,
                        width,
                        height,
                        x[p],
                        y[p],
                        amplitude,
                        sigma
                );

                x[p] += vx[p];
                y[p] += vy[p];

                if (x[p] < 10 || x[p] > width - 10) {
                    vx[p] = -vx[p];
                }

                if (y[p] < 10 || y[p] > height - 10) {
                    vy[p] = -vy[p];
                }
            }

            FloatProcessor fp = new FloatProcessor(width, height, pixels);
            stack.addSlice("Frame " + (t + 1), fp);
        }

        ImagePlus imp = new ImagePlus("Synthetic Single Particle Movie", stack);
        imp.setDimensions(1, 1, frames);
        imp.setOpenAsHyperStack(true);
        imp.setDisplayRange(0, 255);
        imp.show();

        logArea.append("已生成测试图像。\n");
        logArea.append("图像大小：" + width + " × " + height + "\n");
        logArea.append("帧数：" + frames + "\n");
        logArea.append("颗粒数：" + particles + "\n");
        logArea.append("说明：亮点模拟单颗粒分子，背景加入随机噪声。\n\n");
    }

    private void drawGaussianSpot(
            float[] pixels,
            int width,
            int height,
            double centerX,
            double centerY,
            double amplitude,
            double sigma
    ) {

        int radius = (int) Math.ceil(3 * sigma);

        int xMin = Math.max(0, (int) Math.floor(centerX - radius));
        int xMax = Math.min(width - 1, (int) Math.ceil(centerX + radius));
        int yMin = Math.max(0, (int) Math.floor(centerY - radius));
        int yMax = Math.min(height - 1, (int) Math.ceil(centerY + radius));

        for (int y = yMin; y <= yMax; y++) {
            for (int x = xMin; x <= xMax; x++) {

                double dx = x - centerX;
                double dy = y - centerY;

                double value = amplitude * Math.exp(
                        -(dx * dx + dy * dy) / (2 * sigma * sigma)
                );

                int index = y * width + x;
                pixels[index] += (float) value;
            }
        }
    }

    private static class Detection {

        int frame;
        double x;
        double y;
        double intensity;

        Detection(
                int frame,
                double x,
                double y,
                double intensity
        ) {
            this.frame = frame;
            this.x = x;
            this.y = y;
            this.intensity = intensity;
        }
    }
}