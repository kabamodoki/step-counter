package com.stepcounter.report;

import com.stepcounter.model.DiffFileResult;
import com.stepcounter.model.DiffResult;
import com.stepcounter.model.FileStepResult;
import com.stepcounter.model.ScanResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds a single self-contained HTML report (inline CSS/JS, no external requests) matching the
 * approved visual design from detail-design.md: dark "arcade result screen" with pink/cyan/gold
 * accents, medal-ranked file table, and animated step bars.
 */
public class HtmlReportGenerator {

    public String buildFullReport(ScanResult result) {
        long total = result.getTotalSteps();
        long max = result.getFiles().stream().mapToLong(FileStepResult::getSteps).max().orElse(1);
        String otherModeLabel = result.getMode() == com.stepcounter.model.StepMode.FULL
                ? "実質モード換算" : "全量モード換算";

        String rows = rankedRows(result.getFiles(), max);

        String configChips = """
                <span class="chip on">● %s</span>
                <span class="chip">%s <b>%d</b> steps</span>
                <span class="chip">除外 <b>%d</b>件</span>
                <span class="chip">対象 <b>%d</b> ファイル</span>
                """.formatted(
                escape(result.getMode().label()),
                escape(otherModeLabel), result.getSubstantiveTotalSteps(),
                result.getExcludePatterns().size(),
                result.getFileCount());

        String heaviest = result.getFiles().stream()
                .max((a, b) -> Long.compare(a.getSteps(), b.getSteps()))
                .map(FileStepResult::getRelativePath)
                .orElse("-");

        long average = result.getFileCount() == 0 ? 0 : total / result.getFileCount();

        String statsHtml = """
                <div class="stat accent-pink">
                  <p class="label">合計ステップ</p>
                  <p class="value">%,d <small>steps</small></p>
                </div>
                <div class="stat">
                  <p class="label">対象ファイル数</p>
                  <p class="value">%d <small>files</small></p>
                </div>
                <div class="stat accent-gold">
                  <p class="label">最重量ファイル</p>
                  <p class="value" style="font-size:16px; font-family:var(--font-mono);">%s</p>
                </div>
                <div class="stat accent-cyan">
                  <p class="label">平均 / ファイル</p>
                  <p class="value">%,d <small>steps</small></p>
                </div>
                """.formatted(total, result.getFileCount(), escape(shortName(heaviest)), average);

        return page(
                "STEP COUNTER",
                escape(result.getRepoUrl()) + " · ブランチ <b>" + escape(result.getBranch())
                        + "</b> · テンプレート <b>" + escape(result.getTemplateName()) + "</b> でスキャンしました",
                configChips,
                statsHtml,
                """
                <section class="panel">
                  <div class="panel-head">
                    <h2>ファイル別ランキング</h2>
                    <span class="hint">クリックでソート切替</span>
                  </div>
                  <div class="table-scroll">
                    <table id="rankTable">
                      <thead>
                        <tr>
                          <th style="width:40px">#</th>
                          <th data-sort="name">ファイル</th>
                          <th class="num" data-sort="steps">ステップ</th>
                          <th>比率</th>
                        </tr>
                      </thead>
                      <tbody>
                        %s
                      </tbody>
                    </table>
                  </div>
                </section>
                """.formatted(rows));
    }

    public String buildDiffReport(DiffResult result) {
        String configChips = """
                <span class="chip on">● %s</span>
                <span class="chip">%s → %s</span>
                <span class="chip">除外 <b>%d</b>件</span>
                <span class="chip">変更 <b>%d</b> ファイル</span>
                """.formatted(
                escape(result.getMode().label()),
                escape(result.getBranchA()), escape(result.getBranchB()),
                result.getExcludePatterns().size(),
                result.getFiles().size());

        String statsHtml = """
                <div class="stat accent-cyan">
                  <p class="label">追加ステップ</p>
                  <p class="value">+%,d</p>
                </div>
                <div class="stat accent-pink">
                  <p class="label">削除ステップ</p>
                  <p class="value">-%,d</p>
                </div>
                <div class="stat">
                  <p class="label">変更ファイル数</p>
                  <p class="value">%d <small>files</small></p>
                </div>
                <div class="stat accent-gold">
                  <p class="label">純増減</p>
                  <p class="value">%+,d <small>steps</small></p>
                </div>
                """.formatted(result.getTotalAdded(), result.getTotalDeleted(), result.getFiles().size(),
                result.getTotalAdded() - result.getTotalDeleted());

        String rows = result.getFiles().stream()
                .map(f -> """
                        <div class="diff-row">
                          <span class="file">%s</span>
                          <span class="pill-add">+%d</span>
                          <span class="pill-del">−%d</span>
                        </div>
                        """.formatted(escape(f.getRelativePath()), f.getAdded(), f.getDeleted()))
                .collect(Collectors.joining());

        return page(
                "STEP COUNTER — DIFF",
                escape(result.getRepoUrl()) + " · <b>" + escape(result.getBranchA()) + "</b> → <b>"
                        + escape(result.getBranchB()) + "</b> · テンプレート <b>" + escape(result.getTemplateName()) + "</b>",
                configChips,
                statsHtml,
                """
                <section class="panel">
                  <div class="panel-head">
                    <h2>差分ステップ（ファイル単位）</h2>
                    <span class="hint">%s → %s</span>
                  </div>
                  <div class="diff-grid">
                    %s
                  </div>
                </section>
                """.formatted(escape(result.getBranchA()), escape(result.getBranchB()), rows));
    }

    public void write(String html, Path outFile) {
        try {
            Files.createDirectories(outFile.toAbsolutePath().getParent());
            Files.writeString(outFile, html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write report to " + outFile, e);
        }
    }

    private String rankedRows(List<FileStepResult> files, long max) {
        List<FileStepResult> sorted = files.stream()
                .sorted((a, b) -> Long.compare(b.getSteps(), a.getSteps()))
                .toList();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            FileStepResult f = sorted.get(i);
            int rank = i + 1;
            String rowClass = rank <= 3 ? "r" + rank : "";
            double widthPct = max == 0 ? 0 : (f.getSteps() * 100.0 / max);
            sb.append("""
                    <tr class="%s">
                      <td><span class="rank">%d</span></td>
                      <td class="file">%s</td>
                      <td class="num steps">%,d</td>
                      <td class="bar-cell"><div class="bar-track"><div class="bar-fill" style="--w:%.0f%%"></div></div></td>
                    </tr>
                    """.formatted(rowClass, rank, escape(f.getRelativePath()), f.getSteps(), widthPct));
        }
        return sb.toString();
    }

    private String shortName(String path) {
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    private String escape(String s) {
        return s == null ? "" : s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String page(String title, String subtitle, String configChips, String statsHtml, String mainSectionHtml) {
        // Plain token replacement (not String.formatted) because CSS/JS below contain literal
        // '%' characters that would otherwise need doubling-up as format specifiers.
        String template = """
                <!doctype html>
                <html lang="ja">
                <head>
                <meta charset="utf-8">
                <title>{{TITLE}}</title>
                <style>
                {{CSS}}
                </style>
                </head>
                <body>
                <div class="stage">
                  <section class="marquee">
                    <p class="eyebrow"><span class="dot"></span> スキャン完了</p>
                    <h1 class="title">{{TITLE}}</h1>
                    <p class="subtitle">{{SUBTITLE}}</p>
                    <div class="config-strip">
                      {{CHIPS}}
                    </div>
                  </section>
                  <section class="stats">
                    {{STATS}}
                  </section>
                  {{MAIN}}
                  <footer>Generated by step-counter</footer>
                </div>
                <script>
                {{JS}}
                </script>
                </body>
                </html>
                """;
        return template
                .replace("{{TITLE}}", escape(title))
                .replace("{{CSS}}", CSS)
                .replace("{{SUBTITLE}}", subtitle)
                .replace("{{CHIPS}}", configChips)
                .replace("{{STATS}}", statsHtml)
                .replace("{{MAIN}}", mainSectionHtml)
                .replace("{{JS}}", JS);
    }

    private static final String CSS = """
            :root{
              --void:#0d0a1a; --panel:#161129; --panel-raised:#1f1938; --line:#322a54;
              --pink:#ff5fa2; --cyan:#4de8d4; --gold:#ffcf5c;
              --ink:#f3eefc; --ink-dim:#a99bc9; --good:#4ee08a; --bad:#ff6b6b;
              --font-display: ui-rounded, "SF Pro Rounded", "Hiragino Maru Gothic ProN", "Zen Maru Gothic", system-ui, sans-serif;
              --font-body: system-ui, -apple-system, "Segoe UI", "Hiragino Sans", sans-serif;
              --font-mono: ui-monospace, "SF Mono", "Cascadia Code", Consolas, monospace;
            }
            *{ box-sizing:border-box; }
            body{
              margin:0; min-height:100vh; color:var(--ink); font-family:var(--font-body);
              padding:clamp(20px,4vw,56px);
              background:
                radial-gradient(ellipse 900px 500px at 15% -10%, rgba(255,95,162,.16), transparent 60%),
                radial-gradient(ellipse 900px 600px at 90% 0%, rgba(77,232,212,.14), transparent 55%),
                var(--void);
            }
            .stage{ max-width:980px; margin:0 auto; display:flex; flex-direction:column; gap:22px; }
            .marquee{ position:relative; border-radius:22px; padding:clamp(28px,5vw,44px) clamp(24px,5vw,40px);
              background:linear-gradient(180deg, rgba(255,255,255,.05), transparent 40%), linear-gradient(135deg,#1c1436,#241a3f 55%,#1a1330);
              border:1px solid var(--line); overflow:hidden; }
            .eyebrow{ font:600 12px/1 var(--font-body); letter-spacing:.12em; text-transform:uppercase; color:var(--ink-dim);
              display:flex; align-items:center; gap:10px; margin:0 0 14px; }
            .eyebrow .dot{ width:7px; height:7px; border-radius:50%; background:var(--good); box-shadow:0 0 10px var(--good); }
            .title{ font-family:var(--font-display); font-weight:800; font-size:clamp(34px,6vw,56px); letter-spacing:.02em;
              margin:0; text-wrap:balance; color:#fff;
              text-shadow:0 0 6px rgba(255,255,255,.25), 0 0 26px rgba(255,95,162,.55), 0 0 60px rgba(77,232,212,.25);
              animation: glow 3.2s ease-in-out infinite; }
            @keyframes glow{ 0%,100%{filter:brightness(1);} 50%{filter:brightness(1.12);} }
            .subtitle{ margin:10px 0 0; color:var(--ink-dim); font-size:15px; }
            .subtitle b{ color:var(--ink); font-weight:600; }
            .config-strip{ margin-top:22px; display:flex; flex-wrap:wrap; gap:8px; }
            .chip{ font:600 12.5px/1 var(--font-body); padding:8px 14px; border-radius:999px; background:var(--panel-raised);
              border:1px solid var(--line); color:var(--ink-dim); display:flex; align-items:center; gap:6px; }
            .chip b{ color:var(--ink); font-weight:700; }
            .chip.on{ color:var(--void); background:var(--cyan); border-color:transparent; font-weight:700; }
            .stats{ display:grid; grid-template-columns:repeat(4,1fr); gap:14px; }
            @media (max-width:720px){ .stats{ grid-template-columns:repeat(2,1fr); } }
            .stat{ background:var(--panel); border:1px solid var(--line); border-radius:14px; padding:18px 18px 16px; }
            .stat .label{ font:700 11px/1 var(--font-body); letter-spacing:.1em; text-transform:uppercase; color:var(--ink-dim); margin:0 0 10px; }
            .stat .value{ font-family:var(--font-mono); font-variant-numeric:tabular-nums; font-size:28px; font-weight:700; color:var(--ink); }
            .stat .value small{ font-size:13px; color:var(--ink-dim); font-weight:500; margin-left:4px; }
            .stat.accent-gold .value{ color:var(--gold); text-shadow:0 0 18px rgba(255,207,92,.35); }
            .stat.accent-pink .value{ color:var(--pink); text-shadow:0 0 18px rgba(255,95,162,.35); }
            .stat.accent-cyan .value{ color:var(--cyan); text-shadow:0 0 18px rgba(77,232,212,.35); }
            .panel{ background:var(--panel); border:1px solid var(--line); border-radius:22px; padding:22px clamp(16px,3vw,26px) 26px; }
            .panel-head{ display:flex; align-items:baseline; justify-content:space-between; gap:12px; margin-bottom:16px; flex-wrap:wrap; }
            .panel-head h2{ font-family:var(--font-display); font-size:20px; margin:0; color:#fff; }
            .panel-head .hint{ font-size:12.5px; color:var(--ink-dim); }
            .table-scroll{ overflow-x:auto; }
            table{ width:100%; border-collapse:collapse; min-width:560px; }
            thead th{ text-align:left; font:700 11px/1 var(--font-body); letter-spacing:.08em; text-transform:uppercase;
              color:var(--ink-dim); padding:0 10px 10px; border-bottom:1px solid var(--line); cursor:pointer; }
            thead th.num, tbody td.num{ text-align:right; }
            tbody tr{ transition:background .15s ease; }
            tbody tr:hover{ background:rgba(255,255,255,.03); }
            tbody td{ padding:12px 10px; border-bottom:1px solid rgba(50,42,84,.6); font-size:14px; vertical-align:middle; }
            .rank{ display:inline-flex; align-items:center; justify-content:center; width:26px; height:26px; border-radius:8px;
              font:800 12px/1 var(--font-mono); background:var(--panel-raised); color:var(--ink-dim); }
            tr.r1 .rank{ background:linear-gradient(160deg,#ffe29a,var(--gold)); color:#332103; box-shadow:0 0 16px rgba(255,207,92,.5); }
            tr.r2 .rank{ background:linear-gradient(160deg,#b9fff2,var(--cyan)); color:#04302b; box-shadow:0 0 16px rgba(77,232,212,.45); }
            tr.r3 .rank{ background:linear-gradient(160deg,#ffc4de,var(--pink)); color:#3a0a20; box-shadow:0 0 16px rgba(255,95,162,.4); }
            .file{ font-family:var(--font-mono); font-size:13.5px; color:var(--ink); }
            .steps{ font-family:var(--font-mono); font-variant-numeric:tabular-nums; font-weight:700; }
            .bar-cell{ min-width:160px; }
            .bar-track{ height:8px; border-radius:999px; background:var(--panel-raised); overflow:hidden; }
            .bar-fill{ height:100%; border-radius:inherit; background:linear-gradient(90deg,var(--cyan),var(--pink));
              width:var(--w,0%); animation:grow 900ms cubic-bezier(.2,.9,.25,1) both; }
            tr.r1 .bar-fill{ background:linear-gradient(90deg,#ffe29a,var(--gold)); }
            @keyframes grow{ from{ width:0; } }
            @media (prefers-reduced-motion: reduce){ .title{ animation:none; } .bar-fill{ animation:none; width:var(--w,0%); } }
            .diff-grid{ display:flex; flex-direction:column; gap:10px; }
            .diff-row{ display:grid; grid-template-columns:minmax(0,1.4fr) 90px 90px; gap:14px; align-items:center; }
            .diff-row .file{ overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
            .pill-add, .pill-del{ font-family:var(--font-mono); font-weight:700; font-size:12.5px; padding:4px 8px; border-radius:7px; text-align:center; }
            .pill-add{ color:var(--good); background:rgba(78,224,138,.12); }
            .pill-del{ color:var(--bad); background:rgba(255,107,107,.12); }
            footer{ text-align:center; color:var(--ink-dim); font-size:12px; padding:6px 0 0; }
            footer b{ color:var(--ink); }
            """;

    private static final String JS = """
            (function(){
              var table = document.getElementById('rankTable');
              if (!table) return;
              var tbody = table.querySelector('tbody');
              var dir = {};
              table.querySelectorAll('th[data-sort]').forEach(function(th){
                th.addEventListener('click', function(){
                  var key = th.getAttribute('data-sort');
                  dir[key] = !dir[key];
                  var rows = Array.prototype.slice.call(tbody.querySelectorAll('tr'));
                  rows.sort(function(a, b){
                    var av, bv;
                    if (key === 'steps') {
                      av = parseInt(a.querySelector('.steps').textContent.replace(/,/g, ''), 10);
                      bv = parseInt(b.querySelector('.steps').textContent.replace(/,/g, ''), 10);
                    } else {
                      av = a.querySelector('.file').textContent;
                      bv = b.querySelector('.file').textContent;
                    }
                    if (av < bv) return dir[key] ? 1 : -1;
                    if (av > bv) return dir[key] ? -1 : 1;
                    return 0;
                  });
                  rows.forEach(function(r){ tbody.appendChild(r); });
                });
              });
            })();
            """;
}
