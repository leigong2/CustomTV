package com.zune.customtv.bean;

import java.io.Serializable;
import java.util.List;

/**
 * @author wangzhilong
 * @date 2022/7/27 027
 */
public class AiQing implements Serializable {

    public DataDTO data;
    public String msg;
    public Integer ret;

    public static class DataDTO implements Serializable {
        public Integer errorCode;
        public String errorMsg;
        public NormalListDTO normalList;
        public String qcQuery;
        public String sessionId;
        public Integer timeCost;
        public Integer trustState;
        public String uuid;
        public List<?> areaBoxList;

        public static class NormalListDTO implements Serializable {
            public String boxId;
            public BoxTitleDTO boxTitle;
            public String boxUID;
            public Integer errorCode;
            public String errorMsg;
            public String pageContext;
            public String searchSession;
            public Integer totalNum;
            public List<ItemListDTO> itemList;

            public static class BoxTitleDTO implements Serializable {
                public String title;
                public String titleDesc;
                public Integer type;
                public List<?> boxIndexs;
                public List<?> boxTitles;
            }

            public static class ItemListDTO implements Serializable {
                public DocDTO doc;
                public VideoInfoDTO videoInfo;

                public static class DocDTO implements Serializable {
                    public Integer dataType;
                    public String id;
                    public String md;
                    public List<?> areaBoxIndex;
                }

                public static class VideoInfoDTO implements Serializable {
                    public String area;
                    public String checkupTime;
                    public String descrip;
                    public String imgTag;
                    public String imgUrl;
                    public Integer payStatus;
                    public String subTitle;
                    public String title;
                    public String typeName;
                    public String url;
                    public Integer videoType;
                    public Integer viewType;
                    public String views;
                    public String year;
                    public List<?> actors;
                    public List<?> directors;
                    public List<FirstBlockSitesDTO> firstBlockSites;
                    public List<?> language;

                    public static class FirstBlockSitesDTO implements Serializable {
                        public String enName;
                        public String floatingLayerIconUrl;
                        public String iconUrl;
                        public Integer isDanger;
                        public Integer payType;
                        public Integer playsoureType;
                        public String showName;
                        public Integer totalEpisode;
                        public Integer uiType;
                        public List<EpisodeInfoListDTO> episodeInfoList;
                        public List<?> tabs;

                        public static class EpisodeInfoListDTO implements Serializable {
                            public String asnycParams;
                            public String checkUpTime;
                            public Integer dataType;
                            public Integer displayType;
                            public String duration;
                            public String id;
                            public String imgUrl;
                            public String markLabel;
                            public String rawTags;
                            public String title;
                            public String url;
                        }
                    }
                }
            }
        }
    }
}
