package com.evenwell.quickdemo;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class CopyActivity extends AppCompatActivity {
    private static final String TAG = "CopyActivity";
    private EditText mSourcePath;
    private EditText mGoalPath;
    private EditText mExecuteTimes;
    private Button mCopyFileBtn;
    private ProgressDialog mProgressDialogForCopy;
    HandleExecuteThread mHandleExecuteThread;
    private boolean mCanceled = false;
    private boolean mCopyError = false;
    private static final int MODE_SHOW_DIALOG = 1;
    private static final int MODE_DISMISS_DIALOG = 2;
    private static final int MODE_SHOW_TOAST = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_copy);
        mSourcePath = (EditText)findViewById(R.id.edt_inputSourcePath);
        mGoalPath = (EditText)findViewById(R.id.edt_inputGoalPath);
        mExecuteTimes = (EditText)findViewById(R.id.edt_inputExecute);
        mCopyFileBtn = (Button)findViewById(R.id.btn_copy_file);
        mCopyFileBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mCanceled = false;
                mCopyError = false;
                final InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                final View currentFocus = getCurrentFocus();
                if (imm != null && currentFocus != null) {
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                }

                if (TextUtils.isEmpty(mSourcePath.getText())) {
                    Toast.makeText(CopyActivity.this, R.string.error_copy_source_path, Toast.LENGTH_SHORT).show();
                }
                if (TextUtils.isEmpty(mGoalPath.getText())) {
                    Toast.makeText(CopyActivity.this, R.string.error_copy_goal_path, Toast.LENGTH_SHORT).show();
                }
                if (TextUtils.isEmpty(mExecuteTimes.getText())) {
                    Toast.makeText(CopyActivity.this, R.string.error_execute_number_of_times, Toast.LENGTH_SHORT).show();
                }

                if (!TextUtils.isEmpty(mSourcePath.getText()) && !TextUtils.isEmpty(mGoalPath.getText()) && !TextUtils.isEmpty(mExecuteTimes.getText())) {
                    Log.i(TAG,"mCopyFileBtn onClick --- mSourcePath = "+mSourcePath.getText()+", mGoalPath = "+mGoalPath.getText()+", mExecuteTimes = "+mExecuteTimes.getText());
                    String sourcePath = mSourcePath.getText() != null ? mSourcePath.getText().toString() : null;
                    String goalPath = mGoalPath.getText() != null ? mGoalPath.getText().toString() : null;
                    int times = mExecuteTimes.getText() != null ? Integer.valueOf(mExecuteTimes.getText().toString()) : 0;
                    mHandleExecuteThread = new HandleExecuteThread(CopyActivity.this, sourcePath, goalPath, times);
                    if (mHandleExecuteThread != null) {
                        mHandleExecuteThread.start();
                    }
                }
            }
        });
    }

    private class HandleExecuteThread extends Thread {
        private final Context context;
        private String sourcePath = null;
        private String goalPath = null;
        private int times = 0;

        public HandleExecuteThread(Context context, String sourcePath, String goalPath, int times) {
            this.context = context;
            this.sourcePath = sourcePath;
            this.goalPath = goalPath;
            this.times = times;
        }

        @Override
        public void run() {
            try {
                Log.i(TAG,"HandleExecuteThread --- sourcePath = "+sourcePath);
                Log.i(TAG,"HandleExecuteThread --- goalPath = "+goalPath);
                Log.i(TAG,"HandleExecuteThread --- times = "+times);

                File checkFile = new File(sourcePath);
                if (!checkFile.exists()) {
                    runOnUiThread(new HandleExecuteDisplayer(MODE_SHOW_TOAST, getString(R.string.error_source_path)));
                } else {
                    runOnUiThread(new HandleExecuteDisplayer(MODE_SHOW_DIALOG, null));
                    for (int i=0;i<times;i++) {
                        if (mCanceled || mCopyError) {
                            break;
                        }
                        Log.i(TAG,"HandleExecuteThread --- i+1 = "+(i+1));
                        File file1 = new File(sourcePath);
                        File file2 = new File(goalPath);
                        if (file1.exists() && !file2.exists()) {
                            GoCopyFile(sourcePath, goalPath, i+1);
                        } else if (!file1.exists() && file2.exists()) {
                            GoCopyFile(goalPath, sourcePath, i+1);
                        }
                    }
                    runOnUiThread(new HandleExecuteDisplayer(MODE_DISMISS_DIALOG, null));
                }
            } finally {
                if (mHandleExecuteThread != null) {
                    mHandleExecuteThread = null;
                }
            }
        }
    }

    private class HandleExecuteDisplayer implements Runnable {
        private int mode = 1;
        private String msg = null;

        public HandleExecuteDisplayer(int mode, String msg) {
            this.mode = mode;
            this.msg = msg;
        }

        @Override
        public void run() {
            if (mode == MODE_SHOW_DIALOG) {
                showDialog(R.id.dialog_progress_copy);
            } else if (mode == MODE_DISMISS_DIALOG) {
                try {
                    if (mProgressDialogForCopy != null) {
                        mProgressDialogForCopy.dismiss();
                    }
                } catch (Exception e) {}
            } else if (mode == MODE_SHOW_TOAST) {
                Toast.makeText(CopyActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void GoCopyFile(String sourcePath, String goalPath, int runTimes)  {
        Log.e(this.getClass().getSimpleName(),sourcePath);
        Log.e(this.getClass().getSimpleName(),goalPath);
        try {
            File wantfile = new File(sourcePath);
            File newfile = new File(goalPath);
            Log.i(TAG,"GoCopyFile --- sourcePath = "+sourcePath);
            Log.i(TAG,"GoCopyFile --- goalPath = "+goalPath);
            Log.i(TAG,"GoCopyFile --- wantfile = "+wantfile+", newfile = "+newfile);

            InputStream in = new FileInputStream(wantfile);
            Log.i(TAG,"GoCopyFile --- in = "+in);
            OutputStream out = new FileOutputStream(newfile);
            Log.i(TAG,"GoCopyFile --- out = "+out);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            Log.i(TAG,"GoCopyFile --- File copied!");
            goDeleteFile(sourcePath, runTimes);
        } catch (Exception e) {
            Log.e("copy file error", e.toString());
            mCopyError = true;
            runOnUiThread(new HandleExecuteDisplayer(MODE_SHOW_TOAST, getString(R.string.error_goal_path)));
        }
    }

    private void goDeleteFile(String deletePath, int runTimes) {
        Log.i(TAG,"goDeleteFile --- deletePath = "+deletePath);
        try {
            File file_test = new File(deletePath);
            if (file_test.exists()) {
                file_test.delete();
                Log.i(TAG,"goDeleteFile --- File deleted!");
            }
        } catch (Exception e) {
            Log.e("delete file error", e.toString());
            runOnUiThread(new HandleExecuteDisplayer(MODE_SHOW_TOAST, getString(R.string.error_delete_fail)));
        }
    }

    @Override
    protected Dialog onCreateDialog(int resId, Bundle bundle) {
        if (resId == R.id.dialog_progress_copy) {
            if (mProgressDialogForCopy == null) {
                String title = getString(R.string.copy_start_title);
                int speed = 20;
                final String message = getString(R.string.copy_start_message, speed);
                mProgressDialogForCopy = new ProgressDialog(this);
                mProgressDialogForCopy.setTitle(title);
                mProgressDialogForCopy.setMessage(message);
                mProgressDialogForCopy.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialogForCopy.setCancelable(false);
                mProgressDialogForCopy.setOnKeyListener(new DialogOnKeyListener(this));
            }
            return mProgressDialogForCopy;
        }
        return super.onCreateDialog(resId, bundle);
    }

    private class DialogOnKeyListener implements DialogInterface.OnKeyListener {
        private Activity mParent;

        /**
         * @param parent {@link Activity} to use for close.
         */
        public DialogOnKeyListener(Activity parent) {
            mParent = parent;
        }
        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            if (keyCode ==KeyEvent.KEYCODE_BACK ){
                Log.i(TAG, "Cancel, will close myself soon, keycode=" + keyCode);
                dialog.dismiss();
                mCanceled = true;
                Toast.makeText(CopyActivity.this, R.string.test_canceled, Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG,"onSaveInstanceState ---");
    }
}