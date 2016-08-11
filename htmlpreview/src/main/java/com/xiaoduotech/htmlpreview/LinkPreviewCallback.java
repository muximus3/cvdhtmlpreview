package com.xiaoduotech.htmlpreview;

public interface LinkPreviewCallback {

	void onPre();

	/**
	 *
	 * @param cvdHtmlSourceContent
	 *            Class with all contents from preview.
	 * @param isNull
	 *            Indicates if the content is null.
	 */
	void onPos(CVDHtmlSourceContent cvdHtmlSourceContent, boolean isNull);
}
