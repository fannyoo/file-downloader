package org.wlf.filedownloader.file_rename;

import android.text.TextUtils;
import android.util.Log;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener.OnRenameDownloadFileFailReason;
import org.wlf.filedownloader.listener.OnRenameDownloadFileListener.RenameDownloadFileFailReason;
import org.wlf.filedownloader.util.CollectionUtil;
import org.wlf.filedownloader.util.DownloadFileUtil;

import java.io.File;
import java.util.List;

/**
 * RenameDownloadFileTask
 * <br/>
 * 重命名文件任务
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
class RenameDownloadFileTask implements Runnable {

    private static final String TAG = RenameDownloadFileTask.class.getSimpleName();

    private String mUrl;
    private String mNewFileName;
    private boolean includedSuffix;
    private DownloadFileRenamer mDownloadFileRenamer;

    private OnRenameDownloadFileListener mOnRenameDownloadFileListener;

    public RenameDownloadFileTask(String url, String newFileName, boolean includedSuffix, DownloadFileRenamer 
            downloadFileRenamer) {
        super();
        this.mUrl = url;
        this.mNewFileName = newFileName;
        this.includedSuffix = includedSuffix;
        this.mDownloadFileRenamer = downloadFileRenamer;
    }

    /**
     * set OnRenameDownloadFileListener
     *
     * @param onRenameDownloadFileListener OnRenameDownloadFileListener
     */
    public void setOnRenameDownloadFileListener(OnRenameDownloadFileListener onRenameDownloadFileListener) {
        this.mOnRenameDownloadFileListener = onRenameDownloadFileListener;
    }

    @Override
    public void run() {

        DownloadFileInfo downloadFileInfo = null;
        RenameDownloadFileFailReason failReason = null;

        try {
            downloadFileInfo = mDownloadFileRenamer.getDownloadFile(mUrl);

            if (downloadFileInfo == null) {
                failReason = new OnRenameDownloadFileFailReason("the download file is not exist!", 
                        OnRenameDownloadFileFailReason.TYPE_FILE_RECORD_IS_NOT_EXIST);
                // goto finally,notifyFailed()
                return;
            }

            // 1.prepared
            notifyPrepared(downloadFileInfo);

            // check status
            if (!DownloadFileUtil.canRename(downloadFileInfo)) {

                failReason = new OnRenameDownloadFileFailReason("the download file status error!", 
                        OnRenameDownloadFileFailReason.TYPE_FILE_STATUS_ERROR);
                // goto finally,notifyFailed()
                return;
            }

            String dirPath = downloadFileInfo.getFileDir();
            String oldFileName = downloadFileInfo.getFileName();

            String suffix = "";
            if (oldFileName != null && oldFileName.contains(".")) {
                int index = oldFileName.lastIndexOf(".");
                if (index != -1) {
                    suffix = oldFileName.substring(index, oldFileName.length());
                }
            }

            if (!includedSuffix) {
                mNewFileName = mNewFileName + suffix;
            }

            File file = new File(dirPath, oldFileName);
            File newFile = new File(dirPath, mNewFileName);

            if (!file.exists()) {
                failReason = new OnRenameDownloadFileFailReason("the original file not exist!", 
                        OnRenameDownloadFileFailReason.TYPE_ORIGINAL_FILE_NOT_EXIST);
                // goto finally,notifyFailed()
                return;
            }

            if (TextUtils.isEmpty(mNewFileName)) {
                failReason = new OnRenameDownloadFileFailReason("new file name is empty!", 
                        OnRenameDownloadFileFailReason.TYPE_NEW_FILE_NAME_IS_EMPTY);
                // goto finally,notifyFailed()
                return;
            }

            if (checkNewFileExist(newFile)) {
                failReason = new OnRenameDownloadFileFailReason("the new file has been exist!", 
                        OnRenameDownloadFileFailReason.TYPE_NEW_FILE_HAS_BEEN_EXIST);
                // goto finally,notifyFailed()
                return;
            }

            boolean renameResult = false;

            // write to db
            try {
                mDownloadFileRenamer.renameDownloadFile(downloadFileInfo.getUrl(), mNewFileName);
                renameResult = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!renameResult) {
                failReason = new OnRenameDownloadFileFailReason("rename file in db failed!", 
                        OnRenameDownloadFileFailReason.TYPE_UNKNOWN);
                // goto finally,notifyFailed()
                return;
            }

            // rename db record succeed

            // need rename save file
            if (DownloadFileUtil.isCompleted(downloadFileInfo)) {
                // success, rename save file
                renameResult = file.renameTo(newFile);

                if (!renameResult) {
                    // rollback in db
                    try {
                        mDownloadFileRenamer.renameDownloadFile(downloadFileInfo.getUrl(), oldFileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        // try again
                        try {
                            mDownloadFileRenamer.renameDownloadFile(downloadFileInfo.getUrl(), oldFileName);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                            // ignore   
                        }
                    }

                    failReason = new OnRenameDownloadFileFailReason("rename file in file system failed!", 
                            OnRenameDownloadFileFailReason.TYPE_UNKNOWN);
                    // goto finally,notifyFailed()
                    return;
                }

                // rename save file succeed
            }

            // rename succeed
        } catch (Exception e) {
            e.printStackTrace();
            failReason = new OnRenameDownloadFileFailReason(e);
        } finally {
            // rename succeed
            if (failReason == null) {
                // 2.rename success
                notifySuccess(downloadFileInfo);

                Log.d(TAG, TAG + ".run.run 重命名成功，url：" + mUrl);
            } else {
                // 2.rename failed
                notifyFailed(downloadFileInfo, failReason);

                Log.d(TAG, TAG + ".run 重命名失败，url：" + mUrl + ",failReason:" + failReason.getType());
            }

            Log.d(TAG, TAG + ".run 重命名任务【已结束】，是否有异常：" + (failReason == null) + "，url：" + mUrl);
        }
    }

    /**
     * check new file whether exist
     */
    private boolean checkNewFileExist(File newFile) {
        if (newFile != null && newFile.exists()) {// the file has been exist
            return true;
        }

        List<DownloadFileInfo> downloadFileInfos = mDownloadFileRenamer.getDownloadFiles();
        if (!CollectionUtil.isEmpty(downloadFileInfos)) {
            for (DownloadFileInfo info : downloadFileInfos) {
                if (info == null) {
                    continue;
                }
                String path = info.getFilePath();
                if (TextUtils.isEmpty(path)) {
                    continue;
                }
                if (path.equals(newFile.getAbsolutePath())) {// the file has been exist
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * notifyPrepared
     */
    private void notifyPrepared(DownloadFileInfo downloadFileInfo) {
        if (mOnRenameDownloadFileListener == null) {
            return;
        }
        OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFilePrepared(downloadFileInfo, 
                mOnRenameDownloadFileListener);
    }

    /**
     * notifySuccess
     */
    private void notifySuccess(DownloadFileInfo downloadFileInfo) {
        if (mOnRenameDownloadFileListener == null) {
            return;
        }
        OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFileSuccess(downloadFileInfo, 
                mOnRenameDownloadFileListener);
    }

    /**
     * notifyFailed
     */
    private void notifyFailed(DownloadFileInfo downloadFileInfo, RenameDownloadFileFailReason failReason) {
        if (mOnRenameDownloadFileListener == null) {
            return;
        }
        OnRenameDownloadFileListener.MainThreadHelper.onRenameDownloadFileFailed(downloadFileInfo, failReason, 
                mOnRenameDownloadFileListener);
    }

}
