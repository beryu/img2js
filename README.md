# img2js
Toolkit for CreateJS（以下、TFC）で書きだした複数の画像をbase64化して、1個のJavaScriptファイルに纏めるツールです。

## 使い方
1. img2js/out/artifacts/img2js_jar/img2js.jar をダウンロード
2. ダウンロードした img2js.jar をダブルクリック
3. [Browse...]ボタンを押下
4. TFCで出力したimagesディレクトリを選択
5. OKボタンを押下
6. TFCで出力したHTMLファイルのmanifest変数を以下のように書き換える

```
var manifest = ['/PATH/TO/images.js'];
```
