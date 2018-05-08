package cn.wjdiankong.main;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import cn.wjdiankong.chunk.AttributeData;
import cn.wjdiankong.chunk.EndTagChunk;
import cn.wjdiankong.chunk.StartTagChunk;
import cn.wjdiankong.chunk.StringChunk;
import cn.wjdiankong.chunk.TagChunk;

public class XmlEditor {
    public static int tagStartChunkOffset = 0, tagEndChunkOffset;
    public static int subAppTagChunkOffset = 0;
    public static int subTagChunkOffsets = 0;
    public static String[] isNotAppTag = new String[]{
            "uses-permission", "uses-sdk", "compatible-screens", "instrumentation", "library",
            "original-package", "package-verifier", "permission", "permission-group", "permission-tree",
            "protected-broadcast", "resource-overlay", "supports-input", "supports-screens", "upgrade-key-set",
            "uses-configuration", "uses-feature"};
    public static String prefixStr = "http://schemas.android.com/apk/res/android";

    public static void removeTag(String tagName, String name) {
        ParserChunkUtils.parserXml();
        for (TagChunk tag : ParserChunkUtils.xmlStruct.tagChunkList) {
            int tagNameIndex = Utils.byte2int(tag.startTagChunk.name);
            String tagNameTmp = ParserChunkUtils.xmlStruct.stringChunk.stringContentList.get(tagNameIndex);
            if (tagName.equals(tagNameTmp)) {
                for (AttributeData attrData : tag.startTagChunk.attrList) {
                    String attrName = ParserChunkUtils.xmlStruct.stringChunk.stringContentList.get(attrData.name);
                    if ("name".equals(attrName)) {
                        String value = ParserChunkUtils.xmlStruct.stringChunk.stringContentList.get(attrData.valueString);
                        if (name.equals(value)) {
                            //????????tag??????
                            int size = Utils.byte2int(tag.endTagChunk.size);
                            int delStart = tag.startTagChunk.offset;
                            int delSize = (tag.endTagChunk.offset - tag.startTagChunk.offset) + size;
                            ParserChunkUtils.xmlStruct.byteSrc = Utils.removeByte(ParserChunkUtils.xmlStruct.byteSrc, delStart, delSize);

                            modifyFileSize();
                            return;
                        }
                    }
                }
            }
        }
    }

    public static void addTag(String insertXml) {
        ParserChunkUtils.parserXml();
        try {
            XmlPullParserFactory pullParserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser pullParser = pullParserFactory.newPullParser();
            pullParser.setInput(new FileInputStream(insertXml), "UTF-8");
            int event = pullParser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {

                    case XmlPullParser.START_DOCUMENT:
                        break;

                    case XmlPullParser.START_TAG:
                        String tagName = pullParser.getName();
                        int name = getStrIndex(tagName);
                        int attCount = pullParser.getAttributeCount();
                        byte[] attribute = new byte[20 * attCount];
                        for (int i = 0; i < pullParser.getAttributeCount(); i++) {
                            int attruri = getStrIndex(prefixStr);
                            String attrName = pullParser.getAttributeName(i);
                            String[] strAry = attrName.split(":");
                            int[] type = getAttrType(pullParser.getAttributeValue(i));
                            int attrname = getStrIndex(strAry[1]);
                            int attrvalue = getStrIndex(pullParser.getAttributeValue(i));
                            int attrtype = type[0];
                            int attrdata = type[1];
                            AttributeData data = AttributeData.createAttribute(attruri, attrname, attrvalue, attrtype, attrdata);
                            attribute = Utils.byteConcat(attribute, data.getByte(), data.getLen() * i);
                        }

                        StartTagChunk startChunk = StartTagChunk.createChunk(name, attCount, -1, attribute);
                        if (isNotAppTag(tagName)) {
                            ParserChunkUtils.xmlStruct.byteSrc = Utils.insertByte(ParserChunkUtils.xmlStruct.byteSrc, subTagChunkOffsets, startChunk.getChunkByte());
                            subTagChunkOffsets += startChunk.getChunkByte().length;
                        } else {
                            ParserChunkUtils.xmlStruct.byteSrc = Utils.insertByte(ParserChunkUtils.xmlStruct.byteSrc, subAppTagChunkOffset, startChunk.getChunkByte());
                            subAppTagChunkOffset += startChunk.getChunkByte().length;
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        tagName = pullParser.getName();
                        name = getStrIndex(tagName);
                        EndTagChunk endChunk = EndTagChunk.createChunk(name);
                        if (isNotAppTag(tagName)) {
                            ParserChunkUtils.xmlStruct.byteSrc = Utils.insertByte(ParserChunkUtils.xmlStruct.byteSrc, subTagChunkOffsets, endChunk.getChunkByte());
                            subTagChunkOffsets += endChunk.getChunkByte().length;
                        } else {
                            ParserChunkUtils.xmlStruct.byteSrc = Utils.insertByte(ParserChunkUtils.xmlStruct.byteSrc, subAppTagChunkOffset, endChunk.getChunkByte());
                            subAppTagChunkOffset += endChunk.getChunkByte().length;
                        }
                        break;

                }
                event = pullParser.next();
            }
        } catch (XmlPullParserException e) {
            System.out.println("parse xml err:" + e.toString());
        } catch (IOException e) {
            System.out.println("parse xml err:" + e.toString());
        }

        modifStringChunk();

        modifyFileSize();

    }

    public static String getApplicationAttrValue(String attrName) {
        ParserChunkUtils.parserXml();
        for (StartTagChunk chunk : ParserChunkUtils.xmlStruct.startTagChunkList) {
            int tagNameIndex = Utils.byte2int(chunk.name);
            String tagNameTmp = ParserChunkUtils.xmlStruct.stringChunk.stringContentList.get(tagNameIndex);

            if ("application".equals(tagNameTmp)) {
                for (AttributeData data : chunk.attrList) {
                    String attrNameTemp1 = ParserChunkUtils.xmlStruct.stringChunk.stringContentList.get(data.name);
                    if (attrName.equals(attrNameTemp1)) {
                        return ParserChunkUtils.xmlStruct.stringChunk.stringContentList.get(data.valueString);
                    }
                }
            }
        }
        return null;
    }

    public static List<String[]> getApplicationAllAttrValue() {
        ParserChunkUtils.parserXml();
        for (StartTagChunk chunk : ParserChunkUtils.xmlStruct.startTagChunkList) {
            int tagNameIndex = Utils.byte2int(chunk.name);
            String tagNameTmp = ParserChunkUtils.xmlStruct.stringChunk.stringContentList.get(tagNameIndex);

            if ("application".equals(tagNameTmp)) {
                List<String[]> list = new ArrayList<>();
                for (AttributeData data : chunk.attrList) {
                    if (data.valueString >= 0 && ParserChunkUtils.xmlStruct.stringChunk.stringContentList.size() > data.valueString) {
                        String[] arrs = new String[2];
                        arrs[0] = ParserChunkUtils.xmlStruct.stringChunk.stringContentList.get(data.name);
                        arrs[1] = ParserChunkUtils.xmlStruct.stringChunk.stringContentList.get(data.valueString);
                        list.add(arrs);
                    }
                }
                return list;
            }
        }
        return null;
    }

    /**
     * ???????
     *
     * @param tag
     * @param tagName
     * @param attrName
     */
    public static void removeAttr(String tag, String tagName, String attrName) {
        ParserChunkUtils.parserXml();
        for (StartTagChunk chunk : ParserChunkUtils.xmlStruct.startTagChunkList) {
            int tagNameIndex = Utils.byte2int(chunk.name);
            String tagNameTmp = ParserChunkUtils.xmlStruct.stringChunk.stringContentList.get(tagNameIndex);

            if (tag.equals(tagNameTmp)) {

                //?????application??manifest???????????
                if (tag.equals("application") || tag.equals("manifest")) {
                    for (AttributeData data : chunk.attrList) {
                        String attrNameTemp1 = ParserChunkUtils.xmlStruct.stringChunk.stringContentList.get(data.name);
                        if (attrName.equals(attrNameTemp1)) {
                            //????????????????????????????????????????????????????????????????
                            if (chunk.attrList.size() == 1) {
                                removeTag(tag, tagName);
                                return;
                            }
                            //???????????tag chunk??????????????��
                            int countStart = chunk.offset + 28;
                            byte[] modifyByte = Utils.int2Byte(chunk.attrList.size() - 1);
                            ParserChunkUtils.xmlStruct.byteSrc = Utils.replaceBytes(ParserChunkUtils.xmlStruct.byteSrc, modifyByte, countStart);

                            //???chunk???��
                            int chunkSizeStart = chunk.offset + 4;
                            int chunkSize = Utils.byte2int(chunk.size);
                            byte[] modifyByteSize = Utils.int2Byte(chunkSize - 20);//??????????20?????
                            ParserChunkUtils.xmlStruct.byteSrc = Utils.replaceBytes(ParserChunkUtils.xmlStruct.byteSrc, modifyByteSize, chunkSizeStart);

                            //???????????
                            int delStart = data.offset;
                            int delSize = data.getLen();
                            ParserChunkUtils.xmlStruct.byteSrc = Utils.removeByte(ParserChunkUtils.xmlStruct.byteSrc, delStart, delSize);

                            modifyFileSize();
                            return;
                        }
                    }
                }

                //??????????name????????tag
                for (AttributeData attrData : chunk.attrList) {
                    String attrNameTemp = ParserChunkUtils.xmlStruct.stringChunk.stringContentList.get(attrData.name);
                    if ("name".equals(attrNameTemp)) {//???????tag?????��?????
                        String value = ParserChunkUtils.xmlStruct.stringChunk.stringContentList.get(attrData.valueString);
                        if (tagName.equals(value)) {
                            for (AttributeData data : chunk.attrList) {
                                String attrNameTemp1 = ParserChunkUtils.xmlStruct.stringChunk.stringContentList.get(data.name);
                                if (attrName.equals(attrNameTemp1)) {

                                    //????????????????????????????????????????????????????????????????
                                    if (chunk.attrList.size() == 1) {
                                        removeTag(tag, tagName);
                                        return;
                                    }

                                    //???????????tag chunk??????????????��
                                    int countStart = chunk.offset + 28;
                                    byte[] modifyByte = Utils.int2Byte(chunk.attrList.size() - 1);
                                    ParserChunkUtils.xmlStruct.byteSrc = Utils.replaceBytes(ParserChunkUtils.xmlStruct.byteSrc, modifyByte, countStart);

                                    //???chunk???��
                                    int chunkSizeStart = chunk.offset + 4;
                                    int chunkSize = Utils.byte2int(chunk.size);
                                    byte[] modifyByteSize = Utils.int2Byte(chunkSize - 20);
                                    ParserChunkUtils.xmlStruct.byteSrc = Utils.replaceBytes(ParserChunkUtils.xmlStruct.byteSrc, modifyByteSize, chunkSizeStart);

                                    //???????????
                                    int delStart = data.offset;
                                    int delSize = data.getLen();
                                    ParserChunkUtils.xmlStruct.byteSrc = Utils.removeByte(ParserChunkUtils.xmlStruct.byteSrc, delStart, delSize);

                                    modifyFileSize();
                                    return;

                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * ?????????
     *
     * @param tag
     * @param tagName
     * @param attrName
     * @param attrValue
     */
    public static void modifyAttr(String tag, String tagName, String attrName, String attrValue) {
        ParserChunkUtils.parserXml();
        XmlEditor.removeAttr(tag, tagName, attrName);
        ParserChunkUtils.parserXml();
        XmlEditor.addAttr(tag, tagName, attrName, attrValue);
    }

    /**
     * ????????
     *
     * @param tag
     * @param tagName
     * @param attrName
     * @param attrValue
     */
    public static void addAttr(String tag, String tagName, String attrName, String attrValue) {
        ParserChunkUtils.parserXml();
        //??????????????
        int[] type = getAttrType(attrValue);
        int attrname = getStrIndex(attrName);
        int attrvalue = getStrIndex(attrValue);
        int attruri = getStrIndex(prefixStr);
        int attrtype = type[0];//????????
        int attrdata = type[1];//?????????int????

        AttributeData data = AttributeData.createAttribute(attruri, attrname, attrvalue, attrtype, attrdata);

        for (StartTagChunk chunk : ParserChunkUtils.xmlStruct.startTagChunkList) {

            int tagNameIndex = Utils.byte2int(chunk.name);
            String tagNameTmp = ParserChunkUtils.xmlStruct.stringChunk.stringContentList.get(tagNameIndex);
            if (tag.equals(tagNameTmp)) {

                //?????application??manifest???????????
                if (tag.equals("application") || tag.equals("manifest")) {
                    //???????????tag chunk??????????????��
                    int countStart = chunk.offset + 28;
                    byte[] modifyByte = Utils.int2Byte(chunk.attrList.size() + 1);
                    ParserChunkUtils.xmlStruct.byteSrc = Utils.replaceBytes(ParserChunkUtils.xmlStruct.byteSrc, modifyByte, countStart);

                    //???chunk???��
                    int chunkSizeStart = chunk.offset + 4;
                    int chunkSize = Utils.byte2int(chunk.size);
                    byte[] modifyByteSize = Utils.int2Byte(chunkSize + 20);
                    ParserChunkUtils.xmlStruct.byteSrc = Utils.replaceBytes(ParserChunkUtils.xmlStruct.byteSrc, modifyByteSize, chunkSizeStart);

                    //?????????????????chunk??
                    ParserChunkUtils.xmlStruct.byteSrc = Utils.insertByte(ParserChunkUtils.xmlStruct.byteSrc, chunk.offset + chunkSize, data.getByte());

                    modifStringChunk();

                    modifyFileSize();

                    return;
                }

                for (AttributeData attrData : chunk.attrList) {
                    String attrNameTemp = ParserChunkUtils.xmlStruct.stringChunk.stringContentList.get(attrData.name);
                    if ("name".equals(attrNameTemp)) {//???????tag?????��?????

                        //???????????tag chunk??????????????��
                        int countStart = chunk.offset + 28;
                        byte[] modifyByte = Utils.int2Byte(chunk.attrList.size() + 1);
                        ParserChunkUtils.xmlStruct.byteSrc = Utils.replaceBytes(ParserChunkUtils.xmlStruct.byteSrc, modifyByte, countStart);

                        //???chunk???��
                        int chunkSizeStart = chunk.offset + 4;
                        int chunkSize = Utils.byte2int(chunk.size);
                        byte[] modifyByteSize = Utils.int2Byte(chunkSize + 20);
                        ParserChunkUtils.xmlStruct.byteSrc = Utils.replaceBytes(ParserChunkUtils.xmlStruct.byteSrc, modifyByteSize, chunkSizeStart);

                        //?????????????????chunk??
                        ParserChunkUtils.xmlStruct.byteSrc = Utils.insertByte(ParserChunkUtils.xmlStruct.byteSrc, chunk.offset + chunkSize, data.getByte());

                        modifStringChunk();

                        modifyFileSize();

                        return;
                    }
                }
            }
        }

    }

    /**
     * ???????String Chunk?????
     */
    private static void modifStringChunk() {
        //��??StartTagChunk chunk???????????????????????????????????????
        StringChunk strChunk = ParserChunkUtils.xmlStruct.stringChunk;
        byte[] newStrChunkB = strChunk.getByte(ParserChunkUtils.xmlStruct.stringChunk.stringContentList);
        //?????String Chunk
        ParserChunkUtils.xmlStruct.byteSrc = Utils.removeByte(ParserChunkUtils.xmlStruct.byteSrc, ParserChunkUtils.stringChunkOffset, Utils.byte2int(strChunk.size));
        //???????String Chunk
        ParserChunkUtils.xmlStruct.byteSrc = Utils.insertByte(ParserChunkUtils.xmlStruct.byteSrc, ParserChunkUtils.stringChunkOffset, newStrChunkB);
    }

    /**
     * ????????????��
     */
    public static void modifyFileSize() {
        byte[] newFileSize = Utils.int2Byte(ParserChunkUtils.xmlStruct.byteSrc.length);
        ParserChunkUtils.xmlStruct.byteSrc = Utils.replaceBytes(ParserChunkUtils.xmlStruct.byteSrc, newFileSize, 4);
    }

    /**
     * ??????????????????????????????????????????????��?????????????
     *
     * @param str
     * @return
     */
    public static int getStrIndex(String str) {
        if (str == null || str.length() == 0) {
            return -1;
        }
        for (int i = 0; i < ParserChunkUtils.xmlStruct.stringChunk.stringContentList.size(); i++) {
            if (ParserChunkUtils.xmlStruct.stringChunk.stringContentList.get(i).equals(str)) {
                return i;
            }
        }
        ParserChunkUtils.xmlStruct.stringChunk.stringContentList.add(str);
        return ParserChunkUtils.xmlStruct.stringChunk.stringContentList.size() - 1;
    }

    /**
     * ?��??????application????????application?????????????????????
     *
     * @param tagName
     * @return
     */
    public static boolean isNotAppTag(String tagName) {
        for (String str : isNotAppTag) {
            if (str.equals(tagName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ??????????????
     *
     * @param tagValue
     * @return
     */
    public static int[] getAttrType(String tagValue) {

        int[] result = new int[2];

        if (tagValue.equals("true") || tagValue.equals("false")) {//boolean
            result[0] = result[0] | AttributeType.ATTR_BOOLEAN;
            if (tagValue.equals("true")) {
                result[1] = 1;
            } else {
                result[1] = 0;
            }
        } else if (tagValue.equals("singleTask") || tagValue.equals("standard")
                || tagValue.equals("singleTop") || tagValue.equals("singleInstance")) {//??????int????
            result[0] = result[0] | AttributeType.ATTR_FIRSTINT;
            if (tagValue.equals("standard")) {
                result[1] = 0;
            } else if (tagValue.equals("singleTop")) {
                result[1] = 1;
            } else if (tagValue.equals("singleTask")) {
                result[1] = 2;
            } else {
                result[1] = 3;
            }
        } else if (tagValue.equals("minSdkVersion") || tagValue.equals("versionCode")) {
            result[0] = result[0] | AttributeType.ATTR_FIRSTINT;
            result[1] = Integer.valueOf(tagValue);
        } else if (tagValue.startsWith("@")) {//????
            result[0] = result[0] | AttributeType.ATTR_REFERENCE;
            result[1] = 0x7F000000;
        } else if (tagValue.startsWith("#")) {//??
            result[0] = result[0] | AttributeType.ATTR_ARGB4;
            result[1] = 0xFFFFFFFF;
        } else {//?????
            result[0] = result[0] | AttributeType.ATTR_STRING;
            result[1] = getStrIndex(tagValue);
        }

        result[0] = result[0] | 0x08000000;
        result[0] = Utils.byte2int(Utils.reverseBytes(Utils.int2Byte(result[0])));//????????????

        return result;
    }

}
