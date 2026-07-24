# step-counter

Gitリポジトリの指定ブランチ／ブランチ間差分から、コードのステップ数（行数）を集計するデスクトップツール。
Java + Maven + JavaFX。サーバー常駐なし、ローカル実行のみ。

```
step-counter/
└── src/main/java/com/stepcounter/
    ├── app/       … JavaFX画面・起動処理
    ├── git/       … git clone/fetch実行
    ├── scanner/   … 除外パターン適用のファイル走査
    ├── template/  … テンプレート定義（Java/Salesforce/TypeScript）
    ├── counter/   … 全量/実質モードのステップカウント
    ├── diff/      … ブランチ間の差分ステップ算出
    ├── report/    … HTMLレポート生成
    └── model/     … データモデル
```

## できること

- **全量ステップカウンター**: 指定ブランチをclone → ファイル単位・合計のステップ数を表示
- **差分ステップカウンター**: ブランチA・ブランチBをclone → ファイル単位で追加/削除ステップ数を比較
- **除外設定**: 画面上でフォルダ・ファイルの除外パターンを都度設定（保存はしない）
- **テンプレート**: Java / Salesforce / TypeScript(Node,Yarn) の3種を用意。選択すると各言語向けのデフォルト除外・コメント判定ルールが自動適用される
- **集計モード切替**: 全量モード（コメント・空行含む）／実質モード（コメント・空行を除く）
- **HTMLエクスポート**: 実行結果を自己完結型HTMLレポート（外部通信なし）として保存

## 必要環境

- Java 17 以上
- Maven 3.9 以上
- `git` コマンドがインストール済みでPATHが通っていること
- Windows想定（対象リポジトリへの認証はOS側で設定済みであること。ツール側で認証情報は扱わない）

## ビルド・実行

```powershell
mvn package
java -jar target/step-counter.jar
```

`mvn package` は `org.openjfx:javafx-*` を `win` クラシファイアで取得するため、初回はネットワーク接続が必要。

## 使い方

1. モードを選択（全量 / 差分）
2. リポジトリURLとブランチ（差分モードはブランチA・ブランチB）を入力
3. テンプレートを選択 → 除外パターンが自動入力される（この画面内でのみ有効。追加・削除・編集も可能）
4. 集計モード（全量 / 実質）を選択
5. 「実行」→ 結果テーブルが表示される
6. 「HTMLエクスポート」で結果をファイル保存

## テンプレートのデフォルト除外

| テンプレート | デフォルト除外 |
|---|---|
| Java | `target/`, `src/test/`, `src/main/resources/`, `.git/`, `*.class` |
| Salesforce | `.sfdx/`, `**/staticresources/`, `**/lwc/**/__tests__/`, `.git/` |
| TypeScript(Node/Yarn) | `node_modules/`, `dist/`, `build/`, `coverage/`, `.git/`, `*.d.ts` |

テンプレートの追加・保存機能は用意していません（3テンプレートを作り込みで対応する方針）。

## 実質モードの判定について

「実質モード」では、テンプレートごとのコメント構文（Java/TypeScript/Apexは `//` と `/* */`）をもとに、コメント行・空行を除いてカウントする。行内に「コード + 末尾コメント」「先頭コメント + 末尾コード」が混在する行も、コードとして正しく残る。

差分モードでも実質モードを選ぶと、コメントだけの変更は追加/削除としてカウントされない（`java-diff-utils` でコメント除去後の行列を比較しているため）。

ただし文字列リテラル内のコメント記号（例: `String s = "//";`）までは判定していない簡易実装。

## 開発メモ

- `src/test/java` にはJUnitではなく手動確認用のドライバを置いている
  - `LineCounterManualCheck` — 行内コメント判定のエッジケース確認
  - `ReportPreviewGenerator` — サンプルデータでHTMLレポートを生成
  - `EndToEndSmokeRunner` — 実リポジトリに対してclone→スキャン→差分算出まで通して確認
