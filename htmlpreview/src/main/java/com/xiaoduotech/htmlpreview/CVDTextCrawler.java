package com.xiaoduotech.htmlpreview;

import android.os.AsyncTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CVDTextCrawler {

    public static final int ALL = -1;
    public static final int NONE = -2;

    private final String HTTP_PROTOCOL = "http://";
    private final String HTTPS_PROTOCOL = "https://";

    private LinkPreviewCallback callback;

    public CVDTextCrawler() {
    }

    public void makePreview(String url, LinkPreviewCallback callback) {
        this.callback = callback;
        new GetCode(ALL).execute(getLegalUrl(url.toLowerCase()));
    }

    public void makePreview(LinkPreviewCallback callback, String url,
                            int imageQuantity) {
        this.callback = callback;
        new GetCode(imageQuantity).execute(getLegalUrl(url));
    }

    /**
     * Get html code
     */
    public class GetCode extends AsyncTask<String, Void, Void> {

        private CVDHtmlSourceContent cvdHtmlSourceContent = new CVDHtmlSourceContent();
        private int imageQuantity;

        public GetCode(int imageQuantity) {
            this.imageQuantity = imageQuantity;
        }

        @Override
        protected void onPreExecute() {
            if (callback != null) {
                callback.onPre();
            }
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void result) {
            if (callback != null) {
                callback.onPos(cvdHtmlSourceContent, isNull());
            }
            super.onPostExecute(result);
        }

        @Override
        protected Void doInBackground(String... params) {
            // Don't forget the http:// or https://
            try {
                Document doc = Jsoup
                        .connect(params[0])
                        .get();
                cvdHtmlSourceContent.setUrl(params[0]);
                cvdHtmlSourceContent.setHtmlCode(extendedTrim(doc.toString()));

                HashMap<String, String> metaTags = getMetaTags(cvdHtmlSourceContent
                        .getHtmlCode());

                cvdHtmlSourceContent.setMetaTags(metaTags);

                cvdHtmlSourceContent.setTitle(metaTags.get("title"));
                cvdHtmlSourceContent.setDescription(metaTags
                        .get("description"));

                if (cvdHtmlSourceContent.getTitle().equals("")) {
                    String matchTitle = Regex.pregMatch(
                            cvdHtmlSourceContent.getHtmlCode(),
                            Regex.TITLE_PATTERN, 2);

                    if (!matchTitle.equals(""))
                        cvdHtmlSourceContent.setTitle(htmlDecode(matchTitle));
                }

                if (cvdHtmlSourceContent.getDescription().equals(""))
                    cvdHtmlSourceContent
                            .setDescription(crawlCode(cvdHtmlSourceContent
                                    .getHtmlCode()));

                cvdHtmlSourceContent.setDescription(cvdHtmlSourceContent
                        .getDescription().replaceAll(
                                Regex.SCRIPT_PATTERN, ""));
                //image=========================>
                if (imageQuantity != NONE) {
                    if (!metaTags.get("image").equals(""))
                        cvdHtmlSourceContent.setImage(
                                metaTags.get("image"));
                    else {
                        cvdHtmlSourceContent.setImage(getImages(doc,
                                imageQuantity).get(0));
                    }
                }

                cvdHtmlSourceContent.setSuccess(true);
            } catch (Exception e) {
                cvdHtmlSourceContent.setSuccess(false);
            }


            return null;
        }

        /**
         * Verifies if the content could not be retrieved
         */
        public boolean isNull() {
            return !cvdHtmlSourceContent.isSuccess() &&
                    extendedTrim(cvdHtmlSourceContent.getHtmlCode()).equals("");
        }

    }

    /**
     * Gets content from a html tag
     */
    private String getTagContent(String tag, String content) {

        String pattern = "<" + tag + "(.*?)>(.*?)</" + tag + ">";
        String result = "", currentMatch = "";

        List<String> matches = Regex.pregMatchAll(content, pattern, 2);

        int matchesSize = matches.size();
        for (int i = 0; i < matchesSize; i++) {
            currentMatch = stripTags(matches.get(i));
            if (currentMatch.length() >= 120) {
                result = extendedTrim(currentMatch);
                break;
            }
        }

        if (result.equals("")) {
            String matchFinal = Regex.pregMatch(content, pattern, 2);
            result = extendedTrim(matchFinal);
        }

        result = result.replaceAll("&nbsp;", "");

        return htmlDecode(result);
    }

    /**
     * Gets images from the html code
     */
    public List<String> getImages(Document document, int imageQuantity) {
        List<String> matches = new ArrayList<String>();

        Elements media = document.select("[src]");

        for (Element srcElement : media) {
            if (srcElement.tagName().equals("img")) {
                matches.add(srcElement.attr("abs:src"));
            }
        }

        if (imageQuantity != ALL)
            matches = matches.subList(0, imageQuantity);

        return matches;
    }

    /**
     * Transforms from html to normal string
     */
    private String htmlDecode(String content) {
        return Jsoup.parse(content).text();
    }

    /**
     * Crawls the code looking for relevant information
     */
    private String crawlCode(String content) {
        String result = "";
        String resultSpan = "";
        String resultParagraph = "";
        String resultDiv = "";

        resultSpan = getTagContent("span", content);
        resultParagraph = getTagContent("p", content);
        resultDiv = getTagContent("div", content);
        if (resultParagraph.length() > resultSpan.length()
                && resultParagraph.length() >= resultDiv.length())
            result = resultParagraph;
        else if (resultParagraph.length() > resultSpan.length()
                && resultParagraph.length() < resultDiv.length())
            result = resultDiv;
        else
            result = resultParagraph;

        return htmlDecode(result);
    }

    /**
     * Strips the tags from an element
     */
    private String stripTags(String content) {
        return Jsoup.parse(content).text();
    }



    /**
     * Returns meta tags from html code
     */
    private HashMap<String, String> getMetaTags(String content) {

        HashMap<String, String> metaTags = new HashMap<String, String>();
        metaTags.put("url", "");
        metaTags.put("title", "");
        metaTags.put("description", "");
        metaTags.put("image", "");

        List<String> matches = Regex.pregMatchAll(content,
                Regex.METATAG_PATTERN, 1);

        for (String match : matches) {
            final String lowerCase = match.toLowerCase();
            if (lowerCase.contains("property=\"og:url\"")
                    || lowerCase.contains("property='og:url'")
                    || lowerCase.contains("name=\"url\"")
                    || lowerCase.contains("name='url'"))
                updateMetaTag(metaTags, "url", separeMetaTagsContent(match));
            else if (lowerCase.contains("property=\"og:title\"")
                    || lowerCase.contains("property='og:title'")
                    || lowerCase.contains("name=\"title\"")
                    || lowerCase.contains("name='title'"))
                updateMetaTag(metaTags, "title", separeMetaTagsContent(match));
            else if (lowerCase
                    .contains("property=\"og:description\"")
                    || lowerCase
                    .contains("property='og:description'")
                    || lowerCase.contains("name=\"description\"")
                    || lowerCase.contains("name='description'"))
                updateMetaTag(metaTags, "description", separeMetaTagsContent(match));
            else if (lowerCase.contains("property=\"og:image\"")
                    || lowerCase.contains("property='og:image'")
                    || lowerCase.contains("name=\"image\"")
                    || lowerCase.contains("name='image'"))
                updateMetaTag(metaTags, "image", separeMetaTagsContent(match));
        }

        return metaTags;
    }

    private void updateMetaTag(HashMap<String, String> metaTags, String url, String value) {
        if (value != null && (value.length() > 0)) {
            metaTags.put(url, value);
        }
    }

    /**
     * Gets content from metatag
     */
    private String separeMetaTagsContent(String content) {
        String result = Regex.pregMatch(content, Regex.METATAG_CONTENT_PATTERN,
                1);
        return htmlDecode(result);
    }


    /**
     * Removes extra spaces and trim the string
     */
    public static String extendedTrim(String content) {
        return content.replaceAll("\\s+", " ").replace("\n", " ")
                .replace("\r", " ").trim();
    }

    private String getLegalUrl(String string) {
        StringBuilder builder = new StringBuilder(string);
        if (!string.contains("http")) {
            builder.insert(0, HTTP_PROTOCOL);
        }
        return builder.toString();
    }

    public static boolean isUrl(String text) {
        if (null==text) {
            return false;
        } else {
            String regex = "(^(((ht|f)tp(s?))\\://)?(www.|[a-zA-Z].)[a-zA-Z0-9\\-\\.]+\\.(com|edu|gov|mil|net|org|biz|info|name|museum|us|ca|im|uk)(\\:[0-9]+)*(/($|[a-zA-Z0-9\\.\\,\\;\\?\\'\\\\\\+&amp;%\\$#\\=~_\\-]+))*$)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(text.toLowerCase());
            return matcher.matches();
        }
    }
    /**
     * Verifies if the url is an image
     */
    public static boolean isImage(String url) {
        return url.matches(Regex.IMAGE_PATTERN);
    }
}
