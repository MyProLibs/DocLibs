/*
 * 文件名称:          TXTKit.java
 *
 * 编译器:            android2.2
 * 时间:              下午5:16:41
 */
package com.wxiwei.office.fc.doc;

import android.net.Uri;
import android.os.Handler;

import com.wxiwei.office.common.ICustomDialog;
import com.wxiwei.office.constant.DialogConstant;
import com.wxiwei.office.fc.fs.storage.HeaderBlock;
import com.wxiwei.office.fc.fs.storage.LittleEndian;
import com.wxiwei.office.ss.util.StreamUtils;
import com.wxiwei.office.system.FileReaderThread;
import com.wxiwei.office.system.IControl;
import com.wxiwei.office.system.IDialogAction;
import com.wxiwei.office.thirdpart.mozilla.intl.chardet.CharsetDetector;
import com.wxiwei.office.wp.dialog.TXTEncodingDialog;

import java.io.InputStream;
import java.util.Vector;

/**
 * text kit
 * <p>
 * <p>
 * Read版本:        Read V1.0
 * <p>
 * 作者:            ljj8494
 * <p>
 * 日期:            2012-3-12
 * <p>
 * 负责人:          ljj8494
 * <p>
 * 负责小组:
 * <p>
 * <p>
 */
public class TXTKit {
    //
    private static TXTKit kit = new TXTKit();

    //
    public static TXTKit instance() {
        return kit;
    }

    /**
     * @param control
     * @param handler
     * @param filePath
     */
    public void readText(final IControl control, final Handler handler, final Uri uri) {
        try {
            InputStream in = StreamUtils.getInputStream(control.getActivity(), uri);
            final String filePath = uri.getPath();
//            FileInputStream in = new FileInputStream(filePath);
            byte[] b = new byte[16];
            in.read(b);
            long signature = LittleEndian.getLong(b, 0);
            if (signature == HeaderBlock._signature // doc, ppt, xls
                    || signature == 0x0006001404034b50L) // docx, pptx, xls
            {
                in.close();
                control.getSysKit().getErrorKit().writerLog(new Exception("Format error"), true);
                return;
            }
            signature = signature & 0x00FFFFFFFFFFFFFFL;
            if (signature == 0x002e312d46445025L) {
                in.close();
                control.getSysKit().getErrorKit().writerLog(new Exception("Format error"), true);
                return;
            }
            in.close();

            String code = control.isAutoTest() ? "GBK" : CharsetDetector.detect(filePath, uri, control.getActivity());
            if (code != null) {
                new FileReaderThread(control, handler, filePath, code, uri).start();
                return;
            }

            if (control.getMainFrame().isShowTXTEncodeDlg()) {
                Vector<Object> vector = new Vector<Object>();
                vector.add(filePath);
                IDialogAction da = new IDialogAction() {
                    /**
                     *
                     *
                     */
                    public IControl getControl() {
                        return control;
                    }

                    /**
                     *
                     *
                     */
                    public void doAction(int id, Vector<Object> model) {
                        if (TXTEncodingDialog.BACK_PRESSED.equals(model.get(0))) {
                            control.getMainFrame().getActivity().onBackPressed();
                        } else {
                            new FileReaderThread(control, handler, filePath, model.get(0).toString(), uri).start();
                        }
                    }

                    /**
                     *
                     *
                     */
                    public void dispose() {

                    }
                };

                new TXTEncodingDialog(control, control.getMainFrame().getActivity(), da,
                        vector, DialogConstant.ENCODING_DIALOG_ID).show();

            } else {
                String encode = control.getMainFrame().getTXTDefaultEncode();
                if (encode == null) {
                    ICustomDialog dlgListener = control.getCustomDialog();
                    if (dlgListener != null) {
                        dlgListener.showDialog(ICustomDialog.DIALOGTYPE_ENCODE);
                    } else {
                        new FileReaderThread(control, handler, filePath, "UTF-8", uri).start();
                    }
                } else {
                    new FileReaderThread(control, handler, filePath, encode, uri).start();
                }
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    /**
     *
     */
    public void reopenFile(final IControl control, final Handler handler, final String filePath, String encode, Uri uri) {
        new FileReaderThread(control, handler, filePath, encode, uri).start();
    }
}
