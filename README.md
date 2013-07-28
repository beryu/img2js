# img2js
Toolkit for CreateJS（以下、TFC）で書きだした複数の画像をbase64化して、1個のJavaScriptファイルに纏めるツールです。

## 使い方
1. img2js/out/artifacts/img2js_jar/img2js.jar をダウンロード
2. ダウンロードした img2js.jar をダブルクリック  
![スクリーンショット1](./images/ss1.png)
3. [Browse...]ボタンを押下
4. TFCで出力したimagesディレクトリを選択  
![スクリーンショット2](./images/ss2.png)
5. OKボタンを押下
6. 保存先を選択  
![スクリーンショット3](./images/ss3.png)
7. TFCで出力したHTMLファイルのmanifest変数を以下のように書き換える

```
var manifest = ['/PATH/TO/images.js'];
```
