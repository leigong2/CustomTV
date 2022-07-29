package com.zune.customtv.bean;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.List;

public class BaseDataBean implements Serializable {

    public ODTO o;
    public String type;

    public static class ODTO implements Serializable {
        public double duration;
        public String firstPublished;
        public ImagesDTO images;
        public KeyPartsDTO keyParts;
        public String languageAgnosticNaturalKey;
        public String naturalKey;
        public String primaryCategory;
        public String title;
        public List<String> checksums;
        public String name;
        public String type;
        public String key;
        public List<Subcategories> subcategories;

        public String getThumb() {
            if (images == null) {
                return "";
            }
            if (images.lsr != null) {
                if (images.lsr.lg != null) {
                    return images.lsr.lg;
                }
                if (images.lsr.md != null) {
                    return images.lsr.md;
                }
                if (images.lsr.sm != null) {
                    return images.lsr.sm;
                }
                if (images.lsr.xl != null) {
                    return images.lsr.xl;
                }
                if (images.lsr.xs != null) {
                    return images.lsr.xs;
                }
            }
            if (images.sqr != null) {
                if (images.sqr.lg != null) {
                    return images.sqr.lg;
                }
                if (images.sqr.md != null) {
                    return images.sqr.md;
                }
                if (images.sqr.sm != null) {
                    return images.sqr.sm;
                }
                if (images.sqr.xl != null) {
                    return images.sqr.xl;
                }
                if (images.sqr.xs != null) {
                    return images.sqr.xs;
                }
            }
            return "";
        }

        public static class ImagesDTO implements Serializable {
            public LsrDTO lsr;
            public LsrDTO sqr;
            public LsrDTO pnr;
            public LsrDTO sqs;

            public static class LsrDTO implements Serializable {
                public String lg;
                public String md;
                public String sm;
                public String xl;
                public String xs;
            }
        }

        public static class KeyPartsDTO implements Serializable {
            public String formatCode;
            public String languageCode;
            public String pubSymbol;
            public String issueDate;
            public String docID;
            public int track;

            @Override
            public String toString() {
                return "KeyPartsDTO{" +
                        "formatCode='" + formatCode + '\'' +
                        ", languageCode='" + languageCode + '\'' +
                        ", pubSymbol='" + pubSymbol + '\'' +
                        ", track=" + track +
                        '}';
            }
        }

        @Override
        public String toString() {
            return "ODTO{" +
                    "duration=" + duration +
                    ", firstPublished='" + firstPublished + '\'' +
                    ", images=" + images +
                    ", keyParts=" + keyParts +
                    ", languageAgnosticNaturalKey='" + languageAgnosticNaturalKey + '\'' +
                    ", naturalKey='" + naturalKey + '\'' +
                    ", primaryCategory='" + primaryCategory + '\'' +
                    ", title='" + title + '\'' +
                    ", checksums=" + checksums +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "BaseDataBean{" +
                "o=" + o +
                ", type='" + type + '\'' +
                '}';
    }

    public static class Subcategories implements Parcelable {
        public ODTO.ImagesDTO images;
        public String key;
        public String name;
        public String type;
        public List<Subcategories> subcategories;
        public List<String> media;
        public List<String> mTitles;
        public List<String> mImages;
        public List<String> mDurations;
        public List<String> mFirstPublished;

        protected Subcategories(Parcel in) {
            key = in.readString();
            name = in.readString();
            type = in.readString();
            subcategories = in.createTypedArrayList(Subcategories.CREATOR);
            media = in.createStringArrayList();
        }

        public static final Creator<Subcategories> CREATOR = new Creator<Subcategories>() {
            @Override
            public Subcategories createFromParcel(Parcel in) {
                return new Subcategories(in);
            }

            @Override
            public Subcategories[] newArray(int size) {
                return new Subcategories[size];
            }
        };

        public String getThumb() {
            if (images == null) {
                return "";
            }
            if (images.pnr != null) {
                if (images.pnr.lg != null) {
                    return images.pnr.lg;
                }
                if (images.pnr.md != null) {
                    return images.pnr.md;
                }
                if (images.pnr.sm != null) {
                    return images.pnr.sm;
                }
                if (images.pnr.xl != null) {
                    return images.pnr.xl;
                }
                if (images.pnr.xs != null) {
                    return images.pnr.xs;
                }
            }
            if (images.sqs != null) {
                if (images.sqs.lg != null) {
                    return images.sqs.lg;
                }
                if (images.sqs.md != null) {
                    return images.sqs.md;
                }
                if (images.sqs.sm != null) {
                    return images.sqs.sm;
                }
                if (images.sqs.xl != null) {
                    return images.sqs.xl;
                }
                if (images.sqs.xs != null) {
                    return images.sqs.xs;
                }
            }
            return "";
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(key);
            dest.writeString(name);
            dest.writeString(type);
            dest.writeTypedList(subcategories);
            dest.writeStringList(media);
        }
    }
}
