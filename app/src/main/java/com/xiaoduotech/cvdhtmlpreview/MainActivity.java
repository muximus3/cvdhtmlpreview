package com.xiaoduotech.cvdhtmlpreview;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.xiaoduotech.htmlpreview.CVDHtmlSourceContent;
import com.xiaoduotech.htmlpreview.CVDTextCrawler;
import com.xiaoduotech.htmlpreview.LinkPreviewCallback;

public class MainActivity extends AppCompatActivity {

    private ImageView cvdIvUrlPre;
    private TextView cvdTvUrlTitlePre;
    private TextView cvdTvUrlTextPre;
    private EditText editText;
    private Button button;
    private ProgressDialog dialog;
    private LinearLayout llItem;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        parse("https://github.com/muximus3/cvdhtmlpreview/tree/master");
    }
    private void init() {
        dialog  = new ProgressDialog(this);
        editText = (EditText) findViewById(R.id.et);
        button = (Button) findViewById(R.id.btn);
        cvdIvUrlPre = (ImageView) findViewById(R.id.cvd_iv_url_pre);
        cvdTvUrlTitlePre = (TextView) findViewById(R.id.cvd_tv_url_title_pre);
        cvdTvUrlTextPre = (TextView) findViewById(R.id.cvd_tv_url_text_pre);
        llItem = (LinearLayout)findViewById(R.id.ll_item);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button.setEnabled(false);
                parse(editText.getText().toString().replace("\t",""));
            }
        });
    }
    private void parse(String url){
        if (!CVDTextCrawler.isUrl(url)){
            Toast.makeText(MainActivity.this, "Illegal url !", Toast.LENGTH_SHORT).show();
            button.setEnabled(true);
            return;
        }
        new CVDTextCrawler().makePreview(url, new LinkPreviewCallback() {
            @Override
            public void onPre() {
                dialog.show();
            }

            @Override
            public void onPos(final CVDHtmlSourceContent cvdHtmlSourceContent, boolean isNull) {
                dialog.dismiss();
                button.setEnabled(true);
                if (!isNull){
                    String title = cvdHtmlSourceContent.getTitle();
                    String discription = cvdHtmlSourceContent.getDescription();
                    String imageUrl = cvdHtmlSourceContent.getImage();
                    if (!TextUtils.isEmpty(imageUrl))
                        Picasso.with(MainActivity.this).load(imageUrl).resize(300,300).centerCrop().into(cvdIvUrlPre);
                    cvdTvUrlTitlePre.setText(title);
                    cvdTvUrlTextPre.setText(discription);
                    llItem.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String url = cvdHtmlSourceContent.getUrl();
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(url));
                            startActivity(i);
                        }
                    });
                }
            }
        });
    }
}
