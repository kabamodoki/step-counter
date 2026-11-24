# step-counter

Gitリポジトリの指定ブランチ／ブランチ間差分から、コードのステップ数（行数）を集計するデスクトップツール。
Java + Maven + JavaFX。サーバー常駐なし、ローカル実行のみ。

```
step-counter/
└── src/main/java/com/stepcounter/
    ├── app/       … JavaFX画面・起動処理
    ├── git/       … git clone/fetch実行
    ├── scanner/   … 除外パターン適用のファイル走査
    ├── template/  … テンプレート定義（Java/Salesforce/TypeScript/Python/Kotlin/Swift）
    ├── counter/   … 全量/実質モードのステップカウント
    ├── diff/      … ブランチ間の差分ステップ算出
    ├── report/    … HTMLレポート生成
    └── model/     … データモデル
```

## できること

- **全量ステップカウンター**: 指定ブランチをclone → ファイル単位・合計のステップ数を表示
- **差分ステップカウンター**: ブランチA・ブランチBをclone → ファイル単位で「ブランチA全量／ブランチB全量／追加／削除」ステップ数を比較（例: `aaaaa.java` … A(main): 100 → B(develop): 110、+12 / -2）
- **除外設定**: 画面上でフォルダ・ファイルの除外パターンを都度設定（保存はしない）
- **テンプレート**: Java / Salesforce / TypeScript(Node,Yarn) / Python / Kotlin / Swift の6種を用意。選択すると各言語向けのデフォルト除外・コメント判定ルールが自動適用される
- **集計モード切替**: 全量モード（コメント・空行含む）／実質モード（コメント・空行を除く）
- **最終コミット情報**: 結果一覧の各ファイルに「最終更新者」「最終更新日時」「コミットメッセージ（1行目）」を表示（画面・HTML・CSVすべて）。差分モードはブランチBの履歴から取る（Bに無いファイルはAから）。Bで削除されたファイルは削除したコミットが出る
- **エクスポート（HTML / CSV）**: 実行結果を自己完結型HTMLレポート（外部通信なし）、またはExcel等で開けるCSV（UTF-8 BOM付き）として保存。画面のラジオボタンで形式を切り替え

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
   - Salesforce選択時のみ「プロファイル/パーミッション系を含める」チェックボックスが表示される。未チェック（デフォルト）だと`profiles/`・`permissionsets/`・`permissionsetgroups/`が除外される
4. 集計モード（全量 / 実質）を選択
5. 「実行」→ 結果テーブルが表示される。ファイル名欄に `{リポジトリ名}-{ブランチ名}.{拡張子}`（差分モードは `{リポジトリ名}-{ブランチA}-vs-{ブランチB}.{拡張子}`）が自動入力される（変更可、拡張子は選択中のエクスポート形式に連動）
6. エクスポート形式（HTML / CSV）をラジオボタンで選択。切り替えるとファイル名の拡張子も自動的に付け替わる
7. 「出力先を選ぶ」でエクスポート先フォルダを1回選択（以降は選び直すまで保持される）
8. 「エクスポート」でそのフォルダ・ファイル名に直接書き出す（ダイアログは出ない。既存ファイルがあれば上書き）

### CSV出力のフォーマット
- 全量モード: `ファイルパス,ステップ数,最終更新者,最終更新日時,コミットメッセージ`
- 差分モード: `ファイルパス,全量(A),全量(B),追加,削除,最終更新者,最終更新日時,コミットメッセージ`
- 最終更新日時はPCのローカル時刻（`yyyy-MM-dd HH:mm:ss`）、コミットメッセージは1行目のみ

### 最終コミット情報の取り方
- cloneは `--filter=blob:none`（コミット履歴は全部取り、ファイル本体は必要な分だけ）。partial clone非対応のサーバーでは通常のcloneになる
- `git log --no-renames --name-only` を1回流して、各ファイルが最初に出てきた（＝一番新しい）コミットを使う
- リネームされたファイルは「リネームしたコミット」が最終コミットになる。マージ時のコンフリクト解消だけで変わったファイルは、その前の通常コミットが出る
- 文字コードはUTF-8（BOM付き）。Excelで開いても文字化けしない

## テンプレートのデフォルト除外

全テンプレート共通で、以下は常にデフォルト除外に含まれる。

| パターン | 対象 |
|---|---|
| `.github/` | GitHub Actions等のワークフロー定義 |
| `README*` | README.md 等のドキュメント |
| `LICENSE*` | LICENSE, LICENSE.md 等 |
| `.*` | `.gitignore`, `.gitkeep`, `.env` などドットファイル全般 |
| `*.svg`, `*.png`, `*.jpg`, `*.jpeg`, `*.gif`, `*.ico`, `*.webp`, `*.bmp` | 画像・静的アセット全般 |

テンプレートごとの追加除外:

| テンプレート | 追加のデフォルト除外 |
|---|---|
| Java | `target/`, `src/test/`, `src/main/resources/`, `.git/`, `*.class`, `pom.xml`, `build.gradle`, `build.gradle.kts`, `settings.gradle`, `settings.gradle.kts`, `mvnw`, `mvnw.cmd`, `gradlew`, `gradlew.bat`, `.mvn/`, `gradle/`, `lombok.config` |
| Salesforce | `.sfdx/`, `**/staticresources/`, `**/lwc/**/__tests__/`, `.git/`, `*Test.cls`、＋チェックボックス未チェック時のみ: `**/profiles/`, `**/permissionsets/`, `**/permissionsetgroups/` |
| TypeScript(Node/Yarn) | `node_modules/`, `dist/`, `build/`, `coverage/`, `.git/`, `*.d.ts`, `**/__tests__/`, `*.spec.ts`, `*.spec.tsx`, `*.spec.js`, `*.spec.jsx`, `*.test.ts`, `*.test.tsx`, `*.test.js`, `*.test.jsx` |
| Python | `__pycache__/`, `.venv/`, `venv/`, `env/`, `.pytest_cache/`, `.mypy_cache/`, `.tox/`, `dist/`, `build/`, `*.egg-info/`, `*.pyc`, `pyproject.toml`, `setup.py`, `setup.cfg`, `requirements*.txt`, `Pipfile`, `Pipfile.lock`, `poetry.lock`, `tests/`, `test_*.py`, `*_test.py` |
| Kotlin | `build/`, `.gradle/`, `gradle/`, `gradlew`, `gradlew.bat`, `settings.gradle.kts`, `build.gradle.kts`, `src/test/`, `*Test.kt` |
| Swift | `.build/`, `DerivedData/`, `*.xcodeproj/`, `*.xcworkspace/`, `Pods/`, `Package.resolved`, `Package.swift`, `Tests/`, `*Tests.swift` |

`*Test.cls`について: Apexは対象クラス名+`Test`という命名規則（例: `AccountServiceTest.cls`）を前提に除外している。プレフィックス型（`Test_Foo.cls`）や`@isTest`アノテーションでの判定は非対応。

Pythonのdocstring（`"""..."""`）は実質モードでも除外していない（通常の文字列リテラルとの区別が簡易ロジックでは困難なため、コードとして扱う）。

テンプレートの追加・保存機能は用意していません（3テンプレートを作り込みで対応する方針）。

## 実質モードの判定について

「実質モード」では、テンプレートごとのコメント構文（Java/TypeScript/Apexは `//` と `/* */`）をもとに、コメント行・空行を除いてカウントする。行内に「コード + 末尾コメント」「先頭コメント + 末尾コード」が混在する行も、コードとして正しく残る。

差分モードでも実質モードを選ぶと、コメントだけの変更は追加/削除としてカウントされない（`java-diff-utils` でコメント除去後の行列を比較しているため）。

ただし文字列リテラル内のコメント記号（例: `String s = "//";`）までは判定していない簡易実装。

## エラーログ

「実行」「エクスポート」でエラーが発生すると、画面のアラートに加えて `logs/step-counter-YYYY-MM-DD.log`（アプリの実行時カレントディレクトリ基準、同日は追記）にタイムスタンプ・発生箇所・例外・スタックトレースを記録する。ログ出力自体が失敗してもアプリは継続動作する。

## 開発メモ

- `src/test/java` にはJUnitではなく手動確認用のドライバを置いている
  - `LineCounterManualCheck` — 行内コメント判定のエッジケース確認
  - `ReportPreviewGenerator` — サンプルデータでHTML/CSVレポートを生成
  - `CsvReportGeneratorManualCheck` — CSVのカンマ/ダブルクォートのエスケープ、BOM付与の確認
  - `EndToEndSmokeRunner` — 実リポジトリに対してclone→スキャン→差分算出まで通して確認
  - `CommonExcludeManualCheck` — 全テンプレート（Java/Salesforce/TypeScript/Python/Kotlin/Swift）の除外パターン確認
  - `FileNameGenerationManualCheck` — エクスポートファイル名の自動生成ロジックの確認
  - `ErrorLoggerManualCheck` — エラーログ出力の確認
  - `NonUtf8FileManualCheck` — 非UTF-8ファイル読み込み時の挙動確認
  - `PythonCommentManualCheck` — Pythonの`#`コメント判定・docstring非除外の確認
