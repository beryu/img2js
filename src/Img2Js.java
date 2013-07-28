import com.sun.org.apache.xml.internal.security.utils.Base64;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class Img2Js extends JDialog {
    // instance variables
    private File selectedDirSrc;
    private String srcDirPath;

    // components
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField labelSrcDirPath;
    private JButton buttonBrowseSrc;

    public Img2Js() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        buttonBrowseSrc.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onBrowseSrc();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        // base64に変換したJSファイルを保存
        String result = generateJsFromImagesDir(selectedDirSrc);
        generateFileFromStr(result);

        // 通知
        JOptionPane.showMessageDialog(this, "JavaScript file was generated.");

        // 終了
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    private void onBrowseSrc() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int selected = fileChooser.showOpenDialog(this);
        switch (selected) {
            case JFileChooser.APPROVE_OPTION:
                // ディレクトリが選択された→パスを記憶
                selectedDirSrc = fileChooser.getSelectedFile();
                srcDirPath = selectedDirSrc.getPath();
                labelSrcDirPath.setText(srcDirPath);

                // OKボタンを活性化
                buttonOK.setEnabled(true);
                break;
            case JFileChooser.CANCEL_OPTION:
                // キャンセルされた→何もしない
                break;
            case JFileChooser.ERROR_OPTION:
                // エラー発生→何もしない
                break;
        }
    }

    private String generateJsFromImagesDir(File srcDir) {
        String jsText = "(function(p){var i;";
        BufferedImage bfImage;
        File[] fileList = srcDir.listFiles();
        File workFile;
        int fileCnt = fileList.length;
        ByteArrayOutputStream baos;
        BufferedOutputStream bos;
        String fileName;
        String fileNameWOExtensionStr;
        String extensionStr;
        int dotIndex;

        for (int i = 0; i < fileCnt; i++) {
            workFile = fileList[i];
            bfImage = null;
            try {
                bfImage = ImageIO.read(workFile);
            } catch (IOException e) {
                System.out.println(e);
            }

            fileName = workFile.getName();
            if (fileName.indexOf(".") == 0) {
                continue; // 隠しファイルは無視する
            }
            dotIndex = fileName.lastIndexOf(".");
            fileNameWOExtensionStr = fileName.substring(0, dotIndex);
            extensionStr = fileName.substring(dotIndex + 1);
            baos = new ByteArrayOutputStream();
            bos = new BufferedOutputStream(baos);
            bfImage.flush();
            try {
                ImageIO.write(bfImage, extensionStr, bos);
                bos.flush();
                bos.close();
            } catch (IOException e) {
                System.out.println(e);
            }

            jsText += "i=new Image();i.src='data:image/" + extensionStr + ";base64,";
            jsText += Base64.encode(baos.toByteArray());
            jsText += "';p['" + fileNameWOExtensionStr + "']=i;";
        }

        jsText += "})(window.images = window.images || {});";
        jsText = jsText.replace("\r","");
        jsText = jsText.replace("\n","");

        return jsText;
    }

    private void generateFileFromStr(String body) {
        JFileChooser fileChooser = new JFileChooser();
        FileWriter fileWriter;
        BufferedWriter bfWriter;
        int selected = fileChooser.showSaveDialog(this);
        switch (selected) {
            case JFileChooser.APPROVE_OPTION:
                // 保存先が指定された→そこに保存
                File saveFile = fileChooser.getSelectedFile();
                try {
                    fileWriter = new FileWriter(saveFile);
                    bfWriter = new BufferedWriter(fileWriter);
                    bfWriter.write(body);
                    bfWriter.close();
                } catch (IOException e) {
                    System.out.println(e);
                }
                break;
            case JFileChooser.CANCEL_OPTION:
                // キャンセルされた→何もしない
                break;
            case JFileChooser.ERROR_OPTION:
                // エラー発生→何もしない
                break;
        }
    }

    public static void main(String[] args) {
        Img2Js dialog = new Img2Js();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
