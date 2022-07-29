package com.zune.customtv.bean;

import java.io.Serializable;
import java.util.List;

public class Mp4Bean {

    public String pubName;
    public String parentPubName;
    public Object booknum;
    public String pub;
    public String issue;
    public String formattedDate;
    public int track;
    public String specialty;
    public PubImageDTO pubImage;
    public LanguagesDTO languages;
    public FilesDTO files;
    public List<String> fileformat;

    public static class PubImageDTO implements Serializable {
        public String url;
        public String modifiedDatetime;
        public Object checksum;
    }

    public static class LanguagesDTO implements Serializable {
        public CHSDTO CHS;

        public static class CHSDTO implements Serializable {
            public String name;
            public String direction;
            public String locale;
        }
    }

    public static class FilesDTO implements Serializable {
        public CHSDTOX CHS;

        public static class CHSDTOX implements Serializable {
            public List<MP4DTO> MP4;

            public static class MP4DTO implements Serializable {
                public String title;
                public FileDTO file;
                public int filesize;
                public TrackImageDTO trackImage;
                public Object markers;
                public String label;
                public int track;
                public boolean hasTrack;
                public String pub;
                public int docid;
                public int booknum;
                public String mimetype;
                public String edition;
                public String editionDescr;
                public String format;
                public String formatDescr;
                public String specialty;
                public String specialtyDescr;
                public boolean subtitled;
                public int frameWidth;
                public int frameHeight;
                public double frameRate;
                public double duration;
                public double bitRate;
                public FileDTO subtitles;

                public static class FileDTO implements Serializable {
                    public String url;
                    public String stream;
                    public String modifiedDatetime;
                    public String checksum;
                }

                public static class TrackImageDTO implements Serializable {
                    public String url;
                    public String modifiedDatetime;
                    public Object checksum;
                }
            }
        }
    }
}
