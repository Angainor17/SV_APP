package org.geometerplus.android.util

import android.app.Activity
import android.content.Intent
import android.os.Parcelable
import group.pals.android.lib.ui.filechooser.FileChooserActivity
import group.pals.android.lib.ui.filechooser.io.IFile
import group.pals.android.lib.ui.filechooser.io.localfile.LocalFile
import group.pals.android.lib.ui.filechooser.services.IFileProvider
import org.geometerplus.zlibrary.core.resources.ZLResource

object FileChooserUtil {
    @JvmStatic
    fun runFolderListDialog(
        activity: Activity,
        requestCode: Int,
        title: String,
        fileChooserTitle: String,
        initialValue: List<String>,
        chooseWritableDirsOnly: Boolean
    ) {
        val intent = Intent(activity, FolderListDialogActivity::class.java)
        intent.putExtra(FolderListDialogActivity.Key.ACTIVITY_TITLE, title)
        intent.putExtra(FolderListDialogActivity.Key.CHOOSER_TITLE, fileChooserTitle)
        intent.putExtra(FolderListDialogActivity.Key.FOLDER_LIST, ArrayList(initialValue))
        intent.putExtra(FolderListDialogActivity.Key.WRITABLE_FOLDERS_ONLY, chooseWritableDirsOnly)
        activity.startActivityForResult(intent, requestCode)
    }

    @JvmStatic
    fun runFileChooser(
        activity: Activity,
        requestCode: Int,
        title: String,
        initialDir: String,
        regexp: String
    ) {
        val intent = Intent(activity, FileChooserActivity::class.java)
        intent.putExtra(FileChooserActivity._TextResources, textResources(title))
        intent.putExtra(FileChooserActivity._Rootpath, LocalFile(initialDir) as Parcelable)
        intent.putExtra(FileChooserActivity._ActionBar, true)
        intent.putExtra(FileChooserActivity._SaveLastLocation, false)
        intent.putExtra(FileChooserActivity._DisplayHiddenFiles, false)
        intent.putExtra(FileChooserActivity._ShowNewFolderButton, false)
        intent.putExtra(FileChooserActivity._FilenameRegExp, regexp)
        intent.putExtra(
            FileChooserActivity._FilterMode,
            IFileProvider.FilterMode.FilesOnly
        )
        activity.startActivityForResult(intent, requestCode)
    }

    @JvmStatic
    fun runDirectoryChooser(
        activity: Activity,
        requestCode: Int,
        title: String,
        initialValue: String,
        chooseWritableDirsOnly: Boolean
    ) {
        val intent = Intent(activity, FileChooserActivity::class.java)
        intent.putExtra(FileChooserActivity._TextResources, textResources(title))
        intent.putExtra(FileChooserActivity._Rootpath, LocalFile(initialValue) as Parcelable)
        intent.putExtra(FileChooserActivity._ActionBar, true)
        intent.putExtra(FileChooserActivity._SaveLastLocation, false)
        intent.putExtra(FileChooserActivity._DisplayHiddenFiles, true)
        intent.putExtra(
            FileChooserActivity._FilterMode,
            if (chooseWritableDirsOnly)
                IFileProvider.FilterMode.DirectoriesOnly
            else
                IFileProvider.FilterMode.AnyDirectories
        )
        activity.startActivityForResult(intent, requestCode)
    }

    @JvmStatic
    fun folderPathFromData(data: Intent): String? = data.getStringExtra(FileChooserActivity._FolderPath)

    @JvmStatic
    @Suppress("DEPRECATION")
    fun filePathsFromData(data: Intent): List<String> {
        val files = data.getParcelableArrayListExtra<IFile>(FileChooserActivity._Results) ?: return emptyList()
        return files.map { it.getAbsolutePath() }
    }

    @JvmStatic
    fun pathListFromData(data: Intent): List<String>? = data.getStringArrayListExtra(FolderListDialogActivity.Key.FOLDER_LIST)

    private fun textResources(title: String): HashMap<String, String> {
        val map = HashMap<String, String>()

        map["title"] = title
        val dialogResource = ZLResource.resource("dialog")
        val buttonResource = dialogResource.getResource("button")
        map["ok"] = buttonResource.getResource("ok").value
        map["cancel"] = buttonResource.getResource("cancel").value
        val resource = dialogResource.getResource("fileChooser")
        map["root"] = resource.getResource("root").value
        map["newFolder"] = resource.getResource("newFolder").value
        map["folderNameHint"] = resource.getResource("folderNameHint").value
        val menuResource = resource.getResource("menu")
        map["menuOrigin"] = menuResource.getResource("origin").value
        map["menuReload"] = menuResource.getResource("reload").value
        val sortResource = resource.getResource("sortBy")
        map["sortBy"] = sortResource.value
        map["sortByName"] = sortResource.getResource("name").value
        map["sortBySize"] = sortResource.getResource("size").value
        map["sortByDate"] = sortResource.getResource("date").value
        map["permissionDenied"] = resource.getResource("permissionDenied").value

        return map
    }
}
