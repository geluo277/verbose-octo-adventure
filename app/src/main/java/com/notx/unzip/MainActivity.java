package com.notx.unzip;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.graphics.Typeface;
import android.graphics.Color;
import android.view.Gravity;
import android.database.Cursor;
import android.provider.OpenableColumns;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {
    private static final int PICK_NPK = 101;
    private static final int CREATE_ZIP = 102;
    private Uri selectedUri;
    private File unpackDir;
    private File zipFile;
    private TextView status;
    private Button chooseButton, extractButton, saveButton;
    private ProgressBar progress;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private File pathDict;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (!Python.isStarted()) Python.start(new AndroidPlatform(this));
        pathDict = new File(getFilesDir(), "pathdict.txt.gz");
        // Copy the bundled path dictionary into app-private storage. Any failure is
        // checked again before extraction and shown to the user instead of hidden.
        try { ensurePathDict(); } catch (Exception ignored) { }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(20));
        root.setBackgroundColor(Color.rgb(247,248,250));

        TextView title = new TextView(this);
        title.setText("NeoXLab Mobile");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.rgb(28,35,48));
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("NPK 全资源浏览与解包 · Android");
        sub.setTextSize(14); sub.setTextColor(Color.rgb(90,100,115));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.bottomMargin = dp(24); root.addView(sub, subLp);

        chooseButton = new Button(this); chooseButton.setText("① 选择 NPK 文件");
        root.addView(chooseButton, matchWrap());
        extractButton = new Button(this); extractButton.setText("② 开始完整解包");
        LinearLayout.LayoutParams eLp = matchWrap(); eLp.topMargin = dp(8);
        root.addView(extractButton, eLp);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true); progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams pLp = matchWrap(); pLp.topMargin = dp(12);
        root.addView(progress, pLp);

        status = new TextView(this);
        status.setText("请选择一个蛋仔派对 PC 版 NPK 文件。建议先用较小的资源包测试。");
        status.setTextSize(14); status.setTextColor(Color.rgb(50,60,75));
        status.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams sLp = matchWrap(); sLp.topMargin = dp(16);
        root.addView(status, sLp);

        saveButton = new Button(this); saveButton.setText("③ 保存 ZIP 到手机");
        saveButton.setEnabled(false);
        LinearLayout.LayoutParams zLp = matchWrap(); zLp.topMargin = dp(12);
        root.addView(saveButton, zLp);

        TextView note = new TextView(this);
        note.setText("说明：完整解包实验版：导出索引中的资源并按贴图、3D模型、UI、代码与配置、音频、其他分类。未知格式保留原始数据；不同 NPK 版本或加密方式可能不兼容。请确保有足够空间。");
        note.setTextSize(12); note.setTextColor(Color.rgb(110,115,125));
        LinearLayout.LayoutParams nLp = matchWrap(); nLp.topMargin = dp(24);
        root.addView(note, nLp);

        setContentView(root);
        chooseButton.setOnClickListener(v -> chooseNpk());
        extractButton.setOnClickListener(v -> extract());
        saveButton.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/zip");
            i.putExtra(Intent.EXTRA_TITLE, "NeoXLab-all-resources.zip");
            startActivityForResult(i, CREATE_ZIP);
        });
    }

    private synchronized void ensurePathDict() throws Exception {
        if (pathDict == null) pathDict = new File(getFilesDir(), "pathdict.txt.gz");
        if (pathDict.exists() && pathDict.length() > 0) return;
        if (pathDict.exists() && !pathDict.delete()) {
            throw new java.io.IOException("无法删除损坏的路径索引文件：" + pathDict.getAbsolutePath());
        }
        // Android's asset packaging strips the .gz suffix and stores this asset as
        // assets/pathdict.txt. Re-compress it in app-private storage because
        // PathResolver.from_file() expects a gzip-compressed dictionary.
        try (InputStream in = getAssets().open("pathdict.txt");
             OutputStream out = new java.util.zip.GZIPOutputStream(new FileOutputStream(pathDict))) {
            byte[] b = new byte[32768]; int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
        } catch (Exception e) {
            if (pathDict.exists()) pathDict.delete();
            throw new java.io.IOException("APK 内缺少 assets/pathdict.txt。请确认 app/src/main/assets/pathdict.txt.gz 已提交并重新构建 APK。", e);
        }
        if (!pathDict.exists() || pathDict.length() == 0) {
            throw new java.io.IOException("路径索引文件复制后为空，请重新安装 APK。");
        }
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private void chooseNpk() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/octet-stream"});
        startActivityForResult(i, PICK_NPK);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == PICK_NPK) {
            selectedUri = uri;
            status.setText("已选择：" + displayName(uri) + "\n点击“开始完整解包”。");
            saveButton.setEnabled(false);
        } else if (requestCode == CREATE_ZIP) {
            Uri target = uri;
            worker.execute(() -> {
                try (InputStream in = new FileInputStream(zipFile);
                     OutputStream out = getContentResolver().openOutputStream(target)) {
                    byte[] b = new byte[65536]; int n;
                    while ((n = in.read(b)) > 0) out.write(b, 0, n);
                    runOnUiThread(() -> status.setText("ZIP 已保存到你选择的位置。"));
                } catch (Exception e) {
                    runOnUiThread(() -> status.setText("保存失败：" + e.getMessage()));
                }
            });
        }
    }

    private String displayName(Uri uri) {
        String name = "selected.npk";
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        } catch (Exception ignored) {}
        return name;
    }

    private void extract() {
        if (selectedUri == null) { status.setText("请先选择 NPK 文件。"); return; }
        chooseButton.setEnabled(false); extractButton.setEnabled(false);
        saveButton.setEnabled(false); progress.setVisibility(View.VISIBLE);
        status.setText("正在复制文件并解析索引，请保持应用打开……");
        worker.execute(() -> {
            try {
                File input = new File(getCacheDir(), "selected.npk");
                try (InputStream in = getContentResolver().openInputStream(selectedUri);
                     OutputStream out = new FileOutputStream(input)) {
                    byte[] b = new byte[65536]; int n;
                    while ((n = in.read(b)) > 0) out.write(b, 0, n);
                }
                ensurePathDict();
                unpackDir = new File(getCacheDir(), "all_resources_out");
                deleteTree(unpackDir); unpackDir.mkdirs();
                Python py = Python.getInstance();
                PyObject module = py.getModule("mobile_unpack");
                PyObject result = module.callAttr("extract_all_resources", input.getAbsolutePath(),
                        unpackDir.getAbsolutePath(), pathDict.getAbsolutePath());
                String summary = result.toString();
                zipFile = new File(getCacheDir(), "NeoXLab-all-resources.zip");
                if (zipFile.exists()) zipFile.delete();
                zipDirectory(unpackDir, zipFile);
                runOnUiThread(() -> {
                    status.setText("处理完成：\n" + summary + "\n\n点击“保存 ZIP 到手机”导出结果。");
                    saveButton.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("解包失败：\n" + e.getMessage()
                        + "\n\n如果提示索引格式不匹配，请检查 NPK 版本；目前仍需进一步验证其他索引模式和加密包。"));
            } finally {
                runOnUiThread(() -> {
                    chooseButton.setEnabled(true); extractButton.setEnabled(true);
                    progress.setVisibility(View.GONE);
                });
            }
        });
    }

    private void zipDirectory(File root, File zip) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            addToZip(root, root, zos);
        }
    }
    private void addToZip(File root, File file, ZipOutputStream zos) throws Exception {
        File[] children = file.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) addToZip(root, child, zos);
            else {
                String name = root.toURI().relativize(child.toURI()).getPath();
                zos.putNextEntry(new ZipEntry(name));
                try (FileInputStream in = new FileInputStream(child)) {
                    byte[] b = new byte[65536]; int n;
                    while ((n = in.read(b)) > 0) zos.write(b, 0, n);
                }
                zos.closeEntry();
            }
        }
    }
    private void deleteTree(File f) {
        if (f.isDirectory()) { File[] children = f.listFiles(); if (children != null) for (File c : children) deleteTree(c); }
        if (f.exists()) f.delete();
    }
}
