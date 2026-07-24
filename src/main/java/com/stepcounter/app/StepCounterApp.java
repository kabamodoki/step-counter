package com.stepcounter.app;

import com.stepcounter.counter.LineCounter;
import com.stepcounter.diff.DiffCounter;
import com.stepcounter.git.GitOperationService;
import com.stepcounter.model.DiffFileResult;
import com.stepcounter.model.DiffResult;
import com.stepcounter.model.FileStepResult;
import com.stepcounter.model.ScanResult;
import com.stepcounter.model.StepMode;
import com.stepcounter.report.HtmlReportGenerator;
import com.stepcounter.scanner.FileScanner;
import com.stepcounter.template.TemplateDefinition;
import com.stepcounter.template.TemplateType;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StepCounterApp extends Application {

    private enum RunMode { FULL, DIFF }

    private final ToggleGroup runModeGroup = new ToggleGroup();
    private final ToggleGroup stepModeGroup = new ToggleGroup();

    private final TextField repoUrlField = new TextField();
    private final TextField branchField = new TextField("main");
    private final TextField branchAField = new TextField("main");
    private final TextField branchBField = new TextField();

    private final ComboBox<TemplateType> templateCombo = new ComboBox<>(FXCollections.observableArrayList(TemplateType.values()));
    private final ObservableList<String> excludePatterns = FXCollections.observableArrayList();
    private final ListView<String> excludeListView = new ListView<>(excludePatterns);
    private final TextField newExcludeField = new TextField();

    private final HBox branchRow = new HBox(10);
    private final HBox diffBranchRow = new HBox(10);

    private final TableView<FileStepResult> fullTable = new TableView<>();
    private final TableView<DiffFileResult> diffTable = new TableView<>();
    private final Label summaryLabel = new Label();
    private final ProgressIndicator progress = new ProgressIndicator();
    private final Button runButton = new Button("実行");
    private final Button exportButton = new Button("エクスポート");

    private final TextField outputDirField = new TextField();
    private final Button chooseOutputDirButton = new Button("出力先を選ぶ");
    private final TextField fileNameField = new TextField();
    private Path outputDirectory;

    private ScanResult lastScanResult;
    private DiffResult lastDiffResult;

    private final GitOperationService gitService = new GitOperationService();
    private final FileScanner fileScanner = new FileScanner();
    private final LineCounter lineCounter = new LineCounter();
    private final DiffCounter diffCounter = new DiffCounter();
    private final HtmlReportGenerator reportGenerator = new HtmlReportGenerator();

    @Override
    public void start(Stage stage) {
        stage.setTitle("Step Counter");
        stage.setScene(new Scene(buildRoot(), 900, 720));
        stage.show();
    }

    private BorderPane buildRoot() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));
        root.setTop(buildForm());
        root.setCenter(buildResultsArea());

        progress.setVisible(false);
        progress.setManaged(false);
        progress.setPrefSize(20, 20);

        HBox actions = new HBox(10, runButton, progress, exportButton, summaryLabel);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(10, 0, 0, 0));

        outputDirField.setEditable(false);
        outputDirField.setPromptText("（未設定。先に出力先を選んでください）");
        HBox.setHgrow(outputDirField, Priority.ALWAYS);
        HBox outputRow = new HBox(8, new Label("出力先:"), outputDirField, chooseOutputDirButton);
        outputRow.setAlignment(Pos.CENTER_LEFT);

        fileNameField.setPromptText("実行すると自動入力されます（変更可）");
        HBox.setHgrow(fileNameField, Priority.ALWAYS);
        HBox fileNameRow = new HBox(8, new Label("ファイル名:"), fileNameField);
        fileNameRow.setAlignment(Pos.CENTER_LEFT);

        VBox bottomWrap = new VBox(8, outputRow, fileNameRow, actions);
        bottomWrap.setPadding(new Insets(10, 0, 0, 0));
        root.setBottom(bottomWrap);

        updateExportButtonState();

        runButton.setOnAction(e -> onRun());
        exportButton.setOnAction(e -> onExport());
        chooseOutputDirButton.setOnAction(e -> onChooseOutputDir());

        return root;
    }

    private void onChooseOutputDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("出力先フォルダを選択");
        if (outputDirectory != null) {
            chooser.setInitialDirectory(outputDirectory.toFile());
        }
        File selected = chooser.showDialog(runButton.getScene().getWindow());
        if (selected != null) {
            outputDirectory = selected.toPath();
            outputDirField.setText(outputDirectory.toString());
            updateExportButtonState();
        }
    }

    private void updateExportButtonState() {
        boolean hasResult = lastScanResult != null || lastDiffResult != null;
        exportButton.setDisable(outputDirectory == null || !hasResult);
    }

    private VBox buildForm() {
        RadioButton fullModeBtn = new RadioButton("全量ステップカウンター");
        fullModeBtn.setToggleGroup(runModeGroup);
        fullModeBtn.setSelected(true);
        fullModeBtn.setUserData(RunMode.FULL);

        RadioButton diffModeBtn = new RadioButton("差分ステップカウンター");
        diffModeBtn.setToggleGroup(runModeGroup);
        diffModeBtn.setUserData(RunMode.DIFF);

        HBox modeRow = new HBox(16, fullModeBtn, diffModeBtn);

        branchRow.getChildren().addAll(new Label("ブランチ:"), branchField);
        diffBranchRow.getChildren().addAll(
                new Label("ブランチA:"), branchAField,
                new Label("ブランチB:"), branchBField);
        diffBranchRow.setManaged(false);
        diffBranchRow.setVisible(false);
        HBox.setHgrow(branchField, Priority.ALWAYS);

        runModeGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            boolean isDiff = newT != null && newT.getUserData() == RunMode.DIFF;
            branchRow.setVisible(!isDiff);
            branchRow.setManaged(!isDiff);
            diffBranchRow.setVisible(isDiff);
            diffBranchRow.setManaged(isDiff);
            fullTable.setVisible(!isDiff);
            fullTable.setManaged(!isDiff);
            diffTable.setVisible(isDiff);
            diffTable.setManaged(isDiff);
        });

        templateCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                excludePatterns.setAll(TemplateDefinition.forType(newV).getDefaultExcludes());
            }
        });

        RadioButton fullStep = new RadioButton("全量モード（コメント・空行含む）");
        fullStep.setToggleGroup(stepModeGroup);
        fullStep.setSelected(true);
        fullStep.setUserData(StepMode.FULL);
        RadioButton substantiveStep = new RadioButton("実質モード（コメント・空行を除く）");
        substantiveStep.setToggleGroup(stepModeGroup);
        substantiveStep.setUserData(StepMode.SUBSTANTIVE);
        HBox stepModeRow = new HBox(16, fullStep, substantiveStep);

        Button addExclude = new Button("追加");
        addExclude.setOnAction(e -> {
            String v = newExcludeField.getText().trim();
            if (!v.isEmpty() && !excludePatterns.contains(v)) {
                excludePatterns.add(v);
                newExcludeField.clear();
            }
        });
        Button removeExclude = new Button("削除");
        removeExclude.setOnAction(e -> {
            String selected = excludeListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                excludePatterns.remove(selected);
            }
        });
        excludeListView.setPrefHeight(110);
        HBox excludeEditRow = new HBox(8, newExcludeField, addExclude, removeExclude);
        HBox.setHgrow(newExcludeField, Priority.ALWAYS);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("リポジトリURL:"), repoUrlField);
        GridPane.setHgrow(repoUrlField, Priority.ALWAYS);
        grid.addRow(1, new Label("テンプレート:"), templateCombo);

        templateCombo.getSelectionModel().selectFirst();

        VBox form = new VBox(12,
                modeRow,
                grid,
                branchRow,
                diffBranchRow,
                new Label("除外パターン（このセッション限り。保存はしません）:"),
                excludeListView,
                excludeEditRow,
                new Label("ステップ集計モード:"),
                stepModeRow);
        form.setPadding(new Insets(0, 0, 12, 0));
        return form;
    }

    private StackPane buildResultsArea() {
        TableColumn<FileStepResult, String> pathCol = new TableColumn<>("ファイル");
        pathCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRelativePath()));
        pathCol.setPrefWidth(500);
        TableColumn<FileStepResult, Long> stepsCol = new TableColumn<>("ステップ数");
        stepsCol.setCellValueFactory(data -> new javafx.beans.property.SimpleLongProperty(
                data.getValue().getSteps()).asObject());
        fullTable.getColumns().add(pathCol);
        fullTable.getColumns().add(stepsCol);

        TableColumn<DiffFileResult, String> dPathCol = new TableColumn<>("ファイル");
        dPathCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRelativePath()));
        dPathCol.setPrefWidth(420);
        TableColumn<DiffFileResult, Number> addedCol = new TableColumn<>("追加");
        addedCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getAdded()));
        TableColumn<DiffFileResult, Number> deletedCol = new TableColumn<>("削除");
        deletedCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getDeleted()));
        diffTable.getColumns().addAll(List.of(dPathCol, addedCol, deletedCol));
        diffTable.setVisible(false);
        diffTable.setManaged(false);

        StackPane pane = new StackPane(fullTable, diffTable);
        return pane;
    }

    private RunMode currentRunMode() {
        return (RunMode) runModeGroup.getSelectedToggle().getUserData();
    }

    private StepMode currentStepMode() {
        return (StepMode) stepModeGroup.getSelectedToggle().getUserData();
    }

    private void onRun() {
        String repoUrl = repoUrlField.getText().trim();
        if (repoUrl.isEmpty()) {
            showAlert("リポジトリURLを入力してください。");
            return;
        }
        TemplateType templateType = templateCombo.getSelectionModel().getSelectedItem();
        TemplateDefinition template = TemplateDefinition.forType(templateType);
        List<String> excludes = new ArrayList<>(excludePatterns);
        StepMode mode = currentStepMode();
        RunMode runMode = currentRunMode();

        runButton.setDisable(true);
        exportButton.setDisable(true);
        progress.setVisible(true);
        progress.setManaged(true);
        summaryLabel.setText("実行中...");

        Task<Void> task = runMode == RunMode.FULL
                ? buildFullTask(repoUrl, branchField.getText().trim(), templateType, template, excludes, mode)
                : buildDiffTask(repoUrl, branchAField.getText().trim(), branchBField.getText().trim(),
                        templateType, template, excludes, mode);

        task.setOnSucceeded(e -> {
            progress.setVisible(false);
            progress.setManaged(false);
            runButton.setDisable(false);
            updateExportButtonState();
        });
        task.setOnFailed(e -> {
            progress.setVisible(false);
            progress.setManaged(false);
            runButton.setDisable(false);
            Throwable ex = task.getException();
            showAlert("実行に失敗しました: " + (ex != null ? ex.getMessage() : "unknown error"));
            summaryLabel.setText("失敗しました");
        });
        Thread thread = new Thread(task, "step-counter-run");
        thread.setDaemon(true);
        thread.start();
    }

    private Task<Void> buildFullTask(String repoUrl, String branch, TemplateType templateType,
                                      TemplateDefinition template, List<String> excludes, StepMode mode) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                Path workDir = Path.of(System.getProperty("java.io.tmpdir"), "step-counter", UUID.randomUUID().toString());
                Path checkout = null;
                try {
                    checkout = gitService.fetchBranch(repoUrl, branch, workDir, "full");
                    List<Path> files = fileScanner.scan(checkout, excludes);
                    List<FileStepResult> results = new ArrayList<>();
                    long substantiveTotal = 0;
                    StepMode otherMode = mode == StepMode.FULL ? StepMode.SUBSTANTIVE : StepMode.FULL;
                    for (Path relative : files) {
                        Path abs = checkout.resolve(relative);
                        var style = template.styleFor(relative.getFileName().toString());
                        long steps = lineCounter.count(abs, style, mode);
                        results.add(new FileStepResult(relative.toString().replace('\\', '/'), steps));
                        substantiveTotal += lineCounter.count(abs, style, otherMode);
                    }
                    ScanResult scanResult = new ScanResult(repoUrl, branch, templateType.toString(), mode,
                            excludes, results, substantiveTotal);
                    Platform.runLater(() -> applyFullResult(scanResult));
                } finally {
                    if (checkout != null) {
                        gitService.cleanup(workDir);
                    }
                }
                return null;
            }
        };
    }

    private Task<Void> buildDiffTask(String repoUrl, String branchA, String branchB, TemplateType templateType,
                                      TemplateDefinition template, List<String> excludes, StepMode mode) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                Path workDir = Path.of(System.getProperty("java.io.tmpdir"), "step-counter", UUID.randomUUID().toString());
                try {
                    Path rootA = gitService.fetchBranch(repoUrl, branchA, workDir, "a");
                    Path rootB = gitService.fetchBranch(repoUrl, branchB, workDir, "b");
                    List<DiffFileResult> diffs = diffCounter.compare(rootA, rootB, template, excludes, mode);
                    DiffResult diffResult = new DiffResult(repoUrl, branchA, branchB, templateType.toString(),
                            mode, excludes, diffs);
                    Platform.runLater(() -> applyDiffResult(diffResult));
                } finally {
                    gitService.cleanup(workDir);
                }
                return null;
            }
        };
    }

    private void applyFullResult(ScanResult result) {
        this.lastScanResult = result;
        this.lastDiffResult = null;
        fullTable.getItems().setAll(
                result.getFiles().stream()
                        .sorted((a, b) -> Long.compare(b.getSteps(), a.getSteps()))
                        .toList());
        summaryLabel.setText(String.format("合計 %,d steps / %d ファイル", result.getTotalSteps(), result.getFileCount()));
        fileNameField.setText(repoShortName(result.getRepoUrl()) + "-" + sanitizeFileNamePart(result.getBranch()) + ".html");
        updateExportButtonState();
    }

    private void applyDiffResult(DiffResult result) {
        this.lastDiffResult = result;
        this.lastScanResult = null;
        diffTable.getItems().setAll(result.getFiles());
        summaryLabel.setText(String.format("追加 +%,d / 削除 -%,d / %d ファイル",
                result.getTotalAdded(), result.getTotalDeleted(), result.getFiles().size()));
        fileNameField.setText(repoShortName(result.getRepoUrl()) + "-" + sanitizeFileNamePart(result.getBranchA())
                + "-vs-" + sanitizeFileNamePart(result.getBranchB()) + ".html");
        updateExportButtonState();
    }

    /** Derives a filesystem-safe short name from a repo URL, e.g. "https://host/acme-orders.git" -> "acme-orders". */
    static String repoShortName(String repoUrl) {
        String cleaned = repoUrl.trim();
        while (cleaned.endsWith("/") || cleaned.endsWith("\\")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        int slash = Math.max(cleaned.lastIndexOf('/'), cleaned.lastIndexOf('\\'));
        String name = slash >= 0 ? cleaned.substring(slash + 1) : cleaned;
        if (name.endsWith(".git")) {
            name = name.substring(0, name.length() - 4);
        }
        return sanitizeFileNamePart(name);
    }

    /** Replaces characters that aren't safe in a Windows file name (branch names may contain "/"). */
    static String sanitizeFileNamePart(String s) {
        return s.replaceAll("[\\\\/:*?\"<>|]", "-");
    }

    private void onExport() {
        if (outputDirectory == null) {
            showAlert("先に出力先を選んでください。");
            return;
        }
        String html;
        if (lastScanResult != null) {
            html = reportGenerator.buildFullReport(lastScanResult);
        } else if (lastDiffResult != null) {
            html = reportGenerator.buildDiffReport(lastDiffResult);
        } else {
            showAlert("先に実行してください。");
            return;
        }
        String fileName = fileNameField.getText().trim();
        if (fileName.isEmpty()) {
            showAlert("ファイル名を入力してください。");
            return;
        }
        if (!fileName.toLowerCase().endsWith(".html")) {
            fileName = fileName + ".html";
        }
        Path outFile = outputDirectory.resolve(fileName);
        reportGenerator.write(html, outFile);
        showAlert("レポートを出力しました: " + outFile);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
