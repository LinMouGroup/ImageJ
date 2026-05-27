package com.yourname.imagejgui;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class Simple_GUI implements PlugIn {

    private final Font chineseFont = new Font("Microsoft YaHei", Font.PLAIN, 16);
    private final Font titleFont = new Font("Microsoft YaHei", Font.BOLD, 22);

    @Override
    public void run(String arg) {
        SwingUtilities.invokeLater(this::createAndShowGUI);
    }

    private void createAndShowGUI() {

        JFrame frame = new JFrame("My ImageJ GUI Plugin");

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);

        JLabel title = new JLabel("单颗粒分子识别 GUI Demo", SwingConstants.CENTER);
        title.setFont(titleFont);

        JButton generateButton = new JButton("生成测试图像");
        JButton imageButton = new JButton("读取当前图像");
        JButton denoiseButton = new JButton("降噪测试");
        JButton detectButton = new JButton("识别颗粒");
        JButton trackButton = new JButton("简单追踪");
        JButton closeButton = new JButton("关闭");

        generateButton.setFont(chineseFont);
        imageButton.setFont(chineseFont);
        denoiseButton.setFont(chineseFont);
        detectButton.setFont(chineseFont);
        trackButton.setFont(chineseFont);
        closeButton.setFont(chineseFont);

        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));

        generateButton.addActionListener(e -> generateTestImage(logArea));

        imageButton.addActionListener(e -> {
            try {
                ImagePlus imp = IJ.getImage();

                logArea.append("当前图像：" + imp.getTitle() + "\n");
                logArea.append("宽度：" + imp.getWidth() + "\n");
                logArea.append("高度：" + imp.getHeight() + "\n");
                logArea.append("帧数：" + imp.getStackSize() + "\n\n");

            } catch (Exception ex) {
                logArea.append("请先在 Fiji/ImageJ 中打开一张图像。\n\n");
            }
        });

        denoiseButton.addActionListener(e -> {
            try {
                ImagePlus imp = IJ.getImage();

                IJ.run(imp, "Gaussian Blur...", "sigma=1");

                logArea.append("已执行 Gaussian Blur 降噪，sigma = 1。\n\n");

            } catch (Exception ex) {
                logArea.append("请先打开图像，再点击降噪。\n\n");
            }
        });

        detectButton.addActionListener(e -> {
            logArea.append("识别颗粒功能将在 v0.4 添加。\n\n");
        });

        trackButton.addActionListener(e -> {
            logArea.append("简单追踪功能将在 v0.5 添加。\n\n");
        });

        closeButton.addActionListener(e -> frame.dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(2, 3, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        buttonPanel.add(generateButton);
        buttonPanel.add(imageButton);
        buttonPanel.add(denoiseButton);
        buttonPanel.add(detectButton);
        buttonPanel.add(trackButton);
        buttonPanel.add(closeButton);

        frame.setLayout(new BorderLayout());
        frame.add(title, BorderLayout.NORTH);
        frame.add(buttonPanel, BorderLayout.CENTER);
        frame.add(new JScrollPane(logArea), BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void generateTestImage(JTextArea logArea) {

        int width = 256;
        int height = 256;
        int frames = 30;
        int particles = 8;

        double sigma = 2.0;
        double amplitude = 180.0;
        double background = 20.0;
        double noiseLevel = 5.0;

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
}