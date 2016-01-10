package org.wlf.filedownloader.util;


import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.base.Status;

/**
 * Util for {@link Status}
 *
 * @author wlf(Andy)
 * @email 411086563@qq.com
 */
public class DownloadFileUtil {

    /**
     * whether the download file can delete
     *
     * @param downloadFileInfo
     * @return
     */
    public static boolean canDelete(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null) {
            return false;
        }

        switch (downloadFileInfo.getStatus()) {
            // only the status below can NOT be deleted
            case Status.DOWNLOAD_STATUS_WAITING:
            case Status.DOWNLOAD_STATUS_RETRYING:
            case Status.DOWNLOAD_STATUS_PREPARING:
            case Status.DOWNLOAD_STATUS_PREPARED:
            case Status.DOWNLOAD_STATUS_DOWNLOADING:
                return false;
        }

        return true;
    }

    /**
     * whether the download file can move
     *
     * @param downloadFileInfo
     * @return
     */
    public static boolean canMove(DownloadFileInfo downloadFileInfo) {

        if (downloadFileInfo == null) {
            return false;
        }

        switch (downloadFileInfo.getStatus()) {
            // only the status below can NOT be moved
            case Status.DOWNLOAD_STATUS_WAITING:
            case Status.DOWNLOAD_STATUS_RETRYING:
            case Status.DOWNLOAD_STATUS_PREPARING:
            case Status.DOWNLOAD_STATUS_PREPARED:
            case Status.DOWNLOAD_STATUS_DOWNLOADING:
                return false;
        }

        return true;
    }

    /**
     * whether the download file download completed
     *
     * @param downloadFileInfo
     * @return
     */
    public static boolean isCompleted(DownloadFileInfo downloadFileInfo) {
        
        if (downloadFileInfo == null) {
            return false;
        }

        switch (downloadFileInfo.getStatus()) {
            case Status.DOWNLOAD_STATUS_COMPLETED:
                return true;
        }

        return false;
    }

    /**
     * whether the download file can rename
     * @param downloadFileInfo
     * @return
     */
    public static boolean canRename(DownloadFileInfo downloadFileInfo){
        
        if (downloadFileInfo == null) {
            return false;
        }

        switch (downloadFileInfo.getStatus()) {
            // only the status below can NOT be renamed
            case Status.DOWNLOAD_STATUS_WAITING:
            case Status.DOWNLOAD_STATUS_RETRYING:
            case Status.DOWNLOAD_STATUS_PREPARING:
            case Status.DOWNLOAD_STATUS_PREPARED:
            case Status.DOWNLOAD_STATUS_DOWNLOADING:
                return false;
        }

        return true;
    }
}
