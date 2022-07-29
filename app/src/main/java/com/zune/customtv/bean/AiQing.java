package com.zune.customtv.bean;

import java.io.Serializable;
import java.util.List;

/**
 * @author wangzhilong
 * @date 2022/7/27 027
 */
public class AiQing implements Serializable {

    public DataDTO data;
    public int ret;
    public String msg;

    public static class DataDTO implements Serializable {
        public int errorCode;
        public String errorMsg;
        public int trustState;
        public NormalListDTO normalList;
        public String qcQuery;
        public Object SearchReport;
        public String sessionId;
        public int timeCost;
        public String uuid;
        public Object ABTest;
        public Object reportData;
        public List<?> areaBoxList;

        public static class NormalListDTO implements Serializable {
            public int errorCode;
            public String errorMsg;
            public String boxId;
            public String boxUID;
            public int totalNum;
            public Object filters;
            public BoxTitleDTO boxTitle;
            public String pageContext;
            public String searchSession;
            public List<ItemListDTO> itemList;

            public static class BoxTitleDTO implements Serializable {
                public int type;
                public String title;
                public Object title_data;
                public String title_desc;
                public List<?> box_titles;
                public List<?> box_indexs;
            }

            public static class ItemListDTO implements Serializable {
                public DocDTO doc;
                public VideoInfoDTO videoInfo;
                public Object live;
                public Object staticRelateSearchWord;
                public Object starCard;
                public Object cpResult;
                public Object doc_report_info;

                public static class DocDTO implements Serializable {
                    public int dataType;
                    public String md;
                    public String id;
                    public Object ShowReport;
                    public Object ClickReport;
                    public List<?> areaBoxIndex;
                }

                public static class VideoInfoDTO implements Serializable {
                    public Object videoDoc;
                    public Object coverDoc;
                    public Object columnDoc;
                    public Object subjectDoc;
                    public int viewType;
                    public int videoType;
                    public String title;
                    public String typeName;
                    public String year;
                    public String checkupTime;
                    public String imgTag;
                    public String area;
                    public Object secondBlockSite;
                    public String descrip;
                    public String views;
                    public String imgUrl;
                    public String url;
                    public Object recommendInfo;
                    public String subTitle;
                    public int payStatus;
                    public List<?> language;
                    public List<?> directors;
                    public List<?> actors;
                    public List<FirstBlockSitesDTO> firstBlockSites;

                    public static class FirstBlockSitesDTO implements Serializable {
                        public int uiType;
                        public String showName;
                        public String enName;
                        public String iconUrl;
                        public String floatingLayerIconUrl;
                        public int payType;
                        public int playsoureType;
                        public int isDanger;
                        public int totalEpisode;
                        public List<?> tabs;
                        public List<EpisodeInfoListDTO> episodeInfoList;

                        public static class EpisodeInfoListDTO implements Serializable {
                            public String id;
                            public int dataType;
                            public String url;
                            public String title;
                            public String markLabel;
                            public String asnycParams;
                            public int displayType;
                            public String checkUpTime;
                            public String imgUrl;
                            public String duration;
                            public String rawTags;
                        }
                    }
                }
            }
        }
    }
}
