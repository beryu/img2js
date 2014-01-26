import com.sun.org.apache.xml.internal.security.utils.Base64;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Img2Js extends JDialog {
    // instance variables
    private File srcDirFile;
    private String srcDirStr;
    private static final String CONFIG_FILE = "img2js_config.ini";
    private Map<String, String> configMap;

    // components
    private JPanel contentPane;
    private JButton buttonSave;
    private JButton buttonCancel;
    private JTextField labelSrcDirPath;
    private JButton buttonBrowseSrc;
    private JTextField namespace;

    /**
     * コンストラクタ
     */
    public Img2Js() {
        // load config
        configMap = loadConfig();

        // initialize
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonSave);

        // 画像ディレクトリの場所を設定から読み込む（設定が存在しなければ空欄にする）
        srcDirStr = configMap.get("srcDirStr");
        if (srcDirStr != null) {
            // ファイルオブジェクトを生成
            srcDirFile = new File(srcDirStr);
            if (srcDirFile.exists()) {
                // パスをテキストフィールドに書き込む
                labelSrcDirPath.setText(srcDirStr);

                // Saveボタンを活性化
                buttonSave.setEnabled(true);
            }
        }

        // 名前空間を設定から読み込む（設定が存在しなければCreateJSのデフォルト（images）にする
        String namespaceStr = configMap.get("imageNamespaceStr");
        if (namespaceStr != null) {
            namespace.setText(namespaceStr);
        } else {
            namespace.setText("images");
        }

        buttonSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onSave();
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

    /**
     * メインウィンドウの[Save]ボタンのクリックイベントハンドラ
     */
    private void onSave() {
        // base64に変換したJSファイルを保存
        Boolean result = generateJSFile();

        // 成功したなら設定を保存して終了
        if (result) {
            configMap.put("srcDirStr", srcDirStr);
            configMap.put("imageNamespaceStr", namespace.getText());
            saveConfig(configMap);
            dispose();
        }
    }

    /**
     * 設定ファイルをロードする
     * @return 設定が格納されたMapインスタンス
     */
    private Map<String, String> loadConfig() {
        File configFile = new File(CONFIG_FILE);
        Map<String, String> configMap = new HashMap<String, String>();

        // 設定ファイルがなかったら作る
        if (!configFile.exists()) {
            saveConfig(configMap);
        }

        try {
            FileReader fr = new FileReader(configFile);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                int i = line.indexOf('=');
                if (i < 0) {
                    continue;
                }
                configMap.put(line.substring(0, i), line.substring(i + 1));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return configMap;
    }

    /**
     * 設定ファイルを保存する
     * @param configMap 保存対象の設定が格納されたMapインスタンス
     */
    private void saveConfig(Map<String, String> configMap) {
        File configFile = new File(CONFIG_FILE);

        try {
            FileWriter fw = new FileWriter(configFile);
            BufferedWriter bw = new BufferedWriter(fw);

            if (!configMap.isEmpty()) {
                Object[] keyArr = configMap.keySet().toArray();

                for (Object key:keyArr) {
                    bw.write(key.toString() + "=" + configMap.get(key));
                    bw.newLine();
                }
            }
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * メインウィンドウの[cancel]ボタンのクリックイベントハンドラ
     */
    private void onCancel() {
        // 終了
        dispose();
    }

    /**
     * メインウィンドウの[Browse...]ボタンのクリックイベントハンドラ
     */
    private void onBrowseSrc() {
        String defaultPath = null;

        // set default path if file selected.
        if (srcDirStr != null) {
            defaultPath = srcDirStr;
            defaultPath = defaultPath.substring(0, defaultPath.lastIndexOf("/"));
        }

        JFileChooser fileChooser = new JFileChooser(defaultPath);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int selected = fileChooser.showOpenDialog(this);
        switch (selected) {
            case JFileChooser.APPROVE_OPTION:
                // ディレクトリが選択された→パスを記憶
                srcDirFile = fileChooser.getSelectedFile();
                srcDirStr = srcDirFile.getPath();

                // メインウィンドウのテキストフィールドにパスを表示
                labelSrcDirPath.setText(srcDirStr);

                // Saveボタンを活性化
                buttonSave.setEnabled(true);

                break;
            case JFileChooser.CANCEL_OPTION:
                // キャンセルされた→何もしない
                break;
            case JFileChooser.ERROR_OPTION:
                // エラー発生→何もしない
                break;
        }
    }

    /**
     * 指定されたディレクトリからbase64化したJavaScriptテキストを生成
     * @param srcDir base64化したい画像が入ったディレクトリのパス
     * @return 全画像をbase64化したJavaScriptテキスト
     */
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
        String namespaceStr = namespace.getText();

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

        jsText += "})(window." + namespaceStr + " = window." + namespaceStr + " || {});";
        jsText = jsText.replace("\r","");
        jsText = jsText.replace("\n","");

        return jsText;
    }

    /**
     * JavaScriptファイルを生成
     * @return 操作が成功したか否かのフラグ(成功したらtrue)
     */
    private Boolean generateJSFile() {
        String defaultPath = null;
        Boolean isSuccess = false;

        // set default path.
        if (srcDirStr != null) {
            defaultPath = srcDirStr;
            defaultPath = defaultPath.substring(0, defaultPath.lastIndexOf("/"));
        }

        JFileChooser fileChooser = new JFileChooser(defaultPath);
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
                    bfWriter.write(generateJsFromImagesDir(srcDirFile));
                    bfWriter.close();
                } catch (IOException e) {
                    System.out.println(e);
                }

                // 正常に書き出されたことを通知
                JOptionPane.showMessageDialog(this, "JavaScript file was generated.");
                isSuccess = true;

                break;
            case JFileChooser.CANCEL_OPTION:
                // キャンセルされた→何もしない
                break;
            case JFileChooser.ERROR_OPTION:
                // エラー発生→何もしない
                break;
        }

        return isSuccess;
    }

    public static void main(String[] args) {
        Img2Js dialog = new Img2Js();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
