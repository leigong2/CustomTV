package com.zune.customtv.utils;

import com.base.base.BaseConstant;

import java.util.regex.Pattern;

public class MediaTag2MediaUrl {
    /**
     * zune：
     * "pub-ivfe_CHS_1_VIDEO",
     * "pub-jwbai_CHS_201605_3_VIDEO"
     * "docid-502016241_CHS_1_VIDEO"
     * "pub-jwb_CHS_201905_3_VIDEO"
     * <p>
     * pub=%1$s&track=%2$s&issue=%3$s&docid=%4$s&fileformat=%5$s
     **/
    public static String tag2Url(String mediaTag) {
        String pub = null;
        String docid = null;
        String track = null;
        String issueDate = null;
        String fileformat = null;
        String first = mediaTag.split("_")[0];
        if (first.contains("pub-")) {
            pub = first.replaceAll("pub-", "");
        }
        if (first.contains("docid-")) {
            docid = first.replaceAll("docid-", "");
        }
        String substring = mediaTag.substring(mediaTag.indexOf("_"));
        String[] details = substring.split("_");
        for (String detail : details) {
            if (detail.length() == 6 && isNumber(detail)) {
                issueDate = detail;
            } else if (isNumber(detail)) {
                track = detail;
            }
            if ("VIDEO".equals(detail)) {
                fileformat = "mp4%2Cm4v";
            }
            if ("AUDIO".equals(detail)) {
                fileformat = "mp3";
            }
        }
        StringBuilder sb = new StringBuilder(BaseConstant.URL_GET_MEDIA);
        if (pub != null) {
            sb.append("&pub=").append(pub);
        }
        if (track != null) {
            sb.append("&track=").append(track);
        }
        if (issueDate != null) {
            sb.append("&issue=").append(issueDate);
        }
        if (docid != null) {
            sb.append("&docid=").append(docid);
        }
        if (fileformat != null) {
            sb.append("&fileformat=").append(fileformat);
        }
        return sb.toString();
    }

    static Pattern pattern = Pattern.compile("[0-9]+");

    private static boolean isNumber(String detail) {
        return pattern.matcher(detail).matches();
    }
}
