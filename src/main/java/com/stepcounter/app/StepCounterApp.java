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
import javafx.stage.FileChooser;
import javafx.stage.Stage;

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
    private final Button exportButton = new Button("HTMLエクスポート");

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
        exportButton.setDisable(true);

        VBox bottomWrap = new VBox(actions);
        root.setBottom(bottomWrap);

        runButton.setOnAction(e -> onRun());
        exportButton.setOnAction(e -> onExport());

        return root;
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
            exportButton.setDisable(false);
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
    }

    private void applyDiffResult(DiffResult result) {
        this.lastDiffResult = result;
        this.lastScanResult = null;
        diffTable.getItems().setAll(result.getFiles());
        summaryLabel.setText(String.format("追加 +%,d / 削除 -%,d / %d ファイル",
                result.getTotalAdded(), result.getTotalDeleted(), result.getFiles().size()));
    }

    private void onExport() {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("step-counter-report.html");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML", "*.html"));
        java.io.File file = chooser.showSaveDialog(runButton.getScene().getWindow());
        if (file == null) {
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
        reportGenerator.write(html, file.toPath());
        showAlert("レポートを出力しました: " + file.getAbsolutePath());
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
