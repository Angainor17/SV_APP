package org.geometerplus.fbreader.fbreader

import org.geometerplus.zlibrary.core.application.ZLApplication

abstract class FBAction(@JvmField protected val Reader: FBReaderApp) : ZLApplication.ZLAction()
